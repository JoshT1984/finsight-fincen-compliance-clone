import { ChangeDetectorRef, Component, NgZone, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SideNavComponent, NavItem } from '../../shared/side-nav/side-nav.component';
import { DocumentsService, DocumentResponse } from '../../shared/services/documents.service';

@Component({
  selector: 'app-documents',
  standalone: true,
  imports: [CommonModule, FormsModule, SideNavComponent],
  templateUrl: './documents.component.html',
  styleUrls: ['./documents.component.css'],
})
export class DocumentsComponent implements OnInit {
  documentNavItems: NavItem[] = [
    { id: 'all', label: 'All Documents' },
    { id: 'ctr', label: 'CTR Documents' },
    { id: 'sar', label: 'SAR Documents' },
    { id: 'case', label: 'Case Documents' },
  ];

  selectedDocumentCategory: string = 'all';
  documents: DocumentResponse[] = [];
  filteredDocuments: DocumentResponse[] = [];
  loading = false;
  error: string | null = null;

  /** Search by ID: type and parsed ID when searching */
  searchType: 'ctr' | 'sar' | 'case' = 'ctr';
  searchIdInput = '';
  searchLabel: string | null = null;

  constructor(
    private documentsService: DocumentsService,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone,
  ) {}

  ngOnInit(): void {
    this.loadDocuments();
  }

  onCategorySelected(categoryId: string): void {
    this.selectedDocumentCategory = categoryId;
    this.searchLabel = null;
    this.searchIdInput = '';
    this.error = null;
    this.syncSearchTypeToCategory();
    this.loadDocuments();
  }

  searchById(): void {
    const raw = typeof this.searchIdInput === 'string' ? this.searchIdInput.trim() : String(this.searchIdInput ?? '');
    const id = parseInt(raw, 10);
    if (!raw || isNaN(id) || id < 1) {
      this.error = 'Please enter a valid ID (positive number).';
      this.cdr.detectChanges();
      return;
    }
    this.error = null;
    this.searchLabel = `${this.searchType.toUpperCase()} ID ${id}`;
    this.loading = true;

    const request =
      this.searchType === 'ctr'
        ? this.documentsService.getByCtrId(id)
        : this.searchType === 'sar'
          ? this.documentsService.getBySarId(id)
          : this.documentsService.getByCaseId(id);

    request.subscribe({
      next: (docs) => {
        this.ngZone.run(() => {
          this.documents = docs;
          this.applyFilter();
          this.loading = false;
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          this.error = err?.message || `No documents found for ${this.searchLabel}.`;
          this.documents = [];
          this.filteredDocuments = [];
          this.loading = false;
          this.cdr.detectChanges();
        });
      },
    });
  }

  clearSearch(): void {
    this.searchIdInput = '';
    this.searchLabel = null;
    this.error = null;
    this.loadDocuments();
  }

  private loadDocuments(): void {
    this.searchLabel = null;
    this.loading = true;
    this.error = null;
    this.documentsService.getAll().subscribe({
      next: (docs) => {
        this.ngZone.run(() => {
          this.documents = docs;
          this.applyFilter();
          this.loading = false;
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          this.error = err?.message || 'Failed to load documents. Please try again.';
          this.documents = [];
          this.filteredDocuments = [];
          this.loading = false;
          this.cdr.detectChanges();
        });
      },
    });
  }

  private applyFilter(): void {
    switch (this.selectedDocumentCategory) {
      case 'ctr':
        this.filteredDocuments = this.documents.filter((d) => d.documentType === 'CTR');
        break;
      case 'sar':
        this.filteredDocuments = this.documents.filter((d) => d.documentType === 'SAR');
        break;
      case 'case':
        this.filteredDocuments = this.documents.filter((d) => d.documentType === 'CASE');
        break;
      default:
        this.filteredDocuments = [...this.documents];
    }
  }

  formatDate(isoString: string): string {
    if (!isoString) return '-';
    const d = new Date(isoString);
    return isNaN(d.getTime()) ? isoString : d.toLocaleDateString();
  }

  /** Search type options based on selected menu - only show relevant options */
  getSearchTypeOptions(): { value: 'ctr' | 'sar' | 'case'; label: string }[] {
    if (this.selectedDocumentCategory === 'ctr') return [{ value: 'ctr', label: 'CTR ID' }];
    if (this.selectedDocumentCategory === 'sar') return [{ value: 'sar', label: 'SAR ID' }];
    if (this.selectedDocumentCategory === 'case') return [{ value: 'case', label: 'Case ID' }];
    return [
      { value: 'ctr', label: 'CTR ID' },
      { value: 'sar', label: 'SAR ID' },
      { value: 'case', label: 'Case ID' },
    ];
  }

  /** Keep searchType in sync when category changes (e.g. CTR menu → only CTR search) */
  private syncSearchTypeToCategory(): void {
    const options = this.getSearchTypeOptions();
    const valid = options.some((o) => o.value === this.searchType);
    if (!valid && options.length > 0) {
      this.searchType = options[0].value;
    }
  }

  /** Effective category for column visibility (category or inferred from search) */
  getEffectiveCategory(): string {
    if (this.searchLabel) {
      if (this.searchLabel.startsWith('CTR')) return 'ctr';
      if (this.searchLabel.startsWith('SAR')) return 'sar';
      if (this.searchLabel.startsWith('CASE')) return 'case';
    }
    return this.selectedDocumentCategory;
  }

  showCtrIdColumn(): boolean {
    const cat = this.getEffectiveCategory();
    return cat === 'all' || cat === 'ctr';
  }

  showSarIdColumn(): boolean {
    const cat = this.getEffectiveCategory();
    return cat === 'all' || cat === 'sar';
  }

  showCaseIdColumn(): boolean {
    const cat = this.getEffectiveCategory();
    return cat === 'all' || cat === 'case' || cat === 'sar'; // SAR docs can link to cases
  }

  getCategoryLabel(): string {
    if (this.searchLabel) {
      return this.searchLabel;
    }
    const item = this.documentNavItems.find((i) => i.id === this.selectedDocumentCategory);
    return item ? item.label : 'Documents';
  }

  openDocument(doc: DocumentResponse): void {
    this.documentsService.getDownloadUrl(doc.documentId).subscribe({
      next: (response) => {
        window.open(response.downloadUrl, '_blank', 'noopener,noreferrer');
      },
      error: () => {
        this.error = 'Failed to get download link for ' + doc.fileName;
      },
    });
  }
}
