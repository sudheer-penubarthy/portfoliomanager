import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
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
  selector: 'app-goal-details',
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
  templateUrl: './goal-details.component.html',
  styleUrls: ['./goal-details.component.scss']
})
export class GoalDetailsComponent implements OnInit {
  private route: ActivatedRoute = inject(ActivatedRoute);
  private goalService: GoalService = inject(GoalService);
  private authService: AuthService = inject(AuthService);
  private router: Router = inject(Router);

  goal: Goal | null = null;
  loading = true;
  error = '';
  goalId: number = 0;
  Math = Math;

  ngOnInit(): void {
    this.goalId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadGoal();
  }

  loadGoal(): void {
    const userId = this.authService.getCurrentUserId();
    this.goalService.getGoalProgress(userId, this.goalId).subscribe({
      next: (data: any) => {
        this.goal = data;
        this.loading = false;
      },
      error: (err: any) => {
        this.error = 'Failed to load goal details';
        this.loading = false;
        console.error(err);
      }
    });
  }

  editGoal(): void {
    this.router.navigate(['/goals', this.goalId, 'edit']);
  }

  goBack(): void {
    this.router.navigate(['/goals']);
  }

  alignFunds(): void {
    // TODO: Implement fund alignment dialog
    alert('Fund alignment feature coming soon!');
  }
}

