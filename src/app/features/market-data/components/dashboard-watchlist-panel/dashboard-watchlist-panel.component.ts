import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { Section } from '../../../../shared/ui/util/section';
import { QuoteResponse, WatchlistItemResponse } from '../../models/market-data.model';
import { MarketDataService } from '../../services/market-data.service';
import { QuoteStore } from '../../services/quote.store';
import { WatchlistService } from '../../services/watchlist.service';

interface Row { ticker: string; name: string; q: QuoteResponse | null; }

/**
 * Replaces the illustrative "Watchlist snapshot" panel on the dashboard Home tab: real watchlist rows with live price
 * and change. An empty watchlist shows today's top gainers instead.
 */
@Component({
  selector: 'app-dashboard-watchlist-panel',
  templateUrl: './dashboard-watchlist-panel.component.html',
  styleUrls: ['./dashboard-watchlist-panel.component.scss']
})
export class DashboardWatchlistPanelComponent implements OnInit, OnDestroy {
  list: Section<WatchlistItemResponse[]>;
  gainers: Section<QuoteResponse[]>;
  private live = new Map<string, QuoteResponse>();
  private subs = new Subscription();

  constructor(private watchlist: WatchlistService, private market: MarketDataService, private quotes: QuoteStore) {
    this.list = new Section<WatchlistItemResponse[]>(undefined, l => { if (!l.length) this.loadGainers(); });
    this.gainers = new Section<QuoteResponse[]>();
  }

  ngOnInit(): void {
    this.list.load(() => this.watchlist.list());
    this.subs.add(this.quotes.all$.subscribe(qs => qs.forEach(q => this.live.set(q.ticker, q))));
  }
  ngOnDestroy(): void { this.subs.unsubscribe(); this.list.destroy(); this.gainers.destroy(); }

  private loadGainers(): void { this.gainers.load(() => this.market.movers('GAINERS', 5)); }

  get rows(): Row[] {
    return (this.list.data ?? []).slice(0, 6).map(i => ({ ticker: i.ticker, name: i.name, q: this.live.get(i.ticker) ?? i.quote }));
  }
  get gainerRows(): Row[] { return (this.gainers.data ?? []).map(q => ({ ticker: q.ticker, name: q.name, q: this.live.get(q.ticker) ?? q })); }
  trackTicker(_: number, r: Row): string { return r.ticker; }
}
