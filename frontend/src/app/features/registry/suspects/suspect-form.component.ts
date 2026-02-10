import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { BreadcrumbsComponent, BreadcrumbItem } from '../../../shared/breadcrumbs/breadcrumbs.component';
import { ProperCasePipe } from '../../../shared/pipes/proper-case.pipe';
import {
  SuspectService,
  CreateSuspectRequest,
  PatchSuspectRequest,
} from '../../../shared/services/suspect.service';

const RISK_LEVELS = ['UNKNOWN', 'LOW', 'MEDIUM', 'HIGH'];

@Component({
  selector: 'app-suspect-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, BreadcrumbsComponent, ProperCasePipe],
  templateUrl: './suspect-form.component.html',
  styleUrls: ['./suspect-form.component.css'],
})
export class SuspectFormComponent implements OnInit {
  breadcrumbItems: BreadcrumbItem[] = [];
  suspectId: number | null = null;
  loading = false;
  loadError: string | null = null;
  submitError: string | null = null;
  saving = false;

  primaryName = '';
  dob = '';
  ssn = '';
  riskLevel = 'UNKNOWN';

  readonly riskLevels = RISK_LEVELS;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private suspectService: SuspectService,
    private cdr: ChangeDetectorRef,
  ) {}

  get isEditMode(): boolean {
    return this.suspectId != null;
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      const idNum = parseInt(id, 10);
      if (!isNaN(idNum)) {
        this.suspectId = idNum;
        this.breadcrumbItems = [
          { label: 'Home', url: '/dashboard' },
          { label: 'Registry', url: '/registry' },
          { label: 'Suspects', url: '/registry/suspects' },
          { label: 'Edit' },
        ];
        this.loadSuspect(idNum);
        return;
      }
    }
    this.suspectId = null;
    this.breadcrumbItems = [
      { label: 'Home', url: '/dashboard' },
      { label: 'Registry', url: '/registry' },
      { label: 'Suspects', url: '/registry/suspects' },
      { label: 'New suspect' },
    ];
    this.cdr.detectChanges();
  }

  loadSuspect(id: number): void {
    this.loading = true;
    this.loadError = null;
    this.suspectService.getById(id).subscribe({
      next: (s) => {
        this.primaryName = s.primaryName ?? '';
        this.dob = s.dob ?? '';
        this.ssn = s.ssn ?? '';
        this.riskLevel = s.riskLevel ?? 'UNKNOWN';
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.loadError = err?.message ?? 'Failed to load suspect.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  onSubmit(): void {
    this.submitError = null;
    if (this.isEditMode && this.suspectId != null) {
      const body: PatchSuspectRequest = {
        primaryName: this.primaryName.trim() || undefined,
        dob: this.dob.trim() || undefined,
        ssn: this.ssn.trim() || undefined,
        riskLevel: this.riskLevel || undefined,
      };
      this.saving = true;
      this.suspectService.update(this.suspectId, body).subscribe({
        next: () => {
          this.saving = false;
          this.router.navigate(['/registry/suspects', this.suspectId]);
        },
        error: (err) => {
          this.submitError = err?.message ?? 'Failed to update suspect.';
          this.saving = false;
          this.cdr.detectChanges();
        },
      });
    } else {
      const body: CreateSuspectRequest = {
        primaryName: this.primaryName.trim(),
        dob: this.dob.trim() || undefined,
        ssn: this.ssn.trim() || undefined,
        riskLevel: this.riskLevel || undefined,
      };
      this.saving = true;
      this.suspectService.create(body).subscribe({
        next: (created) => {
          this.saving = false;
          this.router.navigate(['/registry/suspects', created.suspectId]);
        },
        error: (err) => {
          this.submitError = err?.message ?? 'Failed to create suspect.';
          this.saving = false;
          this.cdr.detectChanges();
        },
      });
    }
  }

  cancel(): void {
    if (this.isEditMode && this.suspectId != null) {
      this.router.navigate(['/registry/suspects', this.suspectId]);
    } else {
      this.router.navigate(['/registry/suspects']);
    }
  }
}
