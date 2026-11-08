import { Component, OnInit, OnDestroy } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter, Subscription } from 'rxjs';
import { LenisService } from './core/services/lenis.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'pi5emeFront';
  showPublicChrome = true;
  private routerSubscription?: Subscription;

  constructor(private lenisService: LenisService, private router: Router) {}

  ngOnInit(): void {
    this.lenisService.init();
    this.updatePublicChrome(this.router.url);
    this.routerSubscription = this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd)
    ).subscribe(event => this.updatePublicChrome(event.urlAfterRedirects));
  }

  private updatePublicChrome(url: string): void {
    const path = url.split('?')[0].split('#')[0];
    this.showPublicChrome = path !== '/dashboard' && path !== '/profile' && path !== '/admin';
  }

  ngOnDestroy(): void {
    this.routerSubscription?.unsubscribe();
    this.lenisService.destroy();
  }
}
