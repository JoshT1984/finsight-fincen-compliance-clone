import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ComplianceEventsService } from '../../services/compliance-events.service';
import { TransactionService } from '../../shared/services/transaction.service';
import { TransactionFormComponent } from './transaction-form.component';

import { of } from 'rxjs';
import { catchError, finalize, map } from 'rxjs/operators';

@Component({
  selector: 'app-ctrs',
  standalone: true,
  imports: [CommonModule, FormsModule, TransactionFormComponent],
  templateUrl: './ctrs.component.html',
  styleUrls: ['./ctrs.component.css'],
})
export class CtrsComponent implements OnInit {
  ctrs: any[] = [];
  loadingCtrs = false;

  transactions: any[] = [];
  loadingTxns = false;
  txnsError: string | null = null;

  submitSuccess = false;
  submitError: string | null = null;
  submitting = false;
  justSubmittedLabel = false;

  search = '';
  showTransactionForm = false;

  constructor(
    private complianceEventsService: ComplianceEventsService,
    private transactionService: TransactionService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.fetchCtrs();
    this.refreshTransactions();
  }

  // =========================
  // CTRs
  // =========================
  fetchCtrs(): void {
    this.loadingCtrs = true;

    this.complianceEventsService
      .search({
        eventType: 'CTR',
        page: 0,
        size: 200,
      })
      .pipe(
        map((res: any) => res?.content ?? []),
        map((rows: any[]) =>
          rows.map((ctr: any) => {
            // ✅ Preferred values from ComplianceEventResponse "bloat"
            const subjectType =
              ctr.subjectType ??
              ctr.sourceSubjectType ??
              ctr.source_subject_type ??
              ctr['source_subject_type'] ??
              '—';

            const subjectName =
              ctr.customerName ?? ctr.subjectName ?? ctr.subject_name ?? ctr['subject_name'] ?? '—';

            // ✅ Map to a consistent shape that the HTML can render
            return {
              ctrId: ctr.eventId,
              sourceSubjectType: subjectType, // keep consistent with Transactions table style
              subjectName: subjectName,
              suspicionScore: ctr.severityScore != null ? ctr.severityScore : '-',
              ctrTime: ctr.eventTime,
              status: ctr.status || '—',
              amount: ctr.totalAmount ?? 0,
            };
          }),
        ),
        catchError(() => of([])),
        finalize(() => {
          this.loadingCtrs = false;

          // ✅ Forces UI update in cases where Angular doesn't repaint reliably
          this.cdr.detectChanges();
        }),
      )
      .subscribe((mapped: any[]) => {
        this.ctrs = mapped;
      });
  }

  // =========================
  // Transactions
  // =========================
  refreshTransactions(): void {
    this.loadingTxns = true;
    this.txnsError = null;

    this.transactionService
      .getTransactions(0, 200)
      .pipe(
        map((res: any) => res?.content ?? res ?? []),
        catchError((err: any) => {
          console.error(err);
          this.txnsError = err?.error?.message ?? 'Failed to load transactions.';
          return of([]);
        }),
        finalize(() => {
          this.loadingTxns = false;

          // ✅ Forces UI update (helps with “needs refresh” symptom)
          this.cdr.detectChanges();
        }),
      )
      .subscribe((rows: any[]) => {
        this.transactions = rows;
      });
  }

  filteredTransactions(): any[] {
    const q = (this.search || '').trim().toLowerCase();
    if (!q) return this.transactions;

    return this.transactions.filter((t) => {
      const txnId = String(t.txnId ?? '');
      const subjectType =
        t.sourceSubjectType ?? t.subject_type ?? t.subjectType ?? t['source_subject_type'] ?? '—';

      const subjectName = String(t.subjectName ?? t.customerName ?? '');
      const location = String(t.location ?? t['location'] ?? '—');
      const cashIn = String(t.cashIn ?? '');
      const cashOut = String(t.cashOut ?? '');

      return (
        txnId.includes(q) ||
        subjectType.toLowerCase().includes(q) ||
        subjectName.toLowerCase().includes(q) ||
        location.toLowerCase().includes(q) ||
        cashIn.includes(q) ||
        cashOut.includes(q)
      );
    });
  }

  // =========================
  // Modal
  // =========================
  openTransactionForm(): void {
    this.showTransactionForm = true;
  }

  closeTransactionForm(): void {
    this.showTransactionForm = false;
  }

  handleTransactionSubmit(_payload: any): void {
    this.submitError = null;
    this.submitSuccess = true;

    // ✅ Refresh both tables after a submit so user doesn't need manual refresh
    this.refreshTransactions();

    // CTR generation may be synchronous or async; fetch now and again shortly after
    this.fetchCtrs();
    setTimeout(() => this.fetchCtrs(), 1200);

    this.closeTransactionForm();
    this.justSubmittedLabel = true;

    setTimeout(() => (this.justSubmittedLabel = false), 1500);
    setTimeout(() => (this.submitSuccess = false), 2500);
  }

  // =========================
  // Optional stubs
  // =========================
  goToUploadCtr(): void {
    // optional: route later
  }

  // Track by stable id (ctrId is what we map into the table rows)
  trackByEventId = (_: number, item: any) => item?.ctrId ?? _;
}
