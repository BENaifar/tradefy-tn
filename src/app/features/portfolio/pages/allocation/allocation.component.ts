import { Component, OnDestroy, OnInit } from '@angular/core';
import { AbstractControl, FormArray, FormControl, FormGroup, ValidationErrors } from '@angular/forms';
import { EMPTY, Subscription, catchError, concatMap, from, tap, throwError } from 'rxjs';
import { DonutSlice } from '../../../../shared/ui/components/tx-donut/tx-donut.component';
import { SegmentOption } from '../../../../shared/ui/components/tx-segmented/tx-segmented.component';
import { ConfirmService } from '../../../../shared/ui/util/confirm.service';
import { controlError } from '../../../../shared/ui/util/form-errors';
import { fmtMoney, fmtNumber } from '../../../../shared/ui/util/format';
import { PORTFOLIO_TABS } from '../../../../shared/ui/util/module-tabs';
import { Section } from '../../../../shared/ui/util/section';
import { ToastService } from '../../../../shared/ui/util/toast.service';
import { decimalValidator, parseDecimal } from '../../../../shared/ui/validators/tx-validators';
import { MarketDataService } from '../../../market-data/services/market-data.service';
import { QuoteStore } from '../../../market-data/services/quote.store';
import {
  AllocationGroupBy, AllocationResponse, AllocationSlice, AllocationTargetRequest, AllocationTargetResponse, PortfolioApiError,
  RebalanceSuggestion, TargetType
} from '../../models/portfolio.model';
import { AllocationService } from '../../services/allocation.service';
import { PortfolioService } from '../../services/portfolio.service';
import { PortfolioStore } from '../../services/portfolio.store';

type RowGroup = FormGroup<{ key: FormControl<string>; percent: FormControl<string> }>;
interface Step { s: RebalanceSuggestion; status: 'pending' | 'running' | 'ok' | 'error' | 'skipped'; error: string; }

const UNCLASSIFIED = 'Non classé';

function uniqueKeys(arr: AbstractControl): ValidationErrors | null {
  const keys = (arr as FormArray<RowGroup>).controls.map(g => g.controls.key.value).filter(Boolean);
  return new Set(keys).size === keys.length ? null : { dup: { message: 'Each key can only appear once.' } };
}

/** See (current allocation) → Set targets → Rebalance. */
@Component({
  selector: 'app-allocation',
  templateUrl: './allocation.component.html',
  styleUrls: ['./allocation.component.scss']
})
export class AllocationComponent implements OnInit, OnDestroy {
  readonly tabs = PORTFOLIO_TABS;
  readonly groupOptions: SegmentOption[] = [
    { value: 'INSTRUMENT', label: 'Instrument' }, { value: 'SECTOR', label: 'Sector' }, { value: 'ASSET_CLASS', label: 'Asset class' }
  ];
  readonly typeOptions: SegmentOption[] = [{ value: 'INSTRUMENT', label: 'Instrument' }, { value: 'SECTOR', label: 'Sector' }];
  readonly controlError = controlError;
  readonly fmtMoney = fmtMoney;

  // ---- 1. current allocation
  groupBy: AllocationGroupBy = 'SECTOR';
  alloc: Section<AllocationResponse>;
  slices: DonutSlice[] = [];

  // ---- 2. targets
  targets: Section<AllocationTargetResponse[]>;
  targetType: TargetType = 'INSTRUMENT';
  savedType: TargetType | null = null;
  rows = new FormArray<RowGroup>([], { validators: [uniqueKeys] });
  instrumentKeys: string[] = [];
  sectorKeys: string[] = [];
  saving = false;
  saveError = '';
  loadingCurrent = false;

  // ---- 3. rebalance
  suggestions: Section<RebalanceSuggestion[]>;
  selected = new Set<string>();
  steps: Step[] = [];
  running = false;
  private subs = new Subscription();
  private runSub?: Subscription;

  constructor(
    private api: AllocationService,
    private portfolio: PortfolioService,
    private store: PortfolioStore,
    private market: MarketDataService,
    private quotes: QuoteStore,
    private confirm: ConfirmService,
    private toast: ToastService
  ) {
    this.alloc = new Section<AllocationResponse>(toast, a => this.buildSlices(a));
    this.targets = new Section<AllocationTargetResponse[]>(toast, t => this.fillFromTargets(t));
    this.suggestions = new Section<RebalanceSuggestion[]>(toast, s => { this.selected = new Set(s.map(x => x.ticker)); this.steps = []; });
  }

