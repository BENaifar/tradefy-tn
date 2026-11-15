import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiClient } from '../../../shared/ui/util/api-client';
import { toPortfolioError } from '../errors/portfolio-error.handler';
import { AdminPortfolioRow, PortfolioApiError, PortfolioSummaryResponse } from '../models/portfolio.model';

/** Routes 35–37 (admin only; the backend enforces the role). */
@Injectable({ providedIn: 'root' })
export class PortfolioAdminService {
  private readonly api: ApiClient<PortfolioApiError>;

  constructor(http: HttpClient) {
    this.api = new ApiClient<PortfolioApiError>(http, `${environment.apiUrl}/portfolio/admin`, toPortfolioError);
  }

  portfolios(): Observable<AdminPortfolioRow[]> { return this.api.get('/portfolios'); }
  portfolio(userId: number): Observable<PortfolioSummaryResponse> { return this.api.get(`/portfolios/${userId}`); }
  /** Returns the number of snapshots taken. */
  takeSnapshots(date?: string): Observable<number> { return this.api.post('/snapshots', null, { date }); }
}
