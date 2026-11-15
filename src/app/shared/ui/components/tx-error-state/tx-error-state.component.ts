import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

/** Section error with a Retry button. */
@Component({
  selector: 'tx-error-state',
  templateUrl: './tx-error-state.component.html',
  styleUrls: ['./tx-error-state.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TxErrorStateComponent {
  @Input() message = 'Something went wrong.';
  @Input() retryable = true;
  @Output() retry = new EventEmitter<void>();
}
