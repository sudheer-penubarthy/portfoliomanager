import { Component, Inject, OnInit, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { UploadService, UploadTimelineEvent } from '@shared/services/upload.service';

@Component({
  selector: 'app-upload-timeline',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatExpansionModule, MatIconModule, MatButtonModule, MatProgressSpinnerModule],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './upload-timeline.component.html',
  styleUrls: ['./upload-timeline.component.scss']
})
export class UploadTimelineComponent implements OnInit {
  timelineEvents: UploadTimelineEvent[] = [];
  isLoading = true;
  hasError = false;
  errorMessage = '';

  constructor(
    @Inject(MAT_DIALOG_DATA) public uploadId: number,
    private uploadService: UploadService
  ) {}

  ngOnInit(): void {
    this.loadTimeline();
  }

  private loadTimeline(): void {
    this.isLoading = true;
    this.hasError = false;

    this.uploadService.getUploadTimeline(this.uploadId).subscribe({
      next: (events) => {
        this.timelineEvents = events;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading timeline:', error);
        this.hasError = true;
        this.errorMessage = error.error?.message || 'Failed to load timeline';
        this.isLoading = false;
      }
    });
  }

  getStepIcon(stepName: string): string {
    const iconMap: { [key: string]: string } = {
      'FILE_UPLOADED': 'cloud_upload',
      'VALIDATION_STARTED': 'check_circle_outline',
      'VALIDATION_COMPLETED': 'verified',
      'PROCESSING_STARTED': 'play_circle_outline',
      'PROCESSING_COMPLETED': 'check_circle',
      'HISTORICAL_NAV_SYNC_STARTED': 'sync',
      'HISTORICAL_NAV_SYNC_COMPLETED': 'done_all',
      'COMPLETED': 'check_circle',
      'FAILED': 'cancel'
    };
    return iconMap[stepName] || 'info';
  }

  getStepColor(status: string): string {
    switch (status?.toUpperCase()) {
      case 'SUCCESS':
        return 'success';
      case 'FAILED':
        return 'error';
      case 'PENDING':
        return 'pending';
      default:
        return 'info';
    }
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleString();
  }

  retry(): void {
    this.loadTimeline();
  }
}

