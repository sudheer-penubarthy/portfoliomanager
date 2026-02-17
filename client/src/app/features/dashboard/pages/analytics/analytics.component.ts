import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { inject } from '@angular/core';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="analytics-container">
      <h1>Portfolio Analytics</h1>
      <mat-card>
        <mat-card-content>
          <p>Analytics dashboard will display portfolio performance metrics.</p>
          <p>Upload transactions to see detailed analytics.</p>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .analytics-container {
      padding: 20px;
    }

    h1 {
      margin-bottom: 20px;
    }
  `]
})
export class AnalyticsComponent implements OnInit {
  ngOnInit(): void {
    // Component initialization
  }
}

