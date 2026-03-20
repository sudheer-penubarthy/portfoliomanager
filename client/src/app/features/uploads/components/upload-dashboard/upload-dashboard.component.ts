import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { UploadHistory } from '@shared/services/upload.service';

@Component({
  selector: 'app-upload-dashboard',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule],
  templateUrl: './upload-dashboard.component.html',
  styleUrls: ['./upload-dashboard.component.scss']
})
export class UploadDashboardComponent {
  @Input() uploadHistory: UploadHistory[] = [];

  get totalUploads(): number {
    return this.uploadHistory.length;
  }

  get processingUploads(): number {
    return this.uploadHistory.filter(u => u.status === 'PROCESSING').length;
  }

  get completedUploads(): number {
    return this.uploadHistory.filter(u => u.status === 'COMPLETED').length;
  }

  get failedUploads(): number {
    return this.uploadHistory.filter(u => u.status === 'FAILED').length;
  }
}

