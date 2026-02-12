import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';

import { ComplianceEventsService } from '../../services/compliance-events.service';
import { CasesService } from '../../shared/services/cases.service';
import { ComplianceEventDto } from '../../models/compliance-event-dto.interface';
import { ProfileModel } from '../../models/profile.model';
import { IdentityService } from '../../shared/services/identity.service';

type OfficerCaseRow = {
  caseId: number;
  sarId: number | null;
  status: string | null;
  referredToAgency: boolean;
  suspicionScore: number | null;
  subjectLabel: string;
};

@Component({
  selector: 'app-officer-review',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './officer-review.component.html',
  styleUrls: ['./officer-review.component.css'],
})
export class OfficerReviewComponent implements OnInit {
  loading = false;
  error: string | null = null;

  rows: OfficerCaseRow[] = [];
  query = '';

  profile: ProfileModel | null = null;

  private sarIndex: Record<number, { suspicionScore: number | null; subjectLabel: string }> = {};

  constructor(
    private casesService: CasesService,
    private complianceEventsService: ComplianceEventsService,
    private cdr: ChangeDetectorRef,
    private router: Router,
    private identityService: IdentityService,
  ) {}

  ngOnInit(): void {
    this.refresh();
    this.identityService.profile$.subscribe((profile) => {
      this.profile = profile;
    });
  }

  // =========================
  // Data loading
  // =========================
  refresh(): void {
    this.loading = true;
    this.error = null;
    this.rows = [];
    this.cdr.detectChanges();

    // Pull SAR events so we can show suspicion score + subject label alongside cases.
    this.complianceEventsService.getSarEvents(0, 500).subscribe({
      next: (res) => {
        console.log('Officer SAR events:', res);
        const content: ComplianceEventDto[] = Array.isArray(res?.content) ? res.content : [];

        const idx: Record<number, { suspicionScore: number | null; subjectLabel: string }> = {};
        for (const e of content) {
          const sarIdRaw = (e as any)?.sarId ?? (e as any)?.eventId ?? (e as any)?.event_id;
          const sarId = typeof sarIdRaw === 'number' ? sarIdRaw : Number(sarIdRaw);
          if (!Number.isFinite(sarId)) continue;

          const rawScore =
            (e as any)?.suspicionScore ??
            (e as any)?.suspicion_score ??
            (e as any)?.severityScore ??
            (e as any)?.severity_score ??
            null;

          const suspicionScore: number | null =
            typeof rawScore === 'number'
              ? rawScore
              : rawScore != null && !Number.isNaN(Number(rawScore))
                ? Number(rawScore)
                : null;

          const subjectLabel =
            (e as any)?.subjectName ??
            (e as any)?.subjectLabel ??
            (e as any)?.sourceEntityId ??
            (e as any)?.source_entity_id ??
            '';

          idx[sarId] = {
            suspicionScore,
            subjectLabel: String(subjectLabel || '').trim() || '—',
          };
        }

        this.sarIndex = idx;
        this.loadCases();
      },
      error: (err) => {
        // Even if SAR events fail, officers can still see the case queue.
        console.log('Officer SAR index failed:', err);
        this.sarIndex = {};
        this.loadCases();
      },
    });
  }

  private loadCases(): void {
    this.casesService.getAll().subscribe({
      next: (cases) => {
        const mapped: OfficerCaseRow[] = (cases ?? [])
          .map((c: any): OfficerCaseRow | null => {
            console.log(c.referredToAgency === this.profile?.organizationName);
            const caseId = typeof c?.caseId === 'number' ? c.caseId : Number(c?.caseId);
            if (!Number.isFinite(caseId)) return null;

            const sarId = typeof c?.sarId === 'number' ? c.sarId : Number(c?.sarId);
            const sarIdFinal = Number.isFinite(sarId) ? sarId : null;

            const sarMeta = sarIdFinal != null ? this.sarIndex[sarIdFinal] : null;

            return {
              caseId,
              sarId: sarIdFinal,
              status: (c?.status ?? null) as string | null,
              referredToAgency: Boolean(c?.referredToAgency),
              suspicionScore: sarMeta?.suspicionScore ?? null,
              subjectLabel: sarMeta?.subjectLabel ?? '—',
            };
          })
          .filter((x: OfficerCaseRow | null): x is OfficerCaseRow => x !== null);

        // If your API includes referred flag, officers probably only want referred cases.
        // If you want ALL cases, remove this filter.
        this.rows = mapped
          .filter((r) => r.referredToAgency === true)
          .sort((a, b) => b.caseId - a.caseId);

        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.log('Officer load cases failed:', err);
        this.error = 'Failed to load cases.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  // =========================
  // Navigation
  // =========================
  openCase(caseId: number): void {
    this.router.navigate(['/cases', caseId, 'overview']);
  }

  // =========================
  // Search + Filtering
  // =========================
  onQuery(q: string): void {
    this.query = q ?? '';
  }

  filteredRows(): OfficerCaseRow[] {
    const q = (this.query ?? '').trim().toLowerCase();
    if (!q) return this.rows;

    return this.rows.filter((r) => {
      const caseId = String(r.caseId).toLowerCase();
      const sarId = String(r.sarId ?? '').toLowerCase();
      const subject = String(r.subjectLabel ?? '').toLowerCase();
      const status = String(r.status ?? '').toLowerCase();
      return caseId.includes(q) || sarId.includes(q) || subject.includes(q) || status.includes(q);
    });
  }

  // =========================
  // UI Helpers (theme-aligned)
  // =========================
  badgeClass(status: string | null): string {
    const s = (status ?? '').toUpperCase();
    if (s === 'CLOSED') return 'badge badge--danger';
    if (s === 'OPEN') return 'badge badge--success';
    if (s === 'REFERRED') return 'badge badge--warn';
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
