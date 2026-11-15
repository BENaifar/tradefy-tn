import { Component, HostListener, Input, OnDestroy, OnInit, ViewChild, ViewContainerRef } from '@angular/core';
import { Subscription } from 'rxjs';
import { TradeTicketComponent } from '../../../../features/portfolio/components/trade-ticket/trade-ticket.component';
import { TradeTicketService } from '../../util/trade-ticket.service';
import { TxTickerSearchComponent } from '../tx-ticker-search/tx-ticker-search.component';

/**
 * Top of every page of ours: kicker, h1, ticker search, market status, live badge and the Trade button.
 * It also hosts the toast stack, the confirm dialog and (created on first use) the trade ticket drawer,
 * so no change to app.component.html is needed and every page can open the ticket.
 * Keyboard (our pages only): `/` focuses the search, `T` opens the trade ticket.
 */
@Component({
  selector: 'tx-module-header',
  templateUrl: './tx-module-header.component.html',
  styleUrls: ['./tx-module-header.component.scss']
})
export class TxModuleHeaderComponent implements OnInit, OnDestroy {
  @Input() kicker = '';
  @Input() title = '';
  @ViewChild(TxTickerSearchComponent) search?: TxTickerSearchComponent;
  @ViewChild('ticketHost', { read: ViewContainerRef, static: true }) ticketHost!: ViewContainerRef;
  private sub?: Subscription;
  private created = false;

  constructor(private ticket: TradeTicketService) {}

  ngOnInit(): void {
    this.sub = this.ticket.state$.subscribe(s => {
      if (s && !this.created) {
        this.created = true;
        this.ticketHost.createComponent(TradeTicketComponent);
      }
    });
  }
  ngOnDestroy(): void { this.sub?.unsubscribe(); this.ticket.close(); }

  trade(): void { this.ticket.open(); }

  @HostListener('document:keydown', ['$event'])
  onKey(ev: KeyboardEvent): void {
    if (ev.ctrlKey || ev.metaKey || ev.altKey) return;
    const t = ev.target as HTMLElement | null;
    const typing = !!t && (['INPUT', 'TEXTAREA', 'SELECT'].includes(t.tagName) || t.isContentEditable);
    if (typing) return;
    if (ev.key === '/') { ev.preventDefault(); this.search?.focus(); }
    else if (ev.key === 't' || ev.key === 'T') { if (!this.ticket.isOpen) { ev.preventDefault(); this.ticket.open(); } }
  }
}
