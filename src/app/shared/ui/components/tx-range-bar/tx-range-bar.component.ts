import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, Input, NgZone } from '@angular/core';
import { fmtNumber, isNum } from '../../util/format';
import { ResizeAware } from '../svg/resize-aware';

/** Day range / limit band: a track from `min` to `max` with a marker at `value`. */
@Component({
  selector: 'tx-range-bar',
  templateUrl: './tx-range-bar.component.html',
  styleUrls: ['./tx-range-bar.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TxRangeBarComponent extends ResizeAware {
  @Input() min: number | null | undefined;
  @Input() max: number | null | undefined;
  @Input() value: number | null | undefined;
  @Input() label = '';
  readonly pad = 6;

  constructor(host: ElementRef<HTMLElement>, zone: NgZone, cdr: ChangeDetectorRef) { super(host, zone, cdr); }

  get valid(): boolean { return isNum(this.min) && isNum(this.max) && isNum(this.value) && this.max >= this.min; }
  get ratio(): number {
    if (!this.valid) return 0;
    const span = (this.max as number) - (this.min as number);
    return span === 0 ? 0.5 : Math.max(0, Math.min(1, ((this.value as number) - (this.min as number)) / span));
  }
  get x(): number { return this.pad + this.ratio * (this.width - this.pad * 2); }
  fmt(v: number | null | undefined): string { return fmtNumber(v, 3); }
  get aria(): string { return `${this.label || 'Range'}: ${this.fmt(this.min)} to ${this.fmt(this.max)}, current ${this.fmt(this.value)}`; }
}
