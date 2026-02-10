import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { IdentityService } from '../../shared/services/identity.service';

@Component({
  selector: 'app-oauth-callback',
  template: '',
  standalone: true,
  imports: [],
})
export class OauthCallbackComponent implements OnInit {
  constructor(
    private router: Router,
    private identityService: IdentityService,
  ) {}

  ngOnInit() {
    const params = new URLSearchParams(window.location.search);
    const token = params.get('accessToken');
    if (token) {
      localStorage.setItem('authToken', token);
      this.identityService.setCurrentUserProfile().subscribe(() => {
        this.router.navigate(['/dashboard']);
      });
    } else {
      this.router.navigate(['/']);
    }
  }
}
