import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/services/auth.service';
import { ApiClient } from '../../../shared/ui/util/api-client';
import { toMarketError } from '../errors/market-data-error.handler';
import { MarketApiError, WatchlistItemResponse } from '../models/market-data.model';

/** Routes 13–15. Keeps the set of watched tickers so every ☆ in the app stays in sync. */
@Injectable({ providedIn: 'root' })
export class WatchlistService {
  private readonly api: ApiClient<MarketApiError>;
  private readonly items = new BehaviorSubject<WatchlistItemResponse[] | null>(null);
  readonly items$ = this.items.asObservable();
  private readonly tickers = new BehaviorSubject<Set<string>>(new Set());
  readonly tickers$ = this.tickers.asObservable();

  constructor(http: HttpClient, auth: AuthService) {
    this.api = new ApiClient<MarketApiError>(http, `${environment.apiUrl}/market/watchlist`, toMarketError);
    // Reset when the user logs out, so the next user never sees the previous watchlist.
    auth.currentUser$.subscribe(u => { if (!u) { this.items.next(null); this.tickers.next(new Set()); } });
  }

  has(ticker: string): boolean { return this.tickers.value.has(ticker); }

  list(): Observable<WatchlistItemResponse[]> {
    return this.api.get<WatchlistItemResponse[]>('').pipe(tap(list => this.set(list)));
  }

  add(ticker: string): Observable<WatchlistItemResponse> {
    return this.api.post<WatchlistItemResponse>(`/${encodeURIComponent(ticker)}`).pipe(
      tap(item => this.set([...(this.items.value ?? []).filter(i => i.ticker !== ticker), item]))
    );
  }

  remove(ticker: string): Observable<void> {
    return this.api.delete<void>(`/${encodeURIComponent(ticker)}`).pipe(
      tap(() => this.set((this.items.value ?? []).filter(i => i.ticker !== ticker)))
    );
  }

  /** Optimistic only for the star UI state of a not-yet-sent delete (undo window). */
  hideLocally(ticker: string): void { this.set((this.items.value ?? []).filter(i => i.ticker !== ticker)); }

  private set(list: WatchlistItemResponse[]): void {
    this.items.next(list);
    this.tickers.next(new Set(list.map(i => i.ticker)));
  }
}
