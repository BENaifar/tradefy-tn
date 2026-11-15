import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, Input, NgZone } from '@angular/core';
import { fmtNumber } from '../../util/format';
import { ResizeAware } from '../svg/resize-aware';

const R = 48;
const C = 2 * Math.PI * R;

/** Ring gauge: `value` out of `max` (target sum, win rate). Turns warn-coloured above 100 %. */
@Component({
  selector: 'tx-gauge',
  templateUrl: './tx-gauge.component.html',
  styleUrls: ['./tx-gauge.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TxGaugeComponent extends ResizeAware {
  @Input() value = 0;
  @Input() max = 100;
  @Input() label = '';
  @Input() display = '';
  readonly r = R;
  readonly c = C;

  constructor(host: ElementRef<HTMLElement>, zone: NgZone, cdr: ChangeDetectorRef) { super(host, zone, cdr); }

  get size(): number { return Math.max(90, Math.min(this.width, 140)); }
  get ratio(): number { return this.max > 0 ? Math.max(0, Math.min(1, this.value / this.max)) : 0; }
  get over(): boolean { return this.value > this.max; }
  get dash(): string { return `${this.ratio * C} ${C}`; }
  get shown(): string { return this.display || `${fmtNumber(this.value, 0)} %`; }
  get aria(): string { return `${this.label || 'Gauge'}: ${this.shown}`; }
}
