import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

/** Empty section state: a title, a sentence, and an optional action slot. */
@Component({
  selector: 'tx-empty-state',
  templateUrl: './tx-empty-state.component.html',
  styleUrls: ['./tx-empty-state.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TxEmptyStateComponent {
  @Input() title = 'Nothing here yet';
  @Input() message = '';
}
