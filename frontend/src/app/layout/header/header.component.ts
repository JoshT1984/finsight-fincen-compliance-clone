import { Component } from '@angular/core';
import { Router } from '@angular/router';

import { CommonModule } from '@angular/common';
import { LoginDialogService } from '../../shared/services/loginDialog.service';
import { IdentityService } from '../../shared/services/identity.service';
import { LoginDialogComponent } from '../../shared/login-dialog/login-dialog.component';

@Component({
  selector: 'app-header',
  standalone: true,
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css'],
  imports: [CommonModule, LoginDialogComponent],
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
}
