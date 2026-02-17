import { HttpInterceptorFn, HttpErrorResponse, HttpEvent, HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { catchError, switchMap, throwError, BehaviorSubject, filter, take, Observable } from 'rxjs';

let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<any>(null);

export const authInterceptor: HttpInterceptorFn = (req, next): Observable<HttpEvent<any>> => {
  const authService = inject(AuthService);
  const token = localStorage.getItem('token');

  // Add token to request if available and not an auth endpoint
  if (token && !isAuthEndpoint(req.url)) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !isAuthEndpoint(req.url)) {
        // Token expired or invalid, try to refresh
        return handle401Error(req, next, authService);
      }
      return throwError(() => error);
    })
  );
};

/**
 * Handle 401 Unauthorized errors
 */
function handle401Error(req: HttpRequest<any>, next: HttpHandlerFn, authService: AuthService): Observable<HttpEvent<any>> {
  if (!isRefreshing) {
    isRefreshing = true;
    refreshTokenSubject.next(null);

    const refreshToken = localStorage.getItem('refreshToken');
    if (refreshToken) {
      return authService.refreshAccessToken(refreshToken).pipe(
        switchMap((response: any) => {
          isRefreshing = false;
          refreshTokenSubject.next(response.token);

          // Retry original request with new token
          return next(
            req.clone({
              setHeaders: {
                Authorization: `Bearer ${response.token}`
              }
            })
          );
        }),
        catchError((err: any) => {
          isRefreshing = false;
          authService.logout();
          return throwError(() => err);
        })
      );
    } else {
      isRefreshing = false;
      authService.logout();
      return throwError(() => new Error('No refresh token available'));
    }
  } else {
    // Wait for token refresh to complete
    return refreshTokenSubject.pipe(
      filter(token => token != null),
      take(1),
      switchMap((token) => {
        return next(
          req.clone({
            setHeaders: {
              Authorization: `Bearer ${token}`
            }
          })
        );
      })
    );
  }
}

/**
 * Check if URL is an auth endpoint (should skip token addition)
 */
function isAuthEndpoint(url: string): boolean {
  const authEndpoints = ['/api/auth/login', '/api/auth/register', '/api/auth/refresh-token'];
  return authEndpoints.some(endpoint => url.includes(endpoint));
}

