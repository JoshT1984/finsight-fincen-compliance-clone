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

  get homePath(): string {
    return this.isLoggedIn() ? '/dashboard' : '';
  }

  /** Role-filtered nav links: Home first, then links allowed for current role. */
  get items(): { path: string; label: string }[] {
    const role = this.profile()?.roleName ?? null;
    const home = { path: this.homePath || '/dashboard', label: 'Home' };

    if (role === ROLES.COMPLIANCE_USER) {
      // Compliance users can upload PDFs, and READ-only view SARs/CTRs.
      // They should not have access to cases, registry, or the documents browser.
      const allowed = ALL_NAV_LINKS.filter(
        (l) => l.path === '/ctrs' || l.path === '/sars' || l.path === '/upload',
      );
      return [home, ...allowed];
    }
    if (role === ROLES.ANALYST) {
      // Analysts should have: HOME, CASES, SARS, CTRS, REGISTRY, DOCUMENTS.
      // (No Upload and no Officer Review.)
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
    // LAW_ENFORCEMENT_USER (or unknown): minimal set.
    // Per requirements, officers should only review referred cases and download documents.
    const leLinks = ALL_NAV_LINKS.filter((l) => l.path === '/officer' || l.path === '/documents');
    return [home, ...leLinks];
  }
}
