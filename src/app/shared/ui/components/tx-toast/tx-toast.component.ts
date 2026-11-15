import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ToastService } from '../../util/toast.service';

/** Bottom-right toast stack. Rendered by tx-module-header (no change to app.component.html). */
@Component({
  selector: 'tx-toast',
  templateUrl: './tx-toast.component.html',
  styleUrls: ['./tx-toast.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TxToastComponent {
  constructor(public toasts: ToastService) {}
  trackById(_: number, t: { id: number }): number { return t.id; }
}
