import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { fmtMoney, fmtPct, isNum } from '../../util/format';

/** `▲ +1.23 %` / `▼ −45.000 TND`: coloured by sign, with an arrow so colour is never the only signal. */
@Component({
  selector: 'tx-delta',
  templateUrl: './tx-delta.component.html',
  styleUrls: ['./tx-delta.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TxDeltaComponent {
  @Input() value: number | null | undefined;
  @Input() kind: 'pct' | 'money' = 'pct';
  @Input() decimals = 2;

  get text(): string {
    return this.kind === 'money' ? fmtMoney(this.value, true, true) : fmtPct(this.value, true, this.decimals);
  }
  get arrow(): string {
    if (!isNum(this.value) || this.value === 0) return '•';
    return this.value > 0 ? '▲' : '▼';
  }
  get cls(): string {
    if (!isNum(this.value) || this.value === 0) return 'flat';
    return this.value > 0 ? 'pos' : 'neg';
  }
}
