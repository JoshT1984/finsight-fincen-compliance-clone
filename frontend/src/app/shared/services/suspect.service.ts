import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environment/environment';
import { AddressResponse, LinkedAddressResponse } from './address.service';

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

/** Request body for creating a suspect */
export interface CreateSuspectRequest {
  primaryName: string;
  dob?: string | null;
  ssn?: string | null;
  riskLevel?: string | null;
}

/** Request body for updating a suspect (all fields optional) */
export interface PatchSuspectRequest {
  primaryName?: string | null;
  dob?: string | null;
  ssn?: string | null;
  riskLevel?: string | null;
}

/** Alias response from the suspect-registry-service API (alias tied to a suspect) */
export interface AliasResponse {
  aliasId: number;
  suspectId: number;
  aliasName: string;
  aliasType: string;
  isPrimary: boolean | null;
  createdAt: string;
}

/** Request body for creating an alias (links to suspect) */
export interface CreateAliasRequest {
  suspectId: number;
  aliasName: string;
  aliasType?: string | null;
  isPrimary?: boolean | null;
}

/** Request body for updating an alias (all fields optional) */
export interface PatchAliasRequest {
  suspectId?: number | null;
  aliasName?: string | null;
  aliasType?: string | null;
  isPrimary?: boolean | null;
}

/** Organization linked to a suspect (from GET /api/suspects/{id}/organizations) */
export interface LinkedOrganizationResponse {
  orgId: number;
  name: string;
  type: string;
  role: string | null;
  linkedAt: string;
}

/** Request body for linking a suspect to an organization */
export interface LinkSuspectOrganizationRequest {
  orgId: number;
  role?: string | null;
}

/** Request body for linking an address to a suspect */
export interface LinkSuspectAddressRequest {
  addressId: number;
  addressType?: string | null;
  isCurrent?: boolean | null;
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

  /** Create a new suspect */
  create(request: CreateSuspectRequest): Observable<SuspectResponse> {
    return this.http.post<SuspectResponse>(`${this.apiBaseUrl}/suspects`, request);
  }

  /** Update a suspect by ID */
  update(id: number, request: PatchSuspectRequest): Observable<SuspectResponse> {
    return this.http.patch<SuspectResponse>(`${this.apiBaseUrl}/suspects/${id}`, request);
  }

  /** Delete a suspect by ID */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/suspects/${id}`);
  }

  /** Fetch addresses linked to a suspect */
  getAddressesBySuspectId(suspectId: number): Observable<LinkedAddressResponse[]> {
    return this.http.get<LinkedAddressResponse[]>(`${this.apiBaseUrl}/suspects/${suspectId}/addresses`);
  }

  /** Fetch aliases linked to a suspect */
  getAliasesBySuspectId(suspectId: number): Observable<AliasResponse[]> {
    return this.http.get<AliasResponse[]>(`${this.apiBaseUrl}/suspects/${suspectId}/aliases`);
  }

  /** Create an alias (links it to the suspect) */
  createAlias(request: CreateAliasRequest): Observable<AliasResponse> {
    return this.http.post<AliasResponse>(`${this.apiBaseUrl}/aliases`, request);
  }

  /** Update an alias by ID */
  updateAlias(aliasId: number, request: PatchAliasRequest): Observable<AliasResponse> {
    return this.http.patch<AliasResponse>(`${this.apiBaseUrl}/aliases/${aliasId}`, request);
  }

  /** Delete an alias by ID */
  deleteAlias(aliasId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/aliases/${aliasId}`);
  }

  /** Fetch organizations linked to a suspect */
  getOrganizationsBySuspectId(suspectId: number): Observable<LinkedOrganizationResponse[]> {
    return this.http.get<LinkedOrganizationResponse[]>(`${this.apiBaseUrl}/suspects/${suspectId}/linked-organizations`);
  }

  /** Link an organization to a suspect */
  linkSuspectToOrganization(
    suspectId: number,
    request: LinkSuspectOrganizationRequest,
  ): Observable<SuspectResponse> {
    return this.http.post<SuspectResponse>(`${this.apiBaseUrl}/suspects/${suspectId}/organizations`, request);
  }

  /** Link an address to a suspect */
  linkAddressToSuspect(
    suspectId: number,
    request: LinkSuspectAddressRequest,
  ): Observable<SuspectResponse> {
    return this.http.post<SuspectResponse>(`${this.apiBaseUrl}/suspects/${suspectId}/addresses`, request);
  }
}
