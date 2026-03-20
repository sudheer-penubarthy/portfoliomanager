import { Component, OnInit, OnDestroy, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, interval } from 'rxjs';
import { takeUntil, switchMap } from 'rxjs/operators';
import { UploadService, UploadHistory, UploadStatus } from '@shared/services/upload.service';
import { UploadDashboardComponent } from '../../components/upload-dashboard/upload-dashboard.component';
import { UploadTimelineComponent } from '../../components/upload-timeline/upload-timeline.component';
import { UploadErrorDialogComponent } from '../../components/upload-error-dialog/upload-error-dialog.component';

@Component({
  selector: 'app-upload-history',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatIconModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatPaginatorModule,
    MatSortModule,
    MatProgressBarModule,
    MatToolbarModule,
    MatDialogModule,
    MatTooltipModule,
    UploadDashboardComponent
  ],
  templateUrl: './upload-history.component.html',
  styleUrls: ['./upload-history.component.scss']
})
export class UploadHistoryComponent implements OnInit, AfterViewInit, OnDestroy {
  uploadHistory: UploadHistory[] = [];
  dataSource = new MatTableDataSource<UploadHistory>();
  isLoading = true;
  hasError = false;
  errorMessage = '';
  searchFilter = '';
  statusFilter = '';
  pollingIntervals: { [key: number]: number } = {};

  displayedColumns: string[] = [
    'uploadDate',
    'fileName',
    'status',
    'recordsProcessed',
    'recordsFailed',
    'progress',
    'actions'
  ];

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  private destroy$ = new Subject<void>();
  private statusPoll$ = new Subject<number>();

  constructor(
    private uploadService: UploadService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadUploadHistory();
    this.setupPolling();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Refresh upload history
   */
  refresh(): void {
    this.loadUploadHistory();
  }

  /**
   * View processing timeline for an upload
   */
  viewTimeline(uploadId: number): void {
    this.dialog.open(UploadTimelineComponent, {
      data: uploadId,
      width: '600px',
      maxHeight: '80vh'
    });
  }

  /**
   * View error details for an upload
   */
  viewError(upload: UploadHistory): void {
    this.dialog.open(UploadErrorDialogComponent, {
      data: {
        fileName: upload.fileName,
        errorMessage: upload.errorMessage || 'No error message available',
        recordsFailed: upload.recordsFailed,
        recordsProcessed: upload.recordsProcessed,
        totalRecords: upload.totalRecords
      },
      width: '600px'
    });
  }

  /**
   * Load upload history from the API
   */
  private loadUploadHistory(): void {
    this.isLoading = true;
    this.hasError = false;

    this.uploadService.getUploadHistory()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.uploadHistory = data;
          this.dataSource.data = data;
          this.isLoading = false;
          this.applyFilters();
        },
        error: (error) => {
          console.error('Error loading upload history:', error);
          this.hasError = true;
          this.errorMessage = error.error?.message || 'Failed to load upload history';
          this.isLoading = false;
        }
      });
  }

  /**
   * Setup polling for processing uploads
   */
  private setupPolling(): void {
    this.statusPoll$
      .pipe(
        switchMap(uploadId =>
          interval(5000).pipe(
            switchMap(() => this.uploadService.getUploadStatus(uploadId))
          )
        ),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (status) => {
          this.updateUploadStatus(status);
        },
        error: (error) => {
          console.error('Error polling status:', error);
        }
      });
  }

  /**
   * Start polling for a specific upload
   */
  private startPolling(uploadId: number): void {
    if (this.uploadHistory.find(u => u.id === uploadId)?.status === 'PROCESSING') {
      this.statusPoll$.next(uploadId);
    }
  }

  /**
   * Update upload status in the table
   */
  private updateUploadStatus(status: UploadStatus): void {
    const index = this.uploadHistory.findIndex(u => u.id === status.id);
    if (index !== -1) {
      this.uploadHistory[index].recordsProcessed = status.recordsProcessed;
      this.uploadHistory[index].recordsFailed = status.recordsFailed;
      this.uploadHistory[index].errorMessage = status.errorMessage;
      this.uploadHistory[index].status = status.status as any;
      this.dataSource.data = [...this.uploadHistory];
    }
  }

  /**
   * Apply search and filter to table
   */
  applyFilters(): void {
    let filteredData = this.uploadHistory;

    // Search filter
    if (this.searchFilter.trim()) {
      const filterValue = this.searchFilter.toLowerCase();
      filteredData = filteredData.filter(item =>
        item.fileName.toLowerCase().includes(filterValue)
      );
    }

    // Status filter
    if (this.statusFilter) {
      filteredData = filteredData.filter(item =>
        item.status === this.statusFilter
      );
    }

    this.dataSource.data = filteredData;
  }

  /**
   * Clear all filters
   */
  clearFilters(): void {
    this.searchFilter = '';
    this.statusFilter = '';
    this.applyFilters();
  }

  /**
   * Get progress percentage for a record
   */
  getProgressPercentage(upload: UploadHistory): number {
    if (upload.totalRecords === 0) return 0;
    return Math.round((upload.recordsProcessed / upload.totalRecords) * 100);
  }

  /**
   * Determine the color for the status chip
   */
  getStatusColor(status: string): string {
    switch (status?.toUpperCase()) {
      case 'COMPLETED':
        return 'accent'; // green
      case 'FAILED':
        return 'warn'; // red
      case 'PROCESSING':
        return 'primary'; // primary color (orange/amber)
      case 'UPLOADED':
        return 'primary'; // blue (primary)
      default:
        return 'primary';
    }
  }

  /**
   * Determine the icon for the status chip
   *
   * @param status the upload status
   * @returns the icon name
   */
  getStatusIcon(status: string): string {
    switch (status?.toUpperCase()) {
      case 'COMPLETED':
        return 'check_circle';
      case 'FAILED':
        return 'cancel';
      case 'PROCESSING':
        return 'schedule';
      case 'UPLOADED':
        return 'cloud_upload';
      default:
        return 'info';
    }
  }

  /**
   * Format date for display
   *
   * @param date the date string
   * @returns formatted date string
   */
  formatDate(date: string): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleString();
  }

  /**
   * Retry loading upload history
   */
  retry(): void {
    this.loadUploadHistory();
  }
}

