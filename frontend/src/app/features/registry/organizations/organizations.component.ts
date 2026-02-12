import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  BreadcrumbsComponent,
  BreadcrumbItem,
} from '../../../shared/breadcrumbs/breadcrumbs.component';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { ProperCasePipe } from '../../../shared/pipes/proper-case.pipe';
import {
  OrganizationService,
  OrganizationResponse,
} from '../../../shared/services/organization.service';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-organizations',
  standalone: true,
  imports: [CommonModule, BreadcrumbsComponent, ConfirmDialogComponent, ProperCasePipe, RouterModule],
  templateUrl: './organizations.component.html',
  styleUrls: ['./organizations.component.css'],
})
export class OrganizationsComponent implements OnInit {
  breadcrumbItems: BreadcrumbItem[] = [
    { label: 'Home', url: '/dashboard' },
    { label: 'Registry', url: '/registry' },
    { label: 'Organizations' },
  ];

  organizations: OrganizationResponse[] = [];
  loading = false;
  error: string | null = null;
  showDeleteConfirm = false;
  deleteTargetId: number | null = null;
  deleting = false;

  constructor(
    private organizationService: OrganizationService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loadOrganizations();
  }

  loadOrganizations(): void {
    this.loading = true;
    this.error = null;
    this.organizationService.getAll().subscribe({
      next: (list) => {
        this.organizations = list;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err?.message ?? 'Failed to load organizations.';
        this.organizations = [];
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  formatDate(isoString: string | null): string {
    if (!isoString) return '-';
    const d = new Date(isoString);
    return isNaN(d.getTime()) ? isoString : d.toLocaleDateString();
  }

  openDeleteConfirm(orgId: number): void {
    this.deleteTargetId = orgId;
    this.showDeleteConfirm = true;
  }

  onConfirmDelete(): void {
    if (this.deleteTargetId == null) return;
    const id = this.deleteTargetId;
    this.showDeleteConfirm = false;
    this.deleteTargetId = null;
    this.deleting = true;
    this.organizationService.delete(id).subscribe({
      next: () => {
        this.deleting = false;
        this.loadOrganizations();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err?.message ?? 'Failed to delete organization.';
        this.deleting = false;
        this.cdr.detectChanges();
      },
    });
  }

  onCancelDelete(): void {
    this.showDeleteConfirm = false;
    this.deleteTargetId = null;
  }
}
