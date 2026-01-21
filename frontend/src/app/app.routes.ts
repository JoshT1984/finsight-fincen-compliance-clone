import { Routes } from '@angular/router';
import { LandingComponent } from './features/landing/landing.component';
import { ShellComponent } from './layout/shell/shell.component';

export const routes: Routes = [
  {
    path: '',
    component: ShellComponent,
    children: [{ path: '', component: LandingComponent }],
  },
  { path: '**', redirectTo: '' },
];
