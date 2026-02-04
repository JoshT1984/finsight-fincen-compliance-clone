import { ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { DocumentsService, DocumentResponse } from '../services/documents.service';

@Component({
  selector: 'app-case-documents-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './case-documents-modal.component.html',
  styleUrls: ['./case-documents-modal.component.css'],
})
export class CaseDocumentsModalComponent implements OnChanges {
  @Input() caseId: number | null = null;

  @Output() close = new EventEmitter<void>();

  documents: DocumentResponse[] = [];
  loading = false;
  error: string | null = null;
  downloadingId: number | null = null;

  constructor(
    private documentsService: DocumentsService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnChanges(): void {
    if (this.caseId != null) {
      this.loadDocuments();
    } else {
      this.documents = [];
      this.error = null;
    }
  }

  loadDocuments(): void {
    if (this.caseId == null) return;
    this.loading = true;
    this.error = null;
    this.cdr.detectChanges();
    this.documentsService.getByCaseId(this.caseId).subscribe({
      next: (list) => {
        this.documents = list ?? [];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err: HttpErrorResponse) => {
        this.error =
          err?.error?.message ??
          err?.message ??
          (err?.status ? `Request failed (${err.status})` : 'Failed to load documents.');
        this.documents = [];
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  formatDate(isoString: string | null): string {
    if (!isoString) return '—';
    const d = new Date(isoString);
    return isNaN(d.getTime()) ? isoString : d.toLocaleString();
  }

  downloadDocument(doc: DocumentResponse): void {
    if (this.downloadingId != null) return;
    this.downloadingId = doc.documentId;
    this.cdr.detectChanges();
    this.documentsService.getDownloadUrl(doc.documentId).subscribe({
      next: (res) => {
        if (res?.downloadUrl) {
          window.open(res.downloadUrl, '_blank', 'noopener,noreferrer');
        }
        this.downloadingId = null;
        this.cdr.detectChanges();
      },
      error: () => {
        this.downloadingId = null;
        this.cdr.detectChanges();
      },
    });
  }

  onOverlayClick(): void {
    this.close.emit();
  }

  onClose(): void {
    this.close.emit();
  }
}
