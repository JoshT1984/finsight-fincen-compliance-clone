import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { TransactionFormComponent } from './transaction-form.component';
import {
  TransactionService,
  CreateTransactionRequest,
} from '../../shared/services/transaction.service';
import {
  ComplianceService,
  ComplianceEventResponse,
} from '../../shared/services/compliance.service';

@Component({
  selector: 'app-ctrs',
  standalone: true,
  templateUrl: './ctrs.component.html',
  styleUrls: ['./ctrs.component.css'],
  imports: [CommonModule, FormsModule, RouterModule, MatSnackBarModule, TransactionFormComponent],
})
export class CtrsComponent {
  showTransactionForm = false;
  submitting = false;

  submitSuccess = false;
  submitError: string | null = null;

  // ✅ small UI state: flip the main button label to "Submitted" briefly
  justSubmittedLabel = false;

  ctrs: any[] = [];
  loadingCtrs = false;
  ctrsError: string | null = null;

  search = '';

  // Stubs for template compatibility
  downloadCtr(_id: any): void {}

  constructor(
    private transactionService: TransactionService,
    private complianceService: ComplianceService,
    private router: Router,
    private snackBar: MatSnackBar,
  ) {
    this.refreshCtrs();
  }

  refreshCtrs(): void {
    this.loadingCtrs = true;
    this.ctrsError = null;

    this.complianceService.getEvents('CTR', 200).subscribe({
      next: (list) => {
        this.ctrs = [...(list ?? [])].sort((a, b) =>
          (b.eventTime || '').localeCompare(a.eventTime || ''),
        );
        this.loadingCtrs = false;
      },
      error: () => {
        this.ctrsError =
          'Could not load CTRs. If the compliance-event service is offline, you can still add cash transactions.';
        this.ctrs = [];
        this.loadingCtrs = false;
      },
    });
  }

  openTransactionForm(): void {
    this.showTransactionForm = true;
    this.submitSuccess = false;
    this.submitError = null;
  }

  closeTransactionForm(): void {
    this.showTransactionForm = false;
  }

  handleTransactionSubmit(data: CreateTransactionRequest): void {
    if (this.submitting) return;

    this.submitting = true;
    this.submitSuccess = false;
    this.submitError = null;

    this.transactionService.createTransaction(data).subscribe({
      next: () => {
        this.submitting = false;
        this.submitSuccess = true;
        this.showTransactionForm = false;

        // ✅ toast
        this.snackBar.open('Transaction submitted successfully', 'Close', {
          duration: 3000,
        });

        // ✅ optional: flip button label to "Submitted" briefly
        this.justSubmittedLabel = true;
        setTimeout(() => (this.justSubmittedLabel = false), 2500);

        this.refreshCtrs();
      },
      error: (err) => {
        this.submitting = false;

        const msg = err?.error?.message || 'Failed to add transaction.';
        this.submitError = msg;

        // ✅ toast
        this.snackBar.open(msg, 'Close', {
          duration: 5000,
        });
      },
    });
  }

  goToUploadCtr(): void {
    this.router.navigate(['/upload'], { queryParams: { documentType: 'CTR' } });
  }

  openEvent(event: ComplianceEventResponse): void {
    console.log('Open CTR event', event);
  }

  trackByEventId(_: number, e: ComplianceEventResponse): string {
    return String(e.eventId);
  }

  filteredCtrs(): ComplianceEventResponse[] {
    const q = this.search.trim().toLowerCase();
    if (!q) return this.ctrs;

    return this.ctrs.filter((c) => {
      const amount = c.totalAmount != null ? String(c.totalAmount) : '';
      return (
        String(c.eventId).toLowerCase().includes(q) ||
        (c.sourceEntityId || '').toLowerCase().includes(q) ||
        amount.includes(q) ||
        (c.status || '').toLowerCase().includes(q) ||
        (c.eventType || '').toLowerCase().includes(q)
      );
    });
  }
}
