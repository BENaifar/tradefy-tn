import { Injectable } from '@angular/core';
import {
  BehaviorSubject, EMPTY, Observable, Subscription, catchError, defer, exhaustMap, forkJoin, ignoreElements, merge, share, tap, throttleTime
} from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { QuoteStore } from '../../market-data/services/quote.store';
import { BuyingPower, PortfolioApiError, PortfolioSummaryResponse, PositionResponse } from '../models/portfolio.model';
import { PortfolioService } from './portfolio.service';
import { PositionService } from './position.service';

export interface PortfolioVm {
  summary: PortfolioSummaryResponse | null;
  buyingPower: BuyingPower | null;
  positions: PositionResponse[] | null;
  loading: boolean;
  error: PortfolioApiError | null;
}

const EMPTY_VM: PortfolioVm = { summary: null, buyingPower: null, positions: null, loading: false, error: null };

/**
 * Summary, buying power and open positions in one place. Refreshed after any trade or cash action, and at most
 * every 30 s while live quotes arrive. The backend computes every value; the frontend never recomputes P&L.
 */
@Injectable({ providedIn: 'root' })
export class PortfolioStore {
  private readonly subject = new BehaviorSubject<PortfolioVm>(EMPTY_VM);

  /** While somebody watches `vm$`, live quote messages trigger a throttled silent refresh (≤ 1 per 30 s). */
  private readonly auto$: Observable<never> = defer(() =>
    this.quotes.all$.pipe(
      throttleTime(30000, undefined, { leading: false, trailing: true }),
      exhaustMap(() => this.fetch(true))
    )
  ).pipe(ignoreElements(), share()) as Observable<never>;

  readonly vm$: Observable<PortfolioVm> = merge(this.auto$, this.subject.asObservable());

  constructor(
    private portfolio: PortfolioService,
    private positions: PositionService,
    private quotes: QuoteStore,
    auth: AuthService
  ) {
    // The next user on the same browser must never see the previous portfolio.
    auth.currentUser$.subscribe(u => { if (!u) this.subject.next(EMPTY_VM); });
  }

  get value(): PortfolioVm { return this.subject.value; }

  /** Loads once if nothing is loaded yet. */
  ensureLoaded(): void { if (!this.subject.value.summary && !this.subject.value.loading) this.refresh(); }

  /** Reloads summary + buying power + open positions. Call after any trade or cash operation. */
  refresh(): Subscription { return this.fetch(false).subscribe(); }

  private fetch(silent: boolean): Observable<unknown> {
    if (!silent) this.subject.next({ ...this.subject.value, loading: true, error: null });
    return forkJoin({
      summary: this.portfolio.summary(),
      buyingPower: this.portfolio.buyingPower(),
      positions: this.positions.list('OPEN')
    }).pipe(
      tap(r => this.subject.next({ ...r, loading: false, error: null })),
      catchError((e: PortfolioApiError) => {
        this.subject.next({ ...this.subject.value, loading: false, error: silent ? this.subject.value.error : e });
        return EMPTY;
      })
    );
  }
}
