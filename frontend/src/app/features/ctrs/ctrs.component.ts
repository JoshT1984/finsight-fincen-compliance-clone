import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ComplianceEventsService } from '../../services/compliance-events.service';
import { TransactionService } from '../../shared/services/transaction.service';
import { TransactionFormComponent } from './transaction-form.component';

import { of } from 'rxjs';
import { catchError, finalize, map } from 'rxjs/operators';

type LooseRow = {
  [key: string]: any;
};

type TxnRowVm = {
  txnId: string;
  sourceSubjectType: string;
  subjectName: string;
  txnTime?: string | null;
  cashIn: number;
  cashOut: number;
  location: string;
};

type CtrRowVm = {
  ctrId: number | string;
  sourceSubjectType: string;
  subjectName: string;
  subjectId?: string | null;
  sourceLabel?: string; // optional: you can render this as its own column if you want
  suspicionScore: number | string;
  ctrTime?: string | null;
  status: string;
  amount: number;
  [key: string]: any;
};

@Component({
  selector: 'app-ctrs',
  standalone: true,
  imports: [CommonModule, FormsModule, TransactionFormComponent],
  templateUrl: './ctrs.component.html',
  styleUrls: ['./ctrs.component.css'],
})
export class CtrsComponent implements OnInit {
  ctrs: CtrRowVm[] = [];
  loadingCtrs = false;

  transactions: TxnRowVm[] = [];
  loadingTxns = false;
  txnsError: string | null = null;

  submitSuccess = false;
  submitError: string | null = null;
  submitting = false;
  justSubmittedLabel = false;

  search = '';
  showTransactionForm = false;

  // Map of subject/customer id -> friendly name (built from transactions)
  private transactionNameMap: Record<string, string> = {};

  constructor(
    private complianceEventsService: ComplianceEventsService,
    private transactionService: TransactionService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.refreshTransactions();
  }

  private pickFirstString(...values: unknown[]): string | null {
    for (const v of values) {
      if (typeof v === 'string' && v.trim().length > 0) return v;
      if (typeof v === 'number') return String(v);
    }
    return null;
  }

  private normalizeCustomerKey(id: string | null | undefined): string | null {
    if (!id) return null;
    const s = String(id).trim();
    if (!s) return null;

    if (s.toUpperCase().startsWith('CUST-')) return s.toUpperCase();
    if (/^\d+$/.test(s)) return `CUST-${s}`;
    return s;
  }

  private normalizeAndAddLookup(
    mapLookup: Record<string, string>,
    rawId: string | null,
    name: string,
  ): void {
    if (!rawId) return;
    const id = String(rawId).trim();
    if (!id) return;

    // store raw
    mapLookup[id] = name;

    // store canonical CUST-#### and digits-only variants
    const canon = this.normalizeCustomerKey(id);
    if (canon) mapLookup[canon] = name;

    if (canon && canon.toUpperCase().startsWith('CUST-')) {
      const digits = canon.substring(5);
      if (digits) mapLookup[digits] = name;
    } else if (/^\d+$/.test(id)) {
      mapLookup[`CUST-${id}`] = name;
    }
  }

