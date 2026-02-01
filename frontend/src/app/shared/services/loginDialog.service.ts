import { Injectable, signal } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class LoginDialogService {
  private _isLoggedIn = new BehaviorSubject(false);
  isLoggedIn$ = this._isLoggedIn.asObservable();

  open() {
    this._loginOpen.set(true);
  }

  close() {
    this._loginOpen.set(false);
  }

  login(email: string, password: string) {
    console.log('Logging in', email);
    this._isLoggedIn.next(true);
    this._loginOpen.set(false);
  }

  logout() {
    this._isLoggedIn.next(false);
  }

  private _loginOpen = signal(false);
  get loginOpen() {
    return this._loginOpen();
  }
}
