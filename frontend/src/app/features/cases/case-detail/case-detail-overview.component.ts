import { ChangeDetectorRef, Component, inject, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ConfirmDialogComponent, ConfirmDialogMode } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CasesService, CaseFileResponse } from '../../../shared/services/cases.service';
import { RoleService } from '../../../shared/services/role.service';

@Component({
  selector: 'app-case-detail-overview',
  standalone: true,
  imports: [CommonModule, ConfirmDialogComponent],
  templateUrl: './case-detail-overview.component.html',
  styleUrls: ['./case-detail-overview.component.css'],
})
export class CaseDetailOverviewComponent implements OnDestroy {
  private roleService = inject(RoleService);
  private destroy$ = new Subject<void>();

  caseData: CaseFileResponse | null = null;
  showConfirmDialog = false;
  confirmDialogTitle = '';
  confirmDialogMessage = '';
  confirmDialogMode: ConfirmDialogMode = 'close';
  feedbackMessage: string | null = null;
  feedbackSuccess = false;
  private feedbackTimeout: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private route: ActivatedRoute,
    private casesService: CasesService,
    private cdr: ChangeDetectorRef,
  ) {
    this.caseData = this.route.parent?.snapshot.data['case'] ?? null;
    this.route.parent?.data.pipe(takeUntil(this.destroy$)).subscribe((data) => {
      this.caseData = data['case'] ?? null;
      this.cdr.detectChanges();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get caseId(): number {
    const id = this.route.parent?.snapshot.paramMap.get('id');
    return id ? Number(id) : 0;
  }

  formatDate(isoString: string | null): string {
    if (!isoString) return '—';
    const d = new Date(isoString);
    return isNaN(d.getTime()) ? isoString : d.toLocaleDateString();
  }

  canRefer(): boolean {
    return this.roleService.isAnalyst() && this.caseData?.status === 'OPEN';
  }

  canClose(): boolean {
    return (
      this.roleService.isAnalyst() &&
      (this.caseData?.status === 'OPEN' || this.caseData?.status === 'REFERRED')
    );
  }

  openReferDialog(): void {
    this.confirmDialogTitle = 'Refer case';
    this.confirmDialogMessage = `Are you sure you want to refer Case #${this.caseId}? Please enter the agency you are referring to below.`;
    this.confirmDialogMode = 'refer';
    this.showConfirmDialog = true;
    this.cdr.detectChanges();
  }

  openCloseDialog(): void {
    this.confirmDialogTitle = 'Close case';
    this.confirmDialogMessage = `Are you sure you want to close Case #${this.caseId}? This action cannot be undone.`;
    this.confirmDialogMode = 'close';
    this.showConfirmDialog = true;
    this.cdr.detectChanges();
  }

  onConfirm(payload: { agency?: string }): void {
    if (this.confirmDialogMode === 'refer') {
      const agency = (payload.agency ?? '').trim();
      if (!agency) {
        this.showFeedback('Please enter the referred-to agency.', false);
        this.cdr.detectChanges();
        return;
      }
      this.casesService.refer(this.caseId, agency).subscribe({
        next: () => {
          this.showConfirmDialog = false;
          this.showFeedback(`Case #${this.caseId} has been referred successfully.`, true);
          this.refreshCase();
        },
        error: (err) => {
          this.showConfirmDialog = false;
          this.showFeedback(err?.error?.message ?? err?.message ?? 'Failed to refer case.', false);
          this.cdr.detectChanges();
        },
      });
    } else {
      this.casesService.close(this.caseId).subscribe({
        next: () => {
          this.showConfirmDialog = false;
          this.showFeedback(`Case #${this.caseId} has been closed successfully.`, true);
          this.refreshCase();
        },
        error: (err) => {
          this.showConfirmDialog = false;
          this.showFeedback(err?.error?.message ?? err?.message ?? 'Failed to close case.', false);
          this.cdr.detectChanges();
        },
      });
    }
  }

  onCancel(): void {
    this.showConfirmDialog = false;
    this.cdr.detectChanges();
  }

  private refreshCase(): void {
    this.casesService.getById(this.caseId).subscribe({
      next: (c) => {
        this.caseData = c;
        this.cdr.detectChanges();
      },
    });
  }

  private showFeedback(message: string, success: boolean): void {
    if (this.feedbackTimeout) clearTimeout(this.feedbackTimeout);
    this.feedbackMessage = message;
    this.feedbackSuccess = success;
    this.feedbackTimeout = setTimeout(() => {
      this.feedbackMessage = null;
      this.cdr.detectChanges();
    }, 6000);
  }
}
