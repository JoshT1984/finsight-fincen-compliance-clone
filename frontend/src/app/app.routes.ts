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
import { ShellComponent } from './layout/shell/shell.component';
import { UploadComponent } from './features/upload/upload.component';
import { ProfileComponent } from './features/profile/profile.component';
import { LinkAccountComponent } from './features/link-account/link-account.component';
import { OauthCallbackComponent } from './features/oauth-callback/oauth-callback.component';
import { SupportTicketComponent } from './features/support-ticket/support-ticket.component';
import { authGuard } from './shared/guards/auth.guard';
import { publicLandingGuard } from './shared/guards/public-landing.guard';

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
        canActivate: [authGuard],
      },
      {
        path: 'cases/:id',
        component: CaseDetailComponent,
        resolve: { case: caseResolver },
        canActivate: [authGuard],
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
        canActivate: [authGuard],
      },
      {
        path: 'ctrs',
        component: CtrsComponent,
        canActivate: [authGuard],
      },
      {
        path: 'documents',
        component: DocumentsComponent,
        canActivate: [authGuard],
      },
      {
        path: 'upload',
        component: UploadComponent,
        canActivate: [authGuard],
      },
      {
        path: 'profile',
        component: ProfileComponent,
        canActivate: [authGuard],
      },
      {
        path: 'support',
        component: SupportTicketComponent,
      },
      { 
        path: 'linkAccount', component: LinkAccountComponent 
      },
      { 
        path: 'oauth-callback', component: OauthCallbackComponent 
      },
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
