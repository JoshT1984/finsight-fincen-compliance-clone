import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { BreadcrumbsComponent, BreadcrumbItem } from '../../../shared/breadcrumbs/breadcrumbs.component';
import {
  AddressService,
  AddressResponse,
  CreateAddressRequest,
  PatchAddressRequest,
} from '../../../shared/services/address.service';

@Component({
  selector: 'app-address-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, BreadcrumbsComponent],
  templateUrl: './address-form.component.html',
  styleUrls: ['./address-form.component.css'],
})
export class AddressFormComponent implements OnInit {
  breadcrumbItems: BreadcrumbItem[] = [];
  addressId: number | null = null;
  loading = false;
  loadError: string | null = null;
  submitError: string | null = null;
  saving = false;

  line1 = '';
  line2 = '';
  city = '';
  state = '';
  postalCode = '';
  country = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private addressService: AddressService,
    private cdr: ChangeDetectorRef,
  ) {}

  get isEditMode(): boolean {
    return this.addressId != null;
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      const idNum = parseInt(id, 10);
      if (!isNaN(idNum)) {
        this.addressId = idNum;
        this.breadcrumbItems = [
          { label: 'Home', url: '/dashboard' },
          { label: 'Registry', url: '/registry' },
          { label: 'Addresses', url: '/registry/addresses' },
          { label: 'Edit' },
        ];
        this.loadAddress(idNum);
        return;
      }
    }
    this.addressId = null;
    this.breadcrumbItems = [
      { label: 'Home', url: '/dashboard' },
      { label: 'Registry', url: '/registry' },
      { label: 'Addresses', url: '/registry/addresses' },
      { label: 'New address' },
    ];
    this.cdr.detectChanges();
  }

  loadAddress(id: number): void {
    this.loading = true;
    this.loadError = null;
    this.addressService.getById(id).subscribe({
      next: (a) => {
        this.line1 = a.line1 ?? '';
        this.line2 = a.line2 ?? '';
        this.city = a.city ?? '';
        this.state = a.state ?? '';
        this.postalCode = a.postalCode ?? '';
        this.country = a.country ?? '';
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.loadError = err?.message ?? 'Failed to load address.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  onSubmit(): void {
    this.submitError = null;
    if (this.isEditMode && this.addressId != null) {
      const body: PatchAddressRequest = {
        line1: this.line1.trim() || undefined,
        line2: this.line2.trim() || undefined,
        city: this.city.trim() || undefined,
        state: this.state.trim() || undefined,
        postalCode: this.postalCode.trim() || undefined,
        country: this.country.trim() || undefined,
      };
      this.saving = true;
      this.addressService.update(this.addressId, body).subscribe({
        next: () => {
          this.saving = false;
          this.router.navigate(['/registry/addresses']);
        },
        error: (err) => {
          this.submitError = err?.message ?? 'Failed to update address.';
          this.saving = false;
          this.cdr.detectChanges();
        },
      });
    } else {
      const body: CreateAddressRequest = {
        line1: this.line1.trim(),
        line2: this.line2.trim() || undefined,
        city: this.city.trim(),
        state: this.state.trim() || undefined,
        postalCode: this.postalCode.trim() || undefined,
        country: this.country.trim(),
      };
      this.saving = true;
      this.addressService.create(body).subscribe({
        next: () => {
          this.saving = false;
          this.router.navigate(['/registry/addresses']);
        },
        error: (err) => {
          this.submitError = err?.message ?? 'Failed to create address.';
          this.saving = false;
          this.cdr.detectChanges();
        },
      });
    }
  }

  cancel(): void {
    this.router.navigate(['/registry/addresses']);
  }
}
