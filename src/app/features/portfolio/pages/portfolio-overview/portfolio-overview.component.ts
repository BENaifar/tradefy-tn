import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EMPTY, Observable, Subject, Subscription, catchError, exhaustMap, map, tap } from 'rxjs';
import { DonutSlice } from '../../../../shared/ui/components/tx-donut/tx-donut.component';
import { SegmentOption } from '../../../../shared/ui/components/tx-segmented/tx-segmented.component';
import { BarItem } from '../../../../shared/ui/components/tx-bars/tx-bars.component';
import { TreemapItem } from '../../../../shared/ui/components/tx-treemap/tx-treemap.component';
import { ConfirmService } from '../../../../shared/ui/util/confirm.service';
import { applyFieldErrors, controlError } from '../../../../shared/ui/util/form-errors';
import { fmtMoney } from '../../../../shared/ui/util/format';
import { PORTFOLIO_TABS } from '../../../../shared/ui/util/module-tabs';
import { Section } from '../../../../shared/ui/util/section';
import { ToastService } from '../../../../shared/ui/util/toast.service';
import { TradeTicketService } from '../../../../shared/ui/util/trade-ticket.service';
import { decimalValidator, maxLengthMsg, parseDecimal } from '../../../../shared/ui/validators/tx-validators';
import { EquityCurveComponent } from '../../components/equity-curve/equity-curve.component';
import { AllocationGroupBy, AllocationResponse, PortfolioApiError, PortfolioSummaryResponse } from '../../models/portfolio.model';
import { AllocationService } from '../../services/allocation.service';
import { PortfolioService } from '../../services/portfolio.service';
import { PortfolioStore, PortfolioVm } from '../../services/portfolio.store';

@Component({
  selector: 'app-portfolio-overview',
  templateUrl: './portfolio-overview.component.html',
  styleUrls: ['./portfolio-overview.component.scss']
})
export class PortfolioOverviewComponent implements OnInit, OnDestroy {
  readonly tabs = PORTFOLIO_TABS;
  readonly groupOptions: SegmentOption[] = [
    { value: 'SECTOR', label: 'Sector' }, { value: 'INSTRUMENT', label: 'Instrument' }, { value: 'ASSET_CLASS', label: 'Asset class' }
  ];
  readonly cashOptions: SegmentOption[] = [{ value: 'deposit', label: 'Deposit' }, { value: 'withdraw', label: 'Withdraw' }];
  readonly controlError = controlError;
  readonly fmtMoney = fmtMoney;

  alloc: Section<AllocationResponse>;
  groupBy: AllocationGroupBy = 'SECTOR';
  slices: DonutSlice[] = [];
  selectedTicker: string | null = null;

  cashTab: 'deposit' | 'withdraw' = 'deposit';
  cashForm = new FormGroup({
    amount: new FormControl<string>('', { nonNullable: true, validators: [decimalValidator({ min: 0.001, max: 1_000_000, maxDecimals: 3, required: true })] }),
    note: new FormControl<string>('', { nonNullable: true, validators: [maxLengthMsg(255)] })
  });
  cashPending = false;
  cashError = '';
  dangerOpen = false;
  resetPending = false;
  resetError = '';

  @ViewChild(EquityCurveComponent) curve?: EquityCurveComponent;
  private cash$ = new Subject<void>();
  private subs = new Subscription();

  constructor(
    public store: PortfolioStore,
    private portfolio: PortfolioService,
    private allocation: AllocationService,
    private confirm: ConfirmService,
    private ticket: TradeTicketService,
    private toast: ToastService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.alloc = new Section<AllocationResponse>(toast, a => this.buildSlices(a));
  }

  ngOnInit(): void {
    this.store.ensureLoaded();
    this.loadAlloc();
    this.subs.add(this.cash$.pipe(exhaustMap(() => this.runCash())).subscribe());
    // `/portfolio?trade=open` (dashboard quick action) opens the trade ticket, then drops the parameter.
    this.subs.add(this.route.queryParamMap.subscribe(q => {
      if (q.get('trade') === 'open') {
        this.ticket.open();
        void this.router.navigate([], { queryParams: { trade: null }, queryParamsHandling: 'merge', replaceUrl: true });
      }
    }));
  }
  ngOnDestroy(): void { this.subs.unsubscribe(); this.alloc.destroy(); }

