// Portfolio module: its own API response + error types (never merged with user.model.ts or the market-data module).
// Field names come from PIM/src/main/java/tn/esprit/pi5eme/dto (the DTO wins over any doc).

export interface PortfolioApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface PortfolioApiErrorBody {
  status: number;
  error: string;
  message: string;
  path: string;
  timestamp?: string;
  validationErrors?: Record<string, string>;
}

export type PortfolioErrorKind =
  | 'network' | 'validation' | 'bad-request' | 'unprocessable' | 'unauthorized'
  | 'forbidden' | 'not-found' | 'server' | 'unknown';

export interface PortfolioApiError {
  kind: PortfolioErrorKind;
  status: number;
  message: string;
  fieldErrors: Record<string, string>;
  path: string;
}

export interface PageResponse<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number; }

export type TransactionType = 'INITIAL_DEPOSIT' | 'DEPOSIT' | 'WITHDRAWAL' | 'BUY' | 'SELL' | 'RESET';
export type OrderSide = 'BUY' | 'SELL';
export type PositionStatus = 'OPEN' | 'CLOSED';
export type TargetType = 'INSTRUMENT' | 'SECTOR';
export type AllocationGroupBy = 'INSTRUMENT' | 'SECTOR' | 'ASSET_CLASS';
export type PerformanceRange = 'W1' | 'M1' | 'M3' | 'M6' | 'YTD' | 'Y1' | 'ALL';

export interface PortfolioSummaryResponse {
  portfolioId: number; name: string; currency: string; initialCapital: number; netContributions: number;
  cashBalance: number; reservedCash: number; availableCash: number; marketValue: number; totalValue: number;
  realizedPnl: number; unrealizedPnl: number; totalPnl: number; totalReturnPct: number; dayPnl: number; dayPnlPct: number;
  totalFees: number; openPositionsCount: number; missingPrices: string[]; createdAt: string;
}
export interface BuyingPower {
  userId: number; cashBalance: number; reservedCash: number; availableCash: number; availableShares: Record<string, number>;
}
export interface CashOperationRequest { amount: number; note?: string; }
export interface DevExecutionRequest { ticker: string; side: OrderSide; quantity: number; price?: number; }
export interface PositionResponse {
  id: number; ticker: string; name: string; sector: string; status: PositionStatus; quantity: number; reservedQuantity: number;
  availableQuantity: number; avgCost: number; totalCost: number; lastPrice: number; marketValue: number; unrealizedPnl: number;
  unrealizedPnlPct: number; dayChangePct: number; realizedPnl: number; weightPct: number; openedAt: string; closedAt: string | null; holdingDays: number;
}
export interface TransactionResponse {
  id: number; type: TransactionType; ticker: string | null; quantity: number | null; price: number | null; grossAmount: number;
  fee: number; netAmount: number; realizedPnl: number | null; cashBalanceAfter: number; orderId: string | null;
  executionId: string | null; note: string | null; executedAt: string;
}
export interface PositionDetailResponse { position: PositionResponse; transactions: TransactionResponse[]; }
export interface AllocationSlice { key: string; label: string; value: number; weightPct: number; targetPct: number | null; driftPct: number | null; }
export interface AllocationResponse { groupBy: AllocationGroupBy; totalValue: number; slices: AllocationSlice[]; }
export interface AllocationTargetRequest { type: TargetType; key: string; percent: number; }
export interface AllocationTargetResponse { type: TargetType; key: string; percent: number; }
export interface RebalanceSuggestion {
  ticker: string; side: OrderSide; quantity: number; estimatedPrice: number; estimatedAmount: number; currentWeightPct: number; targetWeightPct: number;
}
export interface EquityPoint { date: string; totalValue: number; cumulativeReturnPct: number; }
export interface PositionPerformance { ticker: string; unrealizedPnlPct: number; }
export interface PerformanceResponse {
  range: PerformanceRange; from: string; to: string; points: EquityPoint[]; startValue: number; endValue: number;
  periodReturnPct: number; totalReturnPct: number; realizedPnl: number; unrealizedPnl: number;
  bestPosition: PositionPerformance | null; worstPosition: PositionPerformance | null;
  winningTrades: number; losingTrades: number; winRatePct: number;
}
export interface LeaderboardEntry { rank: number; displayName: string; totalReturnPct: number; totalValue: number; isMe: boolean; }
export interface AdminPortfolioRow {
  userId: number; email: string; fullName: string; totalValue: number; totalReturnPct: number; openPositions: number; lastActivity: string;
}
export interface TransactionQuery { type?: TransactionType | ''; ticker?: string; from?: string; to?: string; page?: number; size?: number; }
