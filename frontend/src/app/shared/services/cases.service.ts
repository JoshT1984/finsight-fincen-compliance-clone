import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environment/environment';

export type CaseStatus = 'OPEN' | 'REFERRED' | 'CLOSED';

/** Case response from the documents-cases-service API */
export interface CaseFileResponse {
  caseId: number;
  sarId: number | null;
  status: CaseStatus;
  createdAt: string;
  referredAt: string | null;
  closedAt: string | null;
  referredToAgency: string | null;
}

/** Audit event response from the documents-cases-service API */
export interface AuditEventResponse {
  auditId: number;
  actorUserId: string | null;
  action: string;
  entityType: string;
  entityId: string;
  metadata: Record<string, unknown>;
  createdAt: string;
}

/** Case note response from the documents-cases-service API */
export interface CaseNoteResponse {
  noteId: number;
  caseId: number;
  authorUserId: string | null;
  noteText: string;
  createdAt: string;
}

/** Request payload for creating a case note. */
export interface CreateCaseNoteRequest {
  caseId: number;
  authorUserId: string;
  noteText: string;
}

@Injectable({
  providedIn: 'root',
})
export class CasesService {
  private readonly apiUrl: string;
  private readonly auditEventsUrl: string;
  private readonly caseNotesUrl: string;

  constructor(private http: HttpClient) {
    const base = environment.documentsApiBaseUrl || '';
    const baseUrl = base ? base.replace(/\/$/, '') : '';
    this.apiUrl = baseUrl ? `${baseUrl}/api/cases` : '/api/cases';
    this.auditEventsUrl = baseUrl ? `${baseUrl}/api/audit-events` : '/api/audit-events';
    this.caseNotesUrl = baseUrl ? `${baseUrl}/api/case-notes` : '/api/case-notes';
  }

  getAll(): Observable<CaseFileResponse[]> {
    return this.http.get<CaseFileResponse[]>(this.apiUrl);
  }

  getById(caseId: number): Observable<CaseFileResponse> {
    return this.http.get<CaseFileResponse>(`${this.apiUrl}/${caseId}`);
  }

  refer(caseId: number, referredToAgency: string): Observable<CaseFileResponse> {
    return this.http.post<CaseFileResponse>(`${this.apiUrl}/${caseId}/refer`, {
      referredToAgency,
    });
  }

  close(caseId: number): Observable<CaseFileResponse> {
    return this.http.post<CaseFileResponse>(`${this.apiUrl}/${caseId}/close`, {});
  }

  /** Fetch audit events for a case (entityType CASE, entityId = caseId). */
  getAuditEventsForCase(caseId: number): Observable<AuditEventResponse[]> {
    return this.getAuditEventsForEntity('CASE', String(caseId));
  }

  /** Fetch audit events for any entity (e.g. CASE, DOCUMENT, CASE_NOTE). */
  getAuditEventsForEntity(entityType: string, entityId: string): Observable<AuditEventResponse[]> {
    return this.http.get<AuditEventResponse[]>(
      `${this.auditEventsUrl}/entity/${entityType}/${entityId}`,
    );
  }

  /** Fetch case notes for a case. */
  getCaseNotes(caseId: number): Observable<CaseNoteResponse[]> {
    return this.http.get<CaseNoteResponse[]>(`${this.caseNotesUrl}/case/${caseId}`);
  }

  /** Create a case note for a case. */
  createCaseNote(request: CreateCaseNoteRequest): Observable<CaseNoteResponse> {
    return this.http.post<CaseNoteResponse>(this.caseNotesUrl, request);
  }
}
