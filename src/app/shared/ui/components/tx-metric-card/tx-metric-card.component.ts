import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

/** Kicker, big value, sub-line, optional delta and sparkline. Same look as the workspace `metric-card`. */
@Component({
  selector: 'tx-metric-card',
  templateUrl: './tx-metric-card.component.html',
  styleUrls: ['./tx-metric-card.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TxMetricCardComponent {
  @Input() kicker = '';
  @Input() value: string | null = null;
  @Input() sub = '';
  @Input() delta: number | null | undefined;
  @Input() deltaKind: 'pct' | 'money' = 'pct';
  @Input() sparkline: number[] | null = null;
  @Input() hint = '';
  @Input() valueClass = '';
}
