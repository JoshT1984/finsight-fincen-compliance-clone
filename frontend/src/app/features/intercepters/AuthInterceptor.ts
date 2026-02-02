import { HttpClient, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { throwError } from 'rxjs/internal/observable/throwError';
import { catchError } from 'rxjs/internal/operators/catchError';
import { switchMap } from 'rxjs/internal/operators/switchMap';
import { environment } from '../../../environment/environment';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private isRefreshing = false;
  private readonly apiBaseUrl: string;

  constructor(private http: HttpClient) {
    const base = environment.identityApiBaseUrl || '';
    this.apiBaseUrl = base ? `${base.replace(/\/$/, '')}` : '';
  }

  intercept(req: HttpRequest<any>, next: HttpHandler) {
    const token = localStorage.getItem('authToken');

    const authReq = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

    return next.handle(authReq).pipe(
      catchError((error) => {
        if (error.status === 401 && !this.isRefreshing) {
          this.isRefreshing = true;
          return this.refreshToken().pipe(
            switchMap((res: { accessToken: string }) => {
              localStorage.setItem('authToken', res.accessToken);
              this.isRefreshing = false;
              return next.handle(
                authReq.clone({
                  setHeaders: {
                    Authorization: `Bearer ${res.accessToken}`,
                  },
                }),
              );
            }),
          );
        }
        return throwError(() => error);
      }),
    );
  }

  refreshToken() {
    return this.http.post<{ accessToken: string }>(
      `${this.apiBaseUrl}/auth/refresh`,
      {},
      { withCredentials: true },
    );
  }
}
