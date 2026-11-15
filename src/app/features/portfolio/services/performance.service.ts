import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiClient } from '../../../shared/ui/util/api-client';
import { toPortfolioError } from '../errors/portfolio-error.handler';
import { LeaderboardEntry, PerformanceRange, PerformanceResponse, PortfolioApiError } from '../models/portfolio.model';

/** Routes 33–34. The backend computes every number; the frontend never recomputes P&L. */
@Injectable({ providedIn: 'root' })
export class PerformanceService {
  private readonly api: ApiClient<PortfolioApiError>;

  constructor(http: HttpClient) {
    this.api = new ApiClient<PortfolioApiError>(http, `${environment.apiUrl}/portfolio`, toPortfolioError);
  }

  performance(range: PerformanceRange): Observable<PerformanceResponse> { return this.api.get('/performance', { range }); }
  leaderboard(limit: number): Observable<LeaderboardEntry[]> { return this.api.get('/leaderboard', { limit }); }
}
