import { HttpErrorResponse } from '@angular/common/http';
import { toMarketError } from './market-data-error.handler';

const err = (status: number, body?: unknown) => new HttpErrorResponse({ status, error: body, url: '/api/market/x' });

describe('toMarketError', () => {
  it('maps status 0 to a network error with market wording', () => {
    const e = toMarketError(err(0));
    expect(e.kind).toBe('network');
    expect(e.message).toBe("Can't reach the market data server.");
  });
  it('keeps field errors of a 400 with validationErrors', () => {
    const e = toMarketError(err(400, { status: 400, message: 'Invalid', path: '/x', validationErrors: { period: 'too small' } }));
    expect(e.kind).toBe('validation');
    expect(e.fieldErrors['period']).toBe('too small');
  });
  it('shows the backend text of a 400 without fields (indicator rules)', () => {
    const e = toMarketError(err(400, { status: 400, message: 'MACD fast period must be smaller than the slow period', path: '/x' }));
    expect(e.kind).toBe('bad-request');
    expect(e.message).toContain('MACD fast');
  });
  it('falls back to the interval / date wording', () => {
    expect(toMarketError(err(400, {})).message).toBe('Invalid interval or date range');
  });
  it('flags 401, 403, 404 and 5xx', () => {
    expect(toMarketError(err(401)).kind).toBe('unauthorized');
    expect(toMarketError(err(403)).message).toBe("You don't have access to this");
    expect(toMarketError(err(404, { message: 'Instrument not found: NOPE' })).kind).toBe('not-found');
    expect(toMarketError(err(503)).kind).toBe('server');
  });
  it('handles non-HTTP errors', () => {
    expect(toMarketError(new Error('boom')).kind).toBe('unknown');
  });
});
