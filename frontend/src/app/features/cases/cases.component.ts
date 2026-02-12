import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { SideNavComponent, NavItem } from '../../shared/side-nav/side-nav.component';
import { BreadcrumbsComponent, BreadcrumbItem } from '../../shared/breadcrumbs/breadcrumbs.component';
import { CasesService, CaseFileResponse } from '../../shared/services/cases.service';
import { RoleService } from '../../shared/services/role.service';

const ALL_CASE_NAV_ITEMS: NavItem[] = [
  { id: 'all', label: 'All Cases' },
  { id: 'open', label: 'Open' },
  { id: 'referred', label: 'Referred' },
  { id: 'closed', label: 'Closed' },
];

@Component({
  selector: 'app-cases',
  standalone: true,
  imports: [CommonModule, RouterLink, SideNavComponent, BreadcrumbsComponent],
  templateUrl: './cases.component.html',
  styleUrls: ['./cases.component.css'],
})
export class CasesComponent implements OnInit {
  private roleService = inject(RoleService);

  /** Law Enforcement only sees referred/closed cases, so "Open" is hidden. */
  get caseNavItems(): NavItem[] {
    if (this.roleService.isLawEnforcement()) {
      return ALL_CASE_NAV_ITEMS.filter((item) => item.id !== 'open');
    }
    return ALL_CASE_NAV_ITEMS;
  }

  breadcrumbItems: BreadcrumbItem[] = [
    { label: 'Home', url: '/' },
    { label: 'Cases' },
  ];

  selectedCaseCategory: string = 'all';
  cases: CaseFileResponse[] = [];
  filteredCases: CaseFileResponse[] = [];
  loading = false;
  error: string | null = null;

  constructor(
    private casesService: CasesService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  navigateToCase(caseId: number): void {
    this.router.navigate(['/cases', caseId]);
  }

  ngOnInit(): void {
    this.loadCases();
  }

  onCategorySelected(categoryId: string): void {
    this.selectedCaseCategory = categoryId;
    this.error = null;
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

}
