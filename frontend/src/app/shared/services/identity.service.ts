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

  constructor(private http: HttpClient) {
    const base = environment.identityApiBaseUrl || '';
    this.apiBaseUrl = base ? `${base.replace(/\/$/, '')}` : '';
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
}
