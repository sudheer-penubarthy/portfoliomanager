import { Component, OnInit, ChangeDetectorRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { PortfolioService, PortfolioSummary, HoldingDetails } from '@shared/services/portfolio.service';
import { AuthService } from '@shared/services/auth.service';
import { AmfiService } from '@shared/services/amfi.service';
import { GoalOption, GoalService } from '@shared/services/goal.service';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { FundDetailsComponent } from '../fund-details/fund-details.component';
import { GoalAllocationDialogComponent } from '../goal-allocation-dialog/goal-allocation-dialog.component';
import { UploadStatusDialogComponent, UploadStatusDialogData } from '../upload-status-dialog/upload-status-dialog.component';

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
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatSelectModule,
    MatChipsModule,
    MatSlideToggleModule,
    MatButtonToggleModule,
    MatDialogModule
  ],
  templateUrl: './portfolio-view.component.html',
  styleUrls: ['./portfolio-view.component.scss']
})
export class PortfolioViewComponent implements OnInit {
  private portfolioService: PortfolioService = inject(PortfolioService);
  private authService: AuthService = inject(AuthService);
  private amfiService: AmfiService = inject(AmfiService);
  private goalService: GoalService = inject(GoalService);
  private dialog: MatDialog = inject(MatDialog);
  private router: Router = inject(Router);
  private changeDetectorRef: ChangeDetectorRef = inject(ChangeDetectorRef);
  private ngZone: NgZone = inject(NgZone);

  portfolio: PortfolioSummary | null = null;
  holdings: HoldingDetails[] = [];
  filteredAndSortedHoldings: HoldingDetails[] = [];
  holdingGroups: { label: string; items: HoldingDetails[] }[] = [];
  availableTypes: string[] = [];
  availableGoals: string[] = [];
  reconciliationRows: HoldingDetails[] = [];
  goalOptions: GoalOption[] = [];
  loading = true;
  error = '';
  syncingAmfi = false;
  syncMessage = '';
  typeFilter = 'ALL';
  performanceFilter = 'ALL';
  goalFilter = 'ALL';
  groupBy = 'NONE';
  sortBy = 'CURRENT_VALUE';
  showAllFunds = false;
  viewMode: 'reconciliation' | 'classic' = 'classic';
  displayedColumns: string[] = ['schemeName', 'units', 'totalCost', 'latestNav', 'currentValue', 'pnl', 'xirr', 'goalNames', 'actions'];

  ngOnInit(): void {
    this.openUploadStatusDialogIfPresent();
    this.loadPortfolio();
  }

  private loadPortfolio(): void {
    const userId = this.authService.getCurrentUserId();
    this.loading = true;
    this.syncMessage = '';
    forkJoin({
      summary: this.portfolioService.getPortfolioSummary(userId),
      holdings: this.portfolioService.getHoldingDetails(userId, this.showAllFunds),
      goalOptions: this.goalService.getGoalOptions(userId).pipe(
        catchError(err => {
          console.error('Failed to preload goal options', err);
          return of([]);
        })
      )
    }).subscribe({
      next: ({ summary, holdings, goalOptions }) => {
        this.ngZone.run(() => {
          this.portfolio = summary;
          this.holdings = holdings;
          this.goalOptions = goalOptions;
          this.refreshDerivedState();
          this.loading = false;
          this.changeDetectorRef.detectChanges();
        });
      },
      error: (err: any) => {
        this.ngZone.run(() => {
        this.error = 'Failed to load portfolio data';
        this.loading = false;
        console.error(err);
          this.changeDetectorRef.detectChanges();
        });
      }
    });
  }

  viewFund(holding: HoldingDetails): void {
    this.dialog.open(FundDetailsComponent, {
      width: '1000px',
      maxWidth: '95vw',
      maxHeight: '90vh',
      autoFocus: false,
      data: {
        schemeCode: holding.schemeCode,
        schemeName: holding.schemeName
      }
    });
  }

