import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import {
  DocumentsService,
  DocumentResponse,
  CaseFileResponse,
} from '../../shared/services/documents.service';
import { ComplianceService, ComplianceEventOption } from '../../shared/services/compliance.service';

export interface CaseOption {
  id: number;
  label: string;
}

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './upload.component.html',
  styleUrls: ['./upload.component.css'],
})
export class UploadComponent implements OnInit, OnDestroy {
  documentType: 'CTR' | 'SAR' | 'CASE' = 'CTR';
  docType: string = 'CTR'; // For template compatibility
  /** 'new' = create new CTR, 'existing' = select from dropdown */
  ctrMode: 'new' | 'existing' = 'new';
  /** 'new' = create new SAR, 'existing' = select from dropdown */
  sarMode: 'new' | 'existing' = 'new';
  ctrId: number | null = null;
  sarId: number | null = null;
  caseId: number | null = null;
  selectedFile: File | null = null;
  loading = false;
  error: string | null = null;
  success: DocumentResponse | null = null;

  ctrOptions: ComplianceEventOption[] = [];
  sarOptions: ComplianceEventOption[] = [];
  caseOptions: CaseOption[] = [];
  loadingOptions = false;
  optionsError: string | null = null;

  private destroy$ = new Subject<void>();

  isUploading: boolean = false;

  constructor(
    private documentsService: DocumentsService,
    private complianceService: ComplianceService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loadOptions();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onDocTypeChange(newType: string): void {
    this.docType = newType;
    this.documentType = (newType as 'CTR' | 'SAR' | 'CASE') ?? 'CTR';
    this.ctrMode = 'new';
    this.sarMode = 'new';
    this.ctrId = null;
    this.sarId = null;
    this.caseId = null;
    this.error = null;
    this.optionsError = null; // Clear so error disappears when changing document type
    this.loadOptions();
  }

  // For template compatibility
  onDocumentTypeChange(newType?: 'CTR' | 'SAR' | 'CASE'): void {
    this.onDocTypeChange(newType ?? 'CTR');
  }

  loadOptions(): void {
    this.loadingOptions = true;
    this.optionsError = null;
    const forType = this.documentType;

    if (forType === 'CTR') {
      this.complianceService
        .getCtrOptions()
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (opts: ComplianceEventOption[]) => {
            if (this.documentType !== forType) return;
            this.ctrOptions = opts;
            this.loadingOptions = false;
            this.cdr.detectChanges();
          },
          error: (err: any) => {
            if (this.documentType !== forType) return;
            this.optionsError =
              'Could not load CTR list. Select "Create new CTR" to upload without linking.';
            this.ctrOptions = [];
            this.loadingOptions = false;
            this.cdr.detectChanges();
          },
        });
    } else if (forType === 'SAR') {
      this.complianceService
        .getSarOptions()
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (opts: ComplianceEventOption[]) => {
            if (this.documentType !== forType) return;
            this.sarOptions = opts;
            this.loadingOptions = false;
            this.cdr.detectChanges();
          },
          error: (err: any) => {
            if (this.documentType !== forType) return;
            this.optionsError =
              'Could not load SAR list. Select "Create new SAR" to upload without linking.';
            this.sarOptions = [];
            this.loadingOptions = false;
            this.cdr.detectChanges();
          },
        });
    } else {
      this.documentsService
        .getCases()
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (cases: CaseFileResponse[]) => {
            if (this.documentType !== forType) return;
            this.caseOptions = cases.map((c: CaseFileResponse) => this.toCaseOption(c));
            this.loadingOptions = false;
            this.cdr.detectChanges();
          },
          error: (err: any) => {
            if (this.documentType !== forType) return;
            this.optionsError = 'Could not load cases. Please try again.';
            this.caseOptions = [];
            this.loadingOptions = false;
            this.cdr.detectChanges();
          },
        });
    }
  }

  private toCaseOption(c: CaseFileResponse): CaseOption {
    const sar = c.sarId != null ? `SAR #${c.sarId}` : '—';
    return { id: c.caseId, label: `Case #${c.caseId} — ${sar} — ${c.status}` };
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    this.selectedFile = file ?? null;
    this.error = null;
    this.success = null;
  }

  upload(fileInput?: HTMLInputElement): void {
    if (!this.selectedFile) {
      this.error = 'Please select a file to upload.';
      this.cdr.detectChanges();
      return;
    }

    if (this.documentType === 'CASE' && (this.caseId == null || this.caseId < 1)) {
      this.error = 'Please select a case. Case is required for Case document uploads.';
      this.cdr.detectChanges();
      return;
    }

    this.loading = true;
    this.error = null;
    this.success = null;

    const ctrIdToUse = this.documentType === 'CTR' && this.ctrMode === 'new' ? null : this.ctrId;
    const sarIdToUse = this.documentType === 'SAR' && this.sarMode === 'new' ? null : this.sarId;
    this.documentsService
      .upload(
        this.selectedFile,
        this.documentType,
        ctrIdToUse ?? undefined,
        sarIdToUse ?? undefined,
        this.caseId ?? undefined,
      )
      .subscribe({
        next: (response: DocumentResponse) => {
          this.success = response;
          this.resetFormOnly(fileInput);
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: (err: any) => {
          this.error = err?.error?.message || err?.message || 'Upload failed. Please try again.';
          this.loading = false;
          this.cdr.detectChanges();
        },
      });
  }

  /** Resets form fields only (keeps success/error). Used after successful upload. */
  private resetFormOnly(fileInput?: HTMLInputElement): void {
    this.selectedFile = null;
    this.ctrId = null;
    this.sarId = null;
    this.caseId = null;
    if (fileInput) fileInput.value = '';
  }

  reset(fileInput?: HTMLInputElement): void {
    this.selectedFile = null;
    this.error = null;
    this.success = null;

    this.ctrMode = 'new';
    this.sarMode = 'new';
    this.ctrId = null;
    this.sarId = null;
    this.caseId = null;

    if (fileInput) fileInput.value = '';
  }
}
