// Market-data module: its own API response + error types (never merged with user.model.ts or the portfolio module).
// Field names come from PIM/src/main/java/tn/esprit/pi5eme/dto (the DTO wins over any doc).

export interface MarketApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

/** What the backend sends on 4xx/5xx. */
export interface MarketApiErrorBody {
  status: number;
  error: string;
  message: string;
  path: string;
  timestamp?: string;
  validationErrors?: Record<string, string>;
}

export type MarketErrorKind =
  | 'network' | 'validation' | 'bad-request' | 'unprocessable' | 'unauthorized'
  | 'forbidden' | 'not-found' | 'server' | 'unknown';

/** The module's error type, produced by market-data-error.handler.ts. */
export interface MarketApiError {
  kind: MarketErrorKind;
  status: number;
  message: string;
  fieldErrors: Record<string, string>;
  path: string;
}

export type AssetClass = 'EQUITY' | 'INDEX' | 'BOND';
export type Market = 'BVMT';
export type MoverType = 'GAINERS' | 'LOSERS' | 'ACTIVE';
export type CandleInterval = 'M5' | 'M15' | 'M30' | 'H1' | 'H2' | 'H4' | 'D1' | 'W1';
export type IndicatorPlacement = 'OVERLAY' | 'PANE';
export type OutputStyle = 'LINE' | 'HISTOGRAM';
export type ParamType = 'INT' | 'DECIMAL';

export interface QuoteResponse {
  ticker: string; name: string; last: number; previousClose: number; change: number; changePercent: number;
  open: number; high: number; low: number; volume: number; turnover: number; bid: number; ask: number;
  bidQty: number; askQty: number; upperLimit: number; lowerLimit: number;
  sessionDate: string; quoteTime: string; updatedAt: string;
}
export interface InstrumentResponse {
  id: number; ticker: string; isin: string; name: string; arabName: string; assetClass: AssetClass; market: Market;
  valGroup: string; sector: string; currency: string; active: boolean; quote: QuoteResponse | null;
}
export interface InstrumentUpdateRequest { sector?: string; active?: boolean; }
export interface PriceBarResponse { date: string; open: number; high: number; low: number; close: number; volume: number; }
export interface IntradayTickResponse { timestamp: string; price: number; volume: number; }
export interface MarketStatusResponse {
  open: boolean; provider: string; lastSyncAt: string; sessionOpen: string; sessionClose: string; zone: string; instrumentsCount: number;
}
export interface WatchlistItemResponse { ticker: string; name: string; sector: string; addedAt: string; quote: QuoteResponse | null; }

export interface Candle { time: number; open: number; high: number; low: number; close: number; volume: number; }
export interface CandleSeriesResponse {
  ticker: string; interval: CandleInterval; from: string; to: string; timezone: string; live: boolean; truncated: boolean; candles: Candle[];
}

export interface IndicatorParam { name: string; label: string; type: ParamType; defaultValue: number; min: number; max: number; }
export interface IndicatorOutput { key: string; label: string; style: OutputStyle; }
export interface IndicatorDefinition {
  code: string; name: string; description: string; placement: IndicatorPlacement;
  params: IndicatorParam[]; outputs: IndicatorOutput[]; referenceLines: number[];
}
export interface IndicatorPoint { time: number; value: number | null; }
export interface IndicatorResult { code: string; params: Record<string, number>; series: Record<string, IndicatorPoint[]>; }

export interface SyncResult { received: number; newInstruments: number; updated: number; provider: string; at: string; }
export interface CsvImportResult { totalRows: number; inserted: number; updated: number; errors: string[]; }

/** One indicator the user added to the chart (SMA 20 and SMA 50 can coexist). Built from the backend catalog. */
export interface IndicatorInstance {
  id: string;
  code: string;
  params: Record<string, number>;
  color: string;
  visible: boolean;
  status: 'loading' | 'ok' | 'error';
  error: string;
  result: IndicatorResult | null;
}
