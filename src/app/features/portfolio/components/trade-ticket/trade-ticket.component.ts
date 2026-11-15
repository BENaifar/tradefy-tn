import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { EMPTY, Observable, Subject, Subscription, catchError, exhaustMap, tap } from 'rxjs';
import { SegmentOption } from '../../../../shared/ui/components/tx-segmented/tx-segmented.component';
import { fmtMoney } from '../../../../shared/ui/util/format';
import { applyFieldErrors, controlError } from '../../../../shared/ui/util/form-errors';
import { ToastService } from '../../../../shared/ui/util/toast.service';
import { TradePrefill, TradeTicketService } from '../../../../shared/ui/util/trade-ticket.service';
import { blockNonDigits, decimalValidator, integerValidator, parseDecimal } from '../../../../shared/ui/validators/tx-validators';
import { QuoteResponse } from '../../../market-data/models/market-data.model';
import { QuoteStore } from '../../../market-data/services/quote.store';
import { PortfolioApiError, PositionResponse } from '../../models/portfolio.model';
import { PortfolioService } from '../../services/portfolio.service';
import { PortfolioStore, PortfolioVm } from '../../services/portfolio.store';

/** The server computes the final amounts: these constants only drive the labelled estimate. */
const FEE_RATE = 0.004;
const BUY_BUFFER = 1.05; // a market BUY reserves 5 % above the last price

interface Estimate {
  price: number | null;
  gross: number | null;
  fee: number | null;
  total: number | null;
  cashAfter: number | null;
  sharesAfter: number | null;
  problem: string;
}

/**
 * Trade ticket drawer: an INSTANT simulated fill (`POST /api/portfolio/dev/execute`).
 * Nothing is optimistic: the backend answers, then the portfolio store refreshes.
 */
@Component({
  selector: 'app-trade-ticket',
  templateUrl: './trade-ticket.component.html',
  styleUrls: ['./trade-ticket.component.scss']
})
export class TradeTicketComponent implements OnInit, OnDestroy {
  open = false;
  readonly sideOptions: SegmentOption[] = [{ value: 'BUY', label: 'Buy' }, { value: 'SELL', label: 'Sell' }];
  form = new FormGroup({
    ticker: new FormControl<string>('', { nonNullable: true, validators: [Validators.required] }),
    side: new FormControl<'BUY' | 'SELL'>('BUY', { nonNullable: true }),
    quantity: new FormControl<string>('', { nonNullable: true, validators: [integerValidator(1)] }),
    price: new FormControl<string>('', { nonNullable: true, validators: [decimalValidator({ min: 0, exclusiveMin: true, maxDecimals: 3 })] })
  });
  quote: QuoteResponse | null = null;
  vm: PortfolioVm | null = null;
  est: Estimate = { price: null, gross: null, fee: null, total: null, cashAfter: null, sharesAfter: null, problem: '' };
  step: 'form' | 'review' | 'done' = 'form';
  pending = false;
  formError = '';
  result: PositionResponse | null = null;
  filled: { side: "BUY" | "SELL"; ticker: string; quantity: number } | null = null;
  readonly blockNonDigits = blockNonDigits;
  readonly controlError = controlError;
  readonly fmtMoney = fmtMoney;

  private subs = new Subscription();
  private quoteSub?: Subscription;
  private submit$ = new Subject<void>();

  constructor(
    private state: TradeTicketService,
    private quotes: QuoteStore,
    private store: PortfolioStore,
    private portfolio: PortfolioService,
    private toast: ToastService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.subs.add(this.state.state$.subscribe(s => {
      if (s) this.show(s); else this.open = false;
    }));
    this.subs.add(this.store.vm$.subscribe(vm => { this.vm = vm; this.recompute(); }));
    this.subs.add(this.form.valueChanges.subscribe(() => { this.formError = ''; this.recompute(); }));
    // exhaustMap: a second click while a request is pending is ignored.
    this.subs.add(this.submit$.pipe(exhaustMap(() => this.execute())).subscribe());
  }

