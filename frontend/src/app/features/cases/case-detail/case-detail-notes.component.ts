import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { CasesService, CaseNoteResponse } from '../../../shared/services/cases.service';
import { IdentityService } from '../../../shared/services/identity.service';
import { RoleService } from '../../../shared/services/role.service';

@Component({
  selector: 'app-case-detail-notes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './case-detail-notes.component.html',
  styleUrls: ['./case-detail-notes.component.css'],
})
export class CaseDetailNotesComponent implements OnInit {
  caseId = 0;
  notes: CaseNoteResponse[] = [];
  loading = false;
  error: string | null = null;

  /** Cache of author userId -> display name (e.g. "Jane Doe") */
  authorDisplayMap: Record<string, string> = {};

  newNoteText = '';
  editingNoteId: number | null = null;
  editText = '';
  saving = false;

  constructor(
    private route: ActivatedRoute,
    private casesService: CasesService,
    private identityService: IdentityService,
    private roleService: RoleService,
    private cdr: ChangeDetectorRef,
  ) {}

  canEdit(): boolean {
    return this.roleService.isAnalyst();
  }

  ngOnInit(): void {
    const id = this.route.parent?.snapshot.paramMap.get('id');
    this.caseId = id ? Number(id) : 0;
    if (this.caseId) this.loadNotes();
  }

  loadNotes(): void {
    this.loading = true;
    this.error = null;
    this.authorDisplayMap = {};
    this.cdr.detectChanges();
    this.casesService.getCaseNotes(this.caseId).subscribe({
      next: (list) => {
        this.notes = [...(list ?? [])].sort(
          (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
        );
        this.loadAuthorNames();
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

  /** Resolve author user IDs to display names via identity service. */
  private loadAuthorNames(): void {
    const userIds = [...new Set(this.notes.map((n) => n.authorUserId).filter(Boolean))] as string[];
    userIds.forEach((userId) => {
      if (this.authorDisplayMap[userId]) return;
      this.identityService.getUserProfile(userId).subscribe({
        next: (profile) => {
          this.authorDisplayMap[userId] =
            [profile.firstName, profile.lastName].filter(Boolean).join(' ').trim() ||
            profile.email ||
            userId;
          this.cdr.detectChanges();
        },
        error: () => {
          this.authorDisplayMap[userId] = userId;
          this.cdr.detectChanges();
        },
      });
    });
  }

  getAuthorDisplay(authorUserId: string | null): string {
    if (authorUserId == null || authorUserId === '') return '—';
    return this.authorDisplayMap[authorUserId] ?? authorUserId;
  }

  formatDate(isoString: string | null): string {
    if (!isoString) return '—';
    const d = new Date(isoString);
    return isNaN(d.getTime()) ? isoString : d.toLocaleString();
  }

  startEdit(note: CaseNoteResponse): void {
    if (!this.canEdit()) return;
    this.editingNoteId = note.noteId;
    this.editText = note.noteText ?? '';
    this.error = null;
    this.cdr.detectChanges();
  }

  cancelEdit(): void {
    this.editingNoteId = null;
    this.editText = '';
    this.cdr.detectChanges();
  }

  saveEdit(noteId: number): void {
    if (!this.canEdit()) return;
    const text = (this.editText ?? '').trim();
    if (!text) {
      this.error = 'Note text cannot be empty.';
      this.cdr.detectChanges();
      return;
    }

    this.saving = true;
    this.error = null;
    this.cdr.detectChanges();
    this.casesService.updateCaseNote(noteId, text).subscribe({
      next: () => {
        this.saving = false;
        this.editingNoteId = null;
        this.editText = '';
        this.loadNotes();
      },
      error: (err: HttpErrorResponse) => {
        this.saving = false;
        this.error = err?.error?.message ?? err?.message ?? 'Failed to update note.';
        this.cdr.detectChanges();
      },
    });
  }

  addNote(): void {
    if (!this.canEdit()) return;
    const text = (this.newNoteText ?? '').trim();
    if (!text) {
      this.error = 'Please enter a note.';
      this.cdr.detectChanges();
      return;
    }

    this.saving = true;
    this.error = null;
    this.cdr.detectChanges();
    this.casesService.createCaseNote(this.caseId, text).subscribe({
      next: () => {
        this.saving = false;
        this.newNoteText = '';
        this.loadNotes();
      },
      error: (err: HttpErrorResponse) => {
        this.saving = false;
        this.error = err?.error?.message ?? err?.message ?? 'Failed to add note.';
        this.cdr.detectChanges();
      },
    });
  }

  deleteNote(noteId: number): void {
    if (!this.canEdit()) return;
    if (!confirm('Delete this note? This cannot be undone.')) return;

    this.saving = true;
    this.error = null;
    this.cdr.detectChanges();
    this.casesService.deleteCaseNote(noteId).subscribe({
      next: () => {
        this.saving = false;
        this.loadNotes();
      },
      error: (err: HttpErrorResponse) => {
        this.saving = false;
        this.error = err?.error?.message ?? err?.message ?? 'Failed to delete note.';
        this.cdr.detectChanges();
      },
    });
  }
}
