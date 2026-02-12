import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

import { ComplianceService } from '../../shared/services/compliance.service';
import { ComplianceEventsService } from '../../services/compliance-events.service';
import { TransactionService } from '../../shared/services/transaction.service';

import {
  SubjectNameLookup,
  addSubjectLookupKeys,
  pickFirstString,
  resolveSubjectName,
} from '../../shared/utils/subject-name.util';

type SarRow = {
  sarId: number; // NOT optional
  subjectName: string; // NOT optional
  sourceEntityId: string | null; // NOT optional (but can be null)
  status: string | null; // NOT optional (but can be null)
  suspicionScore: number | null; // NOT optional (but can be null)
  eventTime: string | null;
  createdAt: string | null;
  updatedAt: string | null;
};

@Component({
  selector: 'app-sars',
  standalone: true,
  templateUrl: './sars.component.html',
  styleUrls: ['./sars.component.css'],
  imports: [CommonModule, FormsModule, RouterModule],
})
export class SarsComponent {
  sars: SarRow[] = [];
  loading = false;
  error: string | null = null;

  search = '';
  query = '';

  ctrOptions: Array<{ id: number; label: string }> = [];
  selectedCtrId: number | null = null;

  generating = false;
  generateError: string | null = null;

  private transactionNameMap: SubjectNameLookup = {};

  constructor(
    private complianceService: ComplianceService,
    private cdr: ChangeDetectorRef,
    private complianceEventsService: ComplianceEventsService,
    private transactionService: TransactionService,
    private router: Router,
  ) {
    this.refresh();
  }

  // =========================
  // Load SARs
  // =========================
  refresh(): void {
    this.loading = true;
    this.error = null;

    this.transactionService.getTransactions(0, 300).subscribe({
      next: (res: any) => {
        const rows: any[] = Array.isArray(res?.content)
          ? res.content
          : Array.isArray(res)
            ? res
            : [];

        const lookup: SubjectNameLookup = {};
        for (const t of rows) {
          const idRaw =
            pickFirstString(
              t?.sourceEntityId,
              t?.customerId,
              t?.subjectId,
              t?.source_entity_id,
              t?.customer_id,
              t?.subject_id,
            ) ?? null;

          const name =
            pickFirstString(
              t?.customerName,
              t?.subjectName,
              t?.fullName,
              t?.customer_name,
              t?.subject_name,
              t?.full_name,
            ) ?? null;

          if (idRaw && name) addSubjectLookupKeys(lookup, String(idRaw), String(name));
        }

        this.transactionNameMap = lookup;
        this.loadSarsWithLookup();
      },
      error: (err: unknown) => {
        console.error(err);
        this.transactionNameMap = {};
        this.loadSarsWithLookup();
      },
    });
  }

