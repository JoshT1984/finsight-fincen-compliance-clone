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
  private readonly apiUrl = `${environment.complianceApiBaseUrl}/api/compliance-events`;

  constructor(private http: HttpClient) {}

  // ====================================================
  // Generic search
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
  // CTR Events
  // ====================================================
  getCtrEvents(page: number = 0, size: number = 200): Observable<PageResponse<ComplianceEventDto>> {
    return this.search({
      eventType: 'CTR',
      page,
      size,
    });
  }

  // ====================================================
  // SAR Events
  // ====================================================
  getSarEvents(page: number = 0, size: number = 200): Observable<PageResponse<ComplianceEventDto>> {
    return this.search({
      eventType: 'SAR',
      page,
      size,
    });
  }

  // ====================================================
  // Events by Suspect
  // ====================================================
  getBySuspectId(
    suspectId: number,
    page: number = 0,
    size: number = 200,
  ): Observable<PageResponse<ComplianceEventDto>> {
    return this.search({
      suspectId,
      page,
      size,
    });
  }

  // ====================================================
  // Linkable Events
  // ====================================================
  getLinkableByEventType(
    eventType: 'CTR' | 'SAR',
    excludeSuspectId: number,
    page: number = 0,
    size: number = 200,
  ): Observable<PageResponse<ComplianceEventDto>> {
    return this.search({
      eventType,
      notLinkedToSuspectId: excludeSuspectId,
      page,
      size,
    });
  }

  // ====================================================
  // Link / Unlink
  // ====================================================
  linkEventToSuspect(eventId: number, suspectId: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${eventId}/suspect`, { suspectId });
  }

  unlinkEventFromSuspect(eventId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${eventId}/suspect`);
  }

  // ====================================================
  // Event by ID
  // ====================================================
  getEventById(eventId: number): Observable<ComplianceEventDto> {
    return this.http.get<ComplianceEventDto>(`${this.apiUrl}/${eventId}`);
  }

  // ====================================================
  // CTR Detail
  // ====================================================
  getCtrDetail(eventId: number): Observable<CtrDetailResponse> {
    return this.http.get<CtrDetailResponse>(`${this.apiUrl}/${eventId}/ctr-detail`);
  }

  // ====================================================
  // Manual SAR Generation
  // ====================================================
  generateSarFromCtr(ctrId: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/ctrs/${ctrId}/generate-sar`, {});
  }
}
