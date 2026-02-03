import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { DocumentsService, DocumentResponse } from '../../../shared/services/documents.service';

@Component({
  selector: 'app-case-detail-documents',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './case-detail-documents.component.html',
  styleUrls: ['./case-detail-documents.component.css'],
})
export class CaseDetailDocumentsComponent implements OnInit {
  caseId = 0;
  documents: DocumentResponse[] = [];
  loading = false;
  error: string | null = null;
  downloadingId: number | null = null;

  constructor(
    private route: ActivatedRoute,
    private documentsService: DocumentsService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const id = this.route.parent?.snapshot.paramMap.get('id');
    this.caseId = id ? Number(id) : 0;
    if (this.caseId) this.loadDocuments();
  }

  loadDocuments(): void {
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
}
