import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '@shared/services/auth.service';
import { MatIconModule } from '@angular/material/icon';
import { inject } from '@angular/core';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule
  ],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent {
  private formBuilder: FormBuilder = inject(FormBuilder);
  private authService: AuthService = inject(AuthService);
  private router: Router = inject(Router);

  form: FormGroup;
  loading = false;
  submitted = false;
  error = '';

  constructor() {
    this.form = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]],
      pan: ['', [Validators.required, Validators.pattern('^[A-Z0-9]{10}$')]],
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator() });
  }

  get f() {
    return this.form.controls;
  }

  passwordMatchValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const password = control.get('password')?.value;
      const confirmPassword = control.get('confirmPassword')?.value;

      if (password && confirmPassword && password !== confirmPassword) {
        control.get('confirmPassword')?.setErrors({ 'passwordMismatch': true });
        return { 'passwordMismatch': true };
      } else if (control.get('confirmPassword')?.errors?.['passwordMismatch']) {
        control.get('confirmPassword')?.setErrors(null);
      }

      return null;
    };
  }

  onSubmit(): void {
    this.submitted = true;

    if (this.form.invalid) {
      return;
    }

    this.loading = true;
    this.error = '';
    this.setFormDisabledState(true);

    const { confirmPassword, ...requestData } = this.form.value;

    this.authService.register(requestData).subscribe({
      next: () => {
        this.router.navigate(['/portfolio']);
      },
      error: (err: any) => {
        this.error = err.error.error || 'Registration failed. Please try again.';
        this.loading = false;
        this.setFormDisabledState(false);
      }
    });
  }

  private setFormDisabledState(disabled: boolean): void {
    if (disabled) {
      this.form.disable({ emitEvent: false });
      return;
    }

    this.form.enable({ emitEvent: false });
  }
}

