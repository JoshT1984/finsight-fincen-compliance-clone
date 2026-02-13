import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { BreadcrumbsComponent, BreadcrumbItem } from '../../../shared/breadcrumbs/breadcrumbs.component';
import { SideNavComponent, NavItem } from '../../../shared/side-nav/side-nav.component';
import { CaseFileResponse } from '../../../shared/services/cases.service';

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

  constructor(private route: ActivatedRoute) {
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
}
