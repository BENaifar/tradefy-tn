import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
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
}
