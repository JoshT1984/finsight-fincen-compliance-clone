import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ComplianceEventResponse } from './compliance.service';
import { environment } from '../../../environment/environment';

@Injectable({ providedIn: 'root' })
export class ComplianceEventsService {
  private readonly apiUrl: string;

  constructor(private http: HttpClient) {
    const base = environment.complianceApiBaseUrl || '';
    this.apiUrl = base
      ? `${base.replace(/\/$/, '')}/api/compliance-events`
      : '/api/compliance-events';
  }

  getCtrEvents(page: number, size: number): Observable<any> {
    const params = new HttpParams()
      .set('eventType', 'CTR')
      .set('page', String(page))
      .set('size', String(size));

    return this.http.get<any>(this.apiUrl, { params });
  }

  getCtrDetail(eventId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${eventId}/ctr-detail`);
  }

  /**
   * Get all compliance events for a suspect by ID (returns a paged response)
   */
  getBySuspectId(suspectId: number): Observable<{ content: ComplianceEventResponse[] }> {
    const params = new HttpParams().set('suspectId', String(suspectId));
    return this.http.get<{ content: ComplianceEventResponse[] }>(this.apiUrl, { params });
  }

  /**
   * Get linkable events by type for a suspect (returns a paged response)
   */
  getLinkableByEventType(
    eventType: string,
    suspectId: number,
  ): Observable<{ content: ComplianceEventResponse[] }> {
    const params = new HttpParams()
      .set('eventType', eventType)
      .set('linkableForSuspectId', String(suspectId));
    return this.http.get<{ content: ComplianceEventResponse[] }>(this.apiUrl, { params });
  }

  /**
   * Link an event to a suspect
   */
  linkEventToSuspect(eventId: number, suspectId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${eventId}/link-suspect`, { suspectId });
  }

  /**
   * Unlink an event from a suspect
   */
  unlinkEventFromSuspect(eventId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${eventId}/unlink-suspect`, {});
  }
}
