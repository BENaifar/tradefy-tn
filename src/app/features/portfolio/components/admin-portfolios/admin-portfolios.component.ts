import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Section } from '../../../../shared/ui/util/section';
import { ToastService } from '../../../../shared/ui/util/toast.service';
import { AdminPortfolioRow, PortfolioSummaryResponse } from '../../models/portfolio.model';
import { PortfolioAdminService } from '../../services/portfolio-admin.service';

type SortKey = 'fullName' | 'email' | 'totalValue' | 'totalReturnPct' | 'openPositions' | 'lastActivity';

/** All portfolios inside the existing admin console: sortable and searchable; a row opens that user's summary (read-only). */
@Component({
  selector: 'app-admin-portfolios',
  templateUrl: './admin-portfolios.component.html',
  styleUrls: ['./admin-portfolios.component.scss']
})
export class AdminPortfoliosComponent implements OnInit, OnDestroy {
  list: Section<AdminPortfolioRow[]>;
  detail: Section<PortfolioSummaryResponse>;
  search = new FormControl<string>('', { nonNullable: true });
  sortKey: SortKey = 'totalValue';
  sortDir: 'asc' | 'desc' = 'desc';
  open: AdminPortfolioRow | null = null;

  constructor(private api: PortfolioAdminService, toast: ToastService) {
    this.list = new Section<AdminPortfolioRow[]>(toast);
    this.detail = new Section<PortfolioSummaryResponse>(toast);
  }

  ngOnInit(): void { this.list.load(() => this.api.portfolios()); }
  ngOnDestroy(): void { this.list.destroy(); this.detail.destroy(); }

  get rows(): AdminPortfolioRow[] {
    const q = this.search.value.trim().toLowerCase();
    const dir = this.sortDir === 'asc' ? 1 : -1;
    return (this.list.data ?? [])
      .filter(r => !q || r.fullName?.toLowerCase().includes(q) || r.email?.toLowerCase().includes(q))
      .sort((a, b) => {
        const x = a[this.sortKey] ?? '', y = b[this.sortKey] ?? '';
        return (typeof x === 'string' ? x.localeCompare(y as string) : (x as number) - (y as number)) * dir;
      });
  }

  sortBy(k: SortKey): void {
    if (this.sortKey === k) this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    else { this.sortKey = k; this.sortDir = k === 'fullName' || k === 'email' ? 'asc' : 'desc'; }
  }
  ariaSort(k: SortKey): 'ascending' | 'descending' | 'none' { return this.sortKey === k ? (this.sortDir === 'asc' ? 'ascending' : 'descending') : 'none'; }

  select(r: AdminPortfolioRow): void {
    this.open = r;
    this.detail.load(() => this.api.portfolio(r.userId));
  }
  trackUser(_: number, r: AdminPortfolioRow): number { return r.userId; }
}
