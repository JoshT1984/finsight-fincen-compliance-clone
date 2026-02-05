import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Component } from '@angular/core';
import { LoginDialogService } from '../services/loginDialog.service';
import { IdentityService } from '../services/identity.service';

@Component({
  selector: 'app-login-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login-dialog.component.html',
  styleUrls: ['./login-dialog.component.css'],
})
export class LoginDialogComponent {
  email = '';
  password = '';

  constructor(
    private loginDialog: LoginDialogService,
    private identityService: IdentityService,
  ) {}

  close() {
    this.loginDialog.close();
  }

  login() {
    this.loginDialog.login(this.email, this.password).subscribe({
      next: (token: string) => {},
      error: (error: any) => {
        console.error('Login failed', error);
      },
    });
  }

  loginWithGoogle() {
    this.identityService.loginWithProvider('google');
  }

  loginWithGithub() {
    this.identityService.loginWithProvider('github');
  }

  forgotPasswordEmail = '';

  get forgotPasswordMode() {
    return this.loginDialog.forgotPasswordMode();
  }
  get forgotPasswordMessage() {
    return this.loginDialog.forgotPasswordMessage();
  }

  openForgotPassword() {
    this.loginDialog.openForgotPassword();
  }
  closeForgotPassword() {
    this.loginDialog.closeForgotPassword();
  }
  requestPasswordReset() {
    this.loginDialog.requestPasswordReset(this.forgotPasswordEmail).subscribe();
  }
}
