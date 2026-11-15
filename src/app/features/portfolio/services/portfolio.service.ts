import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiClient } from '../../../shared/ui/util/api-client';
import { toPortfolioError } from '../errors/portfolio-error.handler';
import {
  BuyingPower, CashOperationRequest, DevExecutionRequest, PageResponse, PortfolioApiError, PortfolioSummaryResponse,
  PositionResponse, TransactionQuery, TransactionResponse
} from '../models/portfolio.model';

/** Routes 20–25 and 28. Money is validated client-side as a courtesy; the backend stays the authority. */
@Injectable({ providedIn: 'root' })
export class PortfolioService {
  private readonly api: ApiClient<PortfolioApiError>;

  constructor(http: HttpClient) {
    this.api = new ApiClient<PortfolioApiError>(http, `${environment.apiUrl}/portfolio`, toPortfolioError);
  }

  summary(): Observable<PortfolioSummaryResponse> { return this.api.get(''); }
  buyingPower(): Observable<BuyingPower> { return this.api.get('/buying-power'); }
  deposit(body: CashOperationRequest): Observable<PortfolioSummaryResponse> { return this.api.post('/deposit', body); }
  withdraw(body: CashOperationRequest): Observable<PortfolioSummaryResponse> { return this.api.post('/withdraw', body); }
  reset(): Observable<PortfolioSummaryResponse> { return this.api.post('/reset'); }
  /** Instant simulated fill (`dev/execute`): answers with the updated position. 404 when the server disabled dev endpoints. */
  execute(body: DevExecutionRequest): Observable<PositionResponse> { return this.api.post('/dev/execute', body); }
  transactions(q: TransactionQuery): Observable<PageResponse<TransactionResponse>> {
    return this.api.get('/transactions', { type: q.type, ticker: q.ticker, from: q.from, to: q.to, page: q.page, size: q.size });
  }
}
