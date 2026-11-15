import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiClient } from '../../../shared/ui/util/api-client';
import { toMarketError } from '../errors/market-data-error.handler';
import { CsvImportResult, InstrumentResponse, InstrumentUpdateRequest, MarketApiError, SyncResult } from '../models/market-data.model';

/** Routes 16–19 (admin only; the backend enforces the role). */
@Injectable({ providedIn: 'root' })
export class MarketAdminService {
  private readonly api: ApiClient<MarketApiError>;

  constructor(http: HttpClient) {
    this.api = new ApiClient<MarketApiError>(http, `${environment.apiUrl}/market`, toMarketError);
  }

  updateInstrument(ticker: string, body: InstrumentUpdateRequest): Observable<InstrumentResponse> {
    return this.api.put(`/instruments/${encodeURIComponent(ticker)}`, body);
  }
  syncNow(): Observable<SyncResult> { return this.api.post('/admin/sync'); }
  closeSession(): Observable<unknown> { return this.api.post('/admin/close-session'); }
  importCsv(file: File): Observable<CsvImportResult> { return this.api.upload('/admin/history/import', file); }
}