  // ---------------------------------------------------------------- derived display values
  split(s: PortfolioSummaryResponse): { label: string; value: number; pct: number; cls: string }[] {
    const total = s.totalValue || 1;
    const parts = [
      { label: 'Cash', value: s.availableCash, cls: 'cash' },
      { label: 'Invested', value: s.marketValue, cls: 'inv' },
      { label: 'Reserved', value: s.reservedCash, cls: 'res' }
    ];
    return parts.map(p => ({ ...p, pct: Math.max(0, (p.value / total) * 100) }));
  }

  bars(vm: PortfolioVm): BarItem[] {
    return [...(vm.positions ?? [])].sort((a, b) => b.unrealizedPnl - a.unrealizedPnl).map(p => ({ label: p.ticker, value: p.unrealizedPnl }));
  }
  tiles(vm: PortfolioVm): TreemapItem[] {
    return (vm.positions ?? []).map(p => ({ id: p.ticker, label: p.ticker, size: p.marketValue, colorValue: p.unrealizedPnlPct }));
  }

  // ---------------------------------------------------------------- allocation donut
  loadAlloc(): void { this.alloc.load(() => this.allocation.allocation(this.groupBy), true); }
  setGroup(v: string): void { this.groupBy = v as AllocationGroupBy; this.loadAlloc(); }
  private buildSlices(a: AllocationResponse): void {
    this.slices = (a.slices ?? []).filter(s => s.weightPct > 0).map(s => ({
      label: s.label || s.key, value: s.value, pct: s.weightPct, hatched: s.key === 'CASH'
    }));
  }

  // ---------------------------------------------------------------- cash operations (routes 21 and 22)
  setCashTab(v: string): void { this.cashTab = v as 'deposit' | 'withdraw'; this.cashError = ''; }
  get availableCash(): number | null { return this.store.value.summary?.availableCash ?? null; }

  submitCash(): void {
    this.cashForm.markAllAsTouched();
    if (this.cashForm.invalid || this.cashPending) return;
    this.cash$.next();
  }

  private runCash(): Observable<unknown> {
    this.cashPending = true;
    this.cashError = '';
    const amount = parseDecimal(this.cashForm.controls.amount.value) as number;
    const note = this.cashForm.controls.note.value.trim();
    const body = { amount, ...(note ? { note } : {}) };
    const call = this.cashTab === 'deposit' ? this.portfolio.deposit(body) : this.portfolio.withdraw(body);
    const verb = this.cashTab === 'deposit' ? 'Deposited' : 'Withdrew';
    return call.pipe(
      tap(() => {
        this.cashPending = false;
        this.toast.success(`${verb} ${fmtMoney(amount)}`);
        this.cashForm.reset({ amount: '', note: '' });
        this.store.refresh();
        this.curve?.reload();
      }),
      catchError((e: PortfolioApiError) => {
        this.cashPending = false;
        const rest = applyFieldErrors(this.cashForm, e.fieldErrors);
        this.cashError = rest.length ? rest.join(' ') : e.message; // e.g. "Insufficient funds…" shown inline, form kept
        this.toast.reportError(e);
        return EMPTY;
      }),
      map(() => undefined)
    );
  }

  // ---------------------------------------------------------------- reset (route 23)
  reset(): void {
    this.confirm.confirm({
      title: 'Reset your portfolio?',
      message: 'This deletes all your positions and transactions and sets your cash back to the initial virtual capital. It cannot be undone.',
      confirmLabel: 'Reset portfolio',
      danger: true,
      requireText: 'RESET'
    }).subscribe(ok => {
      if (!ok) return;
      this.resetPending = true;
      this.resetError = '';
      this.portfolio.reset().subscribe({
        next: () => { this.resetPending = false; this.toast.success('Portfolio reset'); this.store.refresh(); this.curve?.reload(); this.loadAlloc(); },
        error: (e: PortfolioApiError) => { this.resetPending = false; this.resetError = e.message; this.toast.reportError(e); } // "Cancel your open orders before resetting"
      });
    });
  }

  openPosition(ticker: string): void { this.selectedTicker = ticker; }
  trade(): void { this.ticket.open(); }

  hints = {
    netContributions: 'Money you added minus money you withdrew (initial virtual capital included).',
    realized: 'Profit or loss locked in by positions you sold.',
    unrealized: 'Profit or loss on shares you still hold, at the latest price.',
    totalPnl: 'Realized plus unrealized profit and loss.',
    fees: 'Total trading fees paid so far.',
    positions: 'Number of stocks you currently hold.',
    created: 'When this portfolio was created.'
  };
}
