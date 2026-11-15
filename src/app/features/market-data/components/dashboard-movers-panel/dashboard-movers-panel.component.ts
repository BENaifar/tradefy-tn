import { Component, OnDestroy, OnInit } from '@angular/core';
import { Section } from '../../../../shared/ui/util/section';
import { QuoteResponse } from '../../models/market-data.model';
import { MarketDataService } from '../../services/market-data.service';

/** Top 3 gainers and losers (route 7), for the dashboard modules row. Links to /markets. */
@Component({
  selector: 'app-dashboard-movers-panel',
  templateUrl: './dashboard-movers-panel.component.html',
  styleUrls: ['./dashboard-movers-panel.component.scss']
})
export class DashboardMoversPanelComponent implements OnInit, OnDestroy {
  gainers: Section<QuoteResponse[]>;
  losers: Section<QuoteResponse[]>;

  constructor(private market: MarketDataService) {
    this.gainers = new Section<QuoteResponse[]>();
    this.losers = new Section<QuoteResponse[]>();
  }

  ngOnInit(): void { this.load(); }
  ngOnDestroy(): void { this.gainers.destroy(); this.losers.destroy(); }

  load(): void {
    this.gainers.load(() => this.market.movers('GAINERS', 3));
    this.losers.load(() => this.market.movers('LOSERS', 3));
  }
  get failed(): boolean { return !!(this.gainers.error || this.losers.error); }
}
