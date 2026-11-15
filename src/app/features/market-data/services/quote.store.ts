import { Injectable } from '@angular/core';
import {
  BehaviorSubject, EMPTY, Observable, catchError, combineLatest, defer, distinctUntilChanged, ignoreElements, interval, map, merge,
  share, startWith, switchMap, tap
} from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { QuoteResponse } from '../models/market-data.model';
import { MarketDataService } from './market-data.service';
import { MarketStatusStore } from './market-status.store';
import { StompService } from './stomp.service';

/**
 * Seeds from `GET /api/market/quotes`, merges WebSocket messages by ticker (newest `updatedAt` wins).
 * The connection is reference counted: it lives while at least one component subscribes.
 */
@Injectable({ providedIn: 'root' })
export class QuoteStore {
  private readonly state = new BehaviorSubject<Map<string, QuoteResponse>>(new Map());

  private readonly effects$: Observable<never> = defer(() =>
    merge(
      this.api.quotes().pipe(tap(q => this.merge(q)), catchError(() => EMPTY)),
      this.stomp.watch<QuoteResponse[]>('/topic/quotes').pipe(tap(q => this.merge(q))),
      this.stomp.resumed$.pipe(switchMap(() => this.api.quotes().pipe(tap(q => this.merge(q)), catchError(() => EMPTY))))
    )
  ).pipe(ignoreElements(), share());

  /** All quotes, sorted by ticker. Subscribing keeps the live connection open. */
  readonly all$: Observable<QuoteResponse[]> = merge(
    this.effects$,
    this.state.pipe(map(m => [...m.values()].sort((a, b) => a.ticker.localeCompare(b.ticker))))
  );

  /** "Prices may be delayed": market open (or simulated) but no message for more than 2× the poll interval. */
  readonly stale$: Observable<boolean>;

  constructor(
    private api: MarketDataService,
    private stomp: StompService,
    status: MarketStatusStore,
    auth: AuthService
  ) {
    this.stale$ = combineLatest([status.status$, interval(5000).pipe(startWith(0))]).pipe(
      map(([s]) => {
        if (!MarketStatusStore.isLiveSession(s) || !this.stomp.lastMessageAt) return false;
        return Date.now() - this.stomp.lastMessageAt > 2 * MarketStatusStore.pollSeconds(s) * 1000;
      }),
      distinctUntilChanged()
    );
    auth.currentUser$.subscribe(u => { if (!u) this.state.next(new Map()); });
  }

  quote$(ticker: string): Observable<QuoteResponse | null> {
    return merge(this.effects$, this.state.pipe(map(m => m.get(ticker) ?? null))).pipe(distinctUntilChanged());
  }

  snapshot(ticker: string): QuoteResponse | null { return this.state.value.get(ticker) ?? null; }
  has(ticker: string): boolean { return this.state.value.has(ticker); }

  private merge(quotes: QuoteResponse[]): void {
    const next = new Map(this.state.value);
    for (const q of quotes ?? []) {
      const old = next.get(q.ticker);
      if (!old || Date.parse(q.updatedAt) >= Date.parse(old.updatedAt)) next.set(q.ticker, q);
    }
    this.state.next(next);
  }
}
