import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { BreadcrumbsComponent, BreadcrumbItem } from '../../../shared/breadcrumbs/breadcrumbs.component';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { ProperCasePipe } from '../../../shared/pipes/proper-case.pipe';

import {
  SuspectService,
  SuspectResponse,
  AliasResponse,
  LinkedOrganizationResponse,
  CreateAliasRequest,
  PatchAliasRequest,
} from '../../../shared/services/suspect.service';

import { AddressResponse, LinkedAddressResponse, AddressService } from '../../../shared/services/address.service';
import { OrganizationService, OrganizationResponse } from '../../../shared/services/organization.service';
import { ComplianceEventsService } from '../../../services/compliance-events.service';
import { CasesService, CaseFileResponse } from '../../../shared/services/cases.service';
import { ComplianceEventResponse } from '../../../shared/services/compliance.service';

// If you have a model for this, import it instead of using any
// import { ComplianceEventDto } from '../../../models/compliance-event-dto.interface';

const ADDRESS_TYPES = ['HOME', 'WORK', 'MAILING', 'UNKNOWN'];
const ALIAS_TYPES = ['AKA', 'LEGAL', 'NICKNAME', 'BUSINESS'];




@Component({
  selector: 'app-suspect-detail',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    BreadcrumbsComponent,
    ConfirmDialogComponent,
    ProperCasePipe,
  ],
  templateUrl: './suspect-detail.component.html',
  styleUrls: ['./suspect-detail.component.css'],
})
export class SuspectDetailComponent implements OnInit {
  suspect: SuspectResponse | null = null;
  addresses: LinkedAddressResponse[] = [];
  organizations: LinkedOrganizationResponse[] = [];
  aliases: AliasResponse[] = [];
  complianceEvents: ComplianceEventResponse[] = [];
  cases: CaseFileResponse[] = [];
  allCases: CaseFileResponse[] = [];
  allOrganizations: OrganizationResponse[] = [];
  allAddresses: AddressResponse[] = [];
  linkableCtrs: ComplianceEventResponse[] = [];
  linkableSars: ComplianceEventResponse[] = [];
  loading = false;
  loadingRelated = false;
  error: string | null = null;
  organizationsLoadError: string | null = null;
  breadcrumbItems: BreadcrumbItem[] = [];
  showDeleteConfirm = false;
  deleting = false;

  linkOrgId: number | '' = '';
  linkOrgRole = '';
  linkAddressId: number | '' = '';
  linkAddressType = 'UNKNOWN';
  linkAddressIsCurrent = true;
  linkOrgError: string | null = null;
  linkAddressError: string | null = null;
  linkingOrg = false;
  linkingAddr = false;

  linkAliasName = '';
  linkAliasType = 'AKA';
  linkAliasIsPrimary = false;
  linkAliasError: string | null = null;
  linkingAlias = false;

  editingAliasId: number | null = null;
  editAliasName = '';
  editAliasType = 'AKA';
  editAliasIsPrimary = false;
  deleteAliasId: number | null = null;
  deletingAlias = false;
  showDeleteAliasConfirm = false;

  linkCtrEventId: number | '' = '';
  linkSarEventId: number | '' = '';
  linkCaseId: number | '' = '';
  linkCtrError: string | null = null;
  linkSarError: string | null = null;
  linkCaseError: string | null = null;
  linkingCtr = false;
  linkingSar = false;
  linkingCase = false;
  unlinkingCtrEventId: number | null = null;
  unlinkingSarEventId: number | null = null;
  unlinkingCaseId: number | null = null;
  unlinkError: string | null = null;

  readonly addressTypes = ADDRESS_TYPES;
  readonly aliasTypes = ALIAS_TYPES;

  constructor(
  private route: ActivatedRoute,
  private router: Router,
  private suspectService: SuspectService,
  private organizationService: OrganizationService,
  private addressService: AddressService,
  private complianceEventService: ComplianceEventsService,
  private casesService: CasesService,
  private cdr: ChangeDetectorRef,
) {}

  get ctrs(): ComplianceEventResponse[] {
    return this.complianceEvents.filter((e) => e.eventType === 'CTR');
  }

