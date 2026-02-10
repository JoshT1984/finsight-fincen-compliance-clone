import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';

import { ComplianceEventDto } from '../models/compliance-event-dto.interface';
import { CtrDetailResponse } from '../models/ctr-detail.model';

@Injectable({ providedIn: 'root' })
export class ComplianceEventsService {
  private readonly apiUrl: string;

  constructor(private http: HttpClient) {
    const base = environment.complianceApiBaseUrl || '';
    this.apiUrl = base
      ? `${base.replace(/\/$/, '')}/api/compliance-events`
      : '/api/compliance-events';
  }

  /**
   * CTR list (paged)
   * GET /api/compliance-events?eventType=CTR&page=0&size=200
   */
  getCtrEvents(
    page: number,
    size: number,
  ): Observable<{
    content: ComplianceEventDto[];
    totalElements: number;
    number: number;
    size: number;
  }> {
    const params = new HttpParams()
      .set('eventType', 'CTR')
      .set('page', String(page))
      .set('size', String(size));

    return this.http.get<{
      content: ComplianceEventDto[];
      totalElements: number;
      number: number;
      size: number;
    }>(this.apiUrl, { params });
  }

  /**
   * CTR detail
   * GET /api/compliance-events/{eventId}/ctr-detail
   */
  getCtrDetail(eventId: number): Observable<CtrDetailResponse> {
    return this.http.get<CtrDetailResponse>(`${this.apiUrl}/${eventId}/ctr-detail`);
  }

  /**
   * Events linked to a suspect (paged)
   * Expected by suspect-detail.component.ts
   * GET /api/compliance-events/by-suspect/{suspectId}?page=0&size=200
   */
  getBySuspectId(
    suspectId: number,
    page = 0,
    size = 200,
  ): Observable<{
    content: ComplianceEventDto[];
    totalElements: number;
    number: number;
    size: number;
  }> {
    const params = new HttpParams().set('page', String(page)).set('size', String(size));

    return this.http.get<{
      content: ComplianceEventDto[];
      totalElements: number;
      number: number;
      size: number;
    }>(`${this.apiUrl}/by-suspect/${suspectId}`, { params });
  }

  /**
   * Linkable events (not currently linked to suspect), filtered by type (CTR/SAR)
   * Expected by suspect-detail.component.ts
   * GET /api/compliance-events/linkable?eventType=CTR&excludeSuspectId=123&page=0&size=200
   */
  getLinkableByEventType(
    eventType: 'CTR' | 'SAR',
    excludeSuspectId: number,
    page = 0,
    size = 200,
  ): Observable<{
    content: ComplianceEventDto[];
    totalElements: number;
    number: number;
    size: number;
  }> {
    const params = new HttpParams()
      .set('eventType', eventType)
      .set('excludeSuspectId', String(excludeSuspectId))
      .set('page', String(page))
      .set('size', String(size));

    return this.http.get<{
      content: ComplianceEventDto[];
      totalElements: number;
      number: number;
      size: number;
    }>(`${this.apiUrl}/linkable`, { params });
  }

  /**
   * Link event to suspect
   * POST /api/compliance-events/{eventId}/link-suspect/{suspectId}
   */
  linkEventToSuspect(eventId: number, suspectId: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${eventId}/link-suspect/${suspectId}`, {});
  }

  /**
   * Unlink event from suspect
   * DELETE /api/compliance-events/{eventId}/unlink-suspect
   */
  unlinkEventFromSuspect(eventId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${eventId}/unlink-suspect`);
  }

  /**
   * Auto-generate a SAR draft from an existing CTR event.
   * POST /api/compliance-events/{ctrEventId}/generate-sar
   */
  generateSarFromCtr(ctrEventId: number): Observable<ComplianceEventDto> {
    return this.http.post<ComplianceEventDto>(`${this.apiUrl}/${ctrEventId}/generate-sar`, {});
  }
}
