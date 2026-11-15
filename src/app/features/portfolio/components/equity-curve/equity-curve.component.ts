import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { AreaPoint } from '../../../market-data/components/candle-chart/candle-chart.component';
import { SegmentOption } from '../../../../shared/ui/components/tx-segmented/tx-segmented.component';
import { lsGet, lsSet } from '../../../../shared/ui/util/safe-storage';
import { Section } from '../../../../shared/ui/util/section';
import { ToastService } from '../../../../shared/ui/util/toast.service';
import { PerformanceRange, PerformanceResponse } from '../../models/portfolio.model';
import { PerformanceService } from '../../services/performance.service';

const RANGES: PerformanceRange[] = ['W1', 'M1', 'M3', 'M6', 'YTD', 'Y1', 'ALL'];
const LABELS: Record<PerformanceRange, string> = { W1: '1W', M1: '1M', M3: '3M', M6: '6M', YTD: 'YTD', Y1: '1Y', ALL: 'ALL' };

/**
 * Equity curve (area chart, route 33): range buttons, Value / Return % toggle, dashed baseline.
 * Used by the portfolio overview and the performance page. The backend computes every number.
 */
@Component({
  selector: 'app-equity-curve',
  templateUrl: './equity-curve.component.html',
  styleUrls: ['./equity-curve.component.scss']
})
export class EquityCurveComponent implements OnInit, OnDestroy {
  @Input() height = 300;
  @Output() loaded = new EventEmitter<PerformanceResponse>();

  readonly rangeOptions: SegmentOption[] = RANGES.map(r => ({ value: r, label: LABELS[r] }));
  readonly modeOptions: SegmentOption[] = [{ value: 'value', label: 'Value' }, { value: 'return', label: 'Return %' }];
  range: PerformanceRange = 'M1';
  mode: 'value' | 'return' = 'value';
  perf: Section<PerformanceResponse>;
  area: AreaPoint[] = [];
  baseline: number | null = null;
  fitToken = 0;

  constructor(private api: PerformanceService, toast: ToastService) {
    this.perf = new Section<PerformanceResponse>(toast, d => { this.build(d); this.loaded.emit(d); });
  }

  ngOnInit(): void {
    const saved = lsGet<PerformanceRange>('portfolio_range', 'M1');
    this.range = RANGES.includes(saved) ? saved : 'M1';
    this.mode = lsGet<string>('portfolio_curve_mode', 'value') === 'return' ? 'return' : 'value';
    this.load();
  }
  ngOnDestroy(): void { this.perf.destroy(); }

  load(): void { this.perf.load(() => this.api.performance(this.range), true); }
  reload(): void { this.load(); }

  setRange(v: string): void { this.range = v as PerformanceRange; lsSet('portfolio_range', this.range); this.load(); }
  setMode(v: string): void {
    this.mode = v === 'return' ? 'return' : 'value';
    lsSet('portfolio_curve_mode', this.mode);
    if (this.perf.data) this.build(this.perf.data);
  }

  private build(d: PerformanceResponse): void {
    const ret = this.mode === 'return';
    this.area = (d.points ?? []).map(p => ({ time: p.date, value: ret ? p.cumulativeReturnPct : p.totalValue }));
    this.baseline = ret ? 0 : d.startValue;
    this.fitToken++;
  }
}
