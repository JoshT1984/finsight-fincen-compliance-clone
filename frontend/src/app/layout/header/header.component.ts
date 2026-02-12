import { Component } from '@angular/core';
import { Router } from '@angular/router';

import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

import { LoginDialogService } from '../../shared/services/loginDialog.service';
import { IdentityService } from '../../shared/services/identity.service';

@Component({
  selector: 'app-header',
  standalone: true,
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css'],
  imports: [CommonModule, MatIconModule, MatTooltipModule],
})
export class HeaderComponent {
  constructor(
    public loginDialog: LoginDialogService,
    public identityService: IdentityService,
    private router: Router,
  ) {}

  login() {
    this.loginDialog.open();
  }

  logout() {
    this.loginDialog.logout();
    this.router.navigate(['/home']);
  }

  openProfile() {
    this.router.navigate(['/profile']);
  }

  // roleId == app_user
  roleIcon(roleId: number | null | undefined): string {
    switch (roleId) {
      case 1:
        return 'analytics';
      case 2:
        return 'gavel';
      case 3:
        return 'local_police';
      default:
        return 'person';
    }
  }

  roleTooltip(roleId: number | null | undefined): string {
    switch (roleId) {
      case 1:
        return 'Analyst User';
      case 2:
        return 'Compliance User';
      case 3:
        return 'Law Enforcement User';
      default:
        return 'User';
    }
  }
}
