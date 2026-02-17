import { Routes } from '@angular/router';

export const PORTFOLIO_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/portfolio-view/portfolio-view.component').then(m => m.PortfolioViewComponent)
  },
  {
    path: 'fund/:schemeCode',
    loadComponent: () => import('./pages/fund-details/fund-details.component').then(m => m.FundDetailsComponent)
  }
];

