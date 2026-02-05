import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-oauth-callback',
  template: '',
  standalone: true
})
export class OauthCallbackComponent implements OnInit {
  constructor(private router: Router) {}

  ngOnInit() {
    const params = new URLSearchParams(window.location.search);
    const token = params.get('accessToken');
    if (token) {
      localStorage.setItem('authToken', token);
      this.router.navigate(['/profile']);
    } else {
      this.router.navigate(['/login']);
    }
  }
}
