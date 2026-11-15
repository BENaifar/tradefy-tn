import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, forkJoin, map, of, switchMap } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiClient } from '../../../shared/ui/util/api-client';
import { toPortfolioError } from '../errors/portfolio-error.handler';
import { PortfolioApiError, PositionDetailResponse, PositionResponse, PositionStatus } from '../models/portfolio.model';

/** Routes 26–27. */
@Injectable({ providedIn: 'root' })
export class PositionService {
  private readonly api: ApiClient<PortfolioApiError>;

  constructor(http: HttpClient) {
    this.api = new ApiClient<PortfolioApiError>(http, `${environment.apiUrl}/portfolio/positions`, toPortfolioError);
  }

  list(status: PositionStatus): Observable<PositionResponse[]> { return this.api.get('', { status }); }
  detail(ticker: string): Observable<PositionDetailResponse> { return this.api.get(`/${encodeURIComponent(ticker)}`); }

  /**
   * The position detail when the user has ever held `ticker`, else null. It checks the open and closed lists
   * first, so a stock the user never held does not produce a failing request.
   */
  detailIfHeld(ticker: string): Observable<PositionDetailResponse | null> {
    return forkJoin([this.list('OPEN'), this.list('CLOSED')]).pipe(
      map(([open, closed]) => [...open, ...closed].some(p => p.ticker === ticker)),
      switchMap(held => (held ? this.detail(ticker) : of(null)))
    );
  }
}
