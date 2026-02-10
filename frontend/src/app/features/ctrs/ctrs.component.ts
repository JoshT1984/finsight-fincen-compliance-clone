import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ComplianceEventsService } from '../../services/compliance-events.service';
import { TransactionService } from '../../shared/services/transaction.service';
import { TransactionFormComponent } from './transaction-form.component';
import { CtrListComponent } from './ctr-list/ctr-list.component';

@Component({
  selector: 'app-ctrs',
  standalone: true,
  imports: [CommonModule, FormsModule, TransactionFormComponent, CtrListComponent],
  templateUrl: './ctrs.component.html',
  styleUrls: ['./ctrs.component.css'],
})
export class CtrsComponent implements OnInit {
  // ===== Transactions =====
  transactions: any[] = [];
  loadingTxns = false;
  txnsError: string | null = null;

  // ===== Submit / UX =====
  submitSuccess = false;
  submitError: string | null = null;
  submitting = false;
  justSubmittedLabel = false;

  // ===== UI =====
  search = '';
  showTransactionForm = false;

  constructor(
    private complianceService: ComplianceEventsService,
    private transactionService: TransactionService,
  ) {}

  ngOnInit(): void {
    this.refreshTransactions();
  }

  // =========================
  // Transactions
  // =========================
  refreshTransactions(): void {
    this.loadingTxns = true;
    this.txnsError = null;

    this.transactionService.getTransactions(0, 200).subscribe({
      next: (res: any) => {
        this.transactions = res?.content ?? res ?? [];
        this.loadingTxns = false;
      },
      error: (err: any) => {
        console.error(err);
        this.txnsError = err?.error?.message ?? 'Failed to load transactions.';
        this.loadingTxns = false;
      },
    });
  }

  filteredTransactions(): any[] {
    const q = (this.search || '').trim().toLowerCase();
    if (!q) return this.transactions;

    return this.transactions.filter((t) => {
      const txnId = String(t.txnId ?? '');
      const subjectKey = String(t.subjectKey ?? t.externalSubjectKey ?? '');
      const subjectName = String(t.subjectName ?? '');
      const location = String(t.location ?? '');
      const cashIn = String(t.cashIn ?? '');
      const cashOut = String(t.cashOut ?? '');

      return (
        txnId.includes(q) ||
        subjectKey.toLowerCase().includes(q) ||
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

    this.refreshTransactions();

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

  trackByEventId = (_: number, item: any) => item?.eventId ?? _;
}
