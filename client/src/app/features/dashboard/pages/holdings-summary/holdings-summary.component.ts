import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { inject } from '@angular/core';

@Component({
  selector: 'app-holdings-summary',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="holdings-container">
      <h1>Holdings Summary</h1>
      <mat-card>
        <mat-card-content>
          <p>Your current holdings will be displayed here.</p>
          <p>Upload valuation files to see holdings summary.</p>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .holdings-container {
      padding: 20px;
    }

    h1 {
      margin-bottom: 20px;
    }
  `]
})
export class HoldingsSummaryComponent implements OnInit {
  ngOnInit(): void {
    // Component initialization
  }
}

