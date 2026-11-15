import { HttpErrorResponse } from '@angular/common/http';
import { PortfolioApiError, PortfolioApiErrorBody, PortfolioErrorKind } from '../models/portfolio.model';

/** Turns an HttpErrorResponse into the portfolio module's error, with portfolio-specific wording. */
export function toPortfolioError(err: unknown): PortfolioApiError {
  if (!(err instanceof HttpErrorResponse)) {
    return { kind: 'unknown', status: -1, message: 'Something went wrong with your portfolio.', fieldErrors: {}, path: '' };
  }
  const body = (err.error && typeof err.error === 'object' ? err.error : {}) as Partial<PortfolioApiErrorBody>;
  const fieldErrors = body.validationErrors ?? {};
  const backendMessage = typeof body.message === 'string' && body.message ? body.message : '';
  const path = body.path ?? err.url ?? '';
  let kind: PortfolioErrorKind;
  let message = backendMessage;

  if (err.status === 0) {
    kind = 'network';
    message = "Can't reach the portfolio server.";
  } else if (err.status === 400 && Object.keys(fieldErrors).length) {
    kind = 'validation';
    message = backendMessage || 'Some values are not valid.';
  } else if (err.status === 400 || err.status === 422) {
    // e.g. "Insufficient funds…", "Cancel your open orders before resetting": shown as the backend wrote them.
    kind = err.status === 400 ? 'bad-request' : 'unprocessable';
    message = backendMessage || 'The request could not be processed.';
  } else if (err.status === 401) {
    kind = 'unauthorized';
    message = 'Your session has expired.';
  } else if (err.status === 403) {
    kind = 'forbidden';
    message = "You don't have access to this";
  } else if (err.status === 404) {
    kind = 'not-found';
    // dev/execute answers 404 when app.portfolio.dev-endpoints-enabled is off
    message = path.includes('/dev/execute') ? 'Trading is disabled on this server' : backendMessage || 'Not found.';
  } else if (err.status >= 500) {
    kind = 'server';
    message = 'The portfolio server had a problem. Please retry.';
    console.error('[portfolio] server error', { path, timestamp: body.timestamp, status: err.status });
  } else {
    kind = 'unknown';
    message = backendMessage || 'Something went wrong with your portfolio.';
  }
  return { kind, status: err.status, message, fieldErrors, path };
}
