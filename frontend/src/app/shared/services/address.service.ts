import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environment/environment';

/** Address response from the suspect-registry-service API */
export interface AddressResponse {
  addressId: number;
  line1: string;
  line2: string | null;
  city: string;
  state: string | null;
  postalCode: string | null;
  country: string;
  createdAt: string;
}

/** Linked address response (address with suspect link metadata) from GET /api/suspects/{id}/addresses */
export interface LinkedAddressResponse extends AddressResponse {
  addressType: string;
  isCurrent: boolean;
}

/** Request body for creating an address */
export interface CreateAddressRequest {
  line1: string;
  line2?: string | null;
  city: string;
  state?: string | null;
  postalCode?: string | null;
  country: string;
}

/** Request body for updating an address (all fields optional) */
export interface PatchAddressRequest {
  line1?: string | null;
  line2?: string | null;
  city?: string | null;
  state?: string | null;
  postalCode?: string | null;
  country?: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class AddressService {
  private readonly apiBaseUrl: string;

  constructor(private http: HttpClient) {
    const base = environment.suspectApiBaseUrl || '';
    this.apiBaseUrl = base ? `${base.replace(/\/$/, '')}/api` : '/api';
  }

  getAll(): Observable<AddressResponse[]> {
    return this.http.get<AddressResponse[]>(`${this.apiBaseUrl}/addresses`);
  }

  getById(id: number): Observable<AddressResponse> {
    return this.http.get<AddressResponse>(`${this.apiBaseUrl}/addresses/${id}`);
  }

  create(request: CreateAddressRequest): Observable<AddressResponse> {
    return this.http.post<AddressResponse>(`${this.apiBaseUrl}/addresses`, request);
  }

  update(id: number, request: PatchAddressRequest): Observable<AddressResponse> {
    return this.http.patch<AddressResponse>(`${this.apiBaseUrl}/addresses/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/addresses/${id}`);
  }
}
