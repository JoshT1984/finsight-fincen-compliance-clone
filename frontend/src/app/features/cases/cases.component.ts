import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SideNavComponent, NavItem } from '../../shared/side-nav/side-nav.component';
import { ConfirmDialogComponent, ConfirmDialogMode } from '../../shared/confirm-dialog/confirm-dialog.component';
import { AuditEventsModalComponent } from '../../shared/audit-events-modal/audit-events-modal.component';
import { CaseNotesModalComponent } from '../../shared/case-notes-modal/case-notes-modal.component';
import { CasesService, CaseFileResponse } from '../../shared/services/cases.service';

@Component({
  selector: 'app-cases',
  standalone: true,
  imports: [
    CommonModule,
    SideNavComponent,
    ConfirmDialogComponent,
    AuditEventsModalComponent,
    CaseNotesModalComponent,
  ],
  templateUrl: './cases.component.html',
  styleUrls: ['./cases.component.css'],
})
export class CasesComponent implements OnInit {
  caseNavItems: NavItem[] = [
    { id: 'all', label: 'All Cases' },
    { id: 'open', label: 'Open' },
    { id: 'referred', label: 'Referred' },
    { id: 'closed', label: 'Closed' },
  ];

  selectedCaseCategory: string = 'all';
  cases: CaseFileResponse[] = [];
  filteredCases: CaseFileResponse[] = [];
  loading = false;
  error: string | null = null;

  feedbackMessage: string | null = null;
  feedbackSuccess = false;
  private feedbackTimeout: ReturnType<typeof setTimeout> | null = null;

  showConfirmDialog = false;
  confirmDialogTitle = '';
  confirmDialogMessage = '';
  confirmDialogMode: ConfirmDialogMode = 'close';
  caseToActOn: CaseFileResponse | null = null;

  showAuditModal = false;
  caseIdForAudit: number | null = null;

  showNotesModal = false;
  caseIdForNotes: number | null = null;

  constructor(
    private casesService: CasesService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loadCases();
  }

  onCategorySelected(categoryId: string): void {
    this.selectedCaseCategory = categoryId;
    this.error = null;
    this.clearFeedback();
    this.filteredCases = this.applyFilter(this.cases);
    this.cdr.detectChanges();
  }

  loadCases(): void {
    this.loading = true;
    this.error = null;
    this.casesService.getAll().subscribe({
      next: (list) => {
        this.cases = list;
        this.filteredCases = this.applyFilter(list);
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err?.message ?? 'Failed to load cases. Please try again.';
        this.cases = [];
        this.filteredCases = [];
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  private applyFilter(list: CaseFileResponse[]): CaseFileResponse[] {
    switch (this.selectedCaseCategory) {
      case 'open':
        return list.filter((c) => c.status === 'OPEN');
      case 'referred':
        return list.filter((c) => c.status === 'REFERRED');
      case 'closed':
        return list.filter((c) => c.status === 'CLOSED');
      default:
        return [...list];
    }
  }

  formatDate(isoString: string | null): string {
    if (!isoString) return '-';
    const d = new Date(isoString);
    return isNaN(d.getTime()) ? isoString : d.toLocaleDateString();
  }

  getCategoryLabel(): string {
    const item = this.caseNavItems.find((i) => i.id === this.selectedCaseCategory);
    return item ? item.label : 'Cases';
  }

  canRefer(c: CaseFileResponse): boolean {
    return c.status === 'OPEN';
  }

  canClose(c: CaseFileResponse): boolean {
    return c.status === 'OPEN' || c.status === 'REFERRED';
  }

  openReferDialog(c: CaseFileResponse): void {
    this.caseToActOn = c;
    this.confirmDialogTitle = 'Refer case';
    this.confirmDialogMessage = `Are you sure you want to refer Case #${c.caseId}? Please enter the agency you are referring to below.`;
    this.confirmDialogMode = 'refer';
    this.showConfirmDialog = true;
    this.cdr.detectChanges();
  }

  openCloseDialog(c: CaseFileResponse): void {
    this.caseToActOn = c;
    this.confirmDialogTitle = 'Close case';
    this.confirmDialogMessage = `Are you sure you want to close Case #${c.caseId}? This action cannot be undone.`;
    this.confirmDialogMode = 'close';
    this.showConfirmDialog = true;
    this.cdr.detectChanges();
  }

  openAuditModal(c: CaseFileResponse): void {
    this.caseIdForAudit = c.caseId;
    this.showAuditModal = true;
    this.cdr.detectChanges();
  }

  onAuditModalClose(): void {
    this.showAuditModal = false;
    this.caseIdForAudit = null;
    this.cdr.detectChanges();
  }

  openNotesModal(c: CaseFileResponse): void {
    this.caseIdForNotes = c.caseId;
    this.showNotesModal = true;
    this.cdr.detectChanges();
  }

  onNotesModalClose(): void {
    this.showNotesModal = false;
    this.caseIdForNotes = null;
    this.cdr.detectChanges();
  }

  onConfirmDialogConfirm(payload: { agency?: string }): void {
    const c = this.caseToActOn;
    if (!c) {
      this.showConfirmDialog = false;
      this.caseToActOn = null;
      this.cdr.detectChanges();
      return;
    }

    if (this.confirmDialogMode === 'refer') {
      const agency = (payload.agency ?? '').trim();
      if (!agency) {
        this.showFeedback('Please enter the referred-to agency.', false);
        this.cdr.detectChanges();
        return;
      }
      this.casesService.refer(c.caseId, agency).subscribe({
        next: () => {
          this.showConfirmDialog = false;
          this.caseToActOn = null;
          this.showFeedback(`Case #${c.caseId} has been referred successfully.`, true);
          this.loadCases();
        },
        error: (err) => {
          this.showConfirmDialog = false;
          this.caseToActOn = null;
          this.showFeedback(err?.error?.message ?? err?.message ?? 'Failed to refer case.', false);
          this.cdr.detectChanges();
        },
      });
    } else {
      this.casesService.close(c.caseId).subscribe({
        next: () => {
          this.showConfirmDialog = false;
          this.caseToActOn = null;
          this.showFeedback(`Case #${c.caseId} has been closed successfully.`, true);
          this.loadCases();
        },
        error: (err) => {
          this.showConfirmDialog = false;
          this.caseToActOn = null;
          this.showFeedback(err?.error?.message ?? err?.message ?? 'Failed to close case.', false);
          this.cdr.detectChanges();
        },
      });
    }
  }

  onConfirmDialogCancel(): void {
    this.showConfirmDialog = false;
    this.caseToActOn = null;
    this.cdr.detectChanges();
  }

  private showFeedback(message: string, success: boolean): void {
    if (this.feedbackTimeout) {
      clearTimeout(this.feedbackTimeout);
      this.feedbackTimeout = null;
    }
    this.feedbackMessage = message;
    this.feedbackSuccess = success;
    this.feedbackTimeout = setTimeout(() => {
      this.clearFeedback();
      this.cdr.detectChanges();
    }, 6000);
  }

  private clearFeedback(): void {
    if (this.feedbackTimeout) {
      clearTimeout(this.feedbackTimeout);
      this.feedbackTimeout = null;
    }
    this.feedbackMessage = null;
  }
}
