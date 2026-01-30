import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
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
  searchType: 'ctr' | 'sar' | 'case' = 'ctr';
  searchIdInput = '';
  searchLabel: string | null = null;

  searchTypeOptions: { value: 'ctr' | 'sar' | 'case'; label: string }[] = [
    { value: 'ctr', label: 'CTR ID' },
    { value: 'sar', label: 'SAR ID' },
    { value: 'case', label: 'Case ID' },
  ];

  constructor(
    private documentsService: DocumentsService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loadDocuments();
  }

  onCategorySelected(categoryId: string): void {
    this.selectedDocumentCategory = categoryId;
    this.searchIdInput = '';
    this.searchLabel = null;
    this.error = null;
    this.updateSearchTypeOptions();
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
        this.documents = docs;
        this.filteredDocuments = this.applyFilter(docs);
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err?.message || `No documents found for ${this.searchLabel}.`;
        this.documents = [];
        this.filteredDocuments = [];
        this.loading = false;
        this.cdr.detectChanges();
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
        this.documents = docs;
        this.filteredDocuments = this.applyFilter(docs);
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err?.message || 'Failed to load documents. Please try again.';
        this.documents = [];
        this.filteredDocuments = [];
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  private applyFilter(docs: DocumentResponse[]): DocumentResponse[] {
    switch (this.selectedDocumentCategory) {
      case 'ctr':
        return docs.filter((d) => d.documentType === 'CTR');
      case 'sar':
        return docs.filter((d) => d.documentType === 'SAR');
      case 'case':
        return docs.filter((d) => d.documentType === 'CASE');
      default:
        return [...docs];
    }
  }

  formatDate(isoString: string): string {
    if (!isoString) return '-';
    const d = new Date(isoString);
    return isNaN(d.getTime()) ? isoString : d.toLocaleDateString();
  }

  private updateSearchTypeOptions(): void {
    if (this.selectedDocumentCategory === 'ctr') {
      this.searchTypeOptions = [{ value: 'ctr', label: 'CTR ID' }];
    } else if (this.selectedDocumentCategory === 'sar') {
      this.searchTypeOptions = [{ value: 'sar', label: 'SAR ID' }];
    } else if (this.selectedDocumentCategory === 'case') {
      this.searchTypeOptions = [{ value: 'case', label: 'Case ID' }];
    } else {
      this.searchTypeOptions = [
        { value: 'ctr', label: 'CTR ID' },
        { value: 'sar', label: 'SAR ID' },
        { value: 'case', label: 'Case ID' },
      ];
    }
  }

  private syncSearchTypeToCategory(): void {
    const valid = this.searchTypeOptions.some((o) => o.value === this.searchType);
    if (!valid && this.searchTypeOptions.length > 0) {
      this.searchType = this.searchTypeOptions[0].value;
    }
  }

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
    return cat === 'all' || cat === 'case' || cat === 'sar';
  }

  getCategoryLabel(): string {
    if (this.searchLabel) return this.searchLabel;
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
