import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { GoalService, Goal } from '@shared/services/goal.service';
import { AuthService } from '@shared/services/auth.service';
import { inject } from '@angular/core';

@Component({
  selector: 'app-goals-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatChipsModule
  ],
  templateUrl: './goals-list.component.html',
  styleUrls: ['./goals-list.component.scss']
})
export class GoalsListComponent implements OnInit {
  private goalService: GoalService = inject(GoalService);
  private authService: AuthService = inject(AuthService);
  private router: Router = inject(Router);

  goals: Goal[] = [];
  loading = true;
  error = '';
  Math = Math;

  ngOnInit(): void {
    this.loadGoals();
  }

  loadGoals(): void {
    const userId = this.authService.getCurrentUserId();
    this.goalService.getAllGoalsWithTracking(userId).subscribe({
      next: (data: any) => {
        this.goals = data;
        this.loading = false;
      },
      error: (err: any) => {
        this.error = 'Failed to load goals';
        this.loading = false;
        console.error(err);
      }
    });
  }

  createGoal(): void {
    this.router.navigate(['/goals/create']);
  }

  viewGoal(goalId: number): void {
    this.router.navigate(['/goals', goalId]);
  }

  editGoal(goalId: number, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/goals', goalId, 'edit']);
  }

  deleteGoal(goalId: number, event: Event): void {
    event.stopPropagation();
    if (confirm('Are you sure you want to delete this goal?')) {
      const userId = this.authService.getCurrentUserId();
      this.goalService.deleteGoal(userId, goalId).subscribe({
        next: () => {
          this.loadGoals();
        },
        error: (err: any) => {
          console.error('Failed to delete goal', err);
        }
      });
    }
  }

  getProgressColor(progress: number): string {
    if (progress >= 100) return 'accent';
    if (progress >= 75) return 'primary';
    if (progress >= 50) return 'warn';
    return 'warn';
  }
}

