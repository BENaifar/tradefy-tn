import { HttpErrorResponse } from '@angular/common/http';
import { MarketApiError, MarketApiErrorBody, MarketErrorKind } from '../models/market-data.model';

/** Turns an HttpErrorResponse into the market-data module's error, with market-specific wording. */
export function toMarketError(err: unknown): MarketApiError {
  if (!(err instanceof HttpErrorResponse)) {
    return { kind: 'unknown', status: -1, message: 'Something went wrong while loading market data.', fieldErrors: {}, path: '' };
  }
  const body = (err.error && typeof err.error === 'object' ? err.error : {}) as Partial<MarketApiErrorBody>;
  const fieldErrors = body.validationErrors ?? {};
  const backendMessage = typeof body.message === 'string' && body.message ? body.message : '';
  const path = body.path ?? err.url ?? '';
  let kind: MarketErrorKind;
  let message = backendMessage;

  if (err.status === 0) {
    kind = 'network';
    message = "Can't reach the market data server.";
  } else if (err.status === 400 && Object.keys(fieldErrors).length) {
    kind = 'validation';
    message = backendMessage || 'Some values are not valid.';
  } else if (err.status === 400 || err.status === 422) {
    kind = err.status === 400 ? 'bad-request' : 'unprocessable';
    message = backendMessage || 'Invalid interval or date range';
  } else if (err.status === 401) {
    kind = 'unauthorized';
    message = 'Your session has expired.';
  } else if (err.status === 403) {
    kind = 'forbidden';
    message = "You don't have access to this";
  } else if (err.status === 404) {
    kind = 'not-found';
    message = backendMessage || "We couldn't find what you asked for.";
  } else if (err.status >= 500) {
    kind = 'server';
    message = 'The market data server had a problem. Please retry.';
    console.error('[market-data] server error', { path, timestamp: body.timestamp, status: err.status });
  } else {
    kind = 'unknown';
    message = backendMessage || 'Something went wrong while loading market data.';
  }
  return { kind, status: err.status, message, fieldErrors, path };
}
