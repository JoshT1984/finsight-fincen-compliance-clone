import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { BreadcrumbsComponent, BreadcrumbItem } from '../../../shared/breadcrumbs/breadcrumbs.component';
import { ProperCasePipe } from '../../../shared/pipes/proper-case.pipe';
import { SuspectService, SuspectResponse } from '../../../shared/services/suspect.service';

@Component({
  selector: 'app-suspects-list',
  standalone: true,
  imports: [CommonModule, RouterLink, BreadcrumbsComponent, ProperCasePipe],
  templateUrl: './suspects-list.component.html',
  styleUrls: ['./suspects-list.component.css'],
})
export class SuspectsListComponent implements OnInit {
  breadcrumbItems: BreadcrumbItem[] = [
    { label: 'Home', url: '/dashboard' },
    { label: 'Registry', url: '/registry' },
    { label: 'Suspects' },
  ];

  suspects: SuspectResponse[] = [];
  loading = false;
  error: string | null = null;

  constructor(
    private suspectService: SuspectService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  navigateToSuspect(id: number): void {
    this.router.navigate(['/registry/suspects', id]);
  }

  ngOnInit(): void {
    this.loadSuspects();
  }

  loadSuspects(): void {
    this.loading = true;
    this.error = null;
    this.suspectService.getAll().subscribe({
      next: (list) => {
        this.suspects = list;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err?.message ?? 'Failed to load suspects. Please try again.';
        this.suspects = [];
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

  goBackToRegistry(): void {
    this.router.navigate(['/registry']);
  }
}
