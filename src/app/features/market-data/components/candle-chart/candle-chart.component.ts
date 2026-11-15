import {
  AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, Input, NgZone, OnChanges, OnDestroy, SimpleChanges, ViewChild
} from '@angular/core';
import type {
  IChartApi, ISeriesApi, ISeriesMarkersPluginApi, MouseEventParams, SeriesMarker, Time, UTCTimestamp
} from 'lightweight-charts';
import { fmtNumber, fmtPct, fmtQty } from '../../../../shared/ui/util/format';
import { Candle, CandleInterval, IndicatorDefinition, IndicatorResult } from '../../models/market-data.model';

export interface ChartIndicator {
  id: string;
  def: IndicatorDefinition;
  result: IndicatorResult | null;
  color: string;
  visible: boolean;
  label: string;
}
export interface ChartTrade { time: number; side: 'BUY' | 'SELL'; quantity: number; price: number; }
export interface AreaPoint { time: string; value: number; }

interface LegendIndicator { label: string; color: string; value: string; }
interface Legend { title: string; ohlc: { o: string; h: string; l: string; c: string; chg: string; chgClass: string; vol: string } | null; value: string | null; inds: LegendIndicator[]; trades: string[]; }

type Lib = typeof import('lightweight-charts');
type IndSeries = { key: string; style: 'LINE' | 'HISTOGRAM'; series: ISeriesApi<'Line' | 'Histogram'> };

const TZ = 'Africa/Tunis';
const UP = '#4ade80';
const DOWN = '#f87171';
const fDate = new Intl.DateTimeFormat('fr-TN', { timeZone: TZ, day: '2-digit', month: '2-digit', year: 'numeric' });
const fDayMonth = new Intl.DateTimeFormat('fr-TN', { timeZone: TZ, day: '2-digit', month: '2-digit' });
const fDateTime = new Intl.DateTimeFormat('fr-TN', { timeZone: TZ, day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false });
const fTime = new Intl.DateTimeFormat('fr-TN', { timeZone: TZ, hour: '2-digit', minute: '2-digit', hour12: false });
const fMonth = new Intl.DateTimeFormat('fr-TN', { timeZone: TZ, month: 'short' });
const fYear = new Intl.DateTimeFormat('fr-TN', { timeZone: TZ, year: 'numeric' });
const dUTC = new Intl.DateTimeFormat('fr-TN', { timeZone: 'UTC', day: '2-digit', month: '2-digit', year: 'numeric' });

/** Epoch SECONDS (never shifted) → Africa/Tunis wall time. Date strings / business days are plain dates. */
function toDate(t: Time): { date: Date; plain: boolean } {
  if (typeof t === 'number') return { date: new Date(t * 1000), plain: false };
  if (typeof t === 'string') return { date: new Date(t + 'T12:00:00Z'), plain: true };
  return { date: new Date(Date.UTC(t.year, t.month - 1, t.day, 12)), plain: true };
}

/**
 * The ONLY file that imports `lightweight-charts` (v5 API, loaded with a dynamic import so it stays out of the main bundle).
 * Plain data goes in through inputs. Used for candles + volume + indicators, and for line/area series (equity curve, mini charts).
 */
