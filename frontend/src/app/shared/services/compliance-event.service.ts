import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environment/environment';

/** Compliance event (e.g. SAR) response including suspect snapshot when present. */
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
}
