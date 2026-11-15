import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, Input, NgZone } from '@angular/core';
import { fmtMoney, fmtNumber, fmtPct } from '../../util/format';
import { ResizeAware } from '../svg/resize-aware';

export interface BarItem { label: string; value: number; }

/** Horizontal bars. With `diverging` the zero axis sits in the middle (positive right, negative left). */
@Component({
  selector: 'tx-bars',
  templateUrl: './tx-bars.component.html',
  styleUrls: ['./tx-bars.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TxBarsComponent extends ResizeAware {
  @Input() items: BarItem[] = [];
  @Input() diverging = false;
  @Input() format: 'num' | 'pct' | 'money' = 'num';
  readonly rowH = 26;
  readonly labelW = 86;
  readonly valueW = 78;

  constructor(host: ElementRef<HTMLElement>, zone: NgZone, cdr: ChangeDetectorRef) { super(host, zone, cdr); }

  get height(): number { return Math.max(1, this.items.length) * this.rowH + 4; }
  get areaW(): number { return Math.max(40, this.width - this.labelW - this.valueW); }
  get max(): number { return Math.max(1e-9, ...this.items.map(i => Math.abs(i.value))); }
  get zeroX(): number { return this.labelW + (this.diverging ? this.areaW / 2 : 0); }
  get label(): string { return this.items.length ? 'Bar chart: ' + this.items.map(i => `${i.label} ${this.fmt(i.value)}`).join(', ') : 'No data'; }

  fmt(v: number): string { return this.format === 'pct' ? fmtPct(v) : this.format === 'money' ? fmtMoney(v, false, true) : fmtNumber(v, 2); }

  bar(v: number): { x: number; w: number } {
    const half = this.diverging ? this.areaW / 2 : this.areaW;
    const w = Math.max(1, (Math.abs(v) / this.max) * (half - 2));
    return { x: v < 0 && this.diverging ? this.zeroX - w : this.zeroX, w };
  }
  trackById(i: number): number { return i; }
}
