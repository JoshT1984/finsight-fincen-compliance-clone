import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { BehaviorSubject, map, Observable, tap } from 'rxjs';
import { environment } from '../../../environment/environment';
import { ProfileModel } from '../../models/profile.model';

@Injectable({ providedIn: 'root' })
export class IdentityService {
  private _isLoggedIn = new BehaviorSubject(false);
  isLoggedIn$ = this._isLoggedIn.asObservable();

  private profileSubject = new BehaviorSubject<ProfileModel | null>(null);
  profile$ = this.profileSubject.asObservable();

  private readonly apiBaseUrl: string;
  private readonly REDIRECT_URI: string;

  constructor(private http: HttpClient) {
    const base = environment.identityApiBaseUrl || '';
    this.apiBaseUrl = base ? `${base.replace(/\/$/, '')}` : '';
    this.REDIRECT_URI = 'http://localhost:8083/login/oauth2/code/google';
  }

  setProfile(profile: ProfileModel) {
    this.profileSubject.next(profile);
    this._isLoggedIn.next(true);
  }

  fetchAndSetProfile(userId: string) {
    return this.http
      .get<ProfileModel>(`${this.apiBaseUrl}/api/users/${userId}`, { withCredentials: true })
      .pipe(tap((profile) => this.setProfile(profile)));
  }

  setCurrentUserProfile() {
    return this.http
      .get<ProfileModel>(`${this.apiBaseUrl}/api/users/me`, { withCredentials: true })
      .pipe(tap((profile) => this.setProfile(profile)));
  }

  /** Fetch a user's profile by ID (e.g. for displaying actor names). Does not set current user profile. */
  getUserProfile(userId: string): Observable<ProfileModel> {
    return this.http.get<ProfileModel>(`${this.apiBaseUrl}/api/users/${userId}`, {
      withCredentials: true,
    });
  }

  clearProfile() {
    this.profileSubject.next(null);
    this._isLoggedIn.next(false);
  }

  getProfileSnapshot(): ProfileModel | null {
    return this.profileSubject.value;
  }

  checkAuthOnStartup() {
    // This endpoint should return the current user's profile if authenticated, or 401 if not
    if (!localStorage.getItem('authToken')) {
      this._isLoggedIn.next(false);
      return;
    }
    return this.http
      .get<ProfileModel>(`${this.apiBaseUrl}/api/users/me`, { withCredentials: true })
      .pipe(
        tap(
          (profile) => {
            this.setProfile(profile);
            this._isLoggedIn.next(true);
          },
          () => {
            this.clearProfile();
            this._isLoggedIn.next(false);
          },
        ),
      )
      .subscribe();
  }

  saveProfileUpdates(updatedProfile: ProfileModel): Observable<ProfileModel> {
    return this.http
      .put<ProfileModel>(`${this.apiBaseUrl}/api/users/me`, updatedProfile, {
        withCredentials: true,
      })
      .pipe(
        tap({
          next: (profile) => {
            this.setProfile(profile);
          },
          error: () => {
            console.error('Failed to save profile updates');
            /* Do not update local profile if backend fails */
          },
        }),
      );
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.put<void>(
      `${this.apiBaseUrl}/api/users/me/password`,
      { currentPassword, newPassword },
      { withCredentials: true },
    );
  }

  // Inactivity timer (15 minutes)
  private inactivityTimeoutMs = 15 * 60 * 1000;
  private inactivityTimer: any = null;

  /**
   * Call this once (e.g. in App constructor) to start tracking user activity for auto-logout.
   */
  startInactivityTracking() {
    const resetTimer = () => {
      if (this.inactivityTimer) {
        clearTimeout(this.inactivityTimer);
      }
      this.inactivityTimer = setTimeout(() => {
        this.handleInactivityLogout();
      }, this.inactivityTimeoutMs);
    };

    // Listen for user activity events
    ['mousemove', 'keydown', 'mousedown', 'touchstart', 'scroll'].forEach((event) => {
      window.addEventListener(event, resetTimer, true);
    });
    resetTimer(); // Start timer immediately
  }

  private handleInactivityLogout() {
    this.clearProfile();
    localStorage.removeItem('authToken');
    this._isLoggedIn.next(false);
    console.log('Logged out due to inactivity');
  }

  /**
   * Reset password using token and new password.
   */
  resetPassword(token: string, newPassword: string) {
    return this.http.post(`${this.apiBaseUrl}/auth/reset-password`, { token, newPassword });
  }

  /**
   * Initiates OAuth linking for a provider (e.g., Google, GitHub).
   * Redirects the user to the provider's login page.
   * @param provider 'google' | 'github'
   */
  linkProvider(provider: 'google' | 'github') {
    if (provider === 'google') {
      const jwt = localStorage.getItem('jwt'); // your internal JWT
      if (jwt !== null) {
        const state = encodeURIComponent(jwt); // simple, or use Base64
        window.location.href = `https://accounts.google.com/o/oauth2/v2/auth?
        client_id=516987948554-ndm6ct845c0angem1s92aver2i005c30.apps.googleusercontent.com
        &redirect_uri=${this.REDIRECT_URI}?mode=link
        &response_type=code
        &scope=openid email profile
        &state=${state}`;
      }
      window.location.href = `${this.apiBaseUrl}/oauth2/authorization/google?mode=link`;
    } else if (provider === 'github') {
      window.location.href = `${this.apiBaseUrl}/oauth2/authorization/github?mode=link`;
    }
  }

  /**
   * Checks if the current user has a linked provider (e.g., Google, GitHub) by querying the backend.
   * Returns an Observable<boolean>.
   */
  hasLinkedProvider(provider: 'google' | 'github'): Observable<boolean> {
    return this.http.get<boolean>(`${this.apiBaseUrl}/auth/oauth/linked/${provider}`, {
      withCredentials: true,
    });
  }

  /**
   * Initiates OAuth login for a provider (Google, GitHub).
   * Redirects the user to the backend endpoint to start the OAuth flow.
   */
  loginWithProvider(provider: 'google' | 'github'): void {
    window.location.href = `${this.apiBaseUrl}/oauth2/authorization/${provider}?mode=login`;
  }

  linkAccount(): Observable<any> {
    return this.http.post(this.apiBaseUrl + '/auth/oauth/link', {}, { withCredentials: true });
  }
}
