import { Component, ElementRef, EventEmitter, HostListener, Input, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { Subject, Subscription, debounceTime, distinctUntilChanged } from 'rxjs';
import { QuoteResponse } from '../../../../features/market-data/models/market-data.model';
import { QuoteStore } from '../../../../features/market-data/services/quote.store';

/**
 * Autocomplete over instruments (the quotes held by QuoteStore), keyboard navigable.
 * With `navigate` (default) picking a result opens the instrument page; otherwise it only emits `picked`.
 */
@Component({
  selector: 'tx-ticker-search',
  templateUrl: './tx-ticker-search.component.html',
  styleUrls: ['./tx-ticker-search.component.scss']
})
export class TxTickerSearchComponent implements OnInit, OnDestroy {
  @Input() placeholder = 'Search ticker or name  ( / )';
  @Input() navigate = true;
  @Input() initial = '';
  @Input() compact = false;
  @Output() picked = new EventEmitter<string>();
  @ViewChild('box') box?: ElementRef<HTMLInputElement>;

  text = '';
  results: QuoteResponse[] = [];
  open = false;
  active = -1;
  private quotes: QuoteResponse[] = [];
  private typing = new Subject<string>();
  private subs = new Subscription();

  constructor(private store: QuoteStore, private router: Router, private host: ElementRef<HTMLElement>) {}

  ngOnInit(): void {
    this.text = this.initial;
    this.subs.add(this.store.all$.subscribe(q => { this.quotes = q; if (this.open) this.filter(this.text); }));
    this.subs.add(this.typing.pipe(debounceTime(250), distinctUntilChanged()).subscribe(t => this.filter(t)));
  }
  ngOnDestroy(): void { this.subs.unsubscribe(); }

  focus(): void { this.box?.nativeElement.focus(); this.box?.nativeElement.select(); }

  onInput(v: string): void {
    this.text = v.slice(0, 50);
    this.open = true;
    this.typing.next(this.text.trim());
  }

  private filter(t: string): void {
    const q = t.trim().toLowerCase();
    this.results = (q ? this.quotes.filter(x => x.ticker.toLowerCase().includes(q) || (x.name ?? '').toLowerCase().includes(q)) : this.quotes.slice(0, 8))
      .sort((a, b) => Number(b.ticker.toLowerCase().startsWith(q)) - Number(a.ticker.toLowerCase().startsWith(q)) || a.ticker.localeCompare(b.ticker))
      .slice(0, 8);
    this.active = this.results.length ? 0 : -1;
  }

  onFocus(): void { this.open = true; this.filter(this.text); }

  onKey(ev: KeyboardEvent): void {
    if (ev.key === 'ArrowDown') { ev.preventDefault(); this.open = true; this.active = Math.min(this.results.length - 1, this.active + 1); }
    else if (ev.key === 'ArrowUp') { ev.preventDefault(); this.active = Math.max(0, this.active - 1); }
    else if (ev.key === 'Enter') { ev.preventDefault(); const r = this.results[this.active]; if (r) this.pick(r); }
    else if (ev.key === 'Escape') { this.open = false; this.box?.nativeElement.blur(); }
  }

  pick(q: QuoteResponse): void {
    this.open = false;
    this.text = this.navigate ? '' : q.ticker;
    this.picked.emit(q.ticker);
    if (this.navigate) void this.router.navigate(['/markets', q.ticker]);
  }

  @HostListener('document:click', ['$event'])
  onDoc(ev: MouseEvent): void { if (!this.host.nativeElement.contains(ev.target as Node)) this.open = false; }
}