  updateGoals(holding: HoldingDetails): void {
    const dialogRef = this.dialog.open(GoalAllocationDialogComponent, {
      width: '760px',
      maxWidth: '95vw',
      autoFocus: false,
      data: {
        schemeCode: holding.schemeCode,
        schemeName: holding.schemeName,
        goalAllocations: holding.goalAllocations || [],
        availableGoals: this.goalOptions
      }
    });

    dialogRef.afterClosed().subscribe(updated => {
      if (updated) {
        this.loadPortfolio();
      }
    });
  }

  getGainClass(value: number): string {
    return value >= 0 ? 'positive' : 'negative';
  }

  get navStatusMessage(): string {
    if (!this.portfolio?.latestNavDate) {
      return 'No NAV data is available yet. Run AMFI Sync to populate current values.';
    }

    return `Current values use NAV fetched on ${this.formatNavDate(this.portfolio.latestNavDate)}.`;
  }

  get valuationComparisonMessage(): string | null {
    if (!this.portfolio?.latestSnapshotDate || this.portfolio.latestSnapshotValue == null) {
      return null;
    }

    const discrepancyAmount = this.portfolio.valuationDiscrepancyAmount ?? 0;
    const discrepancyPercentage = this.portfolio.valuationDiscrepancyPercentage ?? 0;
    const snapshotDate = this.formatNavDate(this.portfolio.latestSnapshotDate);

    if (!this.portfolio.hasValuationDiscrepancy) {
      if (this.portfolio.snapshotDetailLevel === 'AMC_SUMMARY') {
        return `Uploaded AMC-summary PDF snapshot from ${snapshotDate} matches closely with the current AMFI-based valuation.`;
      }
      return `Uploaded valuation snapshot from ${snapshotDate} matches closely with the current AMFI-based valuation.`;
    }

    if (this.portfolio.snapshotDetailLevel === 'AMC_SUMMARY') {
      return `Uploaded AMC-summary PDF snapshot from ${snapshotDate} differs from the current AMFI-based valuation by ${this.formatCurrency(discrepancyAmount)} (${discrepancyPercentage.toFixed(2)}%).`;
    }

    return `Uploaded valuation snapshot from ${snapshotDate} differs from the current AMFI-based valuation by ${this.formatCurrency(discrepancyAmount)} (${discrepancyPercentage.toFixed(2)}%).`;
  }

  formatCurrency(value: number | null | undefined): string {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(value ?? 0);
  }

