import { Component, Inject, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-upload-error-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatIconModule, MatButtonModule],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './upload-error-dialog.component.html',
  styleUrls: ['./upload-error-dialog.component.scss']
})
export class UploadErrorDialogComponent {
  constructor(@Inject(MAT_DIALOG_DATA) public data: {
    fileName: string;
    errorMessage: string;
    recordsFailed: number;
    recordsProcessed: number;
    totalRecords: number;
  }) {}

  copyErrorToClipboard(): void {
    if (this.data.errorMessage) {
      navigator.clipboard.writeText(this.data.errorMessage).then(() => {
        alert('Error message copied to clipboard');
      });
    }
  }
}

