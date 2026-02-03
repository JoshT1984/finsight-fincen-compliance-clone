import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { CasesService, CaseNoteResponse } from '../../../shared/services/cases.service';

@Component({
  selector: 'app-case-detail-notes',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './case-detail-notes.component.html',
  styleUrls: ['./case-detail-notes.component.css'],
})
export class CaseDetailNotesComponent implements OnInit {
  caseId = 0;
  notes: CaseNoteResponse[] = [];
  loading = false;
  error: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private casesService: CasesService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const id = this.route.parent?.snapshot.paramMap.get('id');
    this.caseId = id ? Number(id) : 0;
    if (this.caseId) this.loadNotes();
  }

  loadNotes(): void {
    this.loading = true;
    this.error = null;
    this.cdr.detectChanges();
    this.casesService.getCaseNotes(this.caseId).subscribe({
      next: (list) => {
        this.notes = [...(list ?? [])].sort(
          (a, b) =>
            new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
        );
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err: HttpErrorResponse) => {
        this.error =
          err?.error?.message ??
          err?.message ??
          (err?.status ? `Request failed (${err.status})` : 'Failed to load notes.');
        this.notes = [];
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
}
