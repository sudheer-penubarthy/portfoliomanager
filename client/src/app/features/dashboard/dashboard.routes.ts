import { Routes } from '@angular/router';

export const DASHBOARD_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },
  {
    path: 'upload',
    loadComponent: () => import('./pages/upload-transactions/upload-transactions.component').then(m => m.UploadTransactionsComponent)
  },
  {
    path: 'transactions',
    loadComponent: () => import('./pages/transactions-list/transactions-list.component').then(m => m.TransactionsListComponent)
  },
  {
    path: 'analytics',
    loadComponent: () => import('./pages/analytics/analytics.component').then(m => m.AnalyticsComponent)
  },
  {
    path: 'holdings',
    loadComponent: () => import('./pages/holdings-summary/holdings-summary.component').then(m => m.HoldingsSummaryComponent)
  }
];

