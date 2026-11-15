import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

/** Loading placeholder: `rows` bars (or a single block with `height`). */
@Component({
  selector: 'tx-skeleton',
  templateUrl: './tx-skeleton.component.html',
  styleUrls: ['./tx-skeleton.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TxSkeletonComponent {
  @Input() rows = 3;
  @Input() height = 16;
  get list(): number[] { return Array.from({ length: Math.max(1, this.rows) }, (_, i) => i); }
}
