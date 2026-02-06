import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environment/environment';

/** Compliance event (e.g. SAR, CTR) response including suspect snapshot when present. */
export interface ComplianceEventResponse {
  eventId: number;
  eventType: string;
  sourceSystem: string;
  sourceEntityId: string;
  eventTime: string;
  totalAmount: number | null;
  status: string | null;
  severityScore: number | null;
  createdAt: string;
  suspectId: number | null;
  suspectMinimal: Record<string, unknown> | null;
}

/** Spring Page response for compliance events */
export interface ComplianceEventPageResponse {
  content: ComplianceEventResponse[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({
  providedIn: 'root',
})
export class ComplianceEventService {
  private readonly apiUrl: string;

  constructor(private http: HttpClient) {
    const base = environment.complianceApiBaseUrl || '';
    this.apiUrl = base ? `${base.replace(/\/$/, '')}/api/compliance-events` : '/api/compliance-events';
  }

  getById(eventId: number): Observable<ComplianceEventResponse> {
    return this.http.get<ComplianceEventResponse>(`${this.apiUrl}/${eventId}`);
  }

  /** Fetch compliance events (CTRs, SARs) linked to a suspect. */
  getBySuspectId(suspectId: number, page = 0, size = 50): Observable<ComplianceEventPageResponse> {
    const params = new HttpParams()
      .set('suspectId', String(suspectId))
      .set('page', String(page))
      .set('size', String(size));
    return this.http.get<ComplianceEventPageResponse>(this.apiUrl, { params });
  }

  /** Fetch events of type CTR or SAR that can be linked to a suspect (not already linked to them). */
  getLinkableByEventType(
    eventType: 'CTR' | 'SAR',
    notLinkedToSuspectId: number,
    page = 0,
    size = 100
  ): Observable<ComplianceEventPageResponse> {
    const params = new HttpParams()
      .set('eventType', eventType)
      .set('notLinkedToSuspectId', String(notLinkedToSuspectId))
      .set('page', String(page))
      .set('size', String(size));
    return this.http.get<ComplianceEventPageResponse>(this.apiUrl, { params });
  }

  /** Link a compliance event (CTR or SAR) to a suspect. */
  linkEventToSuspect(eventId: number, suspectId: number): Observable<ComplianceEventResponse> {
    return this.http.put<ComplianceEventResponse>(`${this.apiUrl}/${eventId}/suspect`, {
      suspectId,
    });
  }

  /** Remove the suspect link from a compliance event. */
  unlinkEventFromSuspect(eventId: number): Observable<ComplianceEventResponse> {
    return this.http.delete<ComplianceEventResponse>(`${this.apiUrl}/${eventId}/suspect`);
  }
}
