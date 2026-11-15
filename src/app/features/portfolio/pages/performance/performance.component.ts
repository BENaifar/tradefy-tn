import { Component } from '@angular/core';
import { PORTFOLIO_TABS } from '../../../../shared/ui/util/module-tabs';
import { PerformanceResponse } from '../../models/portfolio.model';

interface Col { x: number; h: number; neg: boolean; label: string; }

/** Performance: the equity curve (range + value/return toggle live in the curve) and the backend's statistics. */
@Component({
  selector: 'app-performance',
  templateUrl: './performance.component.html',
  styleUrls: ['./performance.component.scss']
})
export class PerformanceComponent {
  readonly tabs = PORTFOLIO_TABS;
  perf: PerformanceResponse | null = null;
  selected: string | null = null;

  /** Daily returns derived from consecutive curve points (display only; the backend owns every other number). */
  cols: Col[] = [];
  readonly colH = 70;

  onLoaded(p: PerformanceResponse): void {
    this.perf = p;
    const pts = p.points ?? [];
    const rets: { v: number; label: string }[] = [];
    for (let i = 1; i < pts.length; i++) {
      if (pts[i - 1].totalValue) rets.push({ v: (pts[i].totalValue / pts[i - 1].totalValue - 1) * 100, label: `${pts[i].date.slice(8, 10)}/${pts[i].date.slice(5, 7)} ${(pts[i].totalValue / pts[i - 1].totalValue - 1) >= 0 ? '+' : '−'}${Math.abs((pts[i].totalValue / pts[i - 1].totalValue - 1) * 100).toFixed(2)} %` });
    }
    const last = rets.slice(-60);
    const max = Math.max(0.0001, ...last.map(r => Math.abs(r.v)));
    this.cols = last.map((r, i) => ({ x: i * 10, h: Math.max(1, (Math.abs(r.v) / max) * (this.colH / 2 - 2)), neg: r.v < 0, label: r.label }));
  }

  get sparse(): boolean { return !!this.perf && (this.perf.points?.length ?? 0) < 3; }
  get width(): number { return Math.max(10, this.cols.length * 10); }
}
