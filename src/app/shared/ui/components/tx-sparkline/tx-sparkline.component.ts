import { ChangeDetectionStrategy, Component, ElementRef, Input, OnDestroy, AfterViewInit, NgZone, ChangeDetectorRef } from '@angular/core';
import { fmtNumber } from '../../util/format';

/** Polyline sparkline, same style as the existing home sparklines. Inputs only. */
@Component({
  selector: 'tx-sparkline',
  templateUrl: './tx-sparkline.component.html',
  styleUrls: ['./tx-sparkline.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TxSparklineComponent implements AfterViewInit, OnDestroy {
  @Input() values: number[] | null | undefined = [];
  @Input() height = 30;
  width = 100;
  private ro?: ResizeObserver;

  constructor(private host: ElementRef<HTMLElement>, private zone: NgZone, private cdr: ChangeDetectorRef) {}

  ngAfterViewInit(): void {
    if (typeof ResizeObserver === 'undefined') return;
    this.ro = new ResizeObserver(e => this.zone.run(() => {
      const w = Math.round(e[0].contentRect.width);
      if (w > 0 && w !== this.width) { this.width = w; this.cdr.markForCheck(); }
    }));
    this.ro.observe(this.host.nativeElement);
  }
  ngOnDestroy(): void { this.ro?.disconnect(); }

  get points(): string {
    const v = (this.values ?? []).filter(x => Number.isFinite(x));
    if (v.length < 2) return '';
    const min = Math.min(...v), max = Math.max(...v), span = max - min || 1, pad = 2;
    return v.map((y, i) => `${((i / (v.length - 1)) * this.width).toFixed(1)},${(this.height - pad - ((y - min) / span) * (this.height - pad * 2)).toFixed(1)}`).join(' ');
  }
  get label(): string {
    const v = (this.values ?? []).filter(x => Number.isFinite(x));
    return v.length < 2 ? 'No trend data' : `Trend from ${fmtNumber(v[0])} to ${fmtNumber(v[v.length - 1])}`;
  }
}
