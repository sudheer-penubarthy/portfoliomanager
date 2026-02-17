import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, Subject, timer } from 'rxjs';
import { tap, switchMap } from 'rxjs/operators';

export interface User {
  id: number;
  email: string;
  name: string;
  pan: string;
  phone?: string;
  address?: string;
  kycStatus?: string;
}

export interface RegistrationRequest {
  email: string;
  pan: string;
  firstName: string;
  lastName: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  id: number;
  email: string;
  name: string;
  pan: string;
  token: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private currentUserSubject = new BehaviorSubject<User | null>(this.getUserFromStorage());
  public currentUser$ = this.currentUserSubject.asObservable();

  private currentTokenSubject = new BehaviorSubject<string | null>(this.getTokenFromStorage());
  public currentToken$ = this.currentTokenSubject.asObservable();

  private refreshTokenSubject = new BehaviorSubject<string | null>(this.getRefreshTokenFromStorage());
  private tokenExpirationSubject = new Subject<void>();

  private tokenRefreshTimer: any;
  private readonly REFRESH_THRESHOLD = 5 * 60 * 1000;  // Refresh 5 minutes before expiration
  private readonly TOKEN_EXPIRATION_KEY = 'token_expiration';

  constructor(private http: HttpClient) {
    // Start monitoring token expiration if user is logged in
    if (this.isLoggedIn()) {
      this.startTokenMonitoring();
    }
  }

  register(request: RegistrationRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/register', request).pipe(
      tap(response => {
        this.setUserAndToken(response);
        this.startTokenMonitoring();
      })
    );
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', request).pipe(
      tap(response => {
        this.setUserAndToken(response);
        this.startTokenMonitoring();
      })
    );
  }

  /**
   * Refresh access token using refresh token
   * @param refreshToken the refresh token
   * @returns Observable<AuthResponse> with new access token
   */
  refreshAccessToken(refreshToken?: string): Observable<any> {
    const token = refreshToken || this.getRefreshTokenFromStorage();

    if (!token) {
      // No refresh token available, need to login again
      this.logout();
      throw new Error('No refresh token available');
    }

    return this.http.post('/api/auth/refresh-token', { refreshToken: token }).pipe(
      tap((response: any) => {
        // Update access token
        localStorage.setItem('token', response.token);
        this.currentTokenSubject.next(response.token);

        // Reset token expiration monitoring
        this.startTokenMonitoring();
      })
    );
  }

  /**
   * Start monitoring token expiration and automatically refresh before it expires
   */
  private startTokenMonitoring(): void {
    // Clear existing timer
    if (this.tokenRefreshTimer) {
      clearTimeout(this.tokenRefreshTimer);
    }

    const expiresIn = localStorage.getItem('expiresIn');
    if (!expiresIn) {
      return;
    }

    const expirationTime = parseInt(expiresIn, 10) * 1000;  // Convert to milliseconds

    // Schedule refresh before token expires
    const refreshTime = Math.max(expirationTime - this.REFRESH_THRESHOLD, 1000);

    this.tokenRefreshTimer = setTimeout(() => {
      console.log('Token about to expire, attempting refresh...');
      const refreshToken = this.getRefreshTokenFromStorage();

      if (refreshToken) {
        this.refreshAccessToken(refreshToken).subscribe({
          next: () => {
            console.log('Token refreshed successfully');
          },
          error: (err) => {
            console.warn('Token refresh failed, logging out', err);
            this.logout();
          }
        });
      } else {
        console.warn('No refresh token available, logging out');
        this.logout();
      }
    }, refreshTime);
  }

  getCurrentUser(): Observable<User> {
    const userId = this.getCurrentUserId();
    return this.http.get<User>('/api/auth/me', { params: { userId: userId.toString() } });
  }

  updateProfile(user: User): Observable<User> {
    const userId = this.getCurrentUserId();
    return this.http.put<User>('/api/auth/profile', user, { params: { userId: userId.toString() } }).pipe(
      tap(response => {
        this.currentUserSubject.next(response);
      })
    );
  }

  logout(): void {
    // Clear token refresh timer
    if (this.tokenRefreshTimer) {
      clearTimeout(this.tokenRefreshTimer);
    }

    localStorage.removeItem('user');
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('expiresIn');
    localStorage.removeItem(this.TOKEN_EXPIRATION_KEY);

    this.currentUserSubject.next(null);
    this.currentTokenSubject.next(null);
    this.refreshTokenSubject.next(null);
  }

  private setUserAndToken(response: AuthResponse): void {
    const user: User = {
      id: response.id,
      email: response.email,
      name: response.name,
      pan: response.pan
    };

    localStorage.setItem('user', JSON.stringify(user));
    localStorage.setItem('token', response.token);
    localStorage.setItem('refreshToken', response.refreshToken);
    localStorage.setItem('expiresIn', response.expiresIn.toString());
    localStorage.setItem(this.TOKEN_EXPIRATION_KEY, (Date.now() + response.expiresIn * 1000).toString());

    this.currentUserSubject.next(user);
    this.currentTokenSubject.next(response.token);
    this.refreshTokenSubject.next(response.refreshToken);
  }

  private getUserFromStorage(): User | null {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  }

  private getTokenFromStorage(): string | null {
    return localStorage.getItem('token');
  }

  private getRefreshTokenFromStorage(): string | null {
    return localStorage.getItem('refreshToken');
  }

  getCurrentUserId(): number {
    const user = this.currentUserSubject.value;
    if (!user) {
      throw new Error('No user logged in');
    }
    return user.id;
  }

  isLoggedIn(): boolean {
    return this.currentUserSubject.value !== null;
  }

  /**
   * Check if token is valid (exists and not expired)
   */
  isTokenValid(): boolean {
    const token = localStorage.getItem('token');
    if (!token) {
      return false;
    }

    const expirationTime = localStorage.getItem(this.TOKEN_EXPIRATION_KEY);
    if (!expirationTime) {
      return true; // If no expiration set, assume valid
    }

    return Date.now() < parseInt(expirationTime, 10);
  }

  /**
   * Check if access token is about to expire
   */
  isTokenAboutToExpire(): boolean {
    const expirationTime = localStorage.getItem(this.TOKEN_EXPIRATION_KEY);
    if (!expirationTime) {
      return false;
    }

    const timeUntilExpiration = parseInt(expirationTime, 10) - Date.now();
    return timeUntilExpiration < this.REFRESH_THRESHOLD;
  }

  /**
   * Get remaining time for token in seconds
   */
  getTokenTimeRemaining(): number {
    const expirationTime = localStorage.getItem(this.TOKEN_EXPIRATION_KEY);
    if (!expirationTime) {
      return 0;
    }

    const timeRemaining = parseInt(expirationTime, 10) - Date.now();
    return Math.max(Math.floor(timeRemaining / 1000), 0);
  }
}

