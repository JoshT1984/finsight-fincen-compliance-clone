import { Component, EventEmitter, Output } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

import {
  TransactionService,
  CreateTransactionRequest,
} from '../../shared/services/transaction.service';

type Option<T extends string = string> = { value: T; label: string };
type SubmitState = 'idle' | 'submitting' | 'success' | 'error';

@Component({
  selector: 'app-transaction-form',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './transaction-form.component.html',
  styleUrls: ['./transaction-form.component.css'],
})
export class TransactionFormComponent {
  @Output() cancel = new EventEmitter<void>();

  /**
   * Emits ONLY after the transaction is successfully persisted.
   * Parent should refresh tables on this event.
   */
  @Output() submitted = new EventEmitter<CreateTransactionRequest>();

  // Component-controlled submit state (clean, reliable UX)
  submitState: SubmitState = 'idle';
  submitErrorMsg: string | null = null;

  form: FormGroup;

  sourceSystems: Option[] = [
    { value: 'CORE_BANKING', label: 'Core Banking' },
    { value: 'TELLER_APP', label: 'Teller App' },
    { value: 'ATM_NETWORK', label: 'ATM Network' },
    { value: 'POS_SYSTEM', label: 'POS System' },
  ];

  subjectTypes: Option[] = [
    { value: 'INDIVIDUAL', label: 'Individual' },
    { value: 'BUSINESS', label: 'Business' },
    { value: 'ORGANIZATION', label: 'Organization' },
  ];

  currencies: Option[] = [
    { value: 'USD', label: 'USD' },
    { value: 'EUR', label: 'EUR' },
    { value: 'GBP', label: 'GBP' },
  ];

  channels: Option[] = [
    { value: 'BRANCH', label: 'Branch' },
    { value: 'ATM', label: 'ATM' },
    { value: 'ONLINE', label: 'Online' },
    { value: 'MOBILE', label: 'Mobile' },
    { value: 'TELLER', label: 'Teller' },
  ];

  locations: Option[] = [
    { value: 'Austin TX', label: 'Austin TX' },
    { value: 'Dallas TX', label: 'Dallas TX' },
    { value: 'Houston TX', label: 'Houston TX' },
    { value: 'Branch 0142', label: 'Branch 0142' },
  ];

  constructor(
    private fb: FormBuilder,
    private transactionService: TransactionService,
  ) {
    this.form = this.fb.group({
      sourceSystem: ['', Validators.required],
      sourceTxnId: ['', Validators.required],
      externalSubjectKey: ['', Validators.required],
      sourceSubjectType: ['', Validators.required],
      sourceSubjectId: ['', Validators.required],
      subjectName: ['', Validators.required],
      txnTime: ['', Validators.required],
      cashIn: [0, [Validators.required, Validators.min(0)]],
      cashOut: [0, [Validators.required, Validators.min(0)]],
      currency: ['', Validators.required],
      channel: ['', Validators.required],
      location: ['', Validators.required],
    });

    this.applyDefaults();
    this.setupDerivedFields();
  }

  get isSubmitting(): boolean {
    return this.submitState === 'submitting';
  }

  get submitLabel(): string {
    if (this.submitState === 'submitting') return 'Submitting…';
    if (this.submitState === 'success') return 'Submitted';
    if (this.submitState === 'error') return 'Retry Submit';
    return 'Submit Transaction';
  }

  private applyDefaults(): void {
    const now = this.toDatetimeLocal(new Date());

    this.form.patchValue({
      sourceSystem: 'CORE_BANKING',
      sourceSubjectType: 'INDIVIDUAL',
      currency: 'USD',
      channel: 'BRANCH',
      location: 'Austin TX',
      txnTime: now,
      cashIn: 12000,
      cashOut: 0,
      subjectName: 'John Doe',
      sourceTxnId: this.generateTxnId(),
      externalSubjectKey: this.generateCustomerKey(),
    });

    const ext = String(this.form.get('externalSubjectKey')?.value || '');
    this.form.patchValue({ sourceSubjectId: this.extractNumericId(ext) });
  }

  private setupDerivedFields(): void {
    this.form.get('externalSubjectKey')?.valueChanges.subscribe((val) => {
      const ext = String(val || '');
      const id = this.extractNumericId(ext);
      if (id) this.form.patchValue({ sourceSubjectId: id }, { emitEvent: false });
    });
  }

  regenerateIds(): void {
    const ext = this.generateCustomerKey();
    this.form.patchValue({
      sourceTxnId: this.generateTxnId(),
      externalSubjectKey: ext,
      sourceSubjectId: this.extractNumericId(ext),
      txnTime: this.toDatetimeLocal(new Date()),
    });

    // reset submit state if user changes payload
    this.submitState = 'idle';
    this.submitErrorMsg = null;
  }

  onSubmit(): void {
    if (this.isSubmitting) return;

    this.submitErrorMsg = null;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const request: CreateTransactionRequest = { ...this.form.value };

    // Normalize numeric fields (Angular forms can keep them as strings)
    request.cashIn = Number((request as any).cashIn ?? 0) as any;
    request.cashOut = Number((request as any).cashOut ?? 0) as any;

    this.submitState = 'submitting';

    // ✅ Persist FIRST, then emit ONLY on success
    this.transactionService.createTransaction(request).subscribe({
      next: () => {
        this.submitState = 'success';
        this.submitted.emit(request);

        // optional: keep it snappy; reset back to idle after a moment
        setTimeout(() => (this.submitState = 'idle'), 1200);
      },
      error: (err: any) => {
        console.error(err);
        this.submitState = 'error';
        this.submitErrorMsg =
          err?.error?.message ?? err?.message ?? 'Failed to create transaction.';
      },
    });
  }

  onCancel(): void {
    this.cancel.emit();
  }

  private generateTxnId(): string {
    const d = new Date();
    const pad = (n: number) => String(n).padStart(2, '0');
    const date = `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}`;
    const time = `${pad(d.getHours())}${pad(d.getMinutes())}${pad(d.getSeconds())}`;
    const rand = Math.floor(100 + Math.random() * 900);
    return `TXN-${date}-${time}-${rand}`;
  }

  private generateCustomerKey(): string {
    const n = Math.floor(100000 + Math.random() * 900000);
    return `CUST-${n}`;
  }

  private extractNumericId(externalKey: string): string {
    const m = externalKey.match(/(\d+)/);
    return m ? m[1] : '';
  }

  private toDatetimeLocal(date: Date): string {
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(
      date.getHours(),
    )}:${pad(date.getMinutes())}`;
  }
}