  private loadSarsWithLookup(): void {
    this.complianceEventsService.getSarEvents(0, 200).subscribe({
      next: (page: any) => {
        const rows: any[] = Array.isArray(page?.content) ? page.content : [];

        const mappedRows: SarRow[] = rows
          .map((e: any): SarRow | null => {
            // SAR ID must be a number. If it isn't present, skip the row.
            const rawId = e?.eventId ?? e?.id ?? e?.sarId;
            const sarId = typeof rawId === 'number' ? rawId : Number(rawId);
            if (!Number.isFinite(sarId)) return null;

            // Resolve subject name using transaction lookup + event fields
            const subjectIdRaw =
              pickFirstString(
                e?.subjectId,
                e?.customerId,
                e?.sourceSubjectId,
                e?.sourceEntityId,
                e?.source_subject_id,
                e?.source_entity_id,
              ) ?? null;

            const resolved = resolveSubjectName({
              lookup: this.transactionNameMap,
              subjectIdRaw: subjectIdRaw ? String(subjectIdRaw) : null,
              eventNames: [
                e?.customerName ?? null,
                e?.subjectName ?? null,
                e?.subject_name ?? null,
              ],
            });

            // Normalize score to (number | null) always
            const rawScore =
              e?.severityScore ?? e?.severity_score ?? e?.suspicionScore ?? e?.suspicion_score;

            const suspicionScore: number | null =
              typeof rawScore === 'number'
                ? rawScore
                : rawScore != null && !Number.isNaN(Number(rawScore))
                  ? Number(rawScore)
                  : null;

            const eventTime = (e?.eventTime ?? e?.event_time ?? null) as string | null;
            const createdAt = (e?.createdAt ?? e?.created_at ?? null) as string | null;
            const updatedAt = (e?.updatedAt ??
              e?.updated_at ??
              e?.eventTime ??
              e?.event_time ??
              e?.createdAt ??
              e?.created_at ??
              null) as string | null;

            return {
              sarId,
              subjectName: resolved.subjectName ?? `SAR ${sarId}`,
              sourceEntityId: (e?.sourceEntityId ?? e?.source_entity_id ?? null) as string | null,
              status: (e?.status ?? null) as string | null,
              suspicionScore,
              eventTime,
              createdAt,
              updatedAt,
            };
          })
          .filter((r: SarRow | null): r is SarRow => r !== null)
          .sort((a, b) =>
            String(b.eventTime || b.createdAt || '').localeCompare(
              String(a.eventTime || a.createdAt || ''),
            ),
          );

        this.sars = mappedRows;
        this.loading = false;

        this.loadCtrOptions();
        this.cdr.detectChanges();
      },
      error: (err: unknown) => {
        console.error(err);
        this.error =
          'Could not load SARs. If the compliance-event service is offline, you can still upload SAR PDFs to create records.';
        this.sars = [];
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  // =========================
  // CTR dropdown for SAR generation
  // =========================
  loadCtrOptions(): void {
    this.complianceService.getCtrOptions().subscribe({
      next: (opts: Array<{ id: number; label: string }> | null) => {
        this.ctrOptions = opts ?? [];
        if (this.ctrOptions.length && this.selectedCtrId == null) {
          this.selectedCtrId = this.ctrOptions[0].id;
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.ctrOptions = [];
        this.cdr.detectChanges();
      },
    });
  }

  // =========================
  // Generate SAR from CTR
  // =========================
  generateSarFromSelectedCtr(): void {
    if (this.selectedCtrId == null) return;

    this.generating = true;
    this.generateError = null;
    this.cdr.detectChanges();

    this.complianceEventsService.generateSarFromCtr(this.selectedCtrId).subscribe({
      next: () => {
        this.generating = false;
        this.cdr.detectChanges();
        this.refresh();
      },
      error: (err: unknown) => {
        console.error(err);
        this.generating = false;

        let message = 'Failed to auto-generate SAR.';
        if (err && typeof err === 'object' && err !== null && 'error' in err) {
          const e = err as any;
          message = e?.error?.message ?? e?.message ?? message;
        }

        this.generateError = message;
        this.cdr.detectChanges();
      },
    });
  }

  // =========================
  // Navigation
  // =========================
  goToUploadSar(): void {
    this.router.navigate(['/upload'], {
      queryParams: { documentType: 'SAR' },
    });
  }

  goToCases(): void {
    this.router.navigate(['/cases']);
  }

  openSar(id: number): void {
    this.router.navigate(['/sars', id]);
  }

  // =========================
  // Search + Filtering
  // =========================
  onQuery(q: string): void {
    this.query = q ?? '';
  }

  filteredSars(): SarRow[] {
    const q = (this.query ?? '').trim().toLowerCase();
    if (!q) return this.sars;

    return this.sars.filter((s) => {
      const id = String(s.sarId).toLowerCase();
      const subj = String(s.subjectName ?? '').toLowerCase();
      const src = String(s.sourceEntityId ?? '').toLowerCase();
      const status = String(s.status ?? '').toLowerCase();
      return id.includes(q) || subj.includes(q) || src.includes(q) || status.includes(q);
    });
  }

  // =========================
  // UI Helpers
  // =========================
  badgeClass(status: string | null): string {
    const s = (status ?? '').toUpperCase();
    if (s === 'SUBMITTED') return 'badge badge--success';
    if (s === 'DRAFT') return 'badge badge--warn';
    if (s === 'REJECTED') return 'badge badge--danger';
    return 'badge';
  }

  scoreClass(score: number | null): string {
    const v = typeof score === 'number' ? score : null;
    if (v == null) return 'pill pill--muted';
    if (v >= 80) return 'pill pill--danger';
    if (v >= 50) return 'pill pill--warn';
    return 'pill pill--ok';
  }

  scoreLabel(score: number | null): string {
    const v = typeof score === 'number' ? score : null;
    if (v == null) return '—';
    if (v >= 80) return 'High';
    if (v >= 50) return 'Medium';
    return 'Low';
  }
}