  formatNavDate(date: string | null | undefined): string {
    if (!date) {
      return 'N/A';
    }

    return new Date(date).toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  syncAmfi(): void {
    this.syncingAmfi = true;
    this.syncMessage = 'Running AMFI sync...';

    this.amfiService.syncDailyNav().subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.syncMessage = 'AMFI sync completed. Refreshing portfolio values...';
          this.loadPortfolio();
          this.syncingAmfi = false;
          this.changeDetectorRef.detectChanges();
        });
      },
      error: (err: any) => {
        this.ngZone.run(() => {
          this.syncingAmfi = false;
          this.syncMessage = err?.error?.message || 'AMFI sync failed';
          this.changeDetectorRef.detectChanges();
        });
      }
    });
  }

  private refreshDerivedState(): void {
    this.availableTypes = Array.from(new Set(this.holdings.map(holding => holding.schemeType).filter(Boolean))).sort();
    this.availableGoals = Array.from(new Set(this.holdings.flatMap(holding => holding.goalNames || []))).sort();

    const filtered = this.holdings.filter(holding => this.matchesFilters(holding));
    this.filteredAndSortedHoldings = filtered.sort((left, right) => this.compareHoldings(left, right));

    const grouped = new Map<string, HoldingDetails[]>();

    for (const holding of this.filteredAndSortedHoldings) {
      const label = this.resolveGroupLabel(holding);
      const items = grouped.get(label) ?? [];
      items.push(holding);
      grouped.set(label, items);
    }

    this.holdingGroups = Array.from(grouped.entries()).map(([label, items]) => ({ label, items }));

    if (this.portfolio?.snapshotDetailLevel === 'AMC_SUMMARY') {
      this.reconciliationRows = [];
      return;
    }

    this.reconciliationRows = this.holdings
      .filter(holding => !!holding.hasValuationDiscrepancy)
      .sort((left, right) => Math.abs(right.valuationDiscrepancyAmount ?? 0) - Math.abs(left.valuationDiscrepancyAmount ?? 0));
  }

  private matchesFilters(holding: HoldingDetails): boolean {
    const matchesType = this.typeFilter === 'ALL' || holding.schemeType === this.typeFilter;
    const matchesPerformance =
      this.performanceFilter === 'ALL' ||
      (this.performanceFilter === 'PROFIT' && holding.pnl > 0) ||
      (this.performanceFilter === 'LOSS' && holding.pnl < 0) ||
      (this.performanceFilter === 'FLAT' && holding.pnl === 0);
    const matchesGoal =
      this.goalFilter === 'ALL' ||
      (this.goalFilter === 'UNASSIGNED' && (!holding.goalNames || holding.goalNames.length === 0)) ||
      (holding.goalNames || []).includes(this.goalFilter);

    return matchesType && matchesPerformance && matchesGoal;
  }

  private compareHoldings(left: HoldingDetails, right: HoldingDetails): number {
    switch (this.sortBy) {
      case 'NAME':
        return left.schemeName.localeCompare(right.schemeName);
      case 'INVESTED':
        return right.totalCost - left.totalCost;
      case 'PNL':
        return right.pnl - left.pnl;
      case 'XIRR':
        return right.xirr - left.xirr;
      case 'CURRENT_VALUE':
      default:
        return right.currentValue - left.currentValue;
    }
  }

  private resolveGroupLabel(holding: HoldingDetails): string {
    switch (this.groupBy) {
      case 'TYPE':
        return holding.schemeType || 'Unknown Type';
      case 'PROFIT_LOSS':
        return holding.pnl >= 0 ? 'Profit Making' : 'Loss Making';
      case 'GOAL':
        return holding.goalNames && holding.goalNames.length > 0
          ? holding.goalNames.join(', ')
          : 'Unassigned';
      case 'NONE':
      default:
        return 'All Funds';
    }
  }

  formatGoals(goalNames: string[]): string {
    return goalNames && goalNames.length > 0 ? goalNames.join(', ') : 'Unassigned';
  }

  formatGoalAllocations(holding: HoldingDetails): string {
    const namedAllocations = (holding.goalAllocations || [])
      .map(allocation => {
        const goalName = allocation.goalName?.trim();
        if (!goalName) {
          return null;
        }

        return `${goalName} (${allocation.allocationPercentage}%)`;
      })
      .filter((value): value is string => !!value);

    if (namedAllocations.length > 0) {
      return `Goals: ${namedAllocations.join(', ')}`;
    }

    const namedGoals = (holding.goalNames || []).filter(goalName => !!goalName?.trim());
    if (namedGoals.length > 0) {
      return `Goals: ${namedGoals.join(', ')}`;
    }

    return 'Goal: Unassigned';
  }

  onShowAllFundsChange(checked: boolean): void {
    this.showAllFunds = checked;
    this.loadPortfolio();
  }

  setViewMode(mode: 'reconciliation' | 'classic'): void {
    this.viewMode = mode;
  }

  onFiltersChanged(): void {
    this.refreshDerivedState();
  }

  private openUploadStatusDialogIfPresent(): void {
    const state = window.history.state as { uploadResult?: UploadStatusDialogData } | null;
    const uploadResult = state?.uploadResult;
    if (!uploadResult || (!uploadResult.importId && !uploadResult.uploadId && !uploadResult.message)) {
      return;
    }

    this.dialog.open(UploadStatusDialogComponent, {
      width: '460px',
      maxWidth: '92vw',
      autoFocus: false,
      data: uploadResult
    });

    this.router.navigate([], {
      replaceUrl: true,
      state: {}
    });
  }
}

