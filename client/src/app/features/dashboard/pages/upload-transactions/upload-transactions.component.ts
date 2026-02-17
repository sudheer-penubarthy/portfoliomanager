import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '@shared/services/auth.service';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { inject } from '@angular/core';

@Component({
  selector: 'app-upload-transactions',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatSnackBarModule,
    MatCheckboxModule
  ],
  templateUrl: './upload-transactions.component.html',
  styleUrls: ['./upload-transactions.component.scss']
})
export class UploadTransactionsComponent implements OnInit {
  private formBuilder: FormBuilder = inject(FormBuilder);
  private authService: AuthService = inject(AuthService);
  private http: HttpClient = inject(HttpClient);

  form: FormGroup;
  loading = false;
  submitted = false;
  error: string | null = null;
  success: string | null = null;

  currentUserEmail: string | null = null;
  transactionFile: File | null = null;
  valuationFile: File | null = null;
  zipFile: File | null = null;

  rtaOptions = [
    { value: 'CAMS', label: 'CAMS' },
    { value: 'Kfintech', label: 'Kfintech' },
    { value: 'ICICI', label: 'ICICI' },
    { value: 'Other', label: 'Other' }
  ];

  constructor() {
    this.form = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]],
      rtaName: ['', Validators.required],
      isValuationFile: [false],
      zipPassword: ['']
    });
  }

  ngOnInit(): void {
    // Get current user's email
    this.authService.currentUser$.subscribe((user: any) => {
      if (user) {
        this.currentUserEmail = user.email;
        this.form.patchValue({ email: user.email });
      }
    });
  }

  get f() {
    return this.form.controls;
  }

  onTransactionFileSelected(event: any): void {
    const file = event.target.files?.[0];
    if (file) {
      this.transactionFile = file;
    }
  }

  onValuationFileSelected(event: any): void {
    const file = event.target.files?.[0];
    if (file) {
      this.valuationFile = file;
    }
  }

  onZipFileSelected(event: any): void {
    const file = event.target.files?.[0];
    if (file) {
      this.zipFile = file;
    }
  }

  onSubmit(): void {
    this.submitted = true;
    this.error = null;
    this.success = null;

    if (this.form.invalid) {
      return;
    }

    // Validate at least one file is selected
    if (!this.transactionFile && !this.valuationFile && !this.zipFile) {
      this.error = 'Please select at least one file (transaction, valuation, or ZIP)';
      return;
    }

    // Validate email is provided
    const email = this.form.get('email')?.value;
    if (!email) {
      this.error = 'Email is required and must be valid';
      return;
    }

    this.loading = true;

    // Create FormData for multipart upload
    const formData = new FormData();
    formData.append('email', email);
    formData.append('rtaName', this.form.get('rtaName')?.value || '');

    if (this.transactionFile) {
      formData.append('transactionFile', this.transactionFile);
    }

    if (this.valuationFile) {
      formData.append('valuationFile', this.valuationFile);
    }

    if (this.zipFile) {
      formData.append('zipFile', this.zipFile);
      const zipPassword = this.form.get('zipPassword')?.value;
      if (zipPassword) {
        formData.append('zipPassword', zipPassword);
      }
    }

    // Make API call
    this.http.post<any>('/api/users/upload-files', formData).subscribe({
      next: (response: any) => {
        this.success = `Upload successful! Import ID: ${response.importId}. Status: ${response.status}`;
        this.loading = false;
        this.resetForm();
      },
      error: (err: any) => {
        this.error = err.error?.error || err.error?.message || 'Upload failed. Please check your email and file contents.';
        this.loading = false;
        console.error('Upload error:', err);
      }
    });
  }

  resetForm(): void {
    this.form.reset();
    this.transactionFile = null;
    this.valuationFile = null;
    this.zipFile = null;
    this.submitted = false;

    // Reset email if user is still logged in
    if (this.currentUserEmail) {
      this.form.patchValue({ email: this.currentUserEmail });
    }
  }

  getFileDisplayName(file: File | null): string {
    return file ? file.name : 'No file selected';
  }

  clearFile(fileType: 'transaction' | 'valuation' | 'zip'): void {
    if (fileType === 'transaction') {
      this.transactionFile = null;
    } else if (fileType === 'valuation') {
      this.valuationFile = null;
    } else if (fileType === 'zip') {
      this.zipFile = null;
    }
  }
}

