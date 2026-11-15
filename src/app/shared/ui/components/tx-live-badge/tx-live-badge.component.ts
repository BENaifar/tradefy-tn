import { Component } from '@angular/core';
import { MarketStatusStore } from '../../../../features/market-data/services/market-status.store';
import { QuoteStore } from '../../../../features/market-data/services/quote.store';
import { StompService } from '../../../../features/market-data/services/stomp.service';

/** LIVE / CONNECTING / RECONNECTING / OFFLINE, or MARKET CLOSED when the session is not running. */
@Component({
  selector: 'tx-live-badge',
  templateUrl: './tx-live-badge.component.html',
  styleUrls: ['./tx-live-badge.component.scss']
})
export class TxLiveBadgeComponent {
  readonly isLiveSession = MarketStatusStore.isLiveSession;
  constructor(public stomp: StompService, public status: MarketStatusStore, public quotes: QuoteStore) {}
  hhmm(t: string | null | undefined): string { return (t ?? '09:00').slice(0, 5); }
}
