import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environment/environment';

export type EventType = 'CTR' | 'SAR';
export type EventStatus = string;

export interface ComplianceEventResponse {
  eventId: number;
  eventType: EventType;
  sourceSystem: string;
  sourceEntityId: string;
  eventTime: string;
  totalAmount: number | null;
  status: EventStatus;
  severityScore: number | null;
  createdAt: string;
  // Template compatibility for SARs
  sarId?: number; // alias for eventId
  subjectName?: string;
  suspicionScore?: number | null; // alias for severityScore
  updatedAt?: string; // alias for eventTime or createdAt
}

export interface ComplianceEventOption {
  id: number;
  label: string;
}

@Injectable({
  providedIn: 'root',
})
export class ComplianceService {
  private readonly apiUrl: string;

  constructor(private http: HttpClient) {
    const base = environment.complianceApiBaseUrl || '';
    this.apiUrl = base
      ? `${base.replace(/\/$/, '')}/api/compliance-events`
      : '/api/compliance-events';
  }

  /** Fetch compliance events (CTR or SAR) for table pages */
  getEvents(eventType: EventType, page = 0, size = 20): Observable<ComplianceEventResponse[]> {
    const params = new HttpParams().set('eventType', eventType).set('page', page).set('size', size);

    return this.http
      .get<
        { content?: ComplianceEventResponse[] } | ComplianceEventResponse[]
      >(this.apiUrl, { params })
      .pipe(map((res) => (Array.isArray(res) ? res : (res?.content ?? []))));
  }

  /** Fetch CTR events for dropdown options */
  getCtrOptions(): Observable<ComplianceEventOption[]> {
    return this.getEvents('CTR', 0, 100).pipe(map((list) => list.map((e) => this.toCtrOption(e))));
  }

  /** Fetch SAR events for dropdown options */
  getSarOptions(): Observable<ComplianceEventOption[]> {
    return this.getEvents('SAR', 0, 100).pipe(map((list) => list.map((e) => this.toSarOption(e))));
  }

  private toCtrOption(e: ComplianceEventResponse): ComplianceEventOption {
    const amount = e.totalAmount != null ? `$${Number(e.totalAmount).toLocaleString()}` : '—';
    const date = e.eventTime ? new Date(e.eventTime).toLocaleDateString() : '—';
    return { id: e.eventId, label: `CTR #${e.eventId} — ${amount} — ${date}` };
  }

  private toSarOption(e: ComplianceEventResponse): ComplianceEventOption {
    const severity = e.severityScore != null ? `Severity ${e.severityScore}` : '—';
    const date = e.eventTime ? new Date(e.eventTime).toLocaleDateString() : '—';
    return { id: e.eventId, label: `SAR #${e.eventId} — ${severity} — ${date}` };
  }
}
