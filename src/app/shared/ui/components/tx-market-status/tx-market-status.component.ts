import { Component } from '@angular/core';
import { MarketStatusResponse } from '../../../../features/market-data/models/market-data.model';
import { MarketStatusStore } from '../../../../features/market-data/services/market-status.store';

const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

/** `● BVMT OPEN · closes 14:10` / `BVMT CLOSED · opens Mon 09:00`, plus a visible SIMULATED tag. */
@Component({
  selector: 'tx-market-status',
  templateUrl: './tx-market-status.component.html',
  styleUrls: ['./tx-market-status.component.scss']
})
export class TxMarketStatusComponent {
  constructor(public store: MarketStatusStore) {}

  hhmm(t: string | null | undefined): string { return (t ?? '').slice(0, 5); }

  /** "today 09:00", "Mon 09:00" … computed in Africa/Tunis (public holidays are not known to the client). */
  nextOpen(s: MarketStatusResponse): string {
    const parts = new Intl.DateTimeFormat('en-GB', { timeZone: s.zone || 'Africa/Tunis', weekday: 'short', hour: '2-digit', minute: '2-digit', hour12: false }).formatToParts(new Date());
    const wd = parts.find(p => p.type === 'weekday')?.value ?? 'Mon';
    const hm = `${parts.find(p => p.type === 'hour')?.value}:${parts.find(p => p.type === 'minute')?.value}`;
    const open = this.hhmm(s.sessionOpen);
    const i = DAYS.indexOf(wd);
    if (i >= 0 && i <= 4 && hm < open) return `today ${open}`;
    const next = i >= 4 ? 'Mon' : DAYS[i + 1];
    return `${next} ${open}`;
  }
}
