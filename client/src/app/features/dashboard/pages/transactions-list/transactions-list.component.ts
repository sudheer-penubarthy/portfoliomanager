import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { inject } from '@angular/core';

@Component({
  selector: 'app-transactions-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="transactions-container">
      <h1>Transaction History</h1>
      <mat-card>
        <mat-card-content>
          <p>Your transaction history will appear here.</p>
          <p>Upload transaction files to get started.</p>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .transactions-container {
      padding: 20px;
    }

    h1 {
      margin-bottom: 20px;
    }
  `]
})
export class TransactionsListComponent implements OnInit {
  ngOnInit(): void {
    // Component initialization
  }
}