  fetchCtrs(): void {
    this.loadingCtrs = true;

    this.complianceEventsService
      .search({ eventType: 'CTR', page: 0, size: 200 })
      .pipe(
        map((res: any) => (Array.isArray(res?.content) ? res.content : [])),
        map((rows: any[]) =>
          rows.map((ctrRaw: any): CtrRowVm => {
            const ctr = ctrRaw as any;

            const subjectType =
              this.pickFirstString(
                ctr?.subjectType,
                ctr?.sourceSubjectType,
                ctr?.source_subject_type,
                ctr?.['source_subject_type'],
              ) ?? '—';

            // 1) Determine Subject ID (we use this to lookup a friendly name from transactions)
            const subjectIdRaw =
              this.pickFirstString(
                ctr?.sourceEntityId,
                ctr?.customerId,
                ctr?.subjectId,
                ctr?.['source_entity_id'],
                ctr?.['customer_id'],
                ctr?.['subject_id'],
              ) ?? null;

            let subjectId = subjectIdRaw ? String(subjectIdRaw).trim() : null;

            // Sanitize (do not show CTR-#### or AGGREGATION as an "id")
            if (subjectId) {
              const up = subjectId.toUpperCase();
              if (up.startsWith('CTR-')) subjectId = null;
              if (up.includes('AGGREGATION')) subjectId = null;
            }

            const subjectIdCanon = this.normalizeCustomerKey(subjectId);

            // Optional source label (kept, but not used for subject name anymore)
            const sourceLabelRaw =
              this.pickFirstString(
                ctr?.eventSource,
                ctr?.source,
                ctr?.eventSubType,
                ctr?.subType,
                ctr?.aggregationType,
                ctr?.['event_source'],
                ctr?.['event_sub_type'],
                ctr?.['aggregation_type'],
              ) ?? '';

            const sourceLabel = (sourceLabelRaw || '').toUpperCase().includes('AGGREGATION')
              ? 'AGGREGATION'
              : '';

            // 2) SUBJECT NAME: ALWAYS try to pull from transactions FIRST
            let subjectName: string | null = null;
            const keyTry = subjectIdCanon ?? subjectId;

            if (keyTry && this.transactionNameMap[keyTry]) {
              subjectName = this.transactionNameMap[keyTry];
            }

            // 3) Fallback to CTR payload only if transaction lookup fails
            if (!subjectName) {
              subjectName =
                this.pickFirstString(
                  ctr?.customerName,
                  ctr?.subjectName,
                  ctr?.subject_name,
                  ctr?.['subject_name'],
                ) ?? null;
            }

            // 4) Final fallback
            if (!subjectName && subjectId) subjectName = subjectId;
            if (!subjectName) subjectName = '—';

            // Never allow AGGREGATION to become a name
            if (String(subjectName).toUpperCase().includes('AGGREGATION')) {
              subjectName = subjectId ?? '—';
            }

            return {
              ctrId: ctr?.eventId ?? ctr?.ctrId ?? '—',
              sourceSubjectType: subjectType,
              subjectName,
              subjectId: subjectIdCanon ?? subjectId ?? null,
              sourceLabel,
              suspicionScore: ctr?.severityScore != null ? ctr.severityScore : '-',
              ctrTime: ctr?.eventTime ?? null,
              status: ctr?.status ?? '—',
              amount: ctr?.totalAmount ?? 0,
            };
          }),
        ),
        catchError(() => of([] as CtrRowVm[])),
        finalize(() => {
          this.loadingCtrs = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe((mapped: CtrRowVm[]) => {
        this.ctrs = mapped;
      });
  }

  refreshTransactions(): void {
    this.loadingTxns = true;
    this.txnsError = null;

    this.transactionService
      .getTransactions(0, 200)
      .pipe(
        map((res: any) =>
          Array.isArray(res?.content) ? res.content : Array.isArray(res) ? res : [],
        ),
        catchError((err: unknown) => {
          console.error(err);
          const e = err as any;
          this.txnsError = e?.error?.message ?? e?.message ?? 'Failed to load transactions.';
          return of([] as LooseRow[]);
        }),
        finalize(() => {
          this.loadingTxns = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe((rows: any[]) => {
        // Normalize to a Transaction View Model so template has guaranteed fields
        const txnsVm: TxnRowVm[] = rows.map((tRaw: any) => {
          const t = tRaw as any;

          const txnId = String(t?.['txnId'] ?? t?.['id'] ?? '');
          const sourceSubjectType = String(
            this.pickFirstString(
              t?.['sourceSubjectType'],
              t?.['subject_type'],
              t?.['subjectType'],
              t?.['source_subject_type'],
            ) ?? '—',
          );

          const subjectName = String(
            this.pickFirstString(
              t?.['subjectName'],
              t?.['customerName'],
              t?.['fullName'],
              t?.['subject_name'],
              t?.['customer_name'],
              t?.['full_name'],
            ) ?? '—',
          );

          const txnTime = (t?.['txnTime'] ?? t?.['transactionTime'] ?? t?.['txn_time'] ?? null) as
            | string
            | null;

          const cashIn = Number(t?.['cashIn'] ?? t?.['cash_in'] ?? 0) || 0;
          const cashOut = Number(t?.['cashOut'] ?? t?.['cash_out'] ?? 0) || 0;

          const location = String(t?.['location'] ?? '—');

          return { txnId, sourceSubjectType, subjectName, txnTime, cashIn, cashOut, location };
        });

        this.transactions = txnsVm;

        // Build lookup map (subject/customer id -> friendly name), store multiple key variants
        const mapLookup: Record<string, string> = {};
        for (const tRaw of rows) {
          const t = tRaw as any;

          const idRaw =
            this.pickFirstString(
              t?.['sourceEntityId'],
              t?.['customerId'],
              t?.['subjectId'],
              t?.['source_entity_id'],
              t?.['customer_id'],
              t?.['subject_id'],
            ) ?? null;

          const name =
            this.pickFirstString(
              t?.['customerName'],
              t?.['subjectName'],
              t?.['fullName'],
              t?.['customer_name'],
              t?.['subject_name'],
              t?.['full_name'],
            ) ?? null;

          if (idRaw && name) {
            this.normalizeAndAddLookup(mapLookup, String(idRaw), name);
          }
        }

        this.transactionNameMap = mapLookup;

        // Now that lookup exists, fetch CTRs
        this.fetchCtrs();
      });
  }

  filteredTransactions(): TxnRowVm[] {
    const q = (this.search || '').trim().toLowerCase();
    if (!q) return this.transactions;

    return this.transactions.filter((t) => {
      const txnId = String(t.txnId ?? '');
      const subjectType = String(t.sourceSubjectType ?? '—').toLowerCase();
      const subjectName = String(t.subjectName ?? '').toLowerCase();
      const location = String(t.location ?? '—').toLowerCase();
      const cashIn = String(t.cashIn ?? '');
      const cashOut = String(t.cashOut ?? '');

      return (
        txnId.includes(q) ||
        subjectType.includes(q) ||
        subjectName.includes(q) ||
        location.includes(q) ||
        cashIn.includes(q) ||
        cashOut.includes(q)
      );
    });
  }

  openTransactionForm(): void {
    this.showTransactionForm = true;
  }

  closeTransactionForm(): void {
    this.showTransactionForm = false;
  }

  handleTransactionSubmit(_payload: any): void {
    this.submitError = null;
    this.submitSuccess = true;

    // Refresh transactions (rebuilds lookup) then CTRs
    this.refreshTransactions();

    // Optional: one extra CTR refresh shortly after (helps if CTR generation is async)
    setTimeout(() => this.fetchCtrs(), 1200);

    this.closeTransactionForm();
    this.justSubmittedLabel = true;

    setTimeout(() => (this.justSubmittedLabel = false), 1500);
    setTimeout(() => (this.submitSuccess = false), 2500);
  }

  goToUploadCtr(): void {
    // optional: route later
  }

  trackByEventId = (_: number, item: CtrRowVm) => item?.ctrId ?? _;

  /**
   * CTR→SAR promotion thresholds (UI-only, matches backend simple model):
   * - >= 60: auto-generate SAR
   * - 40–59: analyst review
   * - < 40: CTR only
   */
  scoreClass(score: number | string | null | undefined): string {
    const n = typeof score === 'number' ? score : score != null ? Number(score) : NaN;
    if (!Number.isFinite(n)) return 'badge';
    if (n >= 60) return 'badge badge--good';
    if (n >= 40) return 'badge badge--warn';
    return 'badge';
  }

  promotionLabel(score: number | string | null | undefined): string {
    const n = typeof score === 'number' ? score : score != null ? Number(score) : NaN;
    if (!Number.isFinite(n)) return '—';
    if (n >= 60) return 'Auto SAR';
    if (n >= 40) return 'Review';
    return 'CTR Only';
  }
}
