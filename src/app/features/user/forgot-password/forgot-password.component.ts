import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.scss']
})
export class ForgotPasswordComponent implements OnInit {
  forgotForm!: FormGroup;
  loading = false;
  errorMessage = '';
  generatedToken = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.forgotForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  onSubmit(): void {
    if (this.forgotForm.invalid) {
      this.forgotForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.generatedToken = '';

    this.authService.forgotPassword(this.forgotForm.value).subscribe({
      next: (res) => {
        this.loading = false;
        if (res && res.data && res.data.resetToken) {
          this.generatedToken = res.data.resetToken;
        }
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err?.error?.message || 'Unable to process reset request. Please verify the email.';
      }
    });
  }

  goToReset(): void {
    this.router.navigate(['/reset-password'], {
      queryParams: { token: this.generatedToken }
    });
  }
}

