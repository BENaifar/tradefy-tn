import { NgModule } from '@angular/core';
import { SharedUiModule } from '../../shared/ui/shared-ui.module';

import { MarketsComponent } from './pages/markets/markets.component';
import { InstrumentComponent } from './pages/instrument/instrument.component';
import { ChartComponent } from './pages/chart/chart.component';
import { WatchlistComponent } from './pages/watchlist/watchlist.component';
import { CandleChartComponent } from './components/candle-chart/candle-chart.component';
import { IndicatorPanelComponent } from './components/indicator-panel/indicator-panel.component';
import { DashboardWatchlistPanelComponent } from './components/dashboard-watchlist-panel/dashboard-watchlist-panel.component';
import { DashboardMoversPanelComponent } from './components/dashboard-movers-panel/dashboard-movers-panel.component';
import { AdminMarketOpsComponent } from './components/admin-market-ops/admin-market-ops.component';

/** Market data: quotes, instruments, candle charts, indicators and the watchlist. */
@NgModule({
  declarations: [
    MarketsComponent, InstrumentComponent, ChartComponent, WatchlistComponent,
    CandleChartComponent, IndicatorPanelComponent, DashboardWatchlistPanelComponent, DashboardMoversPanelComponent, AdminMarketOpsComponent
  ],
  imports: [SharedUiModule],
  // Embedded in the old dashboard / admin pages (declared in AppModule) and reused by the portfolio module.
  exports: [CandleChartComponent, DashboardWatchlistPanelComponent, DashboardMoversPanelComponent, AdminMarketOpsComponent]
})
export class MarketDataModule {}
