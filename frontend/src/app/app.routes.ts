import { Routes } from '@angular/router';
import { CasesComponent } from './features/cases/cases.component';
import { CtrsComponent } from './features/ctrs/ctrs.component';
import { DocumentsComponent } from './features/documents/documents.component';
import { LandingComponent } from './features/landing/landing.component';
import { SarsComponent } from './features/sars/sars.component';
import { ShellComponent } from './layout/shell/shell.component';
import { ProfileComponent } from './features/profile/profile.component';

export const routes: Routes = [
  {
    path: '',
    component: ShellComponent,
    children: [
      { path: '', component: LandingComponent },
      { path: 'cases', component: CasesComponent },
      { path: 'sars', component: SarsComponent },
      { path: 'ctrs', component: CtrsComponent },
      { path: 'documents', component: DocumentsComponent },
      { path: 'profile', component: ProfileComponent },
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
