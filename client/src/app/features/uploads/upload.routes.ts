import { Routes } from '@angular/router';
import { authGuard } from '@shared/guards/auth.guard';

export const UPLOAD_ROUTES: Routes = [
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/upload-history/upload-history.component').then(m => m.UploadHistoryComponent)
  }
];

