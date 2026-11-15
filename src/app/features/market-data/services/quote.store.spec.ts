import { TestBed } from '@angular/core/testing';
import { BehaviorSubject, Subject, firstValueFrom, of } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { QuoteResponse } from '../models/market-data.model';
import { MarketDataService } from './market-data.service';
import { MarketStatusStore } from './market-status.store';
import { QuoteStore } from './quote.store';
import { StompService } from './stomp.service';

const quote = (ticker: string, last: number, updatedAt: string): QuoteResponse => ({
  ticker, name: ticker, last, previousClose: 0, change: 0, changePercent: 0, open: 0, high: 0, low: 0, volume: 0, turnover: 0, bid: 0, ask: 0,
  bidQty: 0, askQty: 0, upperLimit: 0, lowerLimit: 0, sessionDate: '2026-11-15', quoteTime: '10:00:00', updatedAt
});

describe('QuoteStore merge order', () => {
  let live: Subject<QuoteResponse[]>;
  let store: QuoteStore;

  beforeEach(() => {
    live = new Subject<QuoteResponse[]>();
    TestBed.configureTestingModule({
      providers: [
        QuoteStore,
        { provide: MarketDataService, useValue: { quotes: () => of([quote('AB', 10, '2026-11-15T10:00:00Z')]) } },
        { provide: StompService, useValue: { watch: () => live.asObservable(), resumed$: new Subject<void>(), lastMessageAt: 0 } },
        { provide: MarketStatusStore, useValue: { status$: of(null) } },
        { provide: AuthService, useValue: { currentUser$: new BehaviorSubject(null) } }
      ]
    });
    store = TestBed.inject(QuoteStore);
  });

  it('seeds from REST', async () => {
    const sub = store.all$.subscribe();
    expect(store.snapshot('AB')?.last).toBe(10);
    sub.unsubscribe();
  });

  it('the newest updatedAt wins', () => {
    const sub = store.all$.subscribe();
    live.next([quote('AB', 12, '2026-11-15T10:05:00Z')]);
    expect(store.snapshot('AB')?.last).toBe(12);
    sub.unsubscribe();
  });

  it('ignores an older message arriving late', () => {
    const sub = store.all$.subscribe();
    live.next([quote('AB', 12, '2026-11-15T10:05:00Z')]);
    live.next([quote('AB', 9, '2026-11-15T10:01:00Z')]);
    expect(store.snapshot('AB')?.last).toBe(12);
    sub.unsubscribe();
  });

  it('merges different tickers and sorts by ticker', async () => {
    const sub = store.all$.subscribe();
    live.next([quote('ZZ', 1, '2026-11-15T10:05:00Z'), quote('CC', 2, '2026-11-15T10:05:00Z')]);
    const all = await firstValueFrom(store.all$);
    expect(all.map(q => q.ticker)).toEqual(['AB', 'CC', 'ZZ']);
    sub.unsubscribe();
  });
});
