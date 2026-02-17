import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, filter, take, switchMap } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

/**
 * HTTP Interceptor for JWT Token Authorization
 * Automatically adds JWT token to all requests and handles token refresh
 */
@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  private isRefreshing = false;
  private refreshTokenSubject: BehaviorSubject<any> = new BehaviorSubject<any>(null);

  constructor(private authService: AuthService) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    // Skip token addition for auth endpoints
    if (this.isAuthEndpoint(request)) {
      return next.handle(request);
    }

    // Add JWT token to request if available
    const token = localStorage.getItem('token');
    if (token) {
      request = this.addToken(request, token);
    }

    return next.handle(request).pipe(
      catchError(error => {
        if (error instanceof HttpErrorResponse && error.status === 401) {
          // Token expired or invalid, try to refresh
          return this.handleTokenExpiration(request, next);
        }
        return throwError(() => error);
      })
    );
  }

  /**
   * Add JWT token to request header
   */
  private addToken(request: HttpRequest<any>, token: string): HttpRequest<any> {
    return request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  /**
   * Handle 401 error by attempting token refresh
   */
  private handleTokenExpiration(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshTokenSubject.next(null);

      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        return this.authService.refreshAccessToken(refreshToken).pipe(
          switchMap((response: any) => {
            this.isRefreshing = false;
            this.refreshTokenSubject.next(response.token);
            return next.handle(this.addToken(request, response.token));
          }),
          catchError((err) => {
            this.isRefreshing = false;
            this.authService.logout();
            return throwError(() => err);
          })
        );
      } else {
        this.isRefreshing = false;
        this.authService.logout();
        return throwError(() => new Error('No refresh token available'));
      }
    } else {
      // Wait for token refresh to complete, then retry request
      return this.refreshTokenSubject.pipe(
        filter(token => token != null),
        take(1),
        switchMap((token) => {
          return next.handle(this.addToken(request, token));
        })
      );
    }
  }

  /**
   * Check if request is to auth endpoint
   */
  private isAuthEndpoint(request: HttpRequest<any>): boolean {
    const authEndpoints = ['/api/auth/login', '/api/auth/register', '/api/auth/refresh-token'];
    return authEndpoints.some(endpoint => request.url.includes(endpoint));
  }
}

