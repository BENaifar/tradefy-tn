import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiClient } from '../../../shared/ui/util/api-client';
import { toMarketError } from '../errors/market-data-error.handler';
import { CandleInterval, CandleSeriesResponse, MarketApiError } from '../models/market-data.model';

/** Route 39. Candles are built by the backend only; the frontend never builds or aggregates them. */
@Injectable({ providedIn: 'root' })
export class CandleService {
  private readonly api: ApiClient<MarketApiError>;

  constructor(http: HttpClient) {
    this.api = new ApiClient<MarketApiError>(http, `${environment.apiUrl}/market`, toMarketError);
  }

  candles(ticker: string, interval: CandleInterval, from: string, to: string): Observable<CandleSeriesResponse> {
    return this.api.get(`/instruments/${encodeURIComponent(ticker)}/candles`, { interval, from, to });
  }
}
