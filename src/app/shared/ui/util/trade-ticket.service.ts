import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface TradePrefill { ticker?: string; side?: 'BUY' | 'SELL'; quantity?: number; }

/**
 * Open/close state of the trade ticket drawer. The drawer itself (<app-trade-ticket>) lives in the portfolio module;
 * the header Trade button, instrument page, chart, positions and rebalance only talk to this service.
 */
@Injectable({ providedIn: 'root' })
export class TradeTicketService {
  private readonly subject = new BehaviorSubject<TradePrefill | null>(null);
  readonly state$ = this.subject.asObservable();

  open(prefill: TradePrefill = {}): void { this.subject.next(prefill); }
  close(): void { this.subject.next(null); }
  get isOpen(): boolean { return this.subject.value !== null; }
}
