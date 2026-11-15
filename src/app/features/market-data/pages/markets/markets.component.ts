import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, Subscription, combineLatest, debounceTime, distinctUntilChanged, map, startWith } from 'rxjs';
import { SegmentOption } from '../../../../shared/ui/components/tx-segmented/tx-segmented.component';
import { MARKET_TABS } from '../../../../shared/ui/util/module-tabs';
import { Section } from '../../../../shared/ui/util/section';
import { lsGet, lsSet } from '../../../../shared/ui/util/safe-storage';
import { ToastService } from '../../../../shared/ui/util/toast.service';
import { TradeTicketService } from '../../../../shared/ui/util/trade-ticket.service';
import { InstrumentResponse, MarketApiError, MarketStatusResponse, MoverType, QuoteResponse } from '../../models/market-data.model';
import { MarketDataService } from '../../services/market-data.service';
import { MarketStatusStore } from '../../services/market-status.store';
import { QuoteStore } from '../../services/quote.store';
import { WatchlistService } from '../../services/watchlist.service';

type SortKey = 'ticker' | 'name' | 'sector' | 'last' | 'changePercent' | 'volume' | 'turnover' | 'updatedAt';
interface Prefs { sortKey: SortKey; sortDir: 'asc' | 'desc'; sector: string; watchOnly: boolean; }
interface Row { inst: InstrumentResponse; q: QuoteResponse | null; }

@Component({
  selector: 'app-markets',
  templateUrl: './markets.component.html',
  styleUrls: ['./markets.component.scss']
})
export class MarketsComponent implements OnInit, OnDestroy {
  readonly tabs = MARKET_TABS;
  readonly moverOptions: SegmentOption[] = [
    { value: 'GAINERS', label: 'Gainers' }, { value: 'LOSERS', label: 'Losers' }, { value: 'ACTIVE', label: 'Most active' }
  ];

  status$: Observable<MarketStatusResponse | null>;
  moverType: MoverType = 'GAINERS';
  movers: Section<QuoteResponse[]>;
  instruments: Section<InstrumentResponse[]>;
  sectors: string[] = [];
  search = new FormControl<string>('', { nonNullable: true });
  sector = '';
  watchOnly = false;
  sortKey: SortKey = 'ticker';
  sortDir: 'asc' | 'desc' = 'asc';
  rows: Row[] = [];
  flash = new Map<string, 'up' | 'down'>();
  watched = new Set<string>();
  pending = new Set<string>();

  private live = new Map<string, QuoteResponse>();
  private sector$ = new BehaviorSubject<string>('');
  private subs = new Subscription();

  constructor(
    private market: MarketDataService,
    private statusStore: MarketStatusStore,
    private quotes: QuoteStore,
    private watchlist: WatchlistService,
    private ticket: TradeTicketService,
    private toast: ToastService,
    private router: Router
  ) {
    this.status$ = statusStore.status$;
    this.movers = new Section<QuoteResponse[]>(toast);
    this.instruments = new Section<InstrumentResponse[]>(toast, () => this.rebuild());
  }

  ngOnInit(): void {
    const p = lsGet<Partial<Prefs>>('markets_prefs', {});
    this.sortKey = p.sortKey ?? 'ticker';
    this.sortDir = p.sortDir ?? 'asc';
    this.sector = p.sector ?? '';
    this.watchOnly = !!p.watchOnly;
    this.sector$.next(this.sector);

    this.loadMovers();
    this.market.sectors().subscribe({ next: s => (this.sectors = s ?? []), error: () => undefined });

    // Debounced search (250 ms, trimmed, ≤ 50 chars) + sector chip → backend search (routes 8 and 10).
    this.subs.add(combineLatest([
      this.search.valueChanges.pipe(
        startWith(this.search.value), debounceTime(250), map(v => v.trim().slice(0, 50)), distinctUntilChanged()
      ),
      this.sector$
    ]).subscribe(([q, s]) => this.instruments.load(() => this.market.instruments(q, s), true)));

    // Live prices: merge into the table with a 600 ms flash.
    this.subs.add(this.quotes.all$.subscribe(list => this.applyLive(list)));

    this.subs.add(this.watchlist.tickers$.subscribe(t => { this.watched = t; this.rebuild(); }));
    this.watchlist.list().subscribe({ error: () => undefined });
  }

