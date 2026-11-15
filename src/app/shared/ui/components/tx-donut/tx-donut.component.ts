import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, Input, NgZone } from '@angular/core';
import { fmtPct } from '../../util/format';
import { ResizeAware } from '../svg/resize-aware';

export interface DonutSlice { label: string; value: number; pct: number; color?: string; hatched?: boolean; }

const R = 70;
const C = 2 * Math.PI * R;
const GAP = 2; // 2 px gaps
const PALETTE = ['var(--series-1)', 'var(--series-2)', 'var(--series-3)', 'var(--series-4)', 'var(--series-5)', 'var(--series-6)'];

/** Donut with legend; hovering a slice or a legend row highlights both. Silver ramp first. */
@Component({
  selector: 'tx-donut',
  templateUrl: './tx-donut.component.html',
  styleUrls: ['./tx-donut.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TxDonutComponent extends ResizeAware {
  @Input() slices: DonutSlice[] = [];
  @Input() centerLabel = '';
  @Input() centerValue = '';
  hover = -1;

  constructor(host: ElementRef<HTMLElement>, zone: NgZone, cdr: ChangeDetectorRef) { super(host, zone, cdr); }

  get size(): number { return Math.max(120, Math.min(this.width, 220)); }

  color(i: number): string { return this.slices[i]?.color ?? PALETTE[i % PALETTE.length]; }

  arcs(): { dash: string; offset: number }[] {
    const total = this.slices.reduce((a, s) => a + Math.max(0, s.pct), 0) || 1;
    let start = 0;
    return this.slices.map(s => {
      const len = (Math.max(0, s.pct) / total) * C;
      const arc = { dash: `${Math.max(0, len - GAP)} ${C - Math.max(0, len - GAP)}`, offset: -start };
      start += len;
      return arc;
    });
  }

  get label(): string {
    return this.slices.length
      ? 'Allocation: ' + this.slices.map(s => `${s.label} ${fmtPct(s.pct, false)}`).join(', ')
      : 'No allocation data';
  }
  pct(v: number): string { return fmtPct(v, false); }
  get shownLabel(): string { return this.hover >= 0 ? this.slices[this.hover].label : this.centerLabel; }
  get shownValue(): string { return this.hover >= 0 ? this.pct(this.slices[this.hover].pct) : this.centerValue; }
  set(i: number): void { this.hover = i; this.cdr.markForCheck(); }
  readonly r = R;
  readonly c = C;
}
