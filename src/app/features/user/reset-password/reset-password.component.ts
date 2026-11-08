import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-reset-password',
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.scss']
})
export class ResetPasswordComponent implements OnInit {
  resetForm!: FormGroup;
  loading = false;
  errorMessage = '';
  successMessage = '';

  readonly passwordPattern = /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!._\-])(?=\S+$).{8,64}$/;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const prefillToken = this.route.snapshot.queryParams['token'] || '';

    this.resetForm = this.fb.group({
      token: [prefillToken, [Validators.required]],
      newPassword: ['', [Validators.required, Validators.pattern(this.passwordPattern)]],
      confirmPassword: ['', [Validators.required]]
    }, {
      validators: this.passwordMatchValidator
    });
  }

  passwordMatchValidator(control: AbstractControl): { [key: string]: boolean } | null {
    const password = control.get('newPassword')?.value;
    const confirm = control.get('confirmPassword')?.value;
    if (password && confirm && password !== confirm) {
      return { mismatch: true };
    }
    return null;
  }

  get passVal(): string {
    return this.resetForm.get('newPassword')?.value || '';
  }

  get hasMinLength(): boolean {
    return this.passVal.length >= 8 && this.passVal.length <= 64;
  }

  get hasUpper(): boolean {
    return /[A-Z]/.test(this.passVal);
  }

  get hasLower(): boolean {
    return /[a-z]/.test(this.passVal);
  }

  get hasDigit(): boolean {
    return /[0-9]/.test(this.passVal);
  }

  get hasSpecial(): boolean {
    return /[@#$%^&+=!._\-]/.test(this.passVal);
  }

  onSubmit(): void {
    if (this.resetForm.invalid) {
      this.resetForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.authService.resetPassword(this.resetForm.value).subscribe({
      next: () => {
        this.loading = false;
        this.successMessage = 'Password updated successfully! Redirecting to sign in...';
        setTimeout(() => this.router.navigate(['/login']), 1800);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err?.error?.message || 'Password reset failed. Token may be invalid or expired.';
      }
    });
  }
}

