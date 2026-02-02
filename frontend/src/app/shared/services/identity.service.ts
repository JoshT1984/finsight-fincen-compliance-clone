import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { BehaviorSubject, map, Observable, tap } from 'rxjs';
import { environment } from '../../../environment/environment';
import { ProfileModel } from '../../features/profile/profile.model';

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
  }

  fetchAndSetProfile(userId: string) {
    return this.http
      .get<ProfileModel>(`${this.apiBaseUrl}/api/users/${userId}`, { withCredentials: true })
      .pipe(tap((profile) => this.setProfile(profile)));
  }

  clearProfile() {
    this.profileSubject.next(null);
  }

  getProfileSnapshot(): ProfileModel | null {
    return this.profileSubject.value;
  }
}
