import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

/** `panel-kicker` + title + actions slot + body. */
@Component({
  selector: 'tx-panel',
  templateUrl: './tx-panel.component.html',
  styleUrls: ['./tx-panel.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TxPanelComponent {
  @Input() kicker = '';
  @Input() title = '';
}
