import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { GoalService, Goal } from '@shared/services/goal.service';
import { AuthService } from '@shared/services/auth.service';
import { inject } from '@angular/core';

@Component({
  selector: 'app-goal-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './goal-form.component.html',
  styleUrls: ['./goal-form.component.scss']
})
export class GoalFormComponent implements OnInit {
  private formBuilder: FormBuilder = inject(FormBuilder);
  private goalService: GoalService = inject(GoalService);
  private authService: AuthService = inject(AuthService);
  private route: ActivatedRoute = inject(ActivatedRoute);
  private router: Router = inject(Router);

  form: FormGroup;
  loading = false;
  submitted = false;
  error = '';
  goalId: number | null = null;
  isEditMode = false;

  constructor() {
    this.form = this.formBuilder.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      description: [''],
      targetAmount: [0, [Validators.required, Validators.min(1)]],
      targetDate: [new Date(), Validators.required]
    });
  }

  ngOnInit(): void {
    this.goalId = this.route.snapshot.paramMap.get('id') ? Number(this.route.snapshot.paramMap.get('id')) : null;
    if (this.goalId) {
      this.isEditMode = true;
      this.loadGoal();
    }
  }

  loadGoal(): void {
    if (!this.goalId) return;
    const userId = this.authService.getCurrentUserId();
    this.goalService.getGoal(userId, this.goalId).subscribe({
      next: (goal: any) => {
        this.form.patchValue({
          name: goal.name,
          description: goal.description,
          targetAmount: goal.targetAmount,
          targetDate: new Date(goal.targetDate)
        });
      },
      error: (err: any) => {
        this.error = 'Failed to load goal';
        console.error(err);
      }
    });
  }

  get f() {
    return this.form.controls;
  }

  onSubmit(): void {
    this.submitted = true;

    if (this.form.invalid) {
      return;
    }

    this.loading = true;
    this.error = '';

    const userId = this.authService.getCurrentUserId();
    const goal: Goal = {
      ...this.form.value,
      targetDate: new Date(this.form.value.targetDate).toISOString().split('T')[0]
    };

    const request = this.isEditMode && this.goalId
      ? this.goalService.updateGoal(userId, this.goalId, goal)
      : this.goalService.createGoal(userId, goal);

    request.subscribe({
      next: (created: any) => {
        this.router.navigate(['/goals', created.id]);
      },
      error: (err: any) => {
        this.error = 'Failed to save goal';
        this.loading = false;
        console.error(err);
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/goals']);
  }
}

