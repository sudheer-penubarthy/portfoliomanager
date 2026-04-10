import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';

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

export interface GoalOption {
  id: number;
  name: string;
}

export interface SchemeGoalAllocation {
  goalId: number;
  goalName?: string;
  allocationPercentage: number;
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

  getGoalOptions(userId: number): Observable<GoalOption[]> {
    return this.http.get<GoalOption[]>(`${this.apiUrl}/options`, { params: { userId: userId.toString() } });
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
    ).pipe(
      map((response: Goal[] | { value?: Goal[] } | null | undefined) => {
        if (Array.isArray(response)) {
          return response;
        }

        if (response && Array.isArray(response.value)) {
          return response.value;
        }

        return [];
      })
    );
  }

  getSchemeGoalAllocations(userId: number, schemeCode: string): Observable<SchemeGoalAllocation[]> {
    return this.http.get<SchemeGoalAllocation[]>(
      `${this.apiUrl}/schemes/${schemeCode}/alignments`,
      { params: { userId: userId.toString() } }
    );
  }

  updateSchemeGoalAllocations(userId: number, schemeCode: string, allocations: SchemeGoalAllocation[]): Observable<SchemeGoalAllocation[]> {
    return this.http.put<SchemeGoalAllocation[]>(
      `${this.apiUrl}/schemes/${schemeCode}/alignments`,
      { allocations },
      { params: { userId: userId.toString() } }
    );
  }
}