  ngOnDestroy(): void { this.subs.unsubscribe(); this.quoteSub?.unsubscribe(); }

  // ---------------------------------------------------------------- open / close
  private show(p: TradePrefill): void {
    this.open = true;
    this.step = 'form';
    this.result = null;
    this.formError = '';
    this.form.reset({ ticker: p.ticker ?? '', side: p.side ?? 'BUY', quantity: p.quantity ? String(p.quantity) : '', price: '' });
    this.watchQuote(p.ticker ?? '');
    this.store.ensureLoaded();
    this.store.refresh();
  }

  close(): void { this.state.close(); }

  private watchQuote(ticker: string): void {
    this.quoteSub?.unsubscribe();
    this.quote = null;
    if (!ticker) { this.recompute(); return; }
    this.quoteSub = this.quotes.quote$(ticker).subscribe(q => { this.quote = q; this.updatePriceValidator(); this.recompute(); });
  }

  /** The ticker must exist in the quote store: it is picked from the list, never typed free-hand. */
  pickTicker(t: string): void {
    this.form.controls.ticker.setValue(t);
    this.watchQuote(t);
  }
  clearTicker(): void { this.form.controls.ticker.setValue(''); this.watchQuote(''); }

  setSide(v: string): void { this.form.controls.side.setValue(v as 'BUY' | 'SELL'); }
  private updatePriceValidator(): void {
    const q = this.quote;
    const rules = { min: 0, exclusiveMin: true, maxDecimals: 3 };
    const base = decimalValidator(rules);
    this.form.controls.price.setValidators((c): ValidationErrors | null => {
      const e = base(c);
      if (e) return e;
      const n = parseDecimal(c.value);
      if (n !== null && q && Number.isFinite(q.lowerLimit) && Number.isFinite(q.upperLimit) && (n < q.lowerLimit || n > q.upperLimit)) {
        return { limits: { message: `Must be within ${q.lowerLimit.toFixed(3)} – ${q.upperLimit.toFixed(3)}.` } };
      }
      return null;
    });
    this.form.controls.price.updateValueAndValidity({ emitEvent: false });
  }

  // ---------------------------------------------------------------- derived values
  get ticker(): string { return this.form.controls.ticker.value; }
  get side(): 'BUY' | 'SELL' { return this.form.controls.side.value; }
  get qty(): number { const s = this.form.controls.quantity.value; return /^\d+$/.test(s) ? Number(s) : 0; }
  get userPrice(): number | null { return parseDecimal(this.form.controls.price.value); }
  get availableCash(): number { return this.vm?.buyingPower?.availableCash ?? 0; }
  get sharesAvailable(): number { return this.vm?.buyingPower?.availableShares?.[this.ticker] ?? 0; }
  get sharesHeld(): number { return this.vm?.positions?.find(p => p.ticker === this.ticker)?.quantity ?? 0; }
  get tickerKnown(): boolean { return !!this.ticker && this.quotes.has(this.ticker); }
  get canReview(): boolean { return this.form.valid && this.tickerKnown && this.qty > 0 && !this.est.problem; }
  get title(): string { return this.step === 'done' ? 'Order filled' : 'Place a trade'; }

  /** Whole shares for the quick chips (BUY: share of buying power, SELL: share of available shares). */
  chip(pct: number | 'max'): void {
    let n = 0;
    if (this.side === 'BUY') {
      const px = this.userPrice ?? this.quote?.last ?? 0;
      const per = this.userPrice !== null ? px * (1 + FEE_RATE) : px * BUY_BUFFER * (1 + FEE_RATE);
      const max = per > 0 ? Math.floor(this.availableCash / per) : 0;
      n = pct === 'max' ? max : Math.floor(max * (pct / 100));
    } else {
      n = pct === 'max' ? this.sharesAvailable : Math.floor(this.sharesAvailable * (pct / 100));
    }
    this.form.controls.quantity.setValue(String(Math.max(0, n)));
    this.form.controls.quantity.markAsDirty();
  }

