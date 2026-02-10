import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { BreadcrumbsComponent, BreadcrumbItem } from '../../../shared/breadcrumbs/breadcrumbs.component';
import { ProperCasePipe } from '../../../shared/pipes/proper-case.pipe';
import {
  OrganizationService,
  OrganizationResponse,
  CreateOrganizationRequest,
  PatchOrganizationRequest,
} from '../../../shared/services/organization.service';

const ORGANIZATION_TYPES = ['CARTEL', 'GANG', 'TERRORIST', 'FRAUD_RING', 'MONEY_LAUNDERING', 'OTHER'];

@Component({
  selector: 'app-organization-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, BreadcrumbsComponent, ProperCasePipe],
  templateUrl: './organization-form.component.html',
  styleUrls: ['./organization-form.component.css'],
})
export class OrganizationFormComponent implements OnInit {
  breadcrumbItems: BreadcrumbItem[] = [];
  orgId: number | null = null;
  loading = false;
  loadError: string | null = null;
  submitError: string | null = null;
  saving = false;

  name = '';
  type = 'OTHER';

  readonly organizationTypes = ORGANIZATION_TYPES;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private organizationService: OrganizationService,
    private cdr: ChangeDetectorRef,
  ) {}

  get isEditMode(): boolean {
    return this.orgId != null;
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      const idNum = parseInt(id, 10);
      if (!isNaN(idNum)) {
        this.orgId = idNum;
        this.breadcrumbItems = [
          { label: 'Home', url: '/dashboard' },
          { label: 'Registry', url: '/registry' },
          { label: 'Organizations', url: '/registry/organizations' },
          { label: 'Edit' },
        ];
        this.loadOrganization(idNum);
        return;
      }
    }
    this.orgId = null;
    this.breadcrumbItems = [
      { label: 'Home', url: '/dashboard' },
      { label: 'Registry', url: '/registry' },
      { label: 'Organizations', url: '/registry/organizations' },
      { label: 'New organization' },
    ];
    this.cdr.detectChanges();
  }

  loadOrganization(id: number): void {
    this.loading = true;
    this.loadError = null;
    this.organizationService.getById(id).subscribe({
      next: (o) => {
        this.name = o.name ?? '';
        this.type = o.type ?? 'OTHER';
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.loadError = err?.message ?? 'Failed to load organization.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  onSubmit(): void {
    this.submitError = null;
    if (this.isEditMode && this.orgId != null) {
      const body: PatchOrganizationRequest = {
        name: this.name.trim() || undefined,
        type: this.type || undefined,
      };
      this.saving = true;
      this.organizationService.update(this.orgId, body).subscribe({
        next: () => {
          this.saving = false;
          this.router.navigate(['/registry/organizations']);
        },
        error: (err) => {
          this.submitError = err?.message ?? 'Failed to update organization.';
          this.saving = false;
          this.cdr.detectChanges();
        },
      });
    } else {
      const body: CreateOrganizationRequest = {
        name: this.name.trim(),
        type: this.type || undefined,
      };
      this.saving = true;
      this.organizationService.create(body).subscribe({
        next: () => {
          this.saving = false;
          this.router.navigate(['/registry/organizations']);
        },
        error: (err) => {
          this.submitError = err?.message ?? 'Failed to create organization.';
          this.saving = false;
          this.cdr.detectChanges();
        },
      });
    }
  }

  cancel(): void {
    this.router.navigate(['/registry/organizations']);
  }
}
