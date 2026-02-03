import { ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { CasesService, AuditEventResponse } from '../services/cases.service';
import { DocumentsService } from '../services/documents.service';

/** Audit event with a display label for which entity it refers to (Case vs Document). */
export type AuditEventWithEntity = AuditEventResponse & { entityLabel: string };

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

  events: AuditEventWithEntity[] = [];
  loading = false;
  error: string | null = null;

  /** Set of auditId for rows whose metadata is expanded */
  expandedMetadataIds = new Set<number>();

  constructor(
    private casesService: CasesService,
    private documentsService: DocumentsService,
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

    const caseEvents$ = this.casesService.getAuditEventsForCase(this.caseId).pipe(
      map((list) => (list ?? []).map((e) => ({ ...e, entityLabel: 'Case' } as AuditEventWithEntity))),
    );
    const documents$ = this.documentsService.getByCaseId(this.caseId);

    forkJoin({ caseEvents: caseEvents$, documents: documents$ }).pipe(
      switchMap(({ caseEvents, documents }) => {
        const docList = documents ?? [];
        if (docList.length === 0) {
          const merged = [...caseEvents].sort(
            (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
          );
          return of(merged);
        }
        const documentEvents$ = docList.map((doc) =>
          this.casesService
            .getAuditEventsForEntity('DOCUMENT', String(doc.documentId))
            .pipe(
              map((list) =>
                (list ?? []).map((e) =>
                  ({ ...e, entityLabel: `Document: ${doc.fileName}` } as AuditEventWithEntity),
                ),
              ),
            ),
        );
        return forkJoin(documentEvents$).pipe(
          map((arrays) => {
            const all = [...caseEvents, ...arrays.flat()];
            return all.sort(
              (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
            );
          }),
        );
      }),
    ).subscribe({
      next: (merged) => {
        this.events = merged;
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
