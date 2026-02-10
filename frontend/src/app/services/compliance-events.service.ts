import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';

import { ComplianceEventDto } from '../models/compliance-event-dto.interface';
import { CtrDetailResponse } from '../models/ctr-detail.model';

type PageResponse<T> = {
  content: T[];
  totalElements?: number;
  totalPages?: number;
  number?: number;
  size?: number;
};

@Injectable({ providedIn: 'root' })
export class ComplianceEventsService {
  // ✅ Correct base URL from your environment
  private readonly apiUrl = `${environment.complianceApiBaseUrl}/api/compliance-events`;

  constructor(private http: HttpClient) {}

  // ====================================================
  // Generic search (matches backend controller query params)
  // GET /api/compliance-events?eventType=CTR|SAR&suspectId=...&notLinkedToSuspectId=...&page=...&size=...
  // ====================================================
  search(
    params: {
      eventType?: 'CTR' | 'SAR';
      suspectId?: number;
      notLinkedToSuspectId?: number;
      page?: number;
      size?: number;
    } = {},
  ): Observable<PageResponse<ComplianceEventDto>> {
    let httpParams = new HttpParams();

    if (params.eventType) {
      httpParams = httpParams.set('eventType', params.eventType);
    }
    if (params.suspectId != null) {
      httpParams = httpParams.set('suspectId', String(params.suspectId));
    }
    if (params.notLinkedToSuspectId != null) {
      httpParams = httpParams.set('notLinkedToSuspectId', String(params.notLinkedToSuspectId));
    }
    if (params.page != null) {
      httpParams = httpParams.set('page', String(params.page));
    }
    if (params.size != null) {
      httpParams = httpParams.set('size', String(params.size));
    }

    return this.http.get<PageResponse<ComplianceEventDto>>(this.apiUrl, {
      params: httpParams,
    });
  }

  // ====================================================
  // CTR convenience method (used by CTRs feature)
  // ====================================================
  getCtrEvents(page = 0, size = 200): Observable<PageResponse<ComplianceEventDto>> {
    return this.search({
      eventType: 'CTR',
      page,
      size,
    });
  }

  // ====================================================
  // SAR convenience method (optional, future-safe)
  // ====================================================
  getSarEvents(page = 0, size = 200): Observable<PageResponse<ComplianceEventDto>> {
    return this.search({
      eventType: 'SAR',
      page,
      size,
    });
  }

  /**
   * Backend: GET /api/compliance-events?suspectId=123&page=0&size=200
   */
  getBySuspectId(
    suspectId: number,
    page = 0,
    size = 200,
  ): Observable<PageResponse<ComplianceEventDto>> {
    return this.search({
      suspectId,
      page,
      size,
    });
  }

  /**
   * Backend: GET /api/compliance-events?eventType=CTR&notLinkedToSuspectId=123&page=0&size=200
   */
  getLinkableByEventType(
    eventType: 'CTR' | 'SAR',
    excludeSuspectId: number,
    page = 0,
    size = 200,
  ): Observable<PageResponse<ComplianceEventDto>> {
    return this.search({
      eventType,
      notLinkedToSuspectId: excludeSuspectId,
      page,
      size,
    });
  }

  /**
   * Backend: PUT /api/compliance-events/{eventId}/suspect
   * Body: { "suspectId": 123 }
   */
  linkEventToSuspect(eventId: number, suspectId: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${eventId}/suspect`, { suspectId });
  }

  /**
   * Backend: DELETE /api/compliance-events/{eventId}/suspect
   */
  unlinkEventFromSuspect(eventId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${eventId}/suspect`);
  }

  /**
   * Optional: CTR detail endpoint (keep only if backend supports it)
   */
  getCtrDetail(eventId: number): Observable<CtrDetailResponse> {
    return this.http.get<CtrDetailResponse>(`${this.apiUrl}/${eventId}/ctr-detail`);
  }

  generateSarFromCtr(ctrId: number): Observable<void> {
  // Default guess based on your existing service base:
  // {complianceApiBaseUrl}/api/compliance-events/ctrs/{ctrId}/generate-sar
  return this.http.post<void>(`${this.apiUrl}/ctrs/${ctrId}/generate-sar`, {});
  }
  
  
}
