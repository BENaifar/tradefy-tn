import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { PortfolioAdminService } from '../../../portfolio/services/portfolio-admin.service';
import { ConfirmService } from '../../../../shared/ui/util/confirm.service';
import { isIsoDate, todayTunis } from '../../../../shared/ui/util/dates';
import { Section } from '../../../../shared/ui/util/section';
import { ToastService } from '../../../../shared/ui/util/toast.service';
import { CsvCheck, checkCsvFile } from '../../../../shared/ui/validators/tx-validators';
import {
  CsvImportResult, InstrumentResponse, MarketApiError, MarketStatusResponse, QuoteResponse, SyncResult
} from '../../models/market-data.model';
import { MarketAdminService } from '../../services/market-admin.service';
import { MarketDataService } from '../../services/market-data.service';

/** Market operations inside the existing admin console (no new admin page): status, sync, close session, snapshots, CSV import, instrument edit. */
@Component({
  selector: 'app-admin-market-ops',
  templateUrl: './admin-market-ops.component.html',
  styleUrls: ['./admin-market-ops.component.scss']
})
export class AdminMarketOpsComponent implements OnInit, OnDestroy {
  status: Section<MarketStatusResponse>;
  quotes: Section<QuoteResponse[]>;
  filter = new FormControl<string>('', { nonNullable: true });
  readonly today = todayTunis();

  // actions
  busy = { sync: false, close: false, snap: false, csv: false, save: false };
  syncResult = '';
  closeResult = '';
  snapResult = '';
  actionError = '';
  snapDate = new FormControl<string>('', { nonNullable: true });

  // CSV
  file: File | null = null;
  check: CsvCheck | null = null;
  csvResult: CsvImportResult | null = null;
  csvError = '';
  dragging = false;

  // instrument edit
  editing: InstrumentResponse | null = null;
  sectors: string[] = [];
  editForm = new FormGroup({
    sector: new FormControl<string>('', { nonNullable: true }),
    active: new FormControl<boolean>(true, { nonNullable: true })
  });
  editError = '';

  constructor(
    private market: MarketDataService,
    private admin: MarketAdminService,
    private portfolioAdmin: PortfolioAdminService,
    private confirm: ConfirmService,
    private toast: ToastService
  ) {
    this.status = new Section<MarketStatusResponse>(toast);
    this.quotes = new Section<QuoteResponse[]>(toast);
  }

  ngOnInit(): void {
    this.status.load(() => this.market.status());
    this.quotes.load(() => this.market.quotes());
    this.market.sectors().subscribe({ next: s => (this.sectors = s ?? []), error: () => undefined });
  }
  ngOnDestroy(): void { this.status.destroy(); this.quotes.destroy(); }

  get rows(): QuoteResponse[] {
    const q = this.filter.value.trim().toLowerCase();
    return (this.quotes.data ?? []).filter(x => !q || x.ticker.toLowerCase().includes(q) || (x.name ?? '').toLowerCase().includes(q));
  }

  // ---------------------------------------------------------------- sync / close session / snapshots
  syncNow(): void {
    this.busy.sync = true; this.actionError = '';
    this.admin.syncNow().subscribe({
      next: (r: SyncResult) => {
        this.busy.sync = false;
        this.syncResult = `Received ${r.received} quotes: ${r.updated} updated, ${r.newInstruments} new instruments (${r.provider}).`;
        this.toast.success('Market synced');
        this.status.reload();
        this.quotes.reload();
      },
      error: (e: MarketApiError) => this.fail('sync', e)
    });
  }

  closeSession(): void {
    this.confirm.confirm({
      title: 'Close the session?', message: "This writes today's daily bars from the latest quotes. Run it once, after the market has closed.", confirmLabel: 'Close session'
    }).subscribe(ok => {
      if (!ok) return;
      this.busy.close = true; this.actionError = '';
      this.admin.closeSession().subscribe({
        next: () => { this.busy.close = false; this.closeResult = "Session closed: today's daily bars were written."; this.toast.success('Session closed'); },
        error: (e: MarketApiError) => this.fail('close', e)
      });
    });
  }

  takeSnapshots(): void {
    const d = this.snapDate.value;
    if (d && (!isIsoDate(d) || d > this.today)) { this.actionError = 'Pick a date that is not in the future.'; return; }
    this.busy.snap = true; this.actionError = '';
    this.portfolioAdmin.takeSnapshots(d || undefined).subscribe({
      next: n => {
        this.busy.snap = false;
        this.snapResult = `Snapshots taken for ${n} portfolio${n === 1 ? '' : 's'} (${d || 'today'}).`;
        this.toast.success('Snapshots taken');
      },
      error: (e: { kind: string; message: string }) => { this.busy.snap = false; this.actionError = e.message; this.toast.reportError(e); }
    });
  }

  private fail(k: 'sync' | 'close', e: MarketApiError): void {
    this.busy[k] = false;
    this.actionError = e.message;
    this.toast.reportError(e);
  }

  // ---------------------------------------------------------------- CSV import (route 19)
  onFile(ev: Event): void { const f = (ev.target as HTMLInputElement).files?.[0]; if (f) void this.pick(f); }
  onDrop(ev: DragEvent): void { ev.preventDefault(); this.dragging = false; const f = ev.dataTransfer?.files?.[0]; if (f) void this.pick(f); }
  onDrag(ev: DragEvent, on: boolean): void { ev.preventDefault(); this.dragging = on; }

  private async pick(f: File): Promise<void> {
    this.file = f; this.csvResult = null; this.csvError = '';
    this.check = await checkCsvFile(f);
    if (!this.check.ok) this.file = null;
  }
  clearFile(): void { this.file = null; this.check = null; this.csvResult = null; this.csvError = ''; }

  upload(): void {
    if (!this.file || !this.check?.ok || this.busy.csv) return;
    this.busy.csv = true; this.csvError = '';
    this.admin.importCsv(this.file).subscribe({
      next: r => { this.busy.csv = false; this.csvResult = r; this.toast.success(`Imported ${r.inserted + r.updated} of ${r.totalRows} rows`); },
      error: (e: MarketApiError) => { this.busy.csv = false; this.csvError = e.message; this.toast.reportError(e); }
    });
  }

  copyErrors(): void {
    const text = (this.csvResult?.errors ?? []).join('\n');
    void navigator.clipboard?.writeText(text).then(() => this.toast.success('Errors copied'), () => this.toast.error('Could not copy'));
  }

  // ---------------------------------------------------------------- edit instrument (route 16)
  pickInstrument(t: string): void {
    this.editError = '';
    this.market.instrument(t).subscribe({
      next: i => { this.editing = i; this.editForm.reset({ sector: i.sector ?? '', active: i.active }); },
      error: (e: MarketApiError) => { this.editing = null; this.editError = e.message; }
    });
  }
  saveInstrument(): void {
    if (!this.editing || this.busy.save) return;
    this.busy.save = true; this.editError = '';
    const v = this.editForm.getRawValue();
    this.admin.updateInstrument(this.editing.ticker, { sector: v.sector.trim() || undefined, active: v.active }).subscribe({
      next: i => { this.busy.save = false; this.editing = i; this.toast.success(`${i.ticker} updated`); this.market.sectors().subscribe(s => (this.sectors = s)); },
      error: (e: MarketApiError) => { this.busy.save = false; this.editError = e.message; this.toast.reportError(e); }
    });
  }
  trackTicker(_: number, q: QuoteResponse): string { return q.ticker; }
}