  ngOnDestroy(): void { this.subs.unsubscribe(); this.instruments.destroy(); this.movers.destroy(); }

  loadMovers(): void { this.movers.load(() => this.market.movers(this.moverType, 5), true); }
  setMover(v: string): void { this.moverType = v as MoverType; this.loadMovers(); }

  setSector(s: string): void { this.sector = s; this.sector$.next(s); this.savePrefs(); }
  toggleWatchOnly(): void { this.watchOnly = !this.watchOnly; this.rebuild(); this.savePrefs(); }

  sortBy(k: SortKey): void {
    if (this.sortKey === k) this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    else { this.sortKey = k; this.sortDir = k === 'ticker' || k === 'name' || k === 'sector' ? 'asc' : 'desc'; }
    this.rebuild();
    this.savePrefs();
  }
  ariaSort(k: SortKey): 'ascending' | 'descending' | 'none' { return this.sortKey === k ? (this.sortDir === 'asc' ? 'ascending' : 'descending') : 'none'; }

  private applyLive(list: QuoteResponse[]): void {
    for (const q of list) {
      const old = this.live.get(q.ticker) ?? this.instruments.data?.find(i => i.ticker === q.ticker)?.quote ?? null;
      if (old && old.last !== q.last) {
        this.flash.set(q.ticker, q.last > old.last ? 'up' : 'down');
        const t = q.ticker;
        setTimeout(() => this.flash.delete(t), 600);
      }
      this.live.set(q.ticker, q);
    }
    this.rebuild();
  }

  private rebuild(): void {
    const list = this.instruments.data ?? [];
    const rows: Row[] = list
      .filter(i => !this.watchOnly || this.watched.has(i.ticker))
      .map(inst => ({ inst, q: this.live.get(inst.ticker) ?? inst.quote }));
    const dir = this.sortDir === 'asc' ? 1 : -1;
    const val = (r: Row): string | number => {
      switch (this.sortKey) {
        case 'ticker': return r.inst.ticker;
        case 'name': return r.inst.name ?? '';
        case 'sector': return r.inst.sector ?? '';
        case 'updatedAt': return r.q?.updatedAt ?? '';
        default: return r.q ? (r.q[this.sortKey] as number) : -Infinity;
      }
    };
    rows.sort((a, b) => {
      const x = val(a), y = val(b);
      return (typeof x === 'string' ? x.localeCompare(y as string) : x - (y as number)) * dir;
    });
    this.rows = rows;
  }

  trackTicker(_: number, r: Row): string { return r.inst.ticker; }

  open(ticker: string): void { void this.router.navigate(['/markets', ticker]); }
  trade(ticker: string): void { this.ticket.open({ ticker }); }

  toggleWatch(ticker: string): void {
    if (this.pending.has(ticker)) return;
    this.pending.add(ticker);
    const adding = !this.watched.has(ticker);
    const call = adding ? this.watchlist.add(ticker) : this.watchlist.remove(ticker);
    (call as Observable<unknown>).subscribe({
      next: () => { this.pending.delete(ticker); this.toast.success(adding ? `${ticker} added to your watchlist` : `${ticker} removed from your watchlist`); },
      error: (e: MarketApiError) => {
        this.pending.delete(ticker);
        if (e.kind === 'bad-request' || e.kind === 'unprocessable') this.toast.info(e.message); else this.toast.reportError(e);
      }
    });
  }

  private savePrefs(): void {
    lsSet('markets_prefs', { sortKey: this.sortKey, sortDir: this.sortDir, sector: this.sector, watchOnly: this.watchOnly } satisfies Prefs);
  }
}
