import { HttpErrorResponse } from '@angular/common/http';
import { toPortfolioError } from './portfolio-error.handler';

const err = (status: number, body?: unknown, url = '/api/portfolio/x') => new HttpErrorResponse({ status, error: body, url });

describe('toPortfolioError', () => {
  it('maps status 0 with portfolio wording', () => {
    const e = toPortfolioError(err(0));
    expect(e.kind).toBe('network');
    expect(e.message).toBe("Can't reach the portfolio server.");
  });
  it('shows the backend text of a 422 as is', () => {
    const e = toPortfolioError(err(422, { status: 422, message: 'Insufficient funds: need 10.000, available 5.000', path: '/api/portfolio/withdraw' }));
    expect(e.kind).toBe('unprocessable');
    expect(e.message).toContain('Insufficient funds');
  });
  it('shows "Cancel your open orders before resetting" from a 400', () => {
    const e = toPortfolioError(err(400, { status: 400, message: 'Cancel your open orders before resetting', path: '/api/portfolio/reset' }));
    expect(e.kind).toBe('bad-request');
    expect(e.message).toBe('Cancel your open orders before resetting');
  });
  it('maps validation errors on cash and trade forms to fields', () => {
    const e = toPortfolioError(err(400, { status: 400, message: 'Validation failed', path: '/x', validationErrors: { amount: 'must be greater than 0' } }));
    expect(e.kind).toBe('validation');
    expect(e.fieldErrors['amount']).toBe('must be greater than 0');
  });
  it('says trading is disabled when dev/execute answers 404', () => {
    const e = toPortfolioError(err(404, { status: 404, message: 'Not Found', path: '/api/portfolio/dev/execute' }));
    expect(e.message).toBe('Trading is disabled on this server');
  });
  it('flags 401, 403 and 5xx', () => {
    expect(toPortfolioError(err(401)).kind).toBe('unauthorized');
    expect(toPortfolioError(err(403)).kind).toBe('forbidden');
    expect(toPortfolioError(err(500)).kind).toBe('server');
  });
});
