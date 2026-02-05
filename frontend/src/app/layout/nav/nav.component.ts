import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { NgFor } from '@angular/common';
import { IdentityService } from '../../shared/services/identity.service';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-nav',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, NgFor],
  templateUrl: './nav.component.html',
  styleUrls: ['./nav.component.css'],
})
export class NavComponent {
  private identity = inject(IdentityService);
  private isLoggedIn = toSignal(this.identity.isLoggedIn$);

  navItems = [
    { path: '/dashboard', label: 'Home' },
    { path: '/cases', label: 'Cases' },
    { path: '/sars', label: 'SARs' },
    { path: '/ctrs', label: 'CTRs' },
    { path: '/documents', label: 'Documents' },
    { path: '/upload', label: 'Upload' },
  ];

  get homePath(): string {
    return this.isLoggedIn() ? '/dashboard' : '';
  }

  get items(): { path: string; label: string }[] {
    const [home, ...rest] = this.navItems;
    return [{ path: this.homePath, label: home.label }, ...rest];
  }
}
