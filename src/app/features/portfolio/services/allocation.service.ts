import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiClient } from '../../../shared/ui/util/api-client';
import { toPortfolioError } from '../errors/portfolio-error.handler';
import {
  AllocationGroupBy, AllocationResponse, AllocationTargetRequest, AllocationTargetResponse, PortfolioApiError, RebalanceSuggestion
} from '../models/portfolio.model';

/** Routes 29–32. */
@Injectable({ providedIn: 'root' })
export class AllocationService {
  private readonly api: ApiClient<PortfolioApiError>;

  constructor(http: HttpClient) {
    this.api = new ApiClient<PortfolioApiError>(http, `${environment.apiUrl}/portfolio/allocation`, toPortfolioError);
  }

  allocation(by: AllocationGroupBy): Observable<AllocationResponse> { return this.api.get('', { by }); }
  targets(): Observable<AllocationTargetResponse[]> { return this.api.get('/targets'); }
  saveTargets(targets: AllocationTargetRequest[]): Observable<AllocationTargetResponse[]> { return this.api.put('/targets', targets); }
  rebalance(): Observable<RebalanceSuggestion[]> { return this.api.get('/rebalance'); }
}
