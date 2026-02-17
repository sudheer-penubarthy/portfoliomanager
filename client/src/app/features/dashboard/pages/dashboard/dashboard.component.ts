import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, RouterModule } from '@angular/router';
import { inject } from '@angular/core';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatProgressBarModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="dashboard-container">
      <h1>Portfolio Dashboard</h1>
      <div class="dashboard-grid">
        <mat-card class="dashboard-card">
          <mat-card-header>
            <mat-card-title>Upload Transactions</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <p>Import transaction data from your investment accounts</p>
          </mat-card-content>
          <mat-card-actions>
            <button mat-raised-button color="primary" (click)="navigateTo('upload')">
              <mat-icon>cloud_upload</mat-icon> Upload
            </button>
          </mat-card-actions>
        </mat-card>

        <mat-card class="dashboard-card">
          <mat-card-header>
            <mat-card-title>Portfolio Overview</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <p>View your complete portfolio allocation</p>
          </mat-card-content>
          <mat-card-actions>
            <button mat-raised-button color="primary" [routerLink]="['/portfolio']">
              <mat-icon>assessment</mat-icon> View Portfolio
            </button>
          </mat-card-actions>
        </mat-card>

        <mat-card class="dashboard-card">
          <mat-card-header>
            <mat-card-title>Financial Goals</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <p>Track your investment goals</p>
          </mat-card-content>
          <mat-card-actions>
            <button mat-raised-button color="primary" [routerLink]="['/goals']">
              <mat-icon>trending_up</mat-icon> View Goals
            </button>
          </mat-card-actions>
        </mat-card>

        <mat-card class="dashboard-card">
          <mat-card-header>
            <mat-card-title>Analytics</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <p>Detailed portfolio analytics and reports</p>
          </mat-card-content>
          <mat-card-actions>
            <button mat-raised-button color="primary" (click)="navigateTo('analytics')">
              <mat-icon>insights</mat-icon> View Analytics
            </button>
          </mat-card-actions>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .dashboard-container {
      padding: 20px;
    }

    h1 {
      margin-bottom: 30px;
      color: #333;
    }

    .dashboard-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 20px;
    }

    .dashboard-card {
      cursor: pointer;
      transition: transform 0.2s, box-shadow 0.2s;
    }

    .dashboard-card:hover {
      transform: translateY(-4px);
      box-shadow: 0 4px 20px rgba(0,0,0,0.1);
    }

    mat-card-header {
      margin-bottom: 10px;
    }

    mat-card-actions {
      padding: 8px 16px;
    }

    button {
      width: 100%;
    }
  `]
})
export class DashboardComponent implements OnInit {
  private router = inject(Router);

  ngOnInit(): void {
    // Component initialization
  }

  navigateTo(path: string): void {
    this.router.navigate(['/dashboard', path]);
  }
}


