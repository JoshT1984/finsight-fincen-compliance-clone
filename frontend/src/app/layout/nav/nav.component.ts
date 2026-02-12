import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { NgFor } from '@angular/common';
import { SearchButtonComponent } from './search-button.component';
import { IdentityService } from '../../shared/services/identity.service';
import { ROLES } from '../../shared/services/role.service';
import { toSignal } from '@angular/core/rxjs-interop';

const ALL_NAV_LINKS = [
  { path: '/cases', label: 'Cases' },
  { path: '/sars', label: 'SARs' },
  { path: '/ctrs', label: 'CTRs' },
  { path: '/registry', label: 'Registry' },
  { path: '/documents', label: 'Documents' },
  { path: '/upload', label: 'Upload' },
] as const;

@Component({
  selector: 'app-nav',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, NgFor, SearchButtonComponent],
  templateUrl: './nav.component.html',
  styleUrls: ['./nav.component.css'],
})
export class NavComponent {
  private identity = inject(IdentityService);
  private isLoggedIn = toSignal(this.identity.isLoggedIn$);
  private profile = toSignal(this.identity.profile$, { initialValue: null });

  get homePath(): string {
    return this.isLoggedIn() ? '/dashboard' : '';
  }

  /** Role-filtered nav links: Home first, then links allowed for current role. */
  get items(): { path: string; label: string }[] {
    const role = this.profile()?.roleName ?? null;
    const home = { path: this.homePath || '/dashboard', label: 'Home' };

    if (role === ROLES.COMPLIANCE_USER) {
      const allowed = ALL_NAV_LINKS.filter(
        (l) => l.path === '/ctrs' || l.path === '/sars' || l.path === '/documents' || l.path === '/upload'
      );
      return [home, ...allowed];
    }
    if (role === ROLES.ANALYST) {
      return [home, ...ALL_NAV_LINKS];
    }
    // LAW_ENFORCEMENT_USER or unknown: minimal set
    const leLinks = ALL_NAV_LINKS.filter(
      (l) => l.path === '/cases' || l.path === '/documents'
    );
    return [home, ...leLinks];
  }
}
