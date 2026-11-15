import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { downloadCsv } from '../../../../shared/ui/util/csv';
import { addDays, addMonths, isIsoDate, startOfMonth, startOfWeek, todayTunis } from '../../../../shared/ui/util/dates';
import { PORTFOLIO_TABS } from '../../../../shared/ui/util/module-tabs';
import { Section } from '../../../../shared/ui/util/section';
import { ToastService } from '../../../../shared/ui/util/toast.service';
import { dateRangeValidator } from '../../../../shared/ui/validators/tx-validators';
import { PageResponse, TransactionResponse, TransactionType } from '../../models/portfolio.model';
import { PortfolioService } from '../../services/portfolio.service';

type Preset = 'all' | 'week' | 'month' | 'quarter' | 'custom';
const TYPES: ('ALL' | TransactionType)[] = ['ALL', 'INITIAL_DEPOSIT', 'DEPOSIT', 'WITHDRAWAL', 'BUY', 'SELL', 'RESET'];
const SIZES = [10, 20, 50];

/** Filters live in the URL query, so Back and bookmarks work. */
@Component({
  selector: 'app-activity',
  templateUrl: './activity.component.html',
  styleUrls: ['./activity.component.scss']
})
export class ActivityComponent implements OnInit, OnDestroy {
  readonly tabs = PORTFOLIO_TABS;
  readonly types = TYPES;
  readonly sizes = SIZES;
  readonly today = todayTunis();

  type: 'ALL' | TransactionType = 'ALL';
  ticker = '';
  preset: Preset = 'all';
  from = '';
  to = '';
  page = 0;
  size = 20;
  expanded = new Set<number>();

  data: Section<PageResponse<TransactionResponse>>;
  customForm = new FormGroup({
    from: new FormControl<string>('', { nonNullable: true }),
    to: new FormControl<string>('', { nonNullable: true })
  }, { validators: dateRangeValidator('from', 'to', () => todayTunis()) });
  private tickerInput = new FormControl<string>('', { nonNullable: true });
  private subs = new Subscription();

  constructor(private api: PortfolioService, private route: ActivatedRoute, private router: Router, toast: ToastService) {
    this.data = new Section<PageResponse<TransactionResponse>>(toast);
  }

  get tickerControl(): FormControl<string> { return this.tickerInput; }
  get rangeError(): string {
    const e = this.customForm.errors;
    const v = e ? (e['range'] ?? e['future']) : null;
    return v ? String((v as { message: string }).message) : '';
  }

  ngOnInit(): void {
    this.subs.add(this.route.queryParamMap.subscribe(q => {
      const t = q.get('type') as TransactionType | null;
      this.type = t && TYPES.includes(t) ? t : 'ALL';
      this.ticker = (q.get('ticker') ?? '').trim().toUpperCase().slice(0, 20);
      this.preset = (['all', 'week', 'month', 'quarter', 'custom'].includes(q.get('preset') ?? '') ? q.get('preset') : 'all') as Preset;
      this.from = isIsoDate(q.get('from')) ? (q.get('from') as string) : '';
      this.to = isIsoDate(q.get('to')) ? (q.get('to') as string) : '';
      this.page = Math.max(0, Number(q.get('page')) || 0);
      const s = Number(q.get('size'));
      this.size = SIZES.includes(s) ? s : 20;
      this.expanded.clear();
      this.tickerInput.setValue(this.ticker, { emitEvent: false });
      this.customForm.setValue({ from: this.from, to: this.to }, { emitEvent: false });
      this.load();
    }));
  }
  ngOnDestroy(): void { this.subs.unsubscribe(); this.data.destroy(); }

  private load(): void {
    this.data.load(() => this.api.transactions({
      type: this.type === 'ALL' ? '' : this.type, ticker: this.ticker, from: this.from, to: this.to, page: this.page, size: this.size
    }), true);
  }

  /** Writes the filters to the URL (the query subscription then reloads). */
  private go(patch: Params, resetPage = true): void {
    const next: Params = {
      type: this.type === 'ALL' ? null : this.type, ticker: this.ticker || null, preset: this.preset === 'all' ? null : this.preset,
      from: this.from || null, to: this.to || null, page: this.page || null, size: this.size === 20 ? null : this.size, ...patch
    };
    if (resetPage && !('page' in patch)) next['page'] = null;
    void this.router.navigate([], { relativeTo: this.route, queryParams: next });
  }

  setType(t: 'ALL' | TransactionType): void { this.go({ type: t === 'ALL' ? null : t }); }
  applyTicker(): void { this.go({ ticker: this.tickerInput.value.trim().toUpperCase().slice(0, 20) || null }); }
  clearTicker(): void { this.tickerInput.setValue(''); this.go({ ticker: null }); }
  setSize(s: number): void { this.go({ size: s === 20 ? null : s }); }
  goPage(p: number): void { this.go({ page: p || null }, false); }

  setPreset(p: Preset): void {
    const today = todayTunis();
    switch (p) {
      case 'week': this.go({ preset: p, from: startOfWeek(today), to: today }); break;
      case 'month': this.go({ preset: p, from: startOfMonth(today), to: today }); break;
      case 'quarter': this.go({ preset: p, from: addMonths(today, -3), to: today }); break;
      case 'custom': this.preset = 'custom'; this.go({ preset: 'custom' }); break;
      default: this.go({ preset: null, from: null, to: null });
    }
  }

  applyCustom(): void {
    if (this.customForm.invalid) return;
    const v = this.customForm.getRawValue();
    this.go({ preset: 'custom', from: v.from || null, to: v.to || null });
  }

  toggle(id: number): void { if (this.expanded.has(id)) this.expanded.delete(id); else this.expanded.add(id); }

  clearFilters(): void { this.go({ type: null, ticker: null, preset: null, from: null, to: null }); }
  get filtered(): boolean { return this.type !== 'ALL' || !!this.ticker || this.preset !== 'all'; }
  get yesterday(): string { return addDays(this.today, -1); }

  badge(t: TransactionType): string { return t.replace('_', ' '); }

  exportCsv(): void {
    const rows = (this.data.data?.content ?? []).map(t => [
      t.executedAt, t.type, t.ticker, t.quantity, t.price, t.grossAmount, t.fee, t.netAmount, t.realizedPnl, t.cashBalanceAfter, t.orderId, t.executionId, t.note
    ]);
    downloadCsv(`transactions-page-${this.page + 1}.csv`,
      ['executedAt', 'type', 'ticker', 'quantity', 'price', 'gross', 'fee', 'net', 'realizedPnl', 'cashAfter', 'orderId', 'executionId', 'note'], rows);
  }

  trackId(_: number, t: TransactionResponse): number { return t.id; }
}
