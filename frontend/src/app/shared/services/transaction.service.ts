import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
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

@Injectable({
  providedIn: 'root',
})
export class TransactionService {
  private readonly apiUrl: string;

  constructor(private http: HttpClient) {
    const base = environment.complianceApiBaseUrl || '';
    this.apiUrl = base ? `${base.replace(/\/$/, '')}/api/transactions` : '/api/transactions';
  }

  createTransaction(req: CreateTransactionRequest): Observable<any> {
    const isoTxnTime = new Date(req.txnTime).toISOString();
    return this.http.post<any>(this.apiUrl, { ...req, txnTime: isoTxnTime });
  }
}
