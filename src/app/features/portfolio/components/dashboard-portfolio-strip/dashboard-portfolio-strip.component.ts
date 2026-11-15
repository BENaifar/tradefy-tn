import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Section } from '../../../../shared/ui/util/section';
import { TradeTicketService } from '../../../../shared/ui/util/trade-ticket.service';
import { PerformanceResponse } from '../../models/portfolio.model';
import { PerformanceService } from '../../services/performance.service';
import { PortfolioStore } from '../../services/portfolio.store';

/** Four portfolio metric cards for the dashboard Home tab; a friendly first-steps panel for a first-time user. */
@Component({
  selector: 'app-dashboard-portfolio-strip',
  templateUrl: './dashboard-portfolio-strip.component.html',
  styleUrls: ['./dashboard-portfolio-strip.component.scss']
})
export class DashboardPortfolioStripComponent implements OnInit, OnDestroy {
  month: Section<PerformanceResponse>;

  constructor(public store: PortfolioStore, private performance: PerformanceService, private router: Router, private ticket: TradeTicketService) {
    this.month = new Section<PerformanceResponse>();
  }

  ngOnInit(): void {
    this.store.ensureLoaded();
    this.month.load(() => this.performance.performance('M1'), true);
  }
  ngOnDestroy(): void { this.month.destroy(); }

  get spark(): number[] { return (this.month.data?.points ?? []).map(p => p.totalValue); }
  go(path: string): void { void this.router.navigate([path]); }
  trade(): void { void this.router.navigate(['/portfolio'], { queryParams: { trade: 'open' } }); }
}
