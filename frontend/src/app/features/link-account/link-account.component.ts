import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { IdentityService } from '../../shared/services/identity.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-link-account',
  templateUrl: './link-account.component.html',
  standalone: true,
  styleUrls: ['./link-account.component.css'],
  imports: [RouterLink, CommonModule],
})
export class LinkAccountComponent {
  constructor(
    private identityService: IdentityService,
    private router: Router,
  ) {}

  linkAccount() {
    this.identityService.linkAccount().subscribe({
      next: () => {
        if (window.confirm('Account linked successfully!')) {
          this.router.navigate(['/dashboard']);
        }
      },
      error: () => {
        alert('Failed to link account.');
      },
    });
  }

  routeToHome() {
    this.router.navigate(['/']);
  }

  get isLoggedIn() {
    return this.identityService.getProfileSnapshot() !== null;
  }
}