  ngOnInit(): void {
    this.store.ensureLoaded();
    this.loadAlloc();
    this.targets.load(() => this.api.targets());
    this.subs.add(this.quotes.all$.subscribe(q => (this.instrumentKeys = q.map(x => x.ticker))));
    this.market.sectors().subscribe({ next: s => (this.sectorKeys = [...new Set([...(s ?? []), UNCLASSIFIED])].sort()), error: () => (this.sectorKeys = [UNCLASSIFIED]) });
    this.subs.add(this.rows.valueChanges.subscribe(() => this.saveError = ''));
  }
  ngOnDestroy(): void { this.subs.unsubscribe(); this.runSub?.unsubscribe(); this.alloc.destroy(); this.targets.destroy(); this.suggestions.destroy(); }

  // ============================================================ 1. current allocation
  loadAlloc(): void { this.alloc.load(() => this.api.allocation(this.groupBy), true); }
  setGroup(v: string): void { this.groupBy = v as AllocationGroupBy; this.loadAlloc(); }
  private buildSlices(a: AllocationResponse): void {
    this.slices = (a.slices ?? []).filter(s => s.weightPct > 0).map(s => ({ label: s.label || s.key, value: s.value, pct: s.weightPct, hatched: s.key === 'CASH' }));
  }
  isCash(s: AllocationSlice): boolean { return s.key === 'CASH'; }
  hasTarget(s: AllocationSlice): boolean { return s.targetPct !== null && s.targetPct !== undefined; }

  // ============================================================ 2. targets editor
  private fillFromTargets(t: AllocationTargetResponse[]): void {
    this.rows.clear();
    this.savedType = t.length ? t[0].type : null;
    this.targetType = this.savedType ?? this.targetType;
    t.forEach(x => this.addRow(x.key, String(x.percent)));
  }

  private row(key = '', percent = ''): RowGroup {
    return new FormGroup({
      key: new FormControl<string>(key, { nonNullable: true, validators: [c => (c.value ? null : { required: { message: 'Choose a key.' } })] }),
      percent: new FormControl<string>(percent, { nonNullable: true, validators: [decimalValidator({ min: 0, exclusiveMin: true, max: 100, maxDecimals: 2, required: true })] })
    });
  }
  addRow(key = '', percent = ''): void { this.rows.push(this.row(key, percent)); }
  removeRow(i: number): void { this.rows.removeAt(i); }

  setType(v: string): void {
    const next = v as TargetType;
    if (next === this.targetType) return;
    if (!this.rows.length) { this.targetType = next; return; }
    this.confirm.confirm({
      title: 'Switch target type?', message: 'Targets of one type cannot be mixed with the other. Switching clears the rows you are editing.', confirmLabel: 'Clear and switch'
    }).subscribe(ok => { if (ok) { this.rows.clear(); this.targetType = next; this.saveError = ''; } });
  }

  get options(): string[] { return this.targetType === 'INSTRUMENT' ? this.instrumentKeys : this.sectorKeys; }
  /** Keys already used in other rows are filtered out of each row's choices. */
  optionsFor(i: number): string[] {
    const mine = this.rows.at(i).controls.key.value;
    const used = new Set(this.rows.controls.map(g => g.controls.key.value).filter(k => k && k !== mine));
    return this.options.filter(o => !used.has(o));
  }
  get canAddRow(): boolean { return this.rows.length < this.options.length; }

  get sum(): number {
    return this.rows.controls.reduce((a, g) => a + (parseDecimal(g.controls.percent.value) ?? 0), 0);
  }
  get sumRounded(): number { return Math.round(this.sum * 100) / 100; }
  get cashLeft(): number { return Math.max(0, 100 - this.sumRounded); }
  get over(): boolean { return this.sumRounded > 100; }
  get gaugeLabel(): string { return `${fmtNumber(this.sumRounded, 2)} % allocated · ${fmtNumber(this.cashLeft, 2)} % stays in cash`; }
  get saveBlockedReason(): string {
    if (this.over) return `Targets add up to ${fmtNumber(this.sumRounded, 2)} %: the total cannot exceed 100 %.`;
    if (this.rows.invalid) return this.rows.errors?.['dup'] ? 'Each key can only appear once.' : 'Fix the highlighted rows first.';
    return '';
  }