  get sars(): ComplianceEventResponse[] {
    return this.complianceEvents.filter((e) => e.eventType === 'SAR');
  }

  /** Organizations that can be linked (not already linked to this suspect). */
  get availableOrganizations(): OrganizationResponse[] {
    const linkedIds = new Set(this.organizations.map((o) => o.orgId));
    return this.allOrganizations.filter((o) => !linkedIds.has(o.orgId));
  }

  /** Addresses that can be linked (not already linked to this suspect). */
  get availableAddresses(): AddressResponse[] {
    const linkedIds = new Set(this.addresses.map((a) => a.addressId));
    return this.allAddresses.filter((a) => !linkedIds.has(a.addressId));
  }

  /** Cases that can be linked (case's SAR is not already linked to this suspect). */
  get linkableCases(): CaseFileResponse[] {
    const linkedSarIds = new Set(this.sars.map((e) => e.eventId));
    return this.allCases.filter((c) => c.sarId != null && !linkedSarIds.has(c.sarId));
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    const idNum = id ? parseInt(id, 10) : NaN;
    if (!id || isNaN(idNum)) {
      this.error = 'Invalid suspect ID.';
      this.cdr.detectChanges();
      return;
    }
    this.breadcrumbItems = [
      { label: 'Home', url: '/dashboard' },
      { label: 'Registry', url: '/registry' },
      { label: 'Suspects', url: '/registry/suspects' },
      { label: `Suspect ${idNum}` },
    ];
    this.loadSuspect(idNum);
  }

