import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Goal {
  id?: number;
  name: string;
  description?: string;
  targetAmount: number;
  targetDate: string; // yyyy-MM-dd
  currentValue?: number;
  status?: string;
  remainingAmount?: number;
  progressPercentage?: number;
  daysRemaining?: number;
  trackingStatus?: string;
  createdAt?: string;
  updatedAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class GoalService {
  private apiUrl = '/api/goals';

  constructor(private http: HttpClient) {}

  createGoal(userId: number, goal: Goal): Observable<Goal> {
    return this.http.post<Goal>(this.apiUrl, goal, { params: { userId: userId.toString() } });
  }

  getGoals(userId: number): Observable<Goal[]> {
    return this.http.get<Goal[]>(this.apiUrl, { params: { userId: userId.toString() } });
  }

  getGoal(userId: number, goalId: number): Observable<Goal> {
    return this.http.get<Goal>(`${this.apiUrl}/${goalId}`, { params: { userId: userId.toString() } });
  }

  updateGoal(userId: number, goalId: number, goal: Goal): Observable<Goal> {
    return this.http.put<Goal>(`${this.apiUrl}/${goalId}`, goal, { params: { userId: userId.toString() } });
  }

  deleteGoal(userId: number, goalId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${goalId}`, { params: { userId: userId.toString() } });
  }

  alignFundsToGoal(userId: number, goalId: number, schemeCodes: string[]): Observable<void> {
    return this.http.post<void>(
      `${this.apiUrl}/${goalId}/alignments`,
      { schemeCodes },
      { params: { userId: userId.toString() } }
    );
  }

  getGoalAlignments(userId: number, goalId: number): Observable<string[]> {
    return this.http.get<string[]>(
      `${this.apiUrl}/${goalId}/alignments`,
      { params: { userId: userId.toString() } }
    );
  }

  getGoalProgress(userId: number, goalId: number): Observable<Goal> {
    return this.http.get<Goal>(
      `${this.apiUrl}/${goalId}/progress`,
      { params: { userId: userId.toString() } }
    );
  }

  getAllGoalsWithTracking(userId: number): Observable<Goal[]> {
    return this.http.get<Goal[]>(
      `${this.apiUrl}/tracking/all`,
      { params: { userId: userId.toString() } }
    );
  }
}

