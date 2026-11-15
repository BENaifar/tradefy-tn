import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { PORTFOLIO_TABS } from '../../../../shared/ui/util/module-tabs';
import { lsGet, lsSet } from '../../../../shared/ui/util/safe-storage';
import { Section } from '../../../../shared/ui/util/section';
import { ToastService } from '../../../../shared/ui/util/toast.service';
import { TradeTicketService } from '../../../../shared/ui/util/trade-ticket.service';
import { PerformanceResponse, PortfolioSummaryResponse, PositionResponse } from '../../models/portfolio.model';
import { PerformanceService } from '../../services/performance.service';
import { PortfolioStore, PortfolioVm } from '../../services/portfolio.store';
import { PositionService } from '../../services/position.service';

type SortKey = 'ticker' | 'quantity' | 'avgCost' | 'lastPrice' | 'marketValue' | 'unrealizedPnl' | 'unrealizedPnlPct' | 'dayChangePct' | 'weightPct' | 'holdingDays'
  | 'realizedPnl' | 'openedAt' | 'closedAt';
interface Prefs { tab: 'open' | 'closed'; sortKey: SortKey; sortDir: 'asc' | 'desc'; sector: string; }

@Component({
  selector: 'app-positions',
  templateUrl: './positions.component.html',
  styleUrls: ['./positions.component.scss']
})
export class PositionsComponent implements OnInit, OnDestroy {
  readonly tabs = PORTFOLIO_TABS;
  tab: 'open' | 'closed' = 'open';
  sortKey: SortKey = 'marketValue';
  sortDir: 'asc' | 'desc' = 'desc';
  sector = '';
  vm: PortfolioVm | null = null;
  closed: Section<PositionResponse[]>;
  stats: Section<PerformanceResponse>;
  selected: string | null = null;
  private subs = new Subscription();

  constructor(
    public store: PortfolioStore,
    private positions: PositionService,
    private performance: PerformanceService,
    private ticket: TradeTicketService,
    private route: ActivatedRoute,
    private router: Router,
    toast: ToastService
  ) {
    this.closed = new Section<PositionResponse[]>(toast);
    this.stats = new Section<PerformanceResponse>(toast);
  }

  ngOnInit(): void {
    const p = lsGet<Partial<Prefs>>('positions_prefs', {});
    this.tab = p.tab === 'closed' ? 'closed' : 'open';
    this.sortKey = p.sortKey ?? 'marketValue';
    this.sortDir = p.sortDir ?? 'desc';
    this.sector = p.sector ?? '';
    this.store.ensureLoaded();
    this.subs.add(this.store.vm$.subscribe(vm => (this.vm = vm)));
    this.closed.load(() => this.positions.list('CLOSED'));
    this.stats.load(() => this.performance.performance('ALL'));
    // `?position=TJARI` (from the trade ticket's "View position") opens that position's drawer.
    this.subs.add(this.route.queryParamMap.subscribe(q => { const t = q.get('position'); if (t) this.selected = t; }));
  }

  ngOnDestroy(): void { this.subs.unsubscribe(); this.closed.destroy(); this.stats.destroy(); }

  setTab(t: 'open' | 'closed'): void {
    this.tab = t;
    this.sortKey = t === 'open' ? 'marketValue' : 'closedAt';
    this.sortDir = 'desc';
    this.save();
  }

  get list(): PositionResponse[] {
    const src = (this.tab === 'open' ? this.vm?.positions : this.closed.data) ?? [];
    const dir = this.sortDir === 'asc' ? 1 : -1;
    const val = (p: PositionResponse): string | number => {
      const v = p[this.sortKey];
      return v === null || v === undefined ? (typeof v === 'string' ? '' : -Infinity) : (v as string | number);
    };
    return src.filter(p => !this.sector || p.sector === this.sector).sort((a, b) => {
      const x = val(a), y = val(b);
      return (typeof x === 'string' ? x.localeCompare(y as string) : x - (y as number)) * dir;
    });
  }

  get sectors(): string[] {
    const src = (this.tab === 'open' ? this.vm?.positions : this.closed.data) ?? [];
    return [...new Set(src.map(p => p.sector).filter(Boolean))].sort();
  }

  sortBy(k: SortKey): void {
    if (this.sortKey === k) this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    else { this.sortKey = k; this.sortDir = k === 'ticker' ? 'asc' : 'desc'; }
    this.save();
  }
  ariaSort(k: SortKey): 'ascending' | 'descending' | 'none' { return this.sortKey === k ? (this.sortDir === 'asc' ? 'ascending' : 'descending') : 'none'; }
  setSector(s: string): void { this.sector = s; this.save(); }

  open(t: string): void { this.selected = t; }
  closeDrawer(): void {
    this.selected = null;
    if (this.route.snapshot.queryParamMap.has('position')) void this.router.navigate([], { queryParams: { position: null }, queryParamsHandling: 'merge', replaceUrl: true });
  }
  trade(t: string, side: 'BUY' | 'SELL'): void { this.ticket.open({ ticker: t, side }); }
  summary(): PortfolioSummaryResponse | null { return this.vm?.summary ?? null; }
  trackId(_: number, p: PositionResponse): number { return p.id; }

  private save(): void {
    lsSet('positions_prefs', { tab: this.tab, sortKey: this.sortKey, sortDir: this.sortDir, sector: this.sector } satisfies Prefs);
  }
}
