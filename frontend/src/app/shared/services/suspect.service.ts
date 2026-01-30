import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environment/environment';

/** Suspect response from the suspect-registry-service API */
export interface SuspectResponse {
  suspectId: number;
  primaryName: string;
  dob: string | null;
  ssn?: string | null;
  riskLevel: string;
  createdAt: string;
  updatedAt: string;
}

@Injectable({
  providedIn: 'root',
})
export class SuspectService {
  private readonly apiBaseUrl: string;

  constructor(private http: HttpClient) {
    const base = environment.suspectApiBaseUrl || '';
    this.apiBaseUrl = base ? `${base.replace(/\/$/, '')}/api` : '/api';
  }

  /** Fetch all suspects from the API */
  getAll(): Observable<SuspectResponse[]> {
    return this.http.get<SuspectResponse[]>(`${this.apiBaseUrl}/suspects`);
  }

  /** Fetch suspect by ID */
  getById(id: number): Observable<SuspectResponse> {
    return this.http.get<SuspectResponse>(`${this.apiBaseUrl}/suspects/${id}`);
  }
}
