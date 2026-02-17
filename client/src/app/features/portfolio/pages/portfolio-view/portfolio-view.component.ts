import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router } from '@angular/router';
import { PortfolioService, PortfolioSummary } from '@shared/services/portfolio.service';
import { AuthService } from '@shared/services/auth.service';
import { inject } from '@angular/core';

@Component({
  selector: 'app-portfolio-view',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatProgressBarModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './portfolio-view.component.html',
  styleUrls: ['./portfolio-view.component.scss']
})
export class PortfolioViewComponent implements OnInit {
  private portfolioService: PortfolioService = inject(PortfolioService);
  private authService: AuthService = inject(AuthService);
  private router: Router = inject(Router);

  portfolio: PortfolioSummary | null = null;
  loading = true;
  error = '';

  ngOnInit(): void {
    const userId = this.authService.getCurrentUserId();
    this.portfolioService.getPortfolioSummary(userId).subscribe({
      next: (data: any) => {
        this.portfolio = data;
        this.loading = false;
      },
      error: (err: any) => {
        this.error = 'Failed to load portfolio data';
        this.loading = false;
        console.error(err);
      }
    });
  }

  viewFund(schemeCode: string): void {
    this.router.navigate(['/portfolio/fund', schemeCode]);
  }
}

