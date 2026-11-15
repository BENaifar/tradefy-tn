import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

export interface SegmentOption { value: string; label: string; disabled?: boolean; title?: string; }

/** Button group (timeframes, ranges, BUY/SELL, group-by). Arrow keys move the selection. */
@Component({
  selector: 'tx-segmented',
  templateUrl: './tx-segmented.component.html',
  styleUrls: ['./tx-segmented.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TxSegmentedComponent {
  @Input() options: SegmentOption[] = [];
  @Input() value: string | null = null;
  @Input() label = 'Options';
  @Output() valueChange = new EventEmitter<string>();

  select(o: SegmentOption): void {
    if (o.disabled || o.value === this.value) return;
    this.valueChange.emit(o.value);
  }

  onKey(ev: KeyboardEvent, index: number): void {
    const dir = ev.key === 'ArrowRight' || ev.key === 'ArrowDown' ? 1 : ev.key === 'ArrowLeft' || ev.key === 'ArrowUp' ? -1 : 0;
    if (!dir) return;
    ev.preventDefault();
    const n = this.options.length;
    for (let step = 1; step <= n; step++) {
      const next = this.options[(index + dir * step + n * step) % n];
      if (next && !next.disabled) {
        this.valueChange.emit(next.value);
        const buttons = (ev.currentTarget as HTMLElement).parentElement?.querySelectorAll<HTMLButtonElement>('button');
        const idx = this.options.indexOf(next);
        setTimeout(() => buttons?.[idx]?.focus());
        return;
      }
    }
  }
}
