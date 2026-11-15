import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LandingComponent } from './features/landing/landing.component';
import { LoginComponent } from './features/user/login/login.component';
import { RegisterComponent } from './features/user/register/register.component';
import { ForgotPasswordComponent } from './features/user/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './features/user/reset-password/reset-password.component';
import { ProfileComponent } from './features/user/profile/profile.component';
import { AuthGuard } from './core/guards/auth.guard';
import { AdminGuard } from './core/guards/admin.guard';
import { AdminDashboardComponent } from './features/admin/admin-dashboard/admin-dashboard.component';
// MARKET-PORTFOLIO: start
import { MarketsComponent } from './features/market-data/pages/markets/markets.component';
import { InstrumentComponent } from './features/market-data/pages/instrument/instrument.component';
import { ChartComponent } from './features/market-data/pages/chart/chart.component';
import { WatchlistComponent } from './features/market-data/pages/watchlist/watchlist.component';
import { PortfolioOverviewComponent } from './features/portfolio/pages/portfolio-overview/portfolio-overview.component';
import { PositionsComponent } from './features/portfolio/pages/positions/positions.component';
import { ActivityComponent } from './features/portfolio/pages/activity/activity.component';
import { AllocationComponent } from './features/portfolio/pages/allocation/allocation.component';
import { PerformanceComponent } from './features/portfolio/pages/performance/performance.component';
import { LeaderboardComponent } from './features/portfolio/pages/leaderboard/leaderboard.component';
// MARKET-PORTFOLIO: end

const routes: Routes = [
  { path: '', component: LandingComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: 'dashboard', component: ProfileComponent, canActivate: [AuthGuard] },
  { path: 'admin', component: AdminDashboardComponent, canActivate: [AuthGuard, AdminGuard] },
  { path: 'profile', redirectTo: 'dashboard', pathMatch: 'full' },
  // MARKET-PORTFOLIO: start
  { path: 'markets', component: MarketsComponent, canActivate: [AuthGuard] },
  { path: 'markets/:ticker', component: InstrumentComponent, canActivate: [AuthGuard] },
  { path: 'chart', component: ChartComponent, canActivate: [AuthGuard] },          // last used ticker, else first watchlist ticker, else TJARI
  { path: 'chart/:ticker', component: ChartComponent, canActivate: [AuthGuard] },
  { path: 'watchlist', component: WatchlistComponent, canActivate: [AuthGuard] },
  { path: 'portfolio', component: PortfolioOverviewComponent, canActivate: [AuthGuard] },
  { path: 'portfolio/positions', component: PositionsComponent, canActivate: [AuthGuard] },
  { path: 'portfolio/activity', component: ActivityComponent, canActivate: [AuthGuard] },
  { path: 'portfolio/allocation', component: AllocationComponent, canActivate: [AuthGuard] },
  { path: 'portfolio/performance', component: PerformanceComponent, canActivate: [AuthGuard] },
  { path: 'leaderboard', component: LeaderboardComponent, canActivate: [AuthGuard] },
  // MARKET-PORTFOLIO: end
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {
    scrollPositionRestoration: 'top',
    anchorScrolling: 'enabled'
  })],
  exports: [RouterModule]
})
export class AppRoutingModule { }
