import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { MARKET_TABS } from '../../../../shared/ui/util/module-tabs';
import { lsGet, lsSet } from '../../../../shared/ui/util/safe-storage';
import { Section } from '../../../../shared/ui/util/section';
import { ToastService } from '../../../../shared/ui/util/toast.service';
import { TradeTicketService } from '../../../../shared/ui/util/trade-ticket.service';
import { MarketApiError, QuoteResponse, WatchlistItemResponse } from '../../models/market-data.model';
import { QuoteStore } from '../../services/quote.store';
import { WatchlistService } from '../../services/watchlist.service';

@Component({
  selector: 'app-watchlist',
  templateUrl: './watchlist.component.html',
  styleUrls: ['./watchlist.component.scss']
})
export class WatchlistComponent implements OnInit, OnDestroy {
  readonly tabs = MARKET_TABS;
  list: Section<WatchlistItemResponse[]>;
  view: 'cards' | 'table' = 'cards';
  adding = false;
  private live = new Map<string, QuoteResponse>();
  private subs = new Subscription();

  constructor(
    private watchlist: WatchlistService,
    private quotes: QuoteStore,
    private ticket: TradeTicketService,
    private toast: ToastService
  ) {
    this.list = new Section<WatchlistItemResponse[]>(toast);
  }

  ngOnInit(): void {
    this.view = lsGet<'cards' | 'table'>('watchlist_view', 'cards') === 'table' ? 'table' : 'cards';
    this.list.load(() => this.watchlist.list());
    // The page shows whatever the shared watchlist holds (so removals / undo and ☆ elsewhere stay in sync).
    this.subs.add(this.watchlist.items$.subscribe(items => { if (items && this.list.loaded && !this.list.error) this.list.data = items; }));
    this.subs.add(this.quotes.all$.subscribe(q => q.forEach(x => this.live.set(x.ticker, x))));
  }

  ngOnDestroy(): void { this.subs.unsubscribe(); this.list.destroy(); }

  setView(v: 'cards' | 'table'): void { this.view = v; lsSet('watchlist_view', v); }

  q(it: WatchlistItemResponse): QuoteResponse | null { return this.live.get(it.ticker) ?? it.quote; }

  /** Duplicates are blocked client-side; a backend 400 is still handled. */
  add(ticker: string): void {
    if (!ticker || this.adding) return;
    if (this.watchlist.has(ticker)) { this.toast.info(`${ticker} is already in your watchlist`); return; }
    this.adding = true;
    this.watchlist.add(ticker).subscribe({
      next: () => { this.adding = false; this.toast.success(`${ticker} added to your watchlist`); },
      error: (e: MarketApiError) => { this.adding = false; if (e.kind === 'bad-request' || e.kind === 'unprocessable') this.toast.info(e.message); else this.toast.reportError(e); }
    });
  }

  /** Removal with an undo window: the DELETE is only sent when the 5 s toast expires. */
  remove(it: WatchlistItemResponse): void {
    const t = it.ticker;
    this.watchlist.hideLocally(t);
    this.toast.withAction(
      `${t} removed from your watchlist`, 'Undo',
      () => this.watchlist.list().subscribe({ error: () => undefined }),
      () => this.watchlist.remove(t).subscribe({ error: (e: MarketApiError) => { this.toast.reportError(e); this.watchlist.list().subscribe({ error: () => undefined }); } }),
      5000
    );
  }

  trade(t: string): void { this.ticket.open({ ticker: t }); }
  trackTicker(_: number, it: WatchlistItemResponse): string { return it.ticker; }
}
