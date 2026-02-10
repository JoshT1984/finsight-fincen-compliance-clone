import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import {
  ComplianceService,
  ComplianceEventResponse,
} from '../../shared/services/compliance.service';
import { ComplianceEventsService } from '../../services/compliance-events.service';

@Component({
  selector: 'app-sars',
  standalone: true,
  templateUrl: './sars.component.html',
  styleUrls: ['./sars.component.css'],
  imports: [CommonModule, FormsModule, RouterModule],
})
export class SarsComponent {
  sars: ComplianceEventResponse[] = [];
  loading = false;
  error: string | null = null;
  search = '';
  query: string = '';

  // Auto-generate SARs from an existing CTR
  ctrOptions: Array<{ id: number; label: string }> = [];
  selectedCtrId: number | null = null;
  generating = false;
  generateError: string | null = null;

  constructor(
    private complianceService: ComplianceService,
    private complianceEventsService: ComplianceEventsService,
    private router: Router,
  ) {
    this.refresh();
  }

  refresh(): void {
    this.loading = true;
    this.error = null;
    this.complianceService.getEvents('SAR', 0, 200).subscribe({
      next: (list) => {
        // Map SAR table template properties for compatibility
        this.sars = [...(list ?? [])]
          .map((e) => ({
            ...e,
            sarId: e.eventId,
            subjectName: e.sourceEntityId, // or another field if available
            suspicionScore: e.severityScore,
            updatedAt: e.eventTime || e.createdAt,
          }))
          .sort((a, b) => (b.eventTime || '').localeCompare(a.eventTime || ''));
        this.loading = false;
        this.loadCtrOptions();
      },
      error: () => {
        this.error =
          'Could not load SARs. If the compliance-event service is offline, you can still upload SAR PDFs to create records.';
        this.sars = [];
        this.loading = false;
      },
    });
  }


  loadCtrOptions(): void {
    this.complianceService.getCtrOptions().subscribe({
      next: (opts) => {
        this.ctrOptions = opts ?? [];
        if (this.ctrOptions.length && this.selectedCtrId == null) {
          this.selectedCtrId = this.ctrOptions[0].id;
        }
      },
      error: () => {
        this.ctrOptions = [];
      },
    });
  }

  generateSarFromSelectedCtr(): void {
    if (this.selectedCtrId == null) return;

    this.generating = true;
    this.generateError = null;

    this.complianceEventsService.generateSarFromCtr(this.selectedCtrId).subscribe({
      next: () => {
        this.generating = false;
        this.refresh();
      },
      error: (err) => {
        console.error(err);
        this.generating = false;
        this.generateError = err?.error?.message ?? 'Failed to auto-generate SAR.';
      },
    });
  }

  goToUploadSar(): void {
    this.router.navigate(['/upload'], { queryParams: { documentType: 'SAR' } });
  }

  goToCases(): void {
    this.router.navigate(['/cases']);
  }

  filteredSars(): ComplianceEventResponse[] {
    const q = (this.query || this.search).trim().toLowerCase();
    if (!q) return this.sars;
    return this.sars.filter((s) => {
      const sev = s.severityScore != null ? String(s.severityScore) : '';
      return (
        String(s.eventId).includes(q) ||
        (s.status || '').toLowerCase().includes(q) ||
        (s.sourceEntityId || '').toLowerCase().includes(q) ||
        sev.includes(q)
      );
    });
  }

  onQuery(val: string): void {
    this.query = val;
  }

  openSar(_id: any): void {
    // Implement navigation or modal logic here
  }

  badgeClass(status: string | null | undefined): string {
    const s = (status || '').toUpperCase();
    if (s.includes('SUBMIT')) return 'badge badge--good';
    if (s.includes('DRAFT')) return 'badge badge--warn';
    return 'badge';
  }
}
