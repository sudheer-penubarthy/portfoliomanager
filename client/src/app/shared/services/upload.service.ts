import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UploadHistory {
  id: number;
  fileName: string;
  uploadDate: string;
  status: string;
  recordsProcessed: number;
  recordsFailed: number;
  totalRecords: number;
  startedAt?: string;
  completedAt?: string;
  errorMessage?: string;
}

export interface UploadStatus {
  id: number;
  status: string;
  recordsProcessed: number;
  recordsFailed: number;
  totalRecords: number;
  errorMessage?: string;
  progressPercentage?: number;
}

export interface UploadTimelineEvent {
  timestamp: string;
  stepName: string;
  status: string;
  message?: string;
}

@Injectable({
  providedIn: 'root'
})
export class UploadService {
  private apiUrl = '/api/uploads';

  constructor(private http: HttpClient) {}

  /**
   * Get upload history for the current user
   * @returns Observable<UploadHistory[]> - list of user's uploads
   */
  getUploadHistory(): Observable<UploadHistory[]> {
    return this.http.get<UploadHistory[]>(`${this.apiUrl}/history`);
  }

  /**
   * Get current processing status for a specific upload
   * @param uploadId - the upload ID
   * @returns Observable<UploadStatus> - current status
   */
  getUploadStatus(uploadId: number): Observable<UploadStatus> {
    return this.http.get<UploadStatus>(`${this.apiUrl}/${uploadId}/status`);
  }

  /**
   * Get timeline of processing events for a specific upload
   * @param uploadId - the upload ID
   * @returns Observable<UploadTimelineEvent[]> - list of timeline events
   */
  getUploadTimeline(uploadId: number): Observable<UploadTimelineEvent[]> {
    return this.http.get<UploadTimelineEvent[]>(`${this.apiUrl}/${uploadId}/timeline`);
  }
}

