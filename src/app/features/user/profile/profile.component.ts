import { Component, HostListener, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { Role, User, UserCreateRequest, UserUpdateRequest } from '../../../core/models/user.model';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {
  activeTab: 'home' | 'profile' | 'security' | 'admin' = 'home';
  sidebarOpen = false;
  sidebarCollapsed = false;
  readonly today = new Date();
  readonly marketSnapshot = [
    { symbol: 'BTC/USD', price: '$89,420.50', change: '+4.82%', positive: true },
    { symbol: 'ETH/USD', price: '$3,412.10', change: '+2.14%', positive: true },
    { symbol: 'SOL/USD', price: '$198.75', change: '+8.45%', positive: true },
    { symbol: 'EUR/USD', price: '1.0842', change: '-0.18%', positive: false }
  ];

  currentUser: User | null = null;
  isAdmin = false;

  // Forms
  profileForm!: FormGroup;
  passwordForm!: FormGroup;
  createUserForm!: FormGroup;
  editUserForm!: FormGroup;

  // State
  profileLoading = false;
  profileMessage = '';
  profileError = '';

  passwordLoading = false;
  passwordMessage = '';
  passwordError = '';

  // Admin CRUD state
  usersList: User[] = [];
  adminLoading = false;
  adminError = '';
  adminMessage = '';
  showCreateModal = false;
  showEditModal = false;
  selectedUserForEdit: User | null = null;

  readonly passwordPattern = /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!._\-])(?=\S+$).{8,64}$/;

  constructor(
    public authService: AuthService,
    private userService: UserService,
    private fb: FormBuilder,
    private router: Router
  ) {}

  get pageTitle(): string {
    return ({
      home: 'Workspace overview',
      profile: 'Profile settings',
      security: 'Security center',
      admin: 'User administration'
    } as const)[this.activeTab];
  }

  get initials(): string {
    return `${this.currentUser?.firstName?.charAt(0) || 'T'}${this.currentUser?.lastName?.charAt(0) || 'X'}`;
  }

  get sidebarExpanded(): boolean {
    return window.innerWidth <= 1100 ? this.sidebarOpen : !this.sidebarCollapsed;
  }

  setActiveTab(tab: 'home' | 'profile' | 'security' | 'admin'): void {
    if (tab === 'admin' && !this.isAdmin) return;
    this.activeTab = tab;
    this.sidebarOpen = false;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  toggleSidebar(): void {
    if (window.innerWidth <= 1100) {
      this.sidebarOpen = !this.sidebarOpen;
      return;
    }

    this.sidebarCollapsed = !this.sidebarCollapsed;
  }

  @HostListener('document:keydown.escape')
  closeSidebar(): void {
    this.sidebarOpen = false;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  ngOnInit(): void {
    this.currentUser = this.authService.currentUserValue;
    this.isAdmin = this.currentUser?.role === 'ROLE_ADMIN';

    this.initProfileForm();
    this.initPasswordForm();
    this.initAdminForms();

    this.loadFreshUserProfile();

    if (this.isAdmin) {
      this.loadAllUsers();
    }
  }

  private initProfileForm(): void {
    this.profileForm = this.fb.group({
      firstName: [this.currentUser?.firstName || '', [Validators.required, Validators.minLength(2)]],
      lastName: [this.currentUser?.lastName || '', [Validators.required, Validators.minLength(2)]],
      email: [this.currentUser?.email || '', [Validators.required, Validators.email]]
    });
  }

  private initPasswordForm(): void {
    this.passwordForm = this.fb.group({
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.pattern(this.passwordPattern)]],
      confirmPassword: ['', [Validators.required]]
    }, {
      validators: this.passwordMatchValidator
    });
  }

  private initAdminForms(): void {
    this.createUserForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.pattern(this.passwordPattern)]],
      role: ['ROLE_USER', [Validators.required]],
      enabled: [true]
    });

    this.editUserForm = this.fb.group({
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      role: ['ROLE_USER', [Validators.required]],
      enabled: [true],
      accountNonLocked: [true]
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

  loadFreshUserProfile(): void {
    this.userService.getCurrentUser().subscribe({
      next: (res) => {
        if (res && res.data) {
          this.currentUser = res.data;
          this.isAdmin = this.currentUser.role === 'ROLE_ADMIN';
          this.authService.updateStoredUser(this.currentUser);
          this.profileForm.patchValue({
            firstName: this.currentUser.firstName,
            lastName: this.currentUser.lastName,
            email: this.currentUser.email
          });
        }
      }
    });
  }

  onUpdateProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.profileLoading = true;
    this.profileMessage = '';
    this.profileError = '';

    const payload: UserUpdateRequest = {
      firstName: this.profileForm.value.firstName,
      lastName: this.profileForm.value.lastName,
      email: this.profileForm.value.email
    };

    this.userService.updateCurrentUser(payload).subscribe({
      next: (res) => {
        this.profileLoading = false;
        this.profileMessage = 'Profile updated successfully!';
        if (res && res.data) {
          this.currentUser = res.data;
          this.authService.updateStoredUser(res.data);
        }
      },
      error: (err) => {
        this.profileLoading = false;
        this.profileError = err?.error?.message || 'Failed to update profile.';
      }
    });
  }

  onChangePassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    this.passwordLoading = true;
    this.passwordMessage = '';
    this.passwordError = '';

    this.userService.changePassword(this.passwordForm.value).subscribe({
      next: () => {
        this.passwordLoading = false;
        this.passwordMessage = 'Password changed successfully!';
        this.passwordForm.reset();
      },
      error: (err) => {
        this.passwordLoading = false;
        this.passwordError = err?.error?.message || 'Failed to change password. Verify your current password.';
      }
    });
  }

  // ── Admin CRUD Methods ─────────────────────────────────────────
  loadAllUsers(): void {
    this.adminLoading = true;
    this.adminError = '';
    this.userService.getAllUsers().subscribe({
      next: (res) => {
        this.adminLoading = false;
        this.usersList = res.data || [];
      },
      error: (err) => {
        this.adminLoading = false;
        this.adminError = err?.error?.message || 'Failed to load user directory.';
      }
    });
  }

  openCreateModal(): void {
    this.createUserForm.reset({ role: 'ROLE_USER', enabled: true });
    this.showCreateModal = true;
    this.adminMessage = '';
    this.adminError = '';
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
  }

  onSubmitCreateUser(): void {
    if (this.createUserForm.invalid) {
      this.createUserForm.markAllAsTouched();
      return;
    }

    this.adminLoading = true;
    const req: UserCreateRequest = this.createUserForm.value;

    this.userService.createUser(req).subscribe({
      next: () => {
        this.adminLoading = false;
        this.showCreateModal = false;
        this.adminMessage = `User ${req.email} created successfully.`;
        this.loadAllUsers();
      },
      error: (err) => {
        this.adminLoading = false;
        this.adminError = err?.error?.message || 'Failed to create user.';
      }
    });
  }

  openEditModal(user: User): void {
    this.selectedUserForEdit = user;
    this.editUserForm.patchValue({
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      role: user.role,
      enabled: user.enabled,
      accountNonLocked: user.accountNonLocked
    });
    this.showEditModal = true;
    this.adminMessage = '';
    this.adminError = '';
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.selectedUserForEdit = null;
  }

  onSubmitEditUser(): void {
    if (this.editUserForm.invalid || !this.selectedUserForEdit) {
      this.editUserForm.markAllAsTouched();
      return;
    }

    this.adminLoading = true;
    const payload: UserUpdateRequest = this.editUserForm.value;

    this.userService.updateUser(this.selectedUserForEdit.id, payload).subscribe({
      next: () => {
        this.adminLoading = false;
        this.showEditModal = false;
        this.adminMessage = `User #${this.selectedUserForEdit?.id} updated successfully.`;
        this.loadAllUsers();
        if (this.selectedUserForEdit?.id === this.currentUser?.id) {
          this.loadFreshUserProfile();
        }
      },
      error: (err) => {
        this.adminLoading = false;
        this.adminError = err?.error?.message || 'Failed to update user.';
      }
    });
  }

  onToggleStatus(user: User): void {
    if (user.id === this.currentUser?.id) {
      alert('Security violation: You cannot deactivate your own active administrator account.');
      return;
    }

    this.userService.toggleUserStatus(user.id).subscribe({
      next: () => {
        this.adminMessage = `User #${user.id} status changed to ${!user.enabled ? 'ACTIVE' : 'DISABLED'}.`;
        this.loadAllUsers();
      },
      error: (err) => {
        this.adminError = err?.error?.message || 'Failed to toggle status.';
      }
    });
  }

  onDeleteUser(user: User): void {
    if (user.id === this.currentUser?.id) {
      alert('Security violation: You cannot delete your own administrator account.');
      return;
    }

    if (!confirm(`Are you sure you want to permanently delete user "${user.email}"?`)) {
      return;
    }

    this.userService.deleteUser(user.id).subscribe({
      next: () => {
        this.adminMessage = `User #${user.id} permanently deleted.`;
        this.loadAllUsers();
      },
      error: (err) => {
        this.adminError = err?.error?.message || 'Failed to delete user.';
      }
    });
  }
}

