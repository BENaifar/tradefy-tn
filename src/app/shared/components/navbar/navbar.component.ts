import { Component, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { LenisService } from '../../../core/services/lenis.service';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent {
  isScrolled = false;
  mobileMenuOpen = false;

  constructor(
    public authService: AuthService,
    private router: Router,
    private lenisService: LenisService
  ) {}

  @HostListener('window:scroll')
  onWindowScroll(): void {
    this.isScrolled = window.scrollY > 20;
  }

  @HostListener('document:keydown.escape')
  closeMenu(): void { this.mobileMenuOpen = false; }

  scrollTo(target: string): void {
    this.mobileMenuOpen = false;
    if (this.router.url !== '/' && !this.router.url.startsWith('/#')) {
      this.router.navigate(['/']).then(() => {
        setTimeout(() => this.lenisService.scrollTo(target, -90), 150);
      });
    } else {
      this.lenisService.scrollTo(target, -90);
    }
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}

