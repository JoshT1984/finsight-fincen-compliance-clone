import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { BehaviorSubject, map, Observable, tap, switchMap } from 'rxjs';
import { IdentityService } from './identity.service';
import { environment } from '../../../environment/environment';

@Injectable({ providedIn: 'root' })
export class LoginDialogService {
  private _isLoggedIn = new BehaviorSubject(false);
  isLoggedIn$ = this._isLoggedIn.asObservable();

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
          this._isLoggedIn.next(true);
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
    this._isLoggedIn.next(false);
    localStorage.removeItem('authToken');
  }

  private _loginOpen = signal(false);
  get loginOpen() {
    return this._loginOpen();
  }
}
