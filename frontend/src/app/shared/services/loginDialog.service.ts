import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { map, Observable, tap, switchMap } from 'rxjs';
import { IdentityService } from './identity.service';
import { environment } from '../../../environment/environment';

@Injectable({ providedIn: 'root' })
export class LoginDialogService {
  // Use IdentityService for login state and profile
  get isLoggedIn$() {
    return this.identityService.isLoggedIn$;
  }

  private readonly apiBaseUrl: string;

  private userId: string | null = null;

  constructor(
    private http: HttpClient,
    private identityService: IdentityService,
  ) {
    const base = environment.identityApiBaseUrl || '';
    this.apiBaseUrl = base ? `${base.replace(/\/$/, '')}` : '';
  }

  open() {
    this._loginOpen.set(true);
  }

  close() {
    this._loginOpen.set(false);
    this.closeForgotPassword();
  }

  login(email: string, password: string): Observable<string> {
    return this.http
      .post<{
        accessToken: string;
        userId: string;
        refreshToken: string;
      }>(`${this.apiBaseUrl}/auth/login`, { email, password })
      .pipe(
        tap((res) => {
          localStorage.setItem('authToken', res.accessToken);
          this.userId = res.userId;
          this.close();
        }),
        switchMap((res) => this.identityService.fetchAndSetProfile(res.userId)),
        map(() => ''),
      );
  }

  getUserId(): string | null {
    return this.userId;
  }

  logout() {
    localStorage.removeItem('authToken');
    this.identityService.clearProfile();
    // IdentityService will update isLoggedIn$ accordingly
  }

  private _loginOpen = signal(false);
  get loginOpen() {
    return this._loginOpen();
  }

  // Forgot password state
  forgotPasswordMode = signal(false);
  forgotPasswordEmail = signal('');
  forgotPasswordMessage = signal('');

  openForgotPassword() {
    this.forgotPasswordMode.set(true);
    this.forgotPasswordEmail.set('');
    this.forgotPasswordMessage.set('');
  }

  closeForgotPassword() {
    this.forgotPasswordMode.set(false);
    this.forgotPasswordEmail.set('');
    this.forgotPasswordMessage.set('');
  }

  requestPasswordReset(email: string): Observable<any> {
    return this.http.post(`${this.apiBaseUrl}/auth/forgot-password`, { email }).pipe(
      tap({
        next: () => {
          this.forgotPasswordMessage.set(
            'If the email address is found, an email should arrive soon with instructions.',
          );
        },
        error: () => {
          this.forgotPasswordMessage.set(
            'If the email address is found, an email should arrive soon with instructions.',
          );
        },
      }),
    );
  }
}
