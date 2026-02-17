import { Routes } from '@angular/router';
import { authGuard } from '@shared/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'landing',
    pathMatch: 'full'
  },
  {
    path: 'landing',
    loadComponent: () => import('./features/landing/landing.component').then(m => m.LandingComponent)
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/pages/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/pages/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES)
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadChildren: () => import('./features/dashboard/dashboard.routes').then(m => m.DASHBOARD_ROUTES)
  },
  {
    path: 'portfolio',
    canActivate: [authGuard],
    loadChildren: () => import('./features/portfolio/portfolio.routes').then(m => m.PORTFOLIO_ROUTES)
  },
  {
    path: 'goals',
    canActivate: [authGuard],
    loadChildren: () => import('./features/goals/goals.routes').then(m => m.GOALS_ROUTES)
  },
  {
    path: '**',
    redirectTo: 'landing'
  }
];

