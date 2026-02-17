import { Injectable } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '@shared/services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(): boolean {
    const token = localStorage.getItem('token');
    if (token && this.authService.isTokenValid()) {
      return true;
    }
    this.router.navigate(['/login']);
    return false;
  }
}

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const token = localStorage.getItem('token');
  if (token && authService.isTokenValid()) {
    return true;
  }

  router.navigate(['/login']);
  return false;
};

