import {
  HttpClient,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HttpErrorResponse,
} from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError, switchMap, finalize } from 'rxjs/operators';
import { environment } from '../../../environment/environment';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private isRefreshing = false;
  private readonly apiBaseUrl: string;

  constructor(private http: HttpClient) {
    const base = environment.identityApiBaseUrl || '';
    this.apiBaseUrl = base ? base.replace(/\/$/, '') : '';
  }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<any> {
    // Do NOT add Authorization header for login or refresh endpoints
    if (req.url.includes('/auth/login') || req.url.includes('/auth/refresh')) {
      return next.handle(req);
    }

    const token = localStorage.getItem('authToken');
    const authReq = token
      ? req.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`,
          },
        })
      : req;

    return next.handle(authReq).pipe(
      catchError((error: HttpErrorResponse) => {
        // Only attempt refresh once, and never for auth endpoints
        if (
          error.status === 401 &&
          !this.isRefreshing &&
          !authReq.url.includes('/auth/login') &&
          !authReq.url.includes('/auth/refresh')
        ) {
          this.isRefreshing = true;

          return this.refreshToken().pipe(
            switchMap((res) => {
              localStorage.setItem('authToken', res.accessToken);
              // Retry original request with NEW token
              return next.handle(
                authReq.clone({
                  setHeaders: {
                    Authorization: `Bearer ${res.accessToken}`,
                  },
                }),
              );
            }),
            catchError((refreshError) => {
              // If refresh also fails, clear token and trigger logout
              localStorage.removeItem('authToken');
              // Optionally, trigger a logout event or redirect here
              return throwError(() => refreshError);
            }),
            finalize(() => {
              this.isRefreshing = false;
            }),
          );
        }

        // If already tried refresh or not eligible, clear token and trigger logout
        if (error.status === 401) {
          localStorage.removeItem('authToken');
          // Optionally, trigger a logout event or redirect here
        }
        return throwError(() => error);
      }),
    );
  }

  private refreshToken() {
    // withCredentials ONLY belongs here if refresh uses cookies
    return this.http.post<{ accessToken: string }>(
      `${this.apiBaseUrl}/auth/refresh`,
      {},
      { withCredentials: true },
    );
  }
}
