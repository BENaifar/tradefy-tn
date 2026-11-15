import { Component, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges } from '@angular/core';
import { ChartTrade } from '../../../market-data/components/candle-chart/candle-chart.component';
import { CandleSeriesResponse } from '../../../market-data/models/market-data.model';
import { CandleService } from '../../../market-data/services/candle.service';
import { addMonths, todayTunis } from '../../../../shared/ui/util/dates';
import { Section } from '../../../../shared/ui/util/section';
import { ToastService } from '../../../../shared/ui/util/toast.service';
import { TradeTicketService } from '../../../../shared/ui/util/trade-ticket.service';
import { PositionDetailResponse } from '../../models/portfolio.model';
import { PositionService } from '../../services/position.service';

/** Right drawer with one position's fields, a mini chart with the user's trades, and its transactions (route 27). */
@Component({
  selector: 'app-position-drawer',
  templateUrl: './position-drawer.component.html',
  styleUrls: ['./position-drawer.component.scss']
})
export class PositionDrawerComponent implements OnChanges, OnDestroy {
  /** The ticker to show; null closes the drawer. */
  @Input() ticker: string | null = null;
  @Output() closed = new EventEmitter<void>();

  detail: Section<PositionDetailResponse>;
  daily: Section<CandleSeriesResponse>;
  trades: ChartTrade[] = [];
  fitToken = 0;

  constructor(
    private positions: PositionService,
    private candles: CandleService,
    private ticket: TradeTicketService,
    toast: ToastService
  ) {
    this.detail = new Section<PositionDetailResponse>(toast, d => {
      this.trades = d.transactions
        .filter(x => (x.type === 'BUY' || x.type === 'SELL') && x.quantity && x.price !== null)
        .map(x => ({ time: Math.floor(Date.parse(x.executedAt) / 1000), side: x.type as 'BUY' | 'SELL', quantity: x.quantity as number, price: x.price as number }));
    });
    this.daily = new Section<CandleSeriesResponse>(toast, () => this.fitToken++);
  }

  ngOnChanges(c: SimpleChanges): void {
    if (c['ticker'] && this.ticker) {
      const t = this.ticker;
      this.trades = [];
      this.detail.load(() => this.positions.detail(t));
      const to = todayTunis();
      this.daily.load(() => this.candles.candles(t, 'D1', addMonths(to, -6), to));
    }
  }

  ngOnDestroy(): void { this.detail.destroy(); this.daily.destroy(); }

  trade(side: 'BUY' | 'SELL'): void { if (this.ticker) this.ticket.open({ ticker: this.ticker, side }); }
}
