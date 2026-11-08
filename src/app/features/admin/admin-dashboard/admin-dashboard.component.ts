import { Component, HostListener, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { User } from '../../../core/models/user.model';

type AdminView = 'home' | 'users' | 'security' | 'settings';

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {
  activeView: AdminView = 'home';
  sidebarOpen = false;
  sidebarCollapsed = false;
  users: User[] = [];
  loading = false;
  errorMessage = '';
  readonly today = new Date();

  constructor(
    public authService: AuthService,
    private userService: UserService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  get admin(): User | null { return this.authService.currentUserValue; }
  get initials(): string { return `${this.admin?.firstName?.[0] || 'A'}${this.admin?.lastName?.[0] || 'D'}`; }
  get activeUsers(): number { return this.users.filter(user => user.enabled).length; }
  get lockedUsers(): number { return this.users.filter(user => !user.accountNonLocked).length; }
  get adminCount(): number { return this.users.filter(user => user.role === 'ROLE_ADMIN').length; }
  get sidebarExpanded(): boolean { return window.innerWidth <= 1050 ? this.sidebarOpen : !this.sidebarCollapsed; }
  get pageTitle(): string {
    return ({ home: 'Command overview', users: 'User management', security: 'Security center', settings: 'Platform settings' })[this.activeView];
  }

  setView(view: AdminView): void {
    this.activeView = view;
    this.sidebarOpen = false;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  toggleSidebar(): void {
    if (window.innerWidth <= 1050) this.sidebarOpen = !this.sidebarOpen;
    else this.sidebarCollapsed = !this.sidebarCollapsed;
  }

  @HostListener('document:keydown.escape')
  closeSidebar(): void { this.sidebarOpen = false; }

  loadUsers(): void {
    this.loading = true;
    this.errorMessage = '';
    this.userService.getAllUsers().subscribe({
      next: response => {
        this.users = response.data || [];
        this.loading = false;
      },
      error: error => {
        this.loading = false;
        this.errorMessage = error?.error?.message || 'Unable to load the user directory.';
      }
    });
  }

  toggleUser(user: User): void {
    if (user.id === this.admin?.id) return;
    this.userService.toggleUserStatus(user.id).subscribe({ next: () => this.loadUsers() });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
