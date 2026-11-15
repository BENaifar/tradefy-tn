import { Pipe, PipeTransform } from '@angular/core';
import { fmtMoney, fmtPct, fmtQty, fmtTunis } from '../util/format';

/** `12,345.678 TND` (3 decimals). `null` renders —. */
@Pipe({ name: 'money' })
export class MoneyPipe implements PipeTransform {
  transform(v: number | null | undefined, unit = true, signed = false): string { return fmtMoney(v, unit, signed); }
}

/** `+1.23 %` */
@Pipe({ name: 'pct' })
export class PctPipe implements PipeTransform {
  transform(v: number | null | undefined, signed = true, decimals = 2): string { return fmtPct(v, signed, decimals); }
}

/** Whole quantity with thin-space thousands. */
@Pipe({ name: 'qty' })
export class QtyPipe implements PipeTransform {
  transform(v: number | null | undefined): string { return fmtQty(v); }
}

/** Africa/Tunis time. Accepts ISO instants, LocalDateTime, `YYYY-MM-DD` and epoch seconds. */
@Pipe({ name: 'tunisTime' })
export class TunisTimePipe implements PipeTransform {
  transform(v: string | number | null | undefined, mode: 'datetime' | 'time' | 'date' = 'datetime'): string { return fmtTunis(v, mode); }
}
