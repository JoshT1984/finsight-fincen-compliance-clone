import { ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { CasesService, AuditEventResponse } from '../services/cases.service';

@Component({
  selector: 'app-audit-events-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './audit-events-modal.component.html',
  styleUrls: ['./audit-events-modal.component.css'],
})
export class AuditEventsModalComponent implements OnChanges {
  @Input() caseId: number | null = null;

  @Output() close = new EventEmitter<void>();

  events: AuditEventResponse[] = [];
  loading = false;
  error: string | null = null;

  /** Set of auditId for rows whose metadata is expanded */
  expandedMetadataIds = new Set<number>();

  constructor(
    private casesService: CasesService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnChanges(): void {
    if (this.caseId != null) {
      this.loadEvents();
      this.expandedMetadataIds.clear();
    } else {
      this.events = [];
      this.error = null;
      this.expandedMetadataIds.clear();
    }
  }

  loadEvents(): void {
    if (this.caseId == null) return;
    this.loading = true;
    this.error = null;
    this.cdr.detectChanges();
    this.casesService.getAuditEventsForCase(this.caseId).subscribe({
      next: (list) => {
        this.events = list ?? [];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err: HttpErrorResponse) => {
        this.error =
          err?.error?.message ??
          err?.message ??
          (err?.status ? `Request failed (${err.status})` : 'Failed to load audit events.');
        this.events = [];
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  formatDate(isoString: string | null): string {
    if (!isoString) return '-';
    const d = new Date(isoString);
    return isNaN(d.getTime()) ? isoString : d.toLocaleString();
  }

  hasMetadata(metadata: Record<string, unknown> | null): boolean {
    return !!metadata && Object.keys(metadata).length > 0;
  }

  formatMetadataPretty(metadata: Record<string, unknown> | null): string {
    if (!metadata || Object.keys(metadata).length === 0) return '';
    try {
      return JSON.stringify(metadata, null, 2);
    } catch {
      return JSON.stringify(metadata);
    }
  }

  isMetadataExpanded(auditId: number): boolean {
    return this.expandedMetadataIds.has(auditId);
  }

  toggleMetadataExpanded(auditId: number): void {
    if (this.expandedMetadataIds.has(auditId)) {
      this.expandedMetadataIds.delete(auditId);
    } else {
      this.expandedMetadataIds.add(auditId);
    }
    this.cdr.detectChanges();
  }

  onOverlayClick(): void {
    this.close.emit();
  }

  onClose(): void {
    this.close.emit();
  }
}
