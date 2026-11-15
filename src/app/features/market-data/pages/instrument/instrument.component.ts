import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Observable, Subscription, catchError, of } from 'rxjs';
import { PositionDetailResponse } from '../../../portfolio/models/portfolio.model';
import { PositionService } from '../../../portfolio/services/position.service';
import { MARKET_TABS } from '../../../../shared/ui/util/module-tabs';
import { Section } from '../../../../shared/ui/util/section';
import { downloadCsv } from '../../../../shared/ui/util/csv';
import { addMonths, todayTunis } from '../../../../shared/ui/util/dates';
import { ToastService } from '../../../../shared/ui/util/toast.service';
import { TradeTicketService } from '../../../../shared/ui/util/trade-ticket.service';
import { CandleChartComponent, ChartTrade } from '../../components/candle-chart/candle-chart.component';
import { CandleSeriesResponse, InstrumentResponse, MarketApiError, PriceBarResponse, QuoteResponse } from '../../models/market-data.model';
import { CandleService } from '../../services/candle.service';
import { MarketDataService } from '../../services/market-data.service';
import { QuoteStore } from '../../services/quote.store';
import { WatchlistService } from '../../services/watchlist.service';

@Component({
  selector: 'app-instrument',
  templateUrl: './instrument.component.html',
  styleUrls: ['./instrument.component.scss']
})
export class InstrumentComponent implements OnInit, OnDestroy {
  readonly tabs = MARKET_TABS;
  ticker = '';
  instrument: Section<InstrumentResponse>;
  quote: QuoteResponse | null = null;
  flash: 'up' | 'down' | '' = '';
  position: PositionDetailResponse | null = null;
  history: Section<PriceBarResponse[]>;
  daily: Section<CandleSeriesResponse>;
  trades: ChartTrade[] = [];
  fitToken = 0;
  showHistory = false;
  watched = false;
  watchPending = false;
  readonly today = todayTunis();
  readonly from = addMonths(this.today, -6);

  private subs = new Subscription();
  private liveSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private market: MarketDataService,
    private quotes: QuoteStore,
    private watchlist: WatchlistService,
    private positions: PositionService,
    private candleApi: CandleService,
    private ticket: TradeTicketService,
    private toast: ToastService
  ) {
    this.instrument = new Section<InstrumentResponse>(toast);
    this.history = new Section<PriceBarResponse[]>(toast);
    this.daily = new Section<CandleSeriesResponse>(toast, () => this.fitToken++);
  }

  ngOnInit(): void {
    this.subs.add(this.route.paramMap.subscribe(p => this.load((p.get('ticker') ?? '').toUpperCase())));
    this.subs.add(this.watchlist.tickers$.subscribe(t => (this.watched = t.has(this.ticker))));
    this.watchlist.list().subscribe({ error: () => undefined });
  }

  ngOnDestroy(): void { this.subs.unsubscribe(); this.liveSub?.unsubscribe(); this.instrument.destroy(); this.history.destroy(); this.daily.destroy(); }

  private load(ticker: string): void {
    this.ticker = ticker;
    this.quote = null;
    this.position = null;
    this.trades = [];
    this.showHistory = false;
    this.history.data = null;
    this.watched = this.watchlist.has(ticker);
    this.instrument.load(() => this.market.instrument(ticker));
    this.daily.load(() => this.candleApi.candles(ticker, 'D1', this.from, this.today));
    // Route 6 seeds the quote; the live store keeps it fresh afterwards (newest updatedAt wins).
    this.market.quote(ticker).pipe(catchError(() => of(null))).subscribe(q => { if (q && ticker === this.ticker) this.merge(q); });
    this.liveSub?.unsubscribe();
    this.liveSub = this.quotes.quote$(ticker).subscribe(q => { if (q) this.merge(q); });
    // "Your position" (route 27): only requested when the stock was ever held, so there is no failing request.
    this.positions.detailIfHeld(ticker).pipe(catchError(() => of(null))).subscribe(d => {
      if (ticker !== this.ticker) return;
      this.position = d && d.position.status === 'OPEN' ? d : null;
      this.trades = (d?.transactions ?? [])
        .filter(x => (x.type === 'BUY' || x.type === 'SELL') && x.quantity && x.price !== null)
        .map(x => ({ time: Math.floor(Date.parse(x.executedAt) / 1000), side: x.type as 'BUY' | 'SELL', quantity: x.quantity as number, price: x.price as number }));
    });
  }

  private merge(q: QuoteResponse): void {
    if (this.quote && Date.parse(q.updatedAt) < Date.parse(this.quote.updatedAt)) return;
    if (this.quote && this.quote.last !== q.last) {
      this.flash = q.last > this.quote.last ? 'up' : 'down';
      setTimeout(() => (this.flash = ''), 600);
    }
    this.quote = q;
  }

  get q(): QuoteResponse | null { return this.quote ?? this.instrument.data?.quote ?? null; }

  toggleWatch(): void {
    if (this.watchPending) return;
    this.watchPending = true;
    const adding = !this.watched;
    ((adding ? this.watchlist.add(this.ticker) : this.watchlist.remove(this.ticker)) as Observable<unknown>).subscribe({
      next: () => { this.watchPending = false; this.toast.success(adding ? `${this.ticker} added to your watchlist` : `${this.ticker} removed from your watchlist`); },
      error: (e: MarketApiError) => { this.watchPending = false; if (e.kind === 'bad-request') this.toast.info(e.message); else this.toast.reportError(e); }
    });
  }

  trade(side: 'BUY' | 'SELL'): void { this.ticket.open({ ticker: this.ticker, side }); }

  toggleHistory(): void {
    this.showHistory = !this.showHistory;
    if (this.showHistory && !this.history.data) this.loadHistory();
  }
  loadHistory(): void { this.history.load(() => this.market.history(this.ticker, this.from, this.today)); }

  downloadHistory(): void {
    const rows = (this.history.data ?? []).map(b => [b.date, b.open, b.high, b.low, b.close, b.volume]);
    downloadCsv(`${this.ticker}-history-${this.from}_${this.today}.csv`, ['date', 'open', 'high', 'low', 'close', 'volume'], rows);
  }
}