  loadSuspect(id: number): void {
    this.loading = true;
    this.error = null;
    this.suspectService.getById(id).subscribe({
      next: (data) => {
        this.suspect = data;
        this.breadcrumbItems = [
          { label: 'Home', url: '/dashboard' },
          { label: 'Registry', url: '/registry' },
          { label: 'Suspects', url: '/registry/suspects' },
          { label: data.primaryName },
        ];
        this.loading = false;
        this.loadRelated(id);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err?.message ?? 'Failed to load suspect.';
        this.suspect = null;
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  loadRelated(suspectId: number): void {
    this.loadingRelated = true;
    this.organizationsLoadError = null;
    this.linkAliasError = null;
    this.addresses = [];
    this.organizations = [];
    this.aliases = [];
    this.complianceEvents = [];
    this.cases = [];

    const addresses$ = this.suspectService.getAddressesBySuspectId(suspectId).pipe(
      catchError(() => of([] as LinkedAddressResponse[])),
    );
    const organizations$ = this.suspectService.getOrganizationsBySuspectId(suspectId).pipe(
      catchError((err) => {
        this.organizationsLoadError = err?.error?.detail ?? err?.error?.message ?? err?.message ?? 'Failed to load organizations';
        return of([] as LinkedOrganizationResponse[]);
      }),
    );
    const aliases$ = this.suspectService.getAliasesBySuspectId(suspectId).pipe(
      catchError(() => of([] as AliasResponse[])),
    );
    const events$ = this.complianceEventService.getBySuspectId(suspectId).pipe(
      map((page) => page.content),
      catchError(() => of([] as ComplianceEventResponse[])),
    );
    const cases$ = this.casesService.getAll().pipe(
      catchError(() => of([] as CaseFileResponse[])),
    );
    const allOrgs$ = this.organizationService.getAll().pipe(
      catchError(() => of([] as OrganizationResponse[])),
    );
    const allAddrs$ = this.addressService.getAll().pipe(
      catchError(() => of([] as AddressResponse[])),
    );
    const linkableCtrs$ = this.complianceEventService
      .getLinkableByEventType('CTR', suspectId)
      .pipe(map((p) => p.content), catchError(() => of([] as ComplianceEventResponse[])));
    const linkableSars$ = this.complianceEventService
      .getLinkableByEventType('SAR', suspectId)
      .pipe(map((p) => p.content), catchError(() => of([] as ComplianceEventResponse[])));

    forkJoin({
      addresses: addresses$,
      organizations: organizations$,
      aliases: aliases$,
      events: events$,
      allCases: cases$,
      allOrganizations: allOrgs$,
      allAddresses: allAddrs$,
      linkableCtrs: linkableCtrs$,
      linkableSars: linkableSars$,
    }).subscribe({
      next: ({
        addresses,
        organizations,
        aliases,
        events,
        allCases,
        allOrganizations,
        allAddresses,
        linkableCtrs,
        linkableSars,
      }) => {
        this.addresses = addresses;
        this.organizations = organizations;
        this.aliases = aliases;
        this.complianceEvents = events;
        const sarEventIds = new Set(events.filter((e: ComplianceEventResponse) => e.eventType === 'SAR').map((e: ComplianceEventResponse) => e.eventId));
        this.cases = allCases.filter((c) => c.sarId != null && sarEventIds.has(c.sarId));
        this.allCases = allCases;
        this.allOrganizations = allOrganizations;
        this.allAddresses = allAddresses;
        this.linkableCtrs = linkableCtrs;
        this.linkableSars = linkableSars;
        this.loadingRelated = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingRelated = false;
        this.cdr.detectChanges();
      },
    });
  }

  linkOrganization(): void {
    const orgId = Number(this.linkOrgId);
    if (!this.suspect || this.linkOrgId === '' || this.linkingOrg || Number.isNaN(orgId) || orgId < 1) return;
    this.linkOrgError = null;
    this.linkingOrg = true;
    this.suspectService
      .linkSuspectToOrganization(this.suspect.suspectId, {
        orgId,
        role: this.linkOrgRole.trim() || undefined,
      })
      .subscribe({
        next: () => {
          this.linkOrgId = '';
          this.linkOrgRole = '';
          this.linkingOrg = false;
          this.loadRelated(this.suspect!.suspectId);
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.linkOrgError = this.getBackendErrorMessage(err) ?? 'Failed to link organization.';
          this.linkingOrg = false;
          this.loadRelated(this.suspect!.suspectId);
          this.cdr.detectChanges();
        },
      });
  }

  addAlias(): void {
    const name = this.linkAliasName?.trim();
    if (!this.suspect || !name || this.linkingAlias) return;
    this.linkAliasError = null;
    this.linkingAlias = true;
    const request: CreateAliasRequest = {
      suspectId: this.suspect.suspectId,
      aliasName: name,
      aliasType: this.linkAliasType || undefined,
      isPrimary: this.linkAliasIsPrimary,
    };
    this.suspectService.createAlias(request).subscribe({
      next: () => {
        this.linkAliasName = '';
        this.linkAliasType = 'AKA';
        this.linkAliasIsPrimary = false;
        this.linkAliasError = null;
        this.linkingAlias = false;
        this.loadRelated(this.suspect!.suspectId);
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        this.linkAliasError = this.getBackendErrorMessage(err) ?? 'Failed to add alias.';
        this.linkingAlias = false;
        this.cdr.detectChanges();
      },
    });
  }

  startEditAlias(a: AliasResponse): void {
    this.editingAliasId = a.aliasId;
    this.editAliasName = a.aliasName;
    this.editAliasType = a.aliasType || 'AKA';
    this.editAliasIsPrimary = a.isPrimary ?? false;
    this.cdr.detectChanges();
  }

  cancelEditAlias(): void {
    this.editingAliasId = null;
    this.editAliasName = '';
    this.editAliasType = 'AKA';
    this.editAliasIsPrimary = false;
    this.cdr.detectChanges();
  }

  saveEditAlias(): void {
    if (!this.suspect || this.editingAliasId == null) return;
    const name = this.editAliasName?.trim();
    if (!name) return;
    const request: PatchAliasRequest = {
      aliasName: name,
      aliasType: this.editAliasType || undefined,
      isPrimary: this.editAliasIsPrimary,
    };
    this.suspectService.updateAlias(this.editingAliasId, request).subscribe({
      next: () => {
        this.editingAliasId = null;
        this.editAliasName = '';
        this.editAliasType = 'AKA';
        this.editAliasIsPrimary = false;
        this.loadRelated(this.suspect!.suspectId);
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        this.linkAliasError = this.getBackendErrorMessage(err) ?? 'Failed to update alias.';
        this.cdr.detectChanges();
      },
    });
  }

  openDeleteAliasConfirm(aliasId: number): void {
    this.deleteAliasId = aliasId;
    this.showDeleteAliasConfirm = true;
    this.cdr.detectChanges();
  }

  onConfirmDeleteAlias(): void {
    const id = this.deleteAliasId;
    if (!this.suspect || id == null || this.deletingAlias) return;
    this.showDeleteAliasConfirm = false;
    this.deletingAlias = true;
    this.suspectService.deleteAlias(id).subscribe({
      next: () => {
        this.deleteAliasId = null;
        this.deletingAlias = false;
        this.loadRelated(this.suspect!.suspectId);
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        this.linkAliasError = this.getBackendErrorMessage(err) ?? 'Failed to delete alias.';
        this.deleteAliasId = null;
        this.deletingAlias = false;
        this.showDeleteAliasConfirm = false;
        this.loadRelated(this.suspect!.suspectId);
        this.cdr.detectChanges();
      },
    });
  }

  onCancelDeleteAlias(): void {
    this.showDeleteAliasConfirm = false;
    this.deleteAliasId = null;
    this.cdr.detectChanges();
  }

  linkAddress(): void {
    const addressId = Number(this.linkAddressId);
    if (!this.suspect || this.linkAddressId === '' || this.linkingAddr || Number.isNaN(addressId) || addressId < 1) return;
    this.linkAddressError = null;
    this.linkingAddr = true;
    this.suspectService
      .linkAddressToSuspect(this.suspect.suspectId, {
        addressId,
        addressType: this.linkAddressType || undefined,
        isCurrent: this.linkAddressIsCurrent,
      })
      .subscribe({
        next: () => {
          this.linkAddressId = '';
          this.linkAddressType = 'UNKNOWN';
          this.linkAddressIsCurrent = true;
          this.linkingAddr = false;
          this.loadRelated(this.suspect!.suspectId);
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.linkAddressError = this.getBackendErrorMessage(err) ?? 'Failed to link address.';
          this.linkingAddr = false;
          this.loadRelated(this.suspect!.suspectId);
          this.cdr.detectChanges();
        },
      });
  }

  linkCtr(): void {
    const eventId = Number(this.linkCtrEventId);
    if (!this.suspect || this.linkCtrEventId === '' || this.linkingCtr || Number.isNaN(eventId) || eventId < 1) return;
    this.linkCtrError = null;
    this.linkingCtr = true;
    this.complianceEventService.linkEventToSuspect(eventId, this.suspect.suspectId).subscribe({
      next: () => {
        this.linkCtrEventId = '';
        this.linkingCtr = false;
        this.loadRelated(this.suspect!.suspectId);
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        this.linkCtrError = this.getBackendErrorMessage(err) ?? 'Failed to link CTR.';
        this.linkingCtr = false;
        this.loadRelated(this.suspect!.suspectId);
        this.cdr.detectChanges();
      },
    });
  }

  linkSar(): void {
    const eventId = Number(this.linkSarEventId);
    if (!this.suspect || this.linkSarEventId === '' || this.linkingSar || Number.isNaN(eventId) || eventId < 1) return;
    this.linkSarError = null;
    this.linkingSar = true;
    this.complianceEventService.linkEventToSuspect(eventId, this.suspect.suspectId).subscribe({
      next: () => {
        this.linkSarEventId = '';
        this.linkingSar = false;
        this.loadRelated(this.suspect!.suspectId);
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        this.linkSarError = this.getBackendErrorMessage(err) ?? 'Failed to link SAR.';
        this.linkingSar = false;
        this.loadRelated(this.suspect!.suspectId);
        this.cdr.detectChanges();
      },
    });
  }

  linkCase(): void {
    const caseId = Number(this.linkCaseId);
    if (!this.suspect || this.linkCaseId === '' || this.linkingCase || Number.isNaN(caseId) || caseId < 1) return;
    const c = this.linkableCases.find((x) => x.caseId === caseId);
    if (!c?.sarId) {
      this.linkCaseError = 'Case has no linked SAR.';
      return;
    }
    this.linkCaseError = null;
    this.linkingCase = true;
    this.complianceEventService.linkEventToSuspect(c.sarId, this.suspect.suspectId).subscribe({
      next: () => {
        this.linkCaseId = '';
        this.linkingCase = false;
        this.loadRelated(this.suspect!.suspectId);
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        this.linkCaseError = this.getBackendErrorMessage(err) ?? 'Failed to link case.';
        this.linkingCase = false;
        this.loadRelated(this.suspect!.suspectId);
        this.cdr.detectChanges();
      },
    });
  }

  unlinkCtr(eventId: number): void {
    if (!this.suspect || this.unlinkingCtrEventId != null) return;
    this.unlinkError = null;
    this.unlinkingCtrEventId = eventId;
    this.complianceEventService.unlinkEventFromSuspect(eventId).subscribe({
      next: () => {
        this.unlinkingCtrEventId = null;
        this.unlinkError = null;
        this.loadRelated(this.suspect!.suspectId);
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        this.unlinkError = this.getBackendErrorMessage(err) ?? 'Failed to unlink CTR.';
        this.unlinkingCtrEventId = null;
        this.loadRelated(this.suspect!.suspectId);
        this.cdr.detectChanges();
      },
    });
  }

  unlinkSar(eventId: number): void {
    if (!this.suspect || this.unlinkingSarEventId != null) return;
    this.unlinkError = null;
    this.unlinkingSarEventId = eventId;
    this.complianceEventService.unlinkEventFromSuspect(eventId).subscribe({
      next: () => {
        this.unlinkingSarEventId = null;
        this.unlinkError = null;
        this.loadRelated(this.suspect!.suspectId);
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        this.unlinkError = this.getBackendErrorMessage(err) ?? 'Failed to unlink SAR.';
        this.unlinkingSarEventId = null;
        this.loadRelated(this.suspect!.suspectId);
        this.cdr.detectChanges();
      },
    });
  }

  unlinkCase(c: CaseFileResponse): void {
    if (!this.suspect || c.sarId == null || this.unlinkingCaseId != null) return;
    this.unlinkError = null;
    this.unlinkingCaseId = c.caseId;
    this.complianceEventService.unlinkEventFromSuspect(c.sarId).subscribe({
      next: () => {
        this.unlinkingCaseId = null;
        this.unlinkError = null;
        this.loadRelated(this.suspect!.suspectId);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.unlinkError = this.getBackendErrorMessage(err) ?? 'Failed to unlink case.';
        this.unlinkingCaseId = null;
        this.loadRelated(this.suspect!.suspectId);
        this.cdr.detectChanges();
      },
    });
  }

  /** Extract error message from backend (ProblemDetail uses 'detail', not 'message'). */
  private getBackendErrorMessage(err: { error?: { detail?: string; message?: string }; message?: string }): string | null {
    const body = err?.error;
    if (body && typeof body === 'object') {
      if (typeof (body as { detail?: string }).detail === 'string') return (body as { detail: string }).detail;
      if (typeof (body as { message?: string }).message === 'string') return (body as { message: string }).message;
    }
    return err?.message ?? null;
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

  goBack(): void {
    this.router.navigate(['/registry/suspects']);
  }

  openDeleteConfirm(): void {
    this.showDeleteConfirm = true;
  }

  onConfirmDelete(): void {
    if (!this.suspect) return;
    const activeCases = this.cases.filter((c) => c.status === 'OPEN' || c.status === 'REFERRED');
    if (activeCases.length > 0) {
      this.showDeleteConfirm = false;
      this.error =
        'Cannot delete suspect: this suspect is linked to one or more active cases (OPEN or REFERRED). Close or refer the cases before deleting.';
      this.cdr.detectChanges();
      return;
    }
    this.showDeleteConfirm = false;
    this.deleting = true;
    this.error = null;
    this.suspectService.delete(this.suspect.suspectId).subscribe({
      next: () => this.router.navigate(['/registry/suspects']),
      error: (err) => {
        this.error = this.getBackendErrorMessage(err) ?? 'Failed to delete suspect.';
        this.deleting = false;
        this.cdr.detectChanges();
      },
    });
  }

  onCancelDelete(): void {
    this.showDeleteConfirm = false;
  }
}
