import { Component, computed, inject } from '@angular/core';
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
  { path: '/officer', label: 'Officer Review' },
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

  /** Role-filtered nav links: Home first, then links allowed for current role. Stable reference across CD. */
  readonly items = computed((): { path: string; label: string }[] => {
    const loggedIn = this.isLoggedIn();
    const role = this.profile()?.roleName ?? null;
    const homePath = loggedIn ? '/dashboard' : '';
    const home = { path: homePath || '/dashboard', label: 'Home' };

    if (role === ROLES.COMPLIANCE_USER) {
      const allowed = ALL_NAV_LINKS.filter(
        (l) => l.path === '/ctrs' || l.path === '/sars' || l.path === '/upload',
      );
      return [home, ...allowed];
    }
    if (role === ROLES.ANALYST) {
      const allowed = ALL_NAV_LINKS.filter(
        (l) =>
          l.path === '/cases' ||
          l.path === '/sars' ||
          l.path === '/ctrs' ||
          l.path === '/registry' ||
          l.path === '/documents',
      );
      return [home, ...allowed];
    }
    const leLinks = ALL_NAV_LINKS.filter((l) => l.path === '/officer' || l.path === '/documents');
    return [home, ...leLinks];
  });
}
