import { ChangeDetectorRef, Component, NgZone, OnInit, Optional } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { PortfolioService, TransactionDetails } from '@shared/services/portfolio.service';
import { AuthService } from '@shared/services/auth.service';
import { inject } from '@angular/core';

interface FundDetailsDialogData {
  schemeCode: string;
  schemeName?: string;
}

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
  private route: ActivatedRoute | null = inject(ActivatedRoute, { optional: true });
  private portfolioService: PortfolioService = inject(PortfolioService);
  private authService: AuthService = inject(AuthService);
  private router: Router | null = inject(Router, { optional: true });
  private changeDetectorRef: ChangeDetectorRef = inject(ChangeDetectorRef);
  private ngZone: NgZone = inject(NgZone);
  private dialogRef = inject(MatDialogRef<FundDetailsComponent>, { optional: true });
  private dialogData = inject<FundDetailsDialogData | null>(MAT_DIALOG_DATA, { optional: true });

  schemeCode: string = '';
  schemeName: string = '';
  transactions: TransactionDetails[] = [];
  loading = true;
  error = '';
  displayedColumns: string[] = ['txnDate', 'txnType', 'units', 'amount', 'pricePerUnit', 'remarks'];

  get isDialogMode(): boolean {
    return !!this.dialogRef;
  }

  get netUnits(): number {
    return this.transactions.reduce((total, txn) => total + (txn.units || 0), 0);
  }

  get netAmount(): number {
    return this.transactions.reduce((total, txn) => total + (txn.amount || 0), 0);
  }

  get buyCount(): number {
    return this.transactions.filter(txn => txn.txnType === 'BUY' || txn.txnType === 'SWITCH_IN').length;
  }

  get sellCount(): number {
    return this.transactions.filter(txn => txn.txnType === 'SELL' || txn.txnType === 'SWITCH_OUT').length;
  }

  ngOnInit(): void {
    this.schemeCode = this.dialogData?.schemeCode || this.route?.snapshot.paramMap.get('schemeCode') || '';
    this.schemeName = this.dialogData?.schemeName || this.schemeCode;
    this.loadTransactions();
  }

  loadTransactions(): void {
    const userId = this.authService.getCurrentUserId();
    this.portfolioService.getFundTransactions(userId, this.schemeCode).subscribe({
      next: (data: any) => {
        this.ngZone.run(() => {
          this.transactions = data;
          if (data.length > 0 && data[0].schemeName) {
            this.schemeName = data[0].schemeName;
          }
          this.loading = false;
          this.changeDetectorRef.detectChanges();
        });
      },
      error: (err: any) => {
        this.ngZone.run(() => {
          this.error = 'Failed to load transaction details';
          this.loading = false;
          console.error(err);
          this.changeDetectorRef.detectChanges();
        });
      }
    });
  }

  goBack(): void {
    if (this.dialogRef) {
      this.dialogRef.close();
      return;
    }
    this.router?.navigate(['/portfolio']);
  }
}

