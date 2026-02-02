import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environment/environment';

/** Document response from the documents-cases-service API */
export interface DocumentResponse {
  documentId: number;
  documentType: 'CTR' | 'SAR' | 'CASE';
  fileName: string;
  storagePath: string;
  uploadedAt: string;
  ctrId: number | null;
  sarId: number | null;
  caseId: number | null;
}

export interface CaseFileResponse {
  caseId: number;
  sarId: number | null;
  status: string;
  createdAt: string;
  referredAt: string | null;
  closedAt: string | null;
  referredToAgency: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class DocumentsService {
  private readonly apiUrl: string;
  private readonly casesUrl: string;

  constructor(private http: HttpClient) {
    const base = environment.documentsApiBaseUrl || '';
    const baseClean = base ? base.replace(/\/$/, '') : '';
    this.apiUrl = baseClean ? `${baseClean}/api/documents` : '/api/documents';
    this.casesUrl = baseClean ? `${baseClean}/api/cases` : '/api/cases';
  }

  /** Fetch all cases for dropdown options */
  getCases(): Observable<CaseFileResponse[]> {
    return this.http.get<CaseFileResponse[]>(this.casesUrl);
  }

  /** Fetch all documents from the API */
  getAll(): Observable<DocumentResponse[]> {
    return this.http.get<DocumentResponse[]>(this.apiUrl);
  }

  /** Fetch documents by CTR ID */
  getByCtrId(ctrId: number): Observable<DocumentResponse[]> {
    return this.http.get<DocumentResponse[]>(`${this.apiUrl}/ctr/${ctrId}`);
  }

  /** Fetch documents by SAR ID */
  getBySarId(sarId: number): Observable<DocumentResponse[]> {
    return this.http.get<DocumentResponse[]>(`${this.apiUrl}/sar/${sarId}`);
  }

  /** Fetch documents by Case ID */
  getByCaseId(caseId: number): Observable<DocumentResponse[]> {
    return this.http.get<DocumentResponse[]>(`${this.apiUrl}/case/${caseId}`);
  }

  /** Fetch presigned S3 download URL for a document */
  getDownloadUrl(documentId: number): Observable<{ downloadUrl: string }> {
    return this.http.get<{ downloadUrl: string }>(`${this.apiUrl}/${documentId}/download-url`);
  }

  /** Upload a document */
  upload(
    file: File,
    documentType: 'CTR' | 'SAR' | 'CASE',
    ctrId?: number,
    sarId?: number,
    caseId?: number
  ): Observable<DocumentResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('documentType', documentType);
    if (ctrId != null) formData.append('ctrId', String(ctrId));
    if (sarId != null) formData.append('sarId', String(sarId));
    if (caseId != null) formData.append('caseId', String(caseId));
    return this.http.post<DocumentResponse>(`${this.apiUrl}/upload`, formData);
  }
}
