import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { IdentityService } from '../../shared/services/identity.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-reset-password',
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.css'],
  imports: [CommonModule, FormsModule],
})
export class ResetPasswordComponent {
  password: string = '';
  confirmPassword: string = '';
  error: string = '';
  success: boolean = false;
  loading: boolean = false;
  token: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private identity: IdentityService,
    private router: Router,
  ) {
    this.token = this.route.snapshot.queryParamMap.get('token');
  }

  submit() {
    this.error = '';
    if (!this.password || !this.confirmPassword) {
      this.error = 'Please enter both fields.';
      return;
    }
    if (this.password !== this.confirmPassword) {
      this.error = 'Passwords do not match.';
      return;
    }
    if (!this.token) {
      this.error = 'Invalid or missing token.';
      return;
    }
    this.loading = true;
    this.identity.resetPassword(this.token!, this.password).subscribe({
      next: () => {
        this.success = true;
        setTimeout(() => this.router.navigate(['/login']), 2000);
      },
      error: () => {
        this.error = 'Reset failed. Token may be invalid or expired.';
        this.loading = false;
      },
    });
  }
}
