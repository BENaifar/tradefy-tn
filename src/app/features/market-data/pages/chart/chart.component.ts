import { Component, HostListener, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription, catchError, debounceTime, distinctUntilChanged, filter, of, skip } from 'rxjs';
import { PositionService } from '../../../portfolio/services/position.service';
import { SegmentOption } from '../../../../shared/ui/components/tx-segmented/tx-segmented.component';
import { addDays, addMonths, todayTunis } from '../../../../shared/ui/util/dates';
import { MARKET_TABS } from '../../../../shared/ui/util/module-tabs';
import { lsGet, lsSet } from '../../../../shared/ui/util/safe-storage';
import { Section } from '../../../../shared/ui/util/section';
import { ToastService } from '../../../../shared/ui/util/toast.service';
import { CandleChartComponent, ChartIndicator, ChartTrade } from '../../components/candle-chart/candle-chart.component';
import { IndicatorAdd, IndicatorUpdate, PresetName } from '../../components/indicator-panel/indicator-panel.component';
import { indicatorLabel } from '../../models/indicator-label';
import {
  Candle, CandleInterval, CandleSeriesResponse, IndicatorDefinition, IndicatorInstance, MarketApiError, QuoteResponse
} from '../../models/market-data.model';
import { CandleService } from '../../services/candle.service';
import { IndicatorService } from '../../services/indicator.service';
import { MarketStatusStore } from '../../services/market-status.store';
import { QuoteStore } from '../../services/quote.store';
import { WatchlistService } from '../../services/watchlist.service';

type RangeKey = '1D' | '5D' | '1M' | '3M' | '6M' | '1Y' | '5Y' | 'MAX';

interface Prefs {
  timeframe: CandleInterval;
  range: RangeKey;
  volume: boolean;
  legend: boolean;
  showTrades: boolean;
  indicators: { code: string; params: Record<string, number>; color: string; visible: boolean }[];
}

const TIMEFRAMES: { value: CandleInterval; label: string; title: string }[] = [
  { value: 'M5', label: '5m', title: '5-minute candles' },
  { value: 'M15', label: '15m', title: '15-minute candles' },
  { value: 'M30', label: '30m', title: '30-minute candles' },
  { value: 'H1', label: '1h', title: '1-hour candles' },
  { value: 'H2', label: '2h', title: '2-hour candles' },
  { value: 'H4', label: '4h', title: '4-hour candles' },
  { value: 'D1', label: '1D', title: 'One candle per trading day (24 h)' },
  { value: 'W1', label: '1W', title: 'One candle per week' }
];
const RANGES: RangeKey[] = ['1D', '5D', '1M', '3M', '6M', '1Y', '5Y', 'MAX'];
const DEFAULT_RANGE: Record<CandleInterval, RangeKey> = { M5: '1D', M15: '5D', M30: '5D', H1: '1M', H2: '1M', H4: '3M', D1: '1Y', W1: '5Y' };
const INTRADAY: CandleInterval[] = ['M5', 'M15', 'M30', 'H1', 'H2', 'H4'];
const PALETTE = ['#f4f6f8', '#8fb3ff', '#d0a2f7', '#c7cbd1', '#9aa0a8', '#666c75', '#e5c07b', '#4ade80'];
const PRESETS: Record<PresetName, { code: string; values: number[] }[]> = {
  Trend: [{ code: 'SMA', values: [20] }, { code: 'SMA', values: [50] }],
  Momentum: [{ code: 'RSI', values: [14] }, { code: 'MACD', values: [] }],
  Volatility: [{ code: 'BOLL', values: [20, 2] }]
};

@Component({
  selector: 'app-chart',
  templateUrl: './chart.component.html',
  styleUrls: ['./chart.component.scss']
})
export class ChartComponent implements OnInit, OnDestroy {
  readonly tabs = MARKET_TABS;
  readonly tfOptions: SegmentOption[] = TIMEFRAMES.map(t => ({ value: t.value, label: t.label, title: t.title }));
  rangeOptions: SegmentOption[] = [];

  ticker = '';
  interval: CandleInterval = 'D1';
  range: RangeKey = '1Y';
  showVolume = true;
  showLegend = true;
  showTrades = true;
  live = false;
  marketLive = false;
  panelOpen = typeof window !== 'undefined' ? window.innerWidth > 1050 : true;

  series: Section<CandleSeriesResponse>;
  catalog: Section<IndicatorDefinition[]>;
  candles: Candle[] = [];
  chartInterval: CandleInterval = 'D1';
  instances: IndicatorInstance[] = [];
  chartIndicators: ChartIndicator[] = [];
  trades: ChartTrade[] = [];
  quote: QuoteResponse | null = null;
  fitToken = 0;
  from = '';
  to = '';

