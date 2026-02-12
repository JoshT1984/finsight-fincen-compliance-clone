import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { CasesService, CaseNoteResponse } from '../../../shared/services/cases.service';
import { IdentityService } from '../../../shared/services/identity.service';

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
  private lastRefreshToken: string | null = null;

  /** Cache of author userId -> display name (e.g. "Jane Doe") */
  authorDisplayMap: Record<string, string> = {};

  constructor(
    private route: ActivatedRoute,
    private casesService: CasesService,
    private identityService: IdentityService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const id = this.route.parent?.snapshot.paramMap.get('id');
    this.caseId = id ? Number(id) : 0;
    if (this.caseId) this.loadNotes();

    this.route.queryParamMap.subscribe((params) => {
      const refreshToken = params.get('noteRefresh');
      if (!refreshToken || refreshToken === this.lastRefreshToken) return;
      this.lastRefreshToken = refreshToken;
      if (this.caseId) this.loadNotes();
    });
  }

  loadNotes(): void {
    this.loading = true;
    this.error = null;
    this.authorDisplayMap = {};
    this.cdr.detectChanges();
    this.casesService.getCaseNotes(this.caseId).subscribe({
      next: (list) => {
        this.notes = [...(list ?? [])].sort(
          (a, b) =>
            new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
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
          this.authorDisplayMap[userId] = [profile.firstName, profile.lastName].filter(Boolean).join(' ').trim() || profile.email || userId;
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
}