  private recompute(): void {
    const px = this.userPrice ?? this.quote?.last ?? null;
    const q = this.qty;
    const e: Estimate = { price: px, gross: null, fee: null, total: null, cashAfter: null, sharesAfter: null, problem: '' };
    if (px !== null && q > 0) {
      e.gross = q * px;
      e.fee = e.gross * FEE_RATE;
      if (this.side === 'BUY') {
        e.total = e.gross + e.fee;
        e.cashAfter = this.availableCash - e.total;
        e.sharesAfter = this.sharesHeld + q;
        // Market BUY check: qty × last × 1.05 × 1.004 ≤ availableCash (5 % reservation buffer)
        if (this.vm?.buyingPower && this.userPrice === null && this.quote) {
          const need = q * this.quote.last * BUY_BUFFER * (1 + FEE_RATE);
          if (need > this.availableCash) {
            e.problem = `A market buy reserves a 5 % price buffer: this needs about ${fmtMoney(need)} but you have ${fmtMoney(this.availableCash)} available. Lower the quantity or enter a price.`;
          }
        } else if (this.vm?.buyingPower && e.total > this.availableCash) {
          e.problem = `This needs about ${fmtMoney(e.total)} but you have ${fmtMoney(this.availableCash)} available.`;
        }
      } else {
        e.total = e.gross - e.fee;
        e.cashAfter = this.availableCash + e.total;
        e.sharesAfter = this.sharesHeld - q;
        if (this.vm?.buyingPower && q > this.sharesAvailable) {
          e.problem = `You can sell at most ${this.sharesAvailable} available share${this.sharesAvailable === 1 ? '' : 's'} of ${this.ticker}.`;
        }
      }
    }
    this.est = e;
  }

  // ---------------------------------------------------------------- flow
  review(): void {
    this.form.markAllAsTouched();
    if (!this.canReview) return;
    this.step = 'review';
  }
  back(): void { this.step = 'form'; this.formError = ''; }
  confirm(): void { if (!this.pending) this.submit$.next(); }

  get reviewText(): string {
    const verb = this.side === 'BUY' ? 'Buy' : 'Sell';
    const at = this.userPrice !== null ? '' : '~';
    return `${verb} ${this.qty} ${this.ticker} at ${at}${fmtMoney(this.est.price)} for ${at}${fmtMoney(this.est.total)}.`;
  }

  private execute(): Observable<unknown> {
    this.pending = true;
    this.formError = '';
    const price = this.userPrice;
    const filled = { side: this.side, ticker: this.ticker, quantity: this.qty };
    const body = { ticker: this.ticker, side: this.side, quantity: this.qty, ...(price !== null ? { price } : {}) };
    return this.portfolio.execute(body).pipe(
      tap(r => {
        this.pending = false;
        this.result = r;
        this.filled = filled;
        this.step = 'done';
        this.toast.success(`${filled.side === 'BUY' ? 'Bought' : 'Sold'} ${filled.quantity} ${filled.ticker}`);
        this.store.refresh(); // nothing optimistic: refresh after the backend answered
      }),
      // failures are handled inline and the form is kept
      catchError((e: PortfolioApiError) => { this.onError(e); return EMPTY; })
    );
  }

  private onError(e: PortfolioApiError): void {
    this.pending = false;
    this.step = 'form';
    const leftover = applyFieldErrors(this.form, e.fieldErrors);
    this.formError = leftover.length ? leftover.join(' ') : e.message;
    this.toast.reportError(e);
  }

  tradeAgain(): void {
    const t = this.ticker;
    this.step = 'form';
    this.result = null;
    this.form.reset({ ticker: t, side: this.side, quantity: '', price: '' });
    this.store.refresh();
  }

  viewPosition(): void {
    const t = this.ticker;
    this.close();
    void this.router.navigate(['/portfolio/positions'], { queryParams: { position: t } });
  }
}
