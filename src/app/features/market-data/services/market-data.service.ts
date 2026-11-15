import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiClient } from '../../../shared/ui/util/api-client';
import { toMarketError } from '../errors/market-data-error.handler';
import {
  InstrumentResponse, IntradayTickResponse, MarketApiError, MarketStatusResponse, MoverType, PriceBarResponse, QuoteResponse
} from '../models/market-data.model';

/** Routes 4–12. Every call is a GET; the bearer token is added by the existing JwtInterceptor. */
@Injectable({ providedIn: 'root' })
export class MarketDataService {
  private readonly api: ApiClient<MarketApiError>;

  constructor(http: HttpClient) {
    this.api = new ApiClient<MarketApiError>(http, `${environment.apiUrl}/market`, toMarketError);
  }

  status(): Observable<MarketStatusResponse> { return this.api.get('/status'); }
  quotes(): Observable<QuoteResponse[]> { return this.api.get('/quotes'); }
  quote(ticker: string): Observable<QuoteResponse> { return this.api.get(`/quotes/${encodeURIComponent(ticker)}`); }
  movers(type: MoverType, limit = 5): Observable<QuoteResponse[]> { return this.api.get('/movers', { type, limit }); }
  instruments(q?: string, sector?: string): Observable<InstrumentResponse[]> { return this.api.get('/instruments', { q, sector }); }
  instrument(ticker: string): Observable<InstrumentResponse> { return this.api.get(`/instruments/${encodeURIComponent(ticker)}`); }
  sectors(): Observable<string[]> { return this.api.get('/sectors'); }
  history(ticker: string, from?: string, to?: string): Observable<PriceBarResponse[]> {
    return this.api.get(`/instruments/${encodeURIComponent(ticker)}/history`, { from, to });
  }
  intraday(ticker: string, date?: string): Observable<IntradayTickResponse[]> {
    return this.api.get(`/instruments/${encodeURIComponent(ticker)}/intraday`, { date });
  }
}
