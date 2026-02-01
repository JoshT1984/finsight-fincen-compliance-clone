import { Component } from '@angular/core';

import { CommonModule } from '@angular/common';
import { LoginDialogService } from '../../shared/services/loginDialog.service';
import { LoginDialogComponent } from '../../shared/login-dialog/login-dialog.component';

@Component({
  selector: 'app-header',
  standalone: true,
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css'],
  imports: [CommonModule, LoginDialogComponent],
})
export class HeaderComponent {
  constructor(public loginDialog: LoginDialogService) {}

  login() {
    this.loginDialog.open();
  }

  logout() {
    this.loginDialog.logout();
  }
}
