import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { MoneyPipe, PctPipe, QtyPipe, TunisTimePipe } from './pipes/tx-pipes';
import { TxBarsComponent } from './components/tx-bars/tx-bars.component';
import { TxConfirmDialogComponent } from './components/tx-confirm-dialog/tx-confirm-dialog.component';
import { TxDeltaComponent } from './components/tx-delta/tx-delta.component';
import { TxDonutComponent } from './components/tx-donut/tx-donut.component';
import { TxDrawerComponent } from './components/tx-drawer/tx-drawer.component';
import { TxEmptyStateComponent } from './components/tx-empty-state/tx-empty-state.component';
import { TxErrorStateComponent } from './components/tx-error-state/tx-error-state.component';
import { TxGaugeComponent } from './components/tx-gauge/tx-gauge.component';
import { TxLiveBadgeComponent } from './components/tx-live-badge/tx-live-badge.component';
import { TxMarketStatusComponent } from './components/tx-market-status/tx-market-status.component';
import { TxMetricCardComponent } from './components/tx-metric-card/tx-metric-card.component';
import { TxModuleHeaderComponent } from './components/tx-module-header/tx-module-header.component';
import { TxModuleTabsComponent } from './components/tx-module-tabs/tx-module-tabs.component';
import { TxPanelComponent } from './components/tx-panel/tx-panel.component';
import { TxRangeBarComponent } from './components/tx-range-bar/tx-range-bar.component';
import { TxSegmentedComponent } from './components/tx-segmented/tx-segmented.component';
import { TxSkeletonComponent } from './components/tx-skeleton/tx-skeleton.component';
import { TxSparklineComponent } from './components/tx-sparkline/tx-sparkline.component';
import { TxTickerSearchComponent } from './components/tx-ticker-search/tx-ticker-search.component';
import { TxToastComponent } from './components/tx-toast/tx-toast.component';
import { TxTreemapComponent } from './components/tx-treemap/tx-treemap.component';

const DECLARED = [
  MoneyPipe, PctPipe, QtyPipe, TunisTimePipe,
  TxBarsComponent, TxConfirmDialogComponent, TxDeltaComponent, TxDonutComponent, TxDrawerComponent, TxEmptyStateComponent,
  TxErrorStateComponent, TxGaugeComponent, TxLiveBadgeComponent, TxMarketStatusComponent, TxMetricCardComponent,
  TxModuleHeaderComponent, TxModuleTabsComponent, TxPanelComponent, TxRangeBarComponent, TxSegmentedComponent,
  TxSkeletonComponent, TxSparklineComponent, TxTickerSearchComponent, TxToastComponent, TxTreemapComponent
];

/** Shared presentational pieces of the market-data and portfolio modules (components, pipes, SVG charts). */
@NgModule({
  declarations: DECLARED,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  exports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, ...DECLARED]
})
export class SharedUiModule {}
