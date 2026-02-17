import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialogModule } from '@angular/material/dialog';
import { PortfolioService, TransactionDetails } from '@shared/services/portfolio.service';
import { AuthService } from '@shared/services/auth.service';
import { inject } from '@angular/core';

@Component({
  selector: 'app-fund-details',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule
  ],
  templateUrl: './fund-details.component.html',
  styleUrls: ['./fund-details.component.scss']
})
export class FundDetailsComponent implements OnInit {
  private route: ActivatedRoute = inject(ActivatedRoute);
  private portfolioService: PortfolioService = inject(PortfolioService);
  private authService: AuthService = inject(AuthService);
  private router: Router = inject(Router);

  schemeCode: string = '';
  transactions: TransactionDetails[] = [];
  loading = true;
  error = '';
  displayedColumns: string[] = ['txnDate', 'txnType', 'units', 'amount', 'pricePerUnit'];

  ngOnInit(): void {
    this.schemeCode = this.route.snapshot.paramMap.get('schemeCode') || '';
    this.loadTransactions();
  }

  loadTransactions(): void {
    const userId = this.authService.getCurrentUserId();
    this.portfolioService.getFundTransactions(userId, this.schemeCode).subscribe({
      next: (data: any) => {
        this.transactions = data;
        this.loading = false;
      },
      error: (err: any) => {
        this.error = 'Failed to load transaction details';
        this.loading = false;
        console.error(err);
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/portfolio']);
  }
}

