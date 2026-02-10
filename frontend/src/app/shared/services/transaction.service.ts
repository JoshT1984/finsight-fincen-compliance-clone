import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environment/environment';

export interface CreateTransactionRequest {
  sourceSystem: string;
  sourceTxnId: string;
  externalSubjectKey: string;
  sourceSubjectType: string;
  sourceSubjectId: string;
  subjectName: string;
  txnTime: string;
  cashIn: number;
  cashOut: number;
  currency: string;
  channel: string;
  location: string;
}

export interface TransactionResponse {
  txnId: number;
  externalSubjectKey: string | null;
  sourceSystem: string;
  sourceSubjectType: string;
  sourceSubjectId: string;
  subjectName: string;
  txnTime: string;
  cashIn: number;
  cashOut: number;
  createdAt: string;
  currency?: string;
  channel?: string;
  location?: string;
  subjectKey?: string; // computed client-side
}

@Injectable({
  providedIn: 'root',
})
export class TransactionService {
  private readonly apiUrl: string;

  constructor(private http: HttpClient) {
    const base = environment.complianceApiBaseUrl || '';
    this.apiUrl = base ? `${base.replace(/\/$/, '')}/api/transactions` : '/api/transactions';
  }

  getTransactions(page = 0, size = 200): Observable<TransactionResponse[]> {
    const params = { page, size } as any;
    return this.http
      .get<{ content?: TransactionResponse[] } | TransactionResponse[]>(this.apiUrl, { params })
      .pipe(
        // Accept either Spring Page or raw array
        map((res: any) => (Array.isArray(res) ? res : (res?.content ?? []))),
      );
  }

  createTransaction(req: CreateTransactionRequest): Observable<any> {
    const isoTxnTime = new Date(req.txnTime).toISOString();
    return this.http.post<any>(this.apiUrl, { ...req, txnTime: isoTxnTime });
  }
}
