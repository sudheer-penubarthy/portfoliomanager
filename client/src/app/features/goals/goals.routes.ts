import { Routes } from '@angular/router';

export const GOALS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/goals-list/goals-list.component').then(m => m.GoalsListComponent)
  },
  {
    path: 'create',
    loadComponent: () => import('./pages/goal-form/goal-form.component').then(m => m.GoalFormComponent)
  },
  {
    path: ':id',
    loadComponent: () => import('./pages/goal-details/goal-details.component').then(m => m.GoalDetailsComponent)
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./pages/goal-form/goal-form.component').then(m => m.GoalFormComponent)
  }
];

