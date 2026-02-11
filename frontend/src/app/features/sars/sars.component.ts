import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

import {
  ComplianceService,
  ComplianceEventResponse,
} from '../../shared/services/compliance.service';
import { ComplianceEventsService } from '../../services/compliance-events.service';
import { TransactionService } from '../../shared/services/transaction.service';

import {
  SubjectNameLookup,
  addSubjectLookupKeys,
  pickFirstString,
  resolveSubjectName,
} from '../../shared/utils/subject-name.util';

@Component({
  selector: 'app-sars',
  standalone: true,
  templateUrl: './sars.component.html',
  styleUrls: ['./sars.component.css'],
  imports: [CommonModule, FormsModule, RouterModule],
})
export class SarsComponent {
  sars: ComplianceEventResponse[] = [];
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

    // 1) Load transactions first to build a subject-name lookup (best display names)
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
        // Fallback: still load SARs even if transactions fail
        this.transactionNameMap = {};
        this.loadSarsWithLookup();
      },
    });
  }

  private loadSarsWithLookup(): void {
    // IMPORTANT: use the paged ComplianceEventsService (matches backend)
    this.complianceEventsService.getSarEvents(0, 200).subscribe({
      next: (page) => {
        const rows = Array.isArray(page?.content) ? page.content : [];

        this.sars = rows
          .map((e: any) => {
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

            // Build a ComplianceEventResponse-compatible object for your existing template
            const mapped: ComplianceEventResponse = {
              eventId: e.eventId ?? e.id,
              eventType: e.eventType ?? e.event_type ?? 'SAR',
              status: e.status ?? null,
              sourceEntityId: e.sourceEntityId ?? e.source_entity_id ?? null,
              severityScore: e.severityScore ?? e.severity_score ?? null,
              eventTime: e.eventTime ?? e.event_time ?? null,
              createdAt: e.createdAt ?? e.created_at ?? null,

              // extra template compatibility fields
              sarId: e.eventId ?? e.id,
              subjectName: resolved.subjectName,
              updatedAt: e.eventTime ?? e.event_time ?? e.createdAt ?? e.created_at ?? null,
            } as any;

            return mapped;
          })
          .sort((a, b) =>
            String(b.eventTime || b.createdAt || '').localeCompare(
              String(a.eventTime || a.createdAt || ''),
            ),
          );

        this.loading = false;

        // Load CTR dropdown after SARs are loaded
        this.loadCtrOptions();

        // Force UI update (prevents “only shows after refresh” symptoms)
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
        if (err && typeof err === 'object' && 'error' in err) {
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

  // =========================
  // Filtering
  // =========================
  filteredSars(): ComplianceEventResponse[] {
    const q = (this.query || this.search).trim().toLowerCase();
    if (!q) return this.sars;

    return this.sars.filter((s) => {
      const sev = s.severityScore != null ? String(s.severityScore) : '';
      return (
        String(s.eventId).includes(q) ||
        String((s as any).sarId ?? '').includes(q) ||
        (s.status || '').toLowerCase().includes(q) ||
        (s.sourceEntityId || '').toLowerCase().includes(q) ||
        ((s as any).subjectName || '').toLowerCase().includes(q) ||
        sev.includes(q)
      );
    });
  }

  onQuery(val: string): void {
    this.query = val;
  }

  openSar(_id: any): void {
    // Optional: route to SAR detail page later
  }

  badgeClass(status: string | null | undefined): string {
    const s = (status || '').toUpperCase();
    if (s.includes('SUBMIT')) return 'badge badge--good';
    if (s.includes('DRAFT')) return 'badge badge--warn';
    return 'badge';
  }

  scoreClass(score: number | string | null | undefined): string {
    const n = typeof score === 'number' ? score : score != null ? Number(score) : NaN;
    if (!Number.isFinite(n)) return 'badge';
    if (n >= 60) return 'badge badge--good';
    if (n >= 40) return 'badge badge--warn';
    return 'badge';
  }

  scoreLabel(score: number | string | null | undefined): string {
    const n = typeof score === 'number' ? score : score != null ? Number(score) : NaN;
    if (!Number.isFinite(n)) return '—';
    if (n >= 60) return 'Auto';
    if (n >= 40) return 'Review';
    return 'CTR';
  }
}
