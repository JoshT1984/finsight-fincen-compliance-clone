import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environment/environment';

/** Organization response from the suspect-registry-service API */
export interface OrganizationResponse {
  orgId: number;
  name: string;
  type: string;
  createdAt: string;
}

/** Request body for creating an organization */
export interface CreateOrganizationRequest {
  name: string;
  type?: string | null;
}

/** Request body for updating an organization (all fields optional) */
export interface PatchOrganizationRequest {
  name?: string | null;
  type?: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class OrganizationService {
  private readonly apiBaseUrl: string;

  constructor(private http: HttpClient) {
    const base = environment.suspectApiBaseUrl || '';
    this.apiBaseUrl = base ? `${base.replace(/\/$/, '')}/api` : '/api';
  }

  getAll(): Observable<OrganizationResponse[]> {
    return this.http.get<OrganizationResponse[]>(`${this.apiBaseUrl}/organizations`);
  }

  getById(id: number): Observable<OrganizationResponse> {
    return this.http.get<OrganizationResponse>(`${this.apiBaseUrl}/organizations/${id}`);
  }

  create(request: CreateOrganizationRequest): Observable<OrganizationResponse> {
    return this.http.post<OrganizationResponse>(`${this.apiBaseUrl}/organizations`, request);
  }

  update(id: number, request: PatchOrganizationRequest): Observable<OrganizationResponse> {
    return this.http.patch<OrganizationResponse>(`${this.apiBaseUrl}/organizations/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/organizations/${id}`);
  }
}
