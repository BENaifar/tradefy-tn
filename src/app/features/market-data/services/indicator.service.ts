import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiClient } from '../../../shared/ui/util/api-client';
import { toMarketError } from '../errors/market-data-error.handler';
import { CandleInterval, IndicatorDefinition, IndicatorResult, MarketApiError } from '../models/market-data.model';

/** Routes 40–41. Indicators are computed by the backend; nothing about a specific indicator is hard-coded here. */
@Injectable({ providedIn: 'root' })
export class IndicatorService {
  private readonly api: ApiClient<MarketApiError>;
  private catalog$?: Observable<IndicatorDefinition[]>;

  constructor(http: HttpClient) {
    this.api = new ApiClient<MarketApiError>(http, `${environment.apiUrl}/market`, toMarketError);
  }

  /** The catalog, loaded once and shared. A failed load is not cached. */
  catalog(): Observable<IndicatorDefinition[]> {
    if (!this.catalog$) {
      this.catalog$ = this.api.get<IndicatorDefinition[]>('/indicators').pipe(shareReplay(1));
    }
    return this.catalog$;
  }

  resetCatalog(): void { this.catalog$ = undefined; }

  /** Every extra query parameter is an indicator parameter (name → number). */
  compute(
    ticker: string, code: string, interval: CandleInterval, from: string, to: string, params: Record<string, number>
  ): Observable<IndicatorResult> {
    return this.api.get(`/instruments/${encodeURIComponent(ticker)}/indicators/${encodeURIComponent(code)}`, {
      interval, from, to, ...params
    });
  }
}
