import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { BreadcrumbsComponent, BreadcrumbItem } from '../../../shared/breadcrumbs/breadcrumbs.component';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { AddressService, AddressResponse } from '../../../shared/services/address.service';

@Component({
  selector: 'app-addresses',
  standalone: true,
  imports: [CommonModule, RouterLink, BreadcrumbsComponent, ConfirmDialogComponent],
  templateUrl: './addresses.component.html',
  styleUrls: ['./addresses.component.css'],
})
export class AddressesComponent implements OnInit {
  breadcrumbItems: BreadcrumbItem[] = [
    { label: 'Home', url: '/dashboard' },
    { label: 'Registry', url: '/registry' },
    { label: 'Addresses' },
  ];

  addresses: AddressResponse[] = [];
  loading = false;
  error: string | null = null;
  showDeleteConfirm = false;
  deleteTargetId: number | null = null;
  deleting = false;

  constructor(
    private addressService: AddressService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loadAddresses();
  }

  loadAddresses(): void {
    this.loading = true;
    this.error = null;
    this.addressService.getAll().subscribe({
      next: (list) => {
        this.addresses = list;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err?.message ?? 'Failed to load addresses.';
        this.addresses = [];
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  formatDate(isoString: string | null): string {
    if (!isoString) return '-';
    const d = new Date(isoString);
    return isNaN(d.getTime()) ? isoString : d.toLocaleDateString();
  }

  formatAddress(a: AddressResponse): string {
    const parts = [a.line1];
    if (a.line2?.trim()) parts.push(a.line2);
    parts.push([a.city, a.state, a.postalCode].filter(Boolean).join(', '));
    if (a.country) parts.push(a.country);
    return parts.filter(Boolean).join(', ');
  }

  openDeleteConfirm(addressId: number): void {
    this.deleteTargetId = addressId;
    this.showDeleteConfirm = true;
  }

  onConfirmDelete(): void {
    if (this.deleteTargetId == null) return;
    const id = this.deleteTargetId;
    this.showDeleteConfirm = false;
    this.deleteTargetId = null;
    this.deleting = true;
    this.addressService.delete(id).subscribe({
      next: () => {
        this.deleting = false;
        this.loadAddresses();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err?.message ?? 'Failed to delete address.';
        this.deleting = false;
        this.cdr.detectChanges();
      },
    });
  }

  onCancelDelete(): void {
    this.showDeleteConfirm = false;
    this.deleteTargetId = null;
  }
}
