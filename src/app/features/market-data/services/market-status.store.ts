import { Injectable } from '@angular/core';
import { BehaviorSubject, EMPTY, Observable, catchError, defer, ignoreElements, merge, share, switchMap, timer } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { MarketStatusResponse } from '../models/market-data.model';
import { MarketDataService } from './market-data.service';

/** `GET /api/market/status` on load and every 5 minutes (only while somebody is watching). */
@Injectable({ providedIn: 'root' })
export class MarketStatusStore {
  private readonly subject = new BehaviorSubject<MarketStatusResponse | null>(null);

  private readonly refresh$ = defer(() =>
    timer(0, 5 * 60 * 1000).pipe(
      switchMap(() => this.api.status().pipe(catchError(() => EMPTY)))
    )
  ).pipe(
    switchMap(s => { this.subject.next(s); return EMPTY; }),
    ignoreElements(),
    share()
  ) as Observable<never>;

  /** Latest status (null until loaded). Subscribing starts the 5-minute refresh. */
  readonly status$: Observable<MarketStatusResponse | null> = merge(this.refresh$, this.subject.asObservable());

  constructor(private api: MarketDataService, auth: AuthService) {
    auth.currentUser$.subscribe(u => { if (!u) this.subject.next(null); });
  }

  get value(): MarketStatusResponse | null { return this.subject.value; }

  static isSimulated(s: MarketStatusResponse | null): boolean { return s?.provider === 'SIMULATED'; }
  /** Live prices flow when BVMT is open, or always with the simulated provider. */
  static isLiveSession(s: MarketStatusResponse | null): boolean { return !!s && (s.open || s.provider === 'SIMULATED'); }
  static pollSeconds(s: MarketStatusResponse | null): number { return s?.provider === 'SIMULATED' ? 10 : 30; }
}
