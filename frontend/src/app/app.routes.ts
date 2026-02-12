import { Routes } from '@angular/router';
import { CasesComponent } from './features/cases/cases.component';
import { CaseDetailComponent } from './features/cases/case-detail/case-detail.component';
import { CaseDetailOverviewComponent } from './features/cases/case-detail/case-detail-overview.component';
import { CaseDetailAuditComponent } from './features/cases/case-detail/case-detail-audit.component';
import { CaseDetailNotesComponent } from './features/cases/case-detail/case-detail-notes.component';
import { CaseDetailDocumentsComponent } from './features/cases/case-detail/case-detail-documents.component';
import { caseResolver } from './features/cases/case.resolver';
import { CtrsComponent } from './features/ctrs/ctrs.component';
import { DocumentsComponent } from './features/documents/documents.component';
import { LandingComponent } from './features/landing/landing.component';
import { PublicLandingComponent } from './features/public-landing/public-landing.component';
import { SarsComponent } from './features/sars/sars.component';
import { SarDetailComponent } from './features/sars/sar-detail.component';
import { ShellComponent } from './layout/shell/shell.component';
import { UploadComponent } from './features/upload/upload.component';
import { ProfileComponent } from './features/profile/profile.component';
import { RegistryComponent } from './features/registry/registry.component';
import { SuspectsListComponent } from './features/registry/suspects/suspects-list.component';
import { SuspectDetailComponent } from './features/registry/suspects/suspect-detail.component';
import { SuspectFormComponent } from './features/registry/suspects/suspect-form.component';
import { OrganizationsComponent } from './features/registry/organizations/organizations.component';
import { OrganizationFormComponent } from './features/registry/organizations/organization-form.component';
import { AddressesComponent } from './features/registry/addresses/addresses.component';
import { AddressFormComponent } from './features/registry/addresses/address-form.component';
import { SupportTicketComponent } from './features/support-ticket/support-ticket.component';
import { authGuard } from './shared/guards/auth.guard';
import { publicLandingGuard } from './shared/guards/public-landing.guard';
import { roleGuard } from './shared/guards/role.guard';
import { LinkAccountComponent } from './features/link-account/link-account.component';
import { OauthCallbackComponent } from './features/oauth-callback/oauth-callback.component';
import { OfficerReviewComponent } from './features/officer/officer-review.component';

export const routes: Routes = [
  {
    path: '',
    component: ShellComponent,
    children: [
      {
        path: '',
        component: PublicLandingComponent,
        canActivate: [publicLandingGuard],
      },
      {
        path: 'dashboard',
        component: LandingComponent,
        canActivate: [authGuard],
      },
      {
        path: 'cases',
        component: CasesComponent,
        canActivate: [authGuard, roleGuard],
        // Officers should not see the full case queue; they review referred cases via /officer.
        data: { allowedRoles: ['ANALYST'] },
      },
      {
        path: 'cases/:id',
        component: CaseDetailComponent,
        resolve: { case: caseResolver },
        canActivate: [authGuard, roleGuard],
        // Officers can open specific referred cases (linked from /officer).
        data: { allowedRoles: ['ANALYST', 'LAW_ENFORCEMENT_USER'] },
        children: [
          { path: '', redirectTo: 'overview', pathMatch: 'full' },
          { path: 'overview', component: CaseDetailOverviewComponent },
          { path: 'audit', component: CaseDetailAuditComponent },
          { path: 'notes', component: CaseDetailNotesComponent },
          { path: 'documents', component: CaseDetailDocumentsComponent },
        ],
      },
      {
        path: 'sars',
        component: SarsComponent,
        canActivate: [authGuard, roleGuard],
        data: { allowedRoles: ['ANALYST', 'COMPLIANCE_USER'] },
      },
      {
        path: 'sars/:id',
        component: SarDetailComponent,
        canActivate: [authGuard, roleGuard],
        data: { allowedRoles: ['ANALYST', 'COMPLIANCE_USER'] },
      },
      {
        path: 'ctrs',
        component: CtrsComponent,
        canActivate: [authGuard, roleGuard],
        data: { allowedRoles: ['ANALYST', 'COMPLIANCE_USER'] },
      },
      {
        path: 'documents',
        component: DocumentsComponent,
        canActivate: [authGuard, roleGuard],
        data: { allowedRoles: ['ANALYST', 'LAW_ENFORCEMENT_USER'] },
      },
      {
        path: 'upload',
        component: UploadComponent,
        canActivate: [authGuard, roleGuard],
        data: { allowedRoles: ['COMPLIANCE_USER'] },
      },
      {
        path: 'officer',
        component: OfficerReviewComponent,
        canActivate: [authGuard, roleGuard],
        data: { allowedRoles: ['LAW_ENFORCEMENT_USER'] },
      },
      {
        path: 'profile',
        component: ProfileComponent,
        canActivate: [authGuard],
      },
      {
        path: 'registry',
        component: RegistryComponent,
        canActivate: [authGuard, roleGuard],
        data: { allowedRoles: ['ANALYST'] },
      },
      {
        path: 'registry/suspects',
        component: SuspectsListComponent,
        canActivate: [authGuard, roleGuard],
        data: { allowedRoles: ['ANALYST'] },
      },
      {
        path: 'registry/suspects/new',
        component: SuspectFormComponent,
        canActivate: [authGuard, roleGuard],
        data: { allowedRoles: ['ANALYST'] },
      },
      {
        path: 'registry/suspects/:id/edit',
        component: SuspectFormComponent,
        canActivate: [authGuard, roleGuard],
        data: { allowedRoles: ['ANALYST'] },
      },
      {
        path: 'registry/suspects/:id',
        component: SuspectDetailComponent,
        canActivate: [authGuard, roleGuard],
        data: { allowedRoles: ['ANALYST'] },
      },
      {
        path: 'registry/organizations',
        component: OrganizationsComponent,
        canActivate: [authGuard, roleGuard],
        data: { allowedRoles: ['ANALYST'] },
      },
      {
        path: 'registry/organizations/new',
        component: OrganizationFormComponent,
        canActivate: [authGuard, roleGuard],
        data: { allowedRoles: ['ANALYST'] },
      },
      {
        path: 'registry/organizations/:id/edit',
        component: OrganizationFormComponent,
        canActivate: [authGuard, roleGuard],
        data: { allowedRoles: ['ANALYST'] },
      },
      {
        path: 'registry/addresses',
        component: AddressesComponent,
        canActivate: [authGuard, roleGuard],
        data: { allowedRoles: ['ANALYST'] },
      },
      {
        path: 'registry/addresses/new',
        component: AddressFormComponent,
        canActivate: [authGuard, roleGuard],
        data: { allowedRoles: ['ANALYST'] },
      },
      {
        path: 'registry/addresses/:id/edit',
        component: AddressFormComponent,
        canActivate: [authGuard, roleGuard],
        data: { allowedRoles: ['ANALYST'] },
      },
      {
        path: 'support',
        component: SupportTicketComponent,
      },
      { path: 'linkAccount', component: LinkAccountComponent },
      { path: 'oauth-callback', component: OauthCallbackComponent },
      {
        path: 'reset-password',
        loadComponent: () =>
          import('./features/reset-password/reset-password.component').then(
            (m) => m.ResetPasswordComponent,
          ),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
