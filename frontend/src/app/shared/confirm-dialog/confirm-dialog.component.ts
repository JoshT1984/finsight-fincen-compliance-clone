import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export type ConfirmDialogMode = 'close' | 'refer';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './confirm-dialog.component.html',
  styleUrls: ['./confirm-dialog.component.css'],
})
export class ConfirmDialogComponent {
  @Input() title = 'Confirm';
  @Input() message = 'Are you sure?';
  @Input() confirmText = 'Confirm';
  @Input() cancelText = 'Cancel';
  @Input() mode: ConfirmDialogMode = 'close';

  @Output() confirm = new EventEmitter<{ agency?: string }>();
  @Output() cancel = new EventEmitter<void>();

  agencyInput = '';

  get showAgencyInput(): boolean {
    return this.mode === 'refer';
  }

  onConfirm(): void {
    if (this.mode === 'refer') {
      const agency = (this.agencyInput ?? '').trim();
      this.confirm.emit({ agency });
    } else {
      this.confirm.emit({});
    }
    this.agencyInput = '';
  }

  onCancel(): void {
    this.agencyInput = '';
    this.cancel.emit();
  }
}
