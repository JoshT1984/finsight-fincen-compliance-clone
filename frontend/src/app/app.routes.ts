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
import { SarsComponent } from './features/sars/sars.component';
import { ShellComponent } from './layout/shell/shell.component';
import { UploadComponent } from './features/upload/upload.component';
import { ProfileComponent } from './features/profile/profile.component';

export const routes: Routes = [
  {
    path: '',
    component: ShellComponent,
    children: [
      { path: '', component: LandingComponent },
      { path: 'cases', component: CasesComponent },
      {
        path: 'cases/:id',
        component: CaseDetailComponent,
        resolve: { case: caseResolver },
        children: [
          { path: '', redirectTo: 'overview', pathMatch: 'full' },
          { path: 'overview', component: CaseDetailOverviewComponent },
          { path: 'audit', component: CaseDetailAuditComponent },
          { path: 'notes', component: CaseDetailNotesComponent },
          { path: 'documents', component: CaseDetailDocumentsComponent },
        ],
      },
      { path: 'sars', component: SarsComponent },
      { path: 'ctrs', component: CtrsComponent },
      { path: 'documents', component: DocumentsComponent },
      { path: 'upload', component: UploadComponent },
      { path: 'profile', component: ProfileComponent },
    ],
  },
  { path: '**', redirectTo: '' },
];
