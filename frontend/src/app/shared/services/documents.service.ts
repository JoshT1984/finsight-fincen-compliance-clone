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

@Injectable({
  providedIn: 'root',
})
export class DocumentsService {
  private readonly apiUrl: string;

  constructor(private http: HttpClient) {
    const base = environment.documentsApiBaseUrl || '';
    this.apiUrl = base ? `${base.replace(/\/$/, '')}/api/documents` : '/api/documents';
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
}
