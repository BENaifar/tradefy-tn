import { Component, OnDestroy, OnInit } from '@angular/core';
import { DonutSlice } from '../../../../shared/ui/components/tx-donut/tx-donut.component';
import { Section } from '../../../../shared/ui/util/section';
import { AllocationResponse, PageResponse, TransactionResponse } from '../../models/portfolio.model';
import { AllocationService } from '../../services/allocation.service';
import { PortfolioService } from '../../services/portfolio.service';

/** Dashboard modules row: allocation mini-donut by sector, top movers, and the last 5 transactions. */
@Component({
  selector: 'app-dashboard-modules-row',
  templateUrl: './dashboard-modules-row.component.html',
  styleUrls: ['./dashboard-modules-row.component.scss']
})
export class DashboardModulesRowComponent implements OnInit, OnDestroy {
  alloc: Section<AllocationResponse>;
  txs: Section<PageResponse<TransactionResponse>>;
  slices: DonutSlice[] = [];

  constructor(private allocation: AllocationService, private portfolio: PortfolioService) {
    this.alloc = new Section<AllocationResponse>(undefined, a => {
      this.slices = (a.slices ?? []).filter(s => s.weightPct > 0).map(s => ({ label: s.label || s.key, value: s.value, pct: s.weightPct, hatched: s.key === 'CASH' }));
    });
    this.txs = new Section<PageResponse<TransactionResponse>>();
  }

  ngOnInit(): void { this.load(); }
  ngOnDestroy(): void { this.alloc.destroy(); this.txs.destroy(); }

  load(): void {
    this.alloc.load(() => this.allocation.allocation('SECTOR'));
    this.txs.load(() => this.portfolio.transactions({ page: 0, size: 5 }));
  }
  badge(t: string): string { return t.replace('_', ' '); }
}
