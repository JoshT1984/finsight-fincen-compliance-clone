import { Component } from '@angular/core';
import { SideNavComponent, NavItem } from '../../shared/side-nav/side-nav.component';

@Component({
  selector: 'app-documents',
  standalone: true,
  imports: [SideNavComponent],
  templateUrl: './documents.component.html',
  styleUrls: ['./documents.component.css'],
})
export class DocumentsComponent {
  // Example: Define your own nav items for this page
  documentNavItems: NavItem[] = [
    { id: 'all', label: 'All Documents' },
    { id: 'ctr', label: 'CTR Documents' },
    { id: 'sar', label: 'SAR Documents' },
    { id: 'uploaded', label: 'Uploaded' },
  ];

  selectedDocumentCategory: string = 'all';

  onCategorySelected(categoryId: string) {
    this.selectedDocumentCategory = categoryId;
    // Handle the selection - e.g., filter documents, load data, etc.
    console.log('Selected category:', categoryId);
  }
}
