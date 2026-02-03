import { ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { CasesService, CaseNoteResponse } from '../services/cases.service';

@Component({
  selector: 'app-case-notes-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './case-notes-modal.component.html',
  styleUrls: ['./case-notes-modal.component.css'],
})
export class CaseNotesModalComponent implements OnChanges {
  @Input() caseId: number | null = null;

  @Output() close = new EventEmitter<void>();

  notes: CaseNoteResponse[] = [];
  loading = false;
  error: string | null = null;

  constructor(
    private casesService: CasesService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnChanges(): void {
    if (this.caseId != null) {
      this.loadNotes();
    } else {
      this.notes = [];
      this.error = null;
    }
  }

  loadNotes(): void {
    if (this.caseId == null) return;
    this.loading = true;
    this.error = null;
    this.cdr.detectChanges();
    this.casesService.getCaseNotes(this.caseId).subscribe({
      next: (list) => {
        this.notes = [...(list ?? [])].sort(
          (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
        );
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err: HttpErrorResponse) => {
        this.error =
          err?.error?.message ??
          err?.message ??
          (err?.status ? `Request failed (${err.status})` : 'Failed to load case notes.');
        this.notes = [];
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

  onOverlayClick(): void {
    this.close.emit();
  }

  onClose(): void {
    this.close.emit();
  }
}
