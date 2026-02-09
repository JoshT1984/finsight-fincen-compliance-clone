import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChangeDetectorRef, Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { LoginDialogService } from '../services/loginDialog.service';
import { IdentityService } from '../services/identity.service';

@Component({
  selector: 'app-login-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login-dialog.component.html',
  styleUrls: ['./login-dialog.component.css'],
})
export class LoginDialogComponent {
  loginError: string | null = null;
  email = '';
  password = '';

  constructor(
    private loginDialog: LoginDialogService,
    private identityService: IdentityService,
    private cdr: ChangeDetectorRef,
    private router: Router,
  ) {}

  close() {
    this.loginDialog.close();
  }

  login() {
    this.loginError = null;
    this.loginDialog.login(this.email, this.password).subscribe({
      next: (token: string) => {
        this.router.navigate(['/dashboard']);
      },
      error: (error: any) => {
        if (error && error.error && error.error.message) {
          this.loginError = error.error.message;
        } else {
          this.loginError = 'Login failed. Please check your credentials.';
        }
        this.cdr.detectChanges();
      },
    });
  }

  loginWithGoogle() {
    localStorage.setItem('postLoginRedirect', '/dashboard');
    this.identityService.loginWithProvider('google');
  }

  loginWithGithub() {
    localStorage.setItem('postLoginRedirect', '/dashboard');
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