@Component({
  selector: 'app-candle-chart',
  templateUrl: './candle-chart.component.html',
  styleUrls: ['./candle-chart.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CandleChartComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() mode: 'candles' | 'area' = 'candles';
  @Input() candles: Candle[] | null = [];
  @Input() area: AreaPoint[] | null = [];
  @Input() baseline: number | null = null;
  @Input() interval: CandleInterval = 'D1';
  @Input() indicators: ChartIndicator[] = [];
  @Input() showVolume = true;
  @Input() showLegend = true;
  @Input() showTrades = true;
  @Input() trades: ChartTrade[] = [];
  @Input() title = '';
  @Input() height = 560;
  /** Increment to fit the content (ticker / timeframe / range change). Live reloads keep the user's zoom. */
  @Input() fitToken = 0;

  @ViewChild('box', { static: true }) box!: ElementRef<HTMLElement>;
  @ViewChild('surface', { static: true }) surface!: ElementRef<HTMLElement>;

  legend: Legend = { title: '', ohlc: null, value: null, inds: [], trades: [] };
  ready = false;
  failed = false;

  private lib?: Lib;
  private chart?: IChartApi;
  private candleSeries?: ISeriesApi<'Candlestick'>;
  private volSeries?: ISeriesApi<'Histogram'>;
  private areaSeries?: ISeriesApi<'Area'>;
  private markersApi?: ISeriesMarkersPluginApi<Time>;
  private ind = new Map<string, IndSeries[]>();
  private indRefs = new Map<string, { result: IndicatorResult | null; color: string }>();
  private paneOf = new Map<string, number>();
  private index = new Map<number, number>();
  private destroyed = false;
  private lastFit = 0;
  private resizeObs?: ResizeObserver;

  constructor(private zone: NgZone, private cdr: ChangeDetectorRef) {}

  async ngAfterViewInit(): Promise<void> {
    try {
      this.lib = await import('lightweight-charts');
    } catch (e) {
      console.error('[candle-chart] lightweight-charts failed to load', e);
      this.failed = true;
      this.cdr.markForCheck();
      return;
    }
    if (this.destroyed) return;
    this.zone.runOutsideAngular(() => this.createChart());
    this.ready = true;
    this.applyAll(true);
    this.cdr.markForCheck();
  }

  ngOnChanges(c: SimpleChanges): void {
    if (!this.chart) return;
    if (c['mode'] && !c['mode'].firstChange) { this.zone.runOutsideAngular(() => this.recreate()); return; }
    if (c['interval']) this.chart.timeScale().applyOptions({ timeVisible: this.intraday });
    if (c['candles'] || c['area'] || c['baseline']) this.applyData();
    if (c['indicators']) this.applyIndicators();
    if (c['showVolume']) this.volSeries?.applyOptions({ visible: this.showVolume });
    if (c['trades'] || c['showTrades'] || c['candles']) this.applyMarkers();
    if (c['height']) this.layoutPanes();
    if (c['fitToken'] && this.fitToken !== this.lastFit) { this.lastFit = this.fitToken; this.chart.timeScale().fitContent(); }
    this.refreshLegend(undefined);
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    this.resizeObs?.disconnect();
    this.chart?.remove();
    this.chart = undefined;
  }

  // ---------------------------------------------------------------- public actions (used by the chart page)
  resetView(): void { this.chart?.timeScale().fitContent(); }

  fullscreen(): void {
    const el = this.box.nativeElement;
    if (document.fullscreenElement) void document.exitFullscreen();
    else void el.requestFullscreen?.();
  }

  screenshot(filename = 'chart.png'): void {
    if (!this.chart) return;
    const canvas = this.chart.takeScreenshot();
    canvas.toBlob(blob => {
      if (!blob) return;
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      a.click();
      URL.revokeObjectURL(url);
    });
  }

  // ---------------------------------------------------------------- build
  private get intraday(): boolean { return this.interval !== 'D1' && this.interval !== 'W1' && this.mode === 'candles'; }

  private cssVar(name: string, fallback: string): string {
    const v = getComputedStyle(this.box.nativeElement).getPropertyValue(name).trim();
    return v || fallback;
  }

  private createChart(): void {
    const lib = this.lib as Lib;
    const grid = this.cssVar('--chart-grid', 'rgba(255,255,255,0.05)');
    const border = 'rgba(255,255,255,0.1)';
    const cross = { color: 'rgba(220,224,230,0.45)', style: lib.LineStyle.Dashed, labelBackgroundColor: '#22252a' };
    this.chart = lib.createChart(this.surface.nativeElement, {
      autoSize: true,
      layout: {
        background: { type: lib.ColorType.Solid, color: 'transparent' },
        textColor: 'rgba(255,255,255,0.55)',
        fontFamily: 'Inter, sans-serif',
        panes: { separatorColor: border, separatorHoverColor: 'rgba(255,255,255,0.25)' }
      },
      grid: { vertLines: { color: grid }, horzLines: { color: grid } },
      crosshair: { mode: lib.CrosshairMode.Normal, vertLine: cross, horzLine: cross },
      rightPriceScale: { borderColor: border },
      timeScale: {
        borderColor: border, timeVisible: this.intraday, secondsVisible: false, rightOffset: 4,
        tickMarkFormatter: (t: Time, type: number) => this.tick(t, type)
      },
      localization: { timeFormatter: (t: Time) => this.crosshairTime(t) }
    });

    if (this.mode === 'candles') {
      this.candleSeries = this.chart.addSeries(lib.CandlestickSeries, {
        upColor: UP, downColor: DOWN, borderVisible: false, wickUpColor: UP, wickDownColor: DOWN
      });
      this.volSeries = this.chart.addSeries(lib.HistogramSeries, { priceScaleId: 'vol', priceFormat: { type: 'volume' }, priceLineVisible: false, lastValueVisible: false, visible: this.showVolume });
      this.chart.priceScale('vol').applyOptions({ scaleMargins: { top: 0.8, bottom: 0 } }); // volume uses the bottom 20 % of pane 0
      this.markersApi = lib.createSeriesMarkers(this.candleSeries, []);
    } else {
      this.areaSeries = this.chart.addSeries(lib.AreaSeries, {
        lineColor: '#f4f6f8', topColor: 'rgba(244,246,248,0.25)', bottomColor: 'rgba(244,246,248,0)', lineWidth: 2, priceLineVisible: false
      });
    }
    this.chart.subscribeCrosshairMove(p => this.onCrosshair(p));
    // Keep panes proportioned when the container resizes (fullscreen, window).
    this.resizeObs = new ResizeObserver(() => this.layoutPanes());
    this.resizeObs.observe(this.surface.nativeElement);
  }

  private recreate(): void {
    this.resizeObs?.disconnect();
    this.chart?.remove();
    this.ind.clear();
    this.indRefs.clear();
    this.candleSeries = this.volSeries = this.areaSeries = undefined;
    this.markersApi = undefined;
    this.createChart();
    this.applyAll(true);
  }

  private applyAll(fit: boolean): void {
    this.applyData();
    this.applyIndicators();
    this.applyMarkers();
    if (fit) { this.lastFit = this.fitToken; this.chart?.timeScale().fitContent(); }
    this.refreshLegend(undefined);
  }

  private applyData(): void {
    if (!this.chart) return;
    if (this.mode === 'candles') {
      const cs = this.candles ?? [];
      this.index = new Map(cs.map((c, i) => [c.time, i]));
      this.candleSeries?.setData(cs.map(c => ({ time: c.time as UTCTimestamp, open: c.open, high: c.high, low: c.low, close: c.close })));
      this.volSeries?.setData(cs.map(c => ({ time: c.time as UTCTimestamp, value: c.volume, color: c.close >= c.open ? 'rgba(74,222,128,0.35)' : 'rgba(248,113,113,0.35)' })));
    } else {
      this.areaSeries?.setData((this.area ?? []).map(p => ({ time: p.time as Time, value: p.value })));
      this.baselineLine();
    }
  }

  private baselineLine(): void {
    const lib = this.lib as Lib;
    const s = this.areaSeries as (ISeriesApi<'Area'> & { __base?: ReturnType<ISeriesApi<'Area'>['createPriceLine']> }) | undefined;
    if (!s) return;
    if (s.__base) { s.removePriceLine(s.__base); s.__base = undefined; }
    if (this.baseline !== null && Number.isFinite(this.baseline)) {
      s.__base = s.createPriceLine({ price: this.baseline, color: 'rgba(255,255,255,0.35)', lineStyle: lib.LineStyle.Dashed, lineWidth: 1, axisLabelVisible: false });
    }
  }

  // ---------------------------------------------------------------- indicators
  private applyIndicators(): void {
    if (!this.chart || this.mode !== 'candles') return;
    const list = this.indicators ?? [];
    // Visibility only → no rebuild (hiding never refetches or re-adds series).
    const structureSame = list.length === [...this.indRefs.keys()].length
      && list.every(i => { const r = this.indRefs.get(i.id); return !!r && r.result === i.result && r.color === i.color; });
    if (structureSame && list.every(i => i.result ? this.ind.has(i.id) : true)) { this.applyVisibility(); return; }

    for (const arr of this.ind.values()) for (const s of arr) this.chart.removeSeries(s.series);
    this.ind.clear();
    this.indRefs.clear();
    this.paneOf.clear();
    const lib = this.lib as Lib;
    let nextPane = 1;
    for (const inst of list) {
      this.indRefs.set(inst.id, { result: inst.result, color: inst.color });
      if (!inst.result) continue;
      const pane = inst.def.placement === 'PANE' ? nextPane++ : 0;
      this.paneOf.set(inst.id, pane);
      const lines = inst.def.outputs.filter(o => o.style === 'LINE').length;
      const built: IndSeries[] = [];
      let lineIdx = 0;
      for (const out of inst.def.outputs) {
        const pts = (inst.result.series[out.key] ?? []).filter(p => p.value !== null);
        if (out.style === 'HISTOGRAM') {
          const s = this.chart.addSeries(lib.HistogramSeries, { priceLineVisible: false, lastValueVisible: false, visible: inst.visible }, pane);
          s.setData(pts.map(p => ({ time: p.time as UTCTimestamp, value: p.value as number, color: (p.value as number) >= 0 ? 'rgba(74,222,128,0.7)' : 'rgba(248,113,113,0.7)' })));
          built.push({ key: out.key, style: 'HISTOGRAM', series: s });
        } else {
          const s = this.chart.addSeries(lib.LineSeries, {
            color: lineIdx === 0 ? inst.color : this.tint(inst.color, 0.62), lineWidth: lines > 1 ? 1 : 2,
            priceLineVisible: false, lastValueVisible: false, crosshairMarkerVisible: false, visible: inst.visible
          }, pane);
          s.setData(pts.map(p => ({ time: p.time as UTCTimestamp, value: p.value as number })));
          built.push({ key: out.key, style: 'LINE', series: s });
          lineIdx++;
        }
      }
      // Reference lines (RSI 30/70, MACD 0 …) come from the catalog, on the pane's first series.
      const first = built[0]?.series;
      if (first) for (const price of inst.def.referenceLines ?? []) {
        first.createPriceLine({ price, color: '#999999', lineStyle: lib.LineStyle.Dashed, lineWidth: 1, axisLabelVisible: true, title: '' });
      }
      this.ind.set(inst.id, built);
    }
    this.layoutPanes();
  }

  private applyVisibility(): void {
    for (const inst of this.indicators ?? []) {
      for (const s of this.ind.get(inst.id) ?? []) s.series.applyOptions({ visible: inst.visible });
    }
    this.layoutPanes();
  }

  /** Pane 0 gets ~65 % of the height when indicator panes are shown; hidden panes collapse. */
  private layoutPanes(): void {
    const chart = this.chart;
    if (!chart) return;
    const panes = chart.panes();
    if (panes.length <= 1) return;
    const total = this.surface.nativeElement.clientHeight || this.height;
    const paneVisible = new Map<number, boolean>();
    for (const inst of this.indicators ?? []) {
      const p = this.paneOf.get(inst.id);
      if (p !== undefined && p > 0 && inst.visible) paneVisible.set(p, true);
    }
    const visibleCount = paneVisible.size;
    const main = visibleCount ? Math.round(total * 0.65) : total;
    const each = visibleCount ? Math.floor((total - main) / visibleCount) : 0;
    panes.forEach((pane, i) => {
      if (i === 0) pane.setHeight(Math.max(120, main));
      else pane.setHeight(paneVisible.get(i) ? Math.max(70, each) : 1);
    });
  }

  private tint(hex: string, alpha: number): string {
    const m = /^#([0-9a-f]{6})$/i.exec(hex);
    if (!m) return hex;
    const n = parseInt(m[1], 16);
    return `rgba(${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}, ${alpha})`;
  }

  // ---------------------------------------------------------------- trade markers
  private applyMarkers(): void {
    if (!this.markersApi || this.mode !== 'candles') return;
    const cs = this.candles ?? [];
    if (!this.showTrades || !cs.length) { this.markersApi.setMarkers([]); return; }
    const markers: SeriesMarker<Time>[] = [];
    for (const t of this.trades ?? []) {
      const ct = this.snap(cs, t.time);
      if (ct === null) continue;
      markers.push({
        time: ct as UTCTimestamp,
        position: t.side === 'BUY' ? 'belowBar' : 'aboveBar',
        shape: t.side === 'BUY' ? 'arrowUp' : 'arrowDown',
        color: t.side === 'BUY' ? UP : DOWN,
        text: ''
      });
    }
    markers.sort((a, b) => (a.time as number) - (b.time as number));
    this.markersApi.setMarkers(markers);
  }

  /** The candle a trade belongs to: the last candle starting at or before it (null when before the first). */
  private snap(cs: Candle[], time: number): number | null {
    if (time < cs[0].time) return null;
    let lo = 0, hi = cs.length - 1;
    while (lo < hi) { const mid = (lo + hi + 1) >> 1; if (cs[mid].time <= time) lo = mid; else hi = mid - 1; }
    return cs[lo].time;
  }

  // ---------------------------------------------------------------- legend
  private onCrosshair(p: MouseEventParams<Time>): void {
    this.refreshLegend(p, true);
  }

  private refreshLegend(p: MouseEventParams<Time> | undefined, immediate = false): void {
    if (this.destroyed) return;
    const lg: Legend = { title: this.title, ohlc: null, value: null, inds: [], trades: [] };
    const hovered = p && p.time !== undefined ? p.time : undefined;
    if (this.mode === 'candles') {
      const cs = this.candles ?? [];
      let idx = cs.length - 1;
      if (typeof hovered === 'number') { const i = this.index.get(hovered); if (i !== undefined) idx = i; }
      const c = cs[idx];
      if (c) {
        const prev = cs[idx - 1];
        const chg = prev && prev.close ? ((c.close - prev.close) / prev.close) * 100 : null;
        lg.ohlc = {
          o: fmtNumber(c.open), h: fmtNumber(c.high), l: fmtNumber(c.low), c: fmtNumber(c.close),
          chg: chg === null ? '' : fmtPct(chg), chgClass: chg === null ? '' : chg >= 0 ? 'pos' : 'neg', vol: fmtQty(c.volume)
        };
        const time = c.time;
        for (const inst of this.indicators ?? []) {
          if (!inst.visible || !inst.result) continue;
          const vals = inst.def.outputs.map(o => {
            const pt = (inst.result as IndicatorResult).series[o.key]?.find(x => x.time === time);
            return pt && pt.value !== null ? fmtNumber(pt.value, 3) : '—';
          });
          lg.inds.push({ label: inst.label, color: inst.color, value: vals.join('  ') });
        }
        if (this.showTrades) {
          const next = cs[idx + 1]?.time ?? Number.POSITIVE_INFINITY;
          for (const t of this.trades ?? []) if (t.time >= time && t.time < next) lg.trades.push(`${t.side} ${fmtQty(t.quantity)} × ${fmtNumber(t.price)}`);
        }
      }
    } else if (this.areaSeries) {
      const data = p?.seriesData.get(this.areaSeries) as { value?: number } | undefined;
      const last = (this.area ?? [])[(this.area ?? []).length - 1];
      const v = data?.value ?? last?.value;
      lg.value = v === undefined ? null : fmtNumber(v);
    }
    this.legend = lg;
    if (immediate) this.cdr.detectChanges(); else this.cdr.markForCheck();
  }

  // ---------------------------------------------------------------- Tunis formatters (timestamps are never shifted)
  private crosshairTime(t: Time): string {
    const { date, plain } = toDate(t);
    if (plain) return dUTC.format(date);
    return this.intraday ? fDateTime.format(date) : fDate.format(date);
  }

  private tick(t: Time, type: number): string {
    const { date, plain } = toDate(t);
    const f = (tz: Intl.DateTimeFormat, utc: Intl.DateTimeFormat) => (plain ? utc : tz).format(date);
    switch (type) {
      case 0: return plain ? String(date.getUTCFullYear()) : fYear.format(date);
      case 1: return plain ? new Intl.DateTimeFormat('fr-TN', { timeZone: 'UTC', month: 'short' }).format(date) : fMonth.format(date);
      case 2: return f(fDayMonth, new Intl.DateTimeFormat('fr-TN', { timeZone: 'UTC', day: '2-digit', month: '2-digit' }));
      default: return plain ? '' : fTime.format(date);
    }
  }
}
