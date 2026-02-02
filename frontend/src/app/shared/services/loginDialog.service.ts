import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { environment } from '../../../environment/environment';

@Injectable({ providedIn: 'root' })
export class LoginDialogService {
  private _isLoggedIn = new BehaviorSubject(false);
  isLoggedIn$ = this._isLoggedIn.asObservable();

  private readonly apiBaseUrl: string;

  constructor(private http: HttpClient) {
    const base = environment.identityApiBaseUrl || '';
    this.apiBaseUrl = base ? `${base.replace(/\/$/, '')}` : '';
  }

  open() {
    this._loginOpen.set(true);
  }

  close() {
    this._loginOpen.set(false);
  }

  login(email: string, password: string) {
    // this._loginOpen.set(false);
    console.log('Attempting login ', `${this.apiBaseUrl}/auth/login`);
    return this.http
      .post<{ token: string }>(`${this.apiBaseUrl}/auth/login`, { email, password })
      .subscribe({
        next: (response) => {
          localStorage.setItem('authToken', response.token);
          this._isLoggedIn.next(true);
          this.close();
        },
        error: (error) => {
          if (error.status === 401) {
            alert('Invalid email or password. Please try again.');
          }
          console.error('Login failed', error);
        },
      });
  }

  logout() {
    this._isLoggedIn.next(false);
    localStorage.removeItem('authToken');
  }

  private _loginOpen = signal(false);
  get loginOpen() {
    return this._loginOpen();
  }
}