  useCurrent(): void {
    if (this.loadingCurrent) return;
    this.loadingCurrent = true;
    this.api.allocation(this.targetType).subscribe({
      next: a => {
        this.loadingCurrent = false;
        this.rows.clear();
        (a.slices ?? []).filter(s => s.key !== 'CASH' && s.weightPct > 0)
          .forEach(s => this.addRow(s.key, String(Math.round(s.weightPct * 100) / 100)));
        if (!this.rows.length) this.toast.info('You hold nothing of this type yet.');
      },
      error: (e: PortfolioApiError) => { this.loadingCurrent = false; this.toast.reportError(e); this.saveError = e.message; }
    });
  }

  save(): void {
    this.rows.markAllAsTouched();
    if (this.saveBlockedReason || this.saving) return;
    this.saving = true;
    this.saveError = '';
    const body: AllocationTargetRequest[] = this.rows.controls.map(g => ({
      type: this.targetType, key: g.controls.key.value, percent: parseDecimal(g.controls.percent.value) as number
    }));
    this.api.saveTargets(body).subscribe({
      next: saved => {
        this.saving = false;
        this.toast.success('Targets saved');
        this.targets.data = saved;
        this.savedType = saved.length ? saved[0].type : null;
        this.loadAlloc();
        this.suggestions.data = null;
      },
      error: (e: PortfolioApiError) => {
        this.saving = false;
        const fields = Object.entries(e.fieldErrors ?? {}).map(([k, v]) => `${k}: ${v}`);
        this.saveError = fields.length ? `${e.message} ${fields.join(' ')}` : e.message; // backend 400 shown inline
        this.toast.reportError(e);
      }
    });
  }

  // ============================================================ 3. rebalance (INSTRUMENT targets only)
  get canRebalance(): boolean { return this.savedType === 'INSTRUMENT'; }
  compute(): void { this.suggestions.load(() => this.api.rebalance()); }

  toggleSel(t: string): void { if (this.selected.has(t)) this.selected.delete(t); else this.selected.add(t); }
  get chosen(): RebalanceSuggestion[] { return (this.suggestions.data ?? []).filter(s => this.selected.has(s.ticker)); }
  get buyTotal(): number { return this.chosen.filter(s => s.side === 'BUY').reduce((a, s) => a + s.estimatedAmount, 0); }
  get sellTotal(): number { return this.chosen.filter(s => s.side === 'SELL').reduce((a, s) => a + s.estimatedAmount, 0); }

  /** Executes the selected suggestions one at a time: sells first, then buys. Stops on the first error. */
  executeSelected(): void {
    const sells = this.chosen.filter(s => s.side === 'SELL');
    const buys = this.chosen.filter(s => s.side === 'BUY');
    const queue = [...sells, ...buys];
    if (!queue.length || this.running) return;
    this.confirm.confirm({
      title: 'Execute these trades?',
      message: `${sells.length} sell${sells.length === 1 ? '' : 's'} (about ${fmtMoney(this.sellTotal)}) will run first, then ${buys.length} buy${buys.length === 1 ? '' : 's'} (about ${fmtMoney(this.buyTotal)}). They are simulated instant fills at the current price, one at a time; the run stops at the first failure.`,
      confirmLabel: 'Execute'
    }).subscribe(ok => { if (ok) this.run(queue); });
  }

  private run(queue: RebalanceSuggestion[]): void {
    this.steps = queue.map(s => ({ s, status: 'pending', error: '' }));
    this.running = true;
    this.runSub = from(this.steps).pipe(
      concatMap(step => {
        step.status = 'running';
        return this.portfolio.execute({ ticker: step.s.ticker, side: step.s.side, quantity: step.s.quantity }).pipe(
          tap(() => (step.status = 'ok')),
          catchError((e: PortfolioApiError) => { step.status = 'error'; step.error = e.message; return throwError(() => e); })
        );
      })
    ).subscribe({
      complete: () => this.finish(true),
      error: () => { this.steps.filter(x => x.status === 'pending').forEach(x => (x.status = 'skipped')); this.finish(false); }
    });
  }

  private finish(ok: boolean): void {
    this.running = false;
    if (ok) this.toast.success('Rebalance executed'); else this.toast.error('Rebalance stopped at the first error');
    this.store.refresh();
    this.loadAlloc();
    this.suggestions.data = null;
  }

  tr(_: number, s: RebalanceSuggestion): string { return s.ticker; }
  trSlice(_: number, s: AllocationSlice): string { return s.key; }
}