  @ViewChild(CandleChartComponent) chart?: CandleChartComponent;

  private pendingFit = false;
  private seq = 0;
  private prefs: Prefs | null = null;
  private restored = false;
  private subs = new Subscription();
  private tickerSubs = new Subscription();
  private liveSub?: Subscription;
  private indSubs = new Map<string, Subscription>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private candleApi: CandleService,
    private indicatorApi: IndicatorService,
    private quotes: QuoteStore,
    private status: MarketStatusStore,
    private watchlist: WatchlistService,
    private positions: PositionService,
    private toast: ToastService
  ) {
    this.series = new Section<CandleSeriesResponse>(toast, d => {
      this.candles = d.candles;
      this.chartInterval = d.interval;
      if (this.pendingFit) { this.fitToken++; this.pendingFit = false; }
    });
    this.catalog = new Section<IndicatorDefinition[]>(undefined, () => this.onCatalog());
  }

  // ---------------------------------------------------------------- lifecycle
  ngOnInit(): void {
    const p = lsGet<Partial<Prefs> | null>('chart_prefs', null);
    if (p) {
      this.prefs = p as Prefs;
      if (TIMEFRAMES.some(t => t.value === p.timeframe)) this.interval = p.timeframe as CandleInterval;
      this.range = RANGES.includes(p.range as RangeKey) ? (p.range as RangeKey) : DEFAULT_RANGE[this.interval];
      this.showVolume = p.volume !== false;
      this.showLegend = p.legend !== false;
      this.showTrades = p.showTrades !== false;
    }
    this.chartInterval = this.interval;
    this.refreshRangeOptions();
    if (this.rangeDisabled(this.range)) this.range = DEFAULT_RANGE[this.interval];

    this.catalog.load(() => this.indicatorApi.catalog());
    this.subs.add(this.status.status$.subscribe(s => {
      this.marketLive = MarketStatusStore.isLiveSession(s);
      if (!this.marketLive && this.live) this.setLive(false);
    }));
    this.subs.add(this.route.paramMap.subscribe(m => {
      const t = (m.get('ticker') ?? '').trim().toUpperCase();
      if (t) this.openTicker(t); else this.resolveDefault();
    }));
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
    this.tickerSubs.unsubscribe();
    this.liveSub?.unsubscribe();
    this.indSubs.forEach(s => s.unsubscribe());
    this.series.destroy();
    this.catalog.destroy();
  }

  /** `/chart` alone: last used ticker, else the first watchlist ticker, else TJARI. */
  private resolveDefault(): void {
    const last = lsGet<string>('chart_last', '');
    if (last) { void this.router.navigate(['/chart', last], { replaceUrl: true }); return; }
    this.watchlist.list().pipe(catchError(() => of([]))).subscribe(list => {
      void this.router.navigate(['/chart', list[0]?.ticker ?? 'TJARI'], { replaceUrl: true });
    });
  }

  private openTicker(t: string): void {
    if (t === this.ticker && this.series.loaded) return;
    this.ticker = t;
    lsSet('chart_last', t);
    this.tickerSubs.unsubscribe();
    this.tickerSubs = new Subscription();
    this.candles = [];
    this.trades = [];
    this.quote = null;
    this.instances.forEach(i => { i.result = null; });
    this.syncChart();
    this.tickerSubs.add(this.quotes.quote$(t).subscribe(q => { if (q) this.quote = q; }));
    this.tickerSubs.add(
      this.positions.detailIfHeld(t).pipe(catchError(() => of(null))).subscribe(d => {
        this.trades = (d?.transactions ?? [])
          .filter(x => (x.type === 'BUY' || x.type === 'SELL') && x.quantity && x.price !== null)
          .map(x => ({ time: Math.floor(Date.parse(x.executedAt) / 1000), side: x.type as 'BUY' | 'SELL', quantity: x.quantity as number, price: x.price as number }));
      })
    );
    this.setLive(this.live);
    this.loadAll(true);
  }

  // ---------------------------------------------------------------- toolbar
  pick(t: string): void { if (t && t !== this.ticker) void this.router.navigate(['/chart', t]); }

  setTimeframe(v: string): void {
    this.interval = v as CandleInterval;
    this.range = DEFAULT_RANGE[this.interval];
    this.refreshRangeOptions();
    this.savePrefs();
    this.loadAll(true);
  }

  setRange(v: string): void {
    this.range = v as RangeKey;
    this.savePrefs();
    this.loadAll(true);
  }

  private rangeDisabled(r: RangeKey): boolean { return this.rangeReason(r) !== ''; }

  /** Nonsense combinations are disabled with a tooltip. */
  private rangeReason(r: RangeKey): string {
    if (INTRADAY.includes(this.interval) && (r === '1Y' || r === '5Y' || r === 'MAX')) return 'Intraday data is kept for 180 days';
    if (this.interval === 'W1' && (r === '1D' || r === '5D' || r === '1M')) return 'Weekly candles need a longer range';
    if (this.interval === 'D1' && r === '1D') return 'A daily chart needs more than one day';
    return '';
  }

  private refreshRangeOptions(): void {
    this.rangeOptions = RANGES.map(r => {
      const reason = this.rangeReason(r);
      return { value: r, label: r === 'MAX' ? 'Max' : r, disabled: !!reason, title: reason || undefined };
    });
  }

  toggleVolume(): void { this.showVolume = !this.showVolume; this.savePrefs(); }
  toggleLegend(): void { this.showLegend = !this.showLegend; this.savePrefs(); }
  toggleTrades(): void { this.showTrades = !this.showTrades; this.savePrefs(); }
  toggleLive(): void { this.setLive(!this.live); }

  get liveReason(): string { return this.marketLive ? '' : 'Market closed: live prices resume at 09:00 Tunis time'; }

  private setLive(on: boolean): void {
    this.live = on && this.marketLive;
    this.liveSub?.unsubscribe();
    if (!this.live || !this.ticker) return;
    // On each live message: debounce 2 s, then reload candles and indicators WITHOUT fitting (the zoom is kept).
    this.liveSub = this.quotes.quote$(this.ticker).pipe(
      filter((q): q is QuoteResponse => !!q),
      distinctUntilChanged((a, b) => a.updatedAt === b.updatedAt),
      skip(1),
      debounceTime(2000)
    ).subscribe(() => this.loadAll(false));
  }

  switchToDaily(): void { this.setTimeframe('D1'); }
  get isIntraday(): boolean { return INTRADAY.includes(this.interval); }

  // ---------------------------------------------------------------- keyboard: 1–8 switch timeframes
  @HostListener('document:keydown', ['$event'])
  onKey(ev: KeyboardEvent): void {
    if (ev.ctrlKey || ev.metaKey || ev.altKey) return;
    const t = ev.target as HTMLElement | null;
    if (t && (['INPUT', 'TEXTAREA', 'SELECT'].includes(t.tagName) || t.isContentEditable)) return;
    const n = Number(ev.key);
    if (Number.isInteger(n) && n >= 1 && n <= TIMEFRAMES.length) this.setTimeframe(TIMEFRAMES[n - 1].value);
  }

  // ---------------------------------------------------------------- data
  private get window(): { from: string; to: string } {
    const to = todayTunis();
    const from = ((): string => {
      switch (this.range) {
        case '1D': return addDays(to, -1);
        case '5D': return addDays(to, -5);
        case '1M': return addMonths(to, -1);
        case '3M': return addMonths(to, -3);
        case '6M': return addMonths(to, -6);
        case '1Y': return addMonths(to, -12);
        case '5Y': return addMonths(to, -60);
        default: return '2000-01-01';
      }
    })();
    return { from, to };
  }

  private loadAll(fit: boolean): void {
    if (!this.ticker) return;
    const w = this.window;
    this.from = w.from;
    this.to = w.to;
    this.pendingFit = fit;
    this.series.load(() => this.candleApi.candles(this.ticker, this.interval, w.from, w.to), true);
    this.instances.forEach(i => this.refetch(i));
  }

  reload(): void { this.loadAll(false); }

  // ---------------------------------------------------------------- indicators
  private onCatalog(): void {
    if (!this.restored) {
      this.restored = true;
      this.restore();
    }
    this.syncChart();
  }

  retryCatalog(): void { this.indicatorApi.resetCatalog(); this.catalog.load(() => this.indicatorApi.catalog()); }

  private def(code: string): IndicatorDefinition | undefined { return this.catalog.data?.find(d => d.code === code); }
  private panes(list: IndicatorInstance[] = this.instances): number { return list.filter(i => this.def(i.code)?.placement === 'PANE').length; }

  /** Saved indicators are validated against the catalog: unknown codes and out-of-range params are dropped. */
  private restore(): void {
    const saved = this.prefs?.indicators;
    if (!Array.isArray(saved) || !this.catalog.data) return;
    for (const s of saved) {
      const d = this.def(s?.code);
      if (!d || this.instances.length >= 8) continue;
      if (d.placement === 'PANE' && this.panes() >= 3) continue;
      const params: Record<string, number> = {};
      for (const p of d.params) {
        const v = s.params?.[p.name];
        const ok = typeof v === 'number' && Number.isFinite(v) && v >= p.min && v <= p.max && (p.type !== 'INT' || Number.isInteger(v));
        params[p.name] = ok ? v : p.defaultValue;
      }
      const color = typeof s.color === 'string' && /^#[0-9a-f]{6}$/i.test(s.color) ? s.color : this.nextColor();
      this.instances.push({ id: `ind-${++this.seq}`, code: d.code, params, color, visible: s.visible !== false, status: 'loading', error: '', result: null });
    }
    this.instances.forEach(i => this.refetch(i));
  }

  private nextColor(): string {
    const used = new Set(this.instances.map(i => i.color.toLowerCase()));
    return PALETTE.find(c => !used.has(c)) ?? PALETTE[this.instances.length % PALETTE.length];
  }

  private refetch(inst: IndicatorInstance): void {
    this.indSubs.get(inst.id)?.unsubscribe();
    if (!this.ticker) return;
    inst.status = 'loading';
    inst.error = '';
    const w = this.window;
    this.indSubs.set(inst.id, this.indicatorApi.compute(this.ticker, inst.code, this.interval, w.from, w.to, inst.params).subscribe({
      next: r => { inst.result = r; inst.status = 'ok'; this.syncChart(); },
      error: (e: MarketApiError) => {
        // An indicator error never breaks the chart: it shows on that indicator's row.
        inst.result = null;
        inst.status = 'error';
        inst.error = Object.values(e.fieldErrors ?? {}).join(' ') || e.message;
        this.syncChart();
        if (e.kind === 'network') this.toast.reportError(e);
      }
    }));
  }

  private syncChart(): void {
    this.chartIndicators = this.instances.flatMap(i => {
      const def = this.def(i.code);
      return def ? [{ id: i.id, def, result: i.result, color: i.color, visible: i.visible, label: indicatorLabel(def, i.code, i.params) }] : [];
    });
  }

  addIndicator(a: IndicatorAdd): void {
    const d = this.def(a.code);
    if (!d || this.instances.length >= 8 || (d.placement === 'PANE' && this.panes() >= 3)) return;
    const inst: IndicatorInstance = { id: `ind-${++this.seq}`, code: a.code, params: a.params, color: this.nextColor(), visible: true, status: 'loading', error: '', result: null };
    this.instances = [...this.instances, inst];
    this.syncChart();
    this.refetch(inst);
    this.savePrefs();
  }

  updateIndicator(u: IndicatorUpdate): void {
    const inst = this.instances.find(i => i.id === u.id);
    if (!inst) return;
    inst.params = u.params;
    this.refetch(inst); // only this one
    this.savePrefs();
  }

  removeIndicator(id: string): void {
    this.indSubs.get(id)?.unsubscribe();
    this.indSubs.delete(id);
    this.instances = this.instances.filter(i => i.id !== id);
    this.syncChart();
    this.savePrefs();
  }

  /** Show / hide without refetching: the series are only toggled. */
  toggleIndicator(id: string): void {
    const inst = this.instances.find(i => i.id === id);
    if (!inst) return;
    inst.visible = !inst.visible;
    this.syncChart();
    this.savePrefs();
  }

  recolor(e: { id: string; color: string }): void {
    const inst = this.instances.find(i => i.id === e.id);
    if (!inst) return;
    inst.color = e.color;
    this.syncChart();
    this.savePrefs();
  }

  setAllVisible(v: boolean): void { this.instances.forEach(i => (i.visible = v)); this.syncChart(); this.savePrefs(); }

  clearIndicators(): void {
    this.indSubs.forEach(s => s.unsubscribe());
    this.indSubs.clear();
    this.instances = [];
    this.syncChart();
    this.savePrefs();
  }

  applyPreset(name: PresetName): void {
    this.clearIndicators();
    for (const entry of PRESETS[name]) {
      const d = this.def(entry.code); // only codes present in the catalog
      if (!d) continue;
      const params: Record<string, number> = {};
      d.params.forEach((p, i) => (params[p.name] = entry.values[i] ?? p.defaultValue));
      this.addIndicator({ code: d.code, params });
    }
  }

  // ---------------------------------------------------------------- chart actions
  resetView(): void { this.chart?.resetView(); }
  fullscreen(): void { this.chart?.fullscreen(); }
  screenshot(): void { this.chart?.screenshot(`${this.ticker}-${this.interval}.png`); }

  private savePrefs(): void {
    const prefs: Prefs = {
      timeframe: this.interval, range: this.range, volume: this.showVolume, legend: this.showLegend, showTrades: this.showTrades,
      indicators: this.instances.map(i => ({ code: i.code, params: i.params, color: i.color, visible: i.visible }))
    };
    lsSet('chart_prefs', prefs);
  }
}
