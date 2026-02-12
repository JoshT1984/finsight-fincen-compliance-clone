import { ChangeDetectorRef, Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { BreadcrumbsComponent, BreadcrumbItem } from '../../../shared/breadcrumbs/breadcrumbs.component';
import { SideNavComponent, NavItem } from '../../../shared/side-nav/side-nav.component';
import { CaseFileResponse, CasesService } from '../../../shared/services/cases.service';

@Component({
  selector: 'app-case-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, BreadcrumbsComponent, SideNavComponent],
  templateUrl: './case-detail.component.html',
  styleUrls: ['./case-detail.component.css'],
})
export class CaseDetailComponent implements OnDestroy {
  caseData: CaseFileResponse | null = null;
  caseId: number | null = null;
  breadcrumbItems: BreadcrumbItem[] = [];
  showCreateNoteDialog = false;
  noteText = '';
  createNoteLoading = false;
  createNoteError: string | null = null;
  createNoteFeedback: string | null = null;
  createNoteFeedbackSuccess = false;
  private createNoteFeedbackTimeout: ReturnType<typeof setTimeout> | null = null;
  private destroy$ = new Subject<void>();

  caseDetailNavItems: NavItem[] = [
    { id: 'overview', label: 'Overview' },
    { id: 'notes', label: 'Notes' },
    { id: 'audit', label: 'Audit History' },
    { id: 'documents', label: 'Documents' },
  ];

  get caseDetailNavBasePath(): string {
    return this.caseId != null ? `/cases/${this.caseId}` : '';
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private casesService: CasesService,
    private cdr: ChangeDetectorRef,
  ) {
    this.route.data.pipe(takeUntil(this.destroy$)).subscribe((data) => {
      this.caseData = data['case'] ?? null;
      this.updateBreadcrumbs();
    });
    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const id = params.get('id');
      this.caseId = id ? Number(id) : null;
      this.updateBreadcrumbs();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private updateBreadcrumbs(): void {
    this.breadcrumbItems = [
      { label: 'Home', url: '/' },
      { label: 'Cases', url: '/cases' },
      ...(this.caseId != null ? [{ label: `Case #${this.caseId}` }] : []),
    ];
  }

  openCreateNoteDialog(): void {
    this.noteText = '';
    this.createNoteError = null;
    this.showCreateNoteDialog = true;
  }

  closeCreateNoteDialog(): void {
    this.showCreateNoteDialog = false;
    this.createNoteLoading = false;
    this.createNoteError = null;
  }

  submitCreateNote(): void {
    if (this.caseId == null) return;

    const text = this.noteText.trim();
    if (!text) {
      this.createNoteError = 'Please enter a note.';
      return;
    }

    this.createNoteLoading = true;
    this.createNoteError = null;
    this.casesService
      .createCaseNote(this.caseId, text)
      .subscribe({
        next: () => {
          this.createNoteLoading = false;
          this.showCreateNoteDialog = false;
          this.noteText = '';
          this.showCreateNoteFeedback('Case note created successfully.', true);
          this.refreshNotesIfActive();
          this.cdr.detectChanges();
        },
        error: (err: HttpErrorResponse) => {
          this.createNoteLoading = false;
          this.createNoteError =
            err?.error?.message ??
            err?.message ??
            (err?.status ? `Request failed (${err.status})` : 'Failed to create note.');
          this.cdr.detectChanges();
        },
      });
  }

  private showCreateNoteFeedback(message: string, success: boolean): void {
    if (this.createNoteFeedbackTimeout) {
      clearTimeout(this.createNoteFeedbackTimeout);
    }
    this.createNoteFeedback = message;
    this.createNoteFeedbackSuccess = success;
    this.createNoteFeedbackTimeout = setTimeout(() => {
      this.createNoteFeedback = null;
      this.cdr.detectChanges();
    }, 6000);
  }

  private refreshNotesIfActive(): void {
    if (!this.router.url.includes('/notes')) return;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { noteRefresh: Date.now() },
      queryParamsHandling: 'merge',
    });
  }

}
