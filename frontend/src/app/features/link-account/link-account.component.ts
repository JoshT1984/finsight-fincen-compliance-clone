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
    const sessionInfo = JSON.parse(sessionStorage.getItem('sessionInfo') || '{}');
    const authToken = localStorage.getItem('authToken');
    this.identityService.linkAccount().subscribe({
      next: () => {
        alert('Account linked successfully!');
        this.router.navigate(['/']);
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
