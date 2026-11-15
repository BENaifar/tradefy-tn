import { NgModule } from '@angular/core';
import { SharedUiModule } from '../../shared/ui/shared-ui.module';
import { MarketDataModule } from '../market-data/market-data.module';

import { PortfolioOverviewComponent } from './pages/portfolio-overview/portfolio-overview.component';
import { PositionsComponent } from './pages/positions/positions.component';
import { ActivityComponent } from './pages/activity/activity.component';
import { AllocationComponent } from './pages/allocation/allocation.component';
import { PerformanceComponent } from './pages/performance/performance.component';
import { LeaderboardComponent } from './pages/leaderboard/leaderboard.component';
import { TradeTicketComponent } from './components/trade-ticket/trade-ticket.component';
import { PositionDrawerComponent } from './components/position-drawer/position-drawer.component';
import { DashboardPortfolioStripComponent } from './components/dashboard-portfolio-strip/dashboard-portfolio-strip.component';
import { DashboardModulesRowComponent } from './components/dashboard-modules-row/dashboard-modules-row.component';
import { AdminPortfoliosComponent } from './components/admin-portfolios/admin-portfolios.component';

/** Portfolio: cash, trades, positions, activity, allocation, performance and the leaderboard. */
@NgModule({
  declarations: [
    PortfolioOverviewComponent, PositionsComponent, ActivityComponent, AllocationComponent, PerformanceComponent, LeaderboardComponent,
    TradeTicketComponent, PositionDrawerComponent, DashboardPortfolioStripComponent, DashboardModulesRowComponent, AdminPortfoliosComponent
  ],
  imports: [SharedUiModule, MarketDataModule],
  // Embedded in the old dashboard / admin pages (declared in AppModule).
  exports: [TradeTicketComponent, DashboardPortfolioStripComponent, DashboardModulesRowComponent, AdminPortfoliosComponent]
})
export class PortfolioModule {}
