import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ValidationErrors, ValidatorFn } from '@angular/forms';
import { indicatorLabel } from '../../models/indicator-label';
import { IndicatorDefinition, IndicatorInstance, IndicatorParam } from '../../models/market-data.model';

export type PresetName = 'Trend' | 'Momentum' | 'Volatility';
export interface IndicatorAdd { code: string; params: Record<string, number>; }
export interface IndicatorUpdate { id: string; params: Record<string, number>; }

function paramValidator(p: IndicatorParam): ValidatorFn {
  return (c: AbstractControl): ValidationErrors | null => {
    const raw = c.value;
    if (raw === null || raw === undefined || String(raw).trim() === '') return { msg: 'Required.' };
    const n = Number(String(raw).replace(',', '.'));
    if (!Number.isFinite(n)) return { msg: 'Enter a number.' };
    if (p.type === 'INT' && !Number.isInteger(n)) return { msg: 'Whole numbers only.' };
    if (n < p.min || n > p.max) return { msg: `Between ${p.min} and ${p.max}.` };
    return null;
  };
}

/**
 * The indicator picker. Built only from the backend catalog (`GET /api/market/indicators`): names, descriptions,
 * parameters, min / max and placement all come from the definitions, so a new backend indicator appears with no change here.
 */
@Component({
  selector: 'app-indicator-panel',
  templateUrl: './indicator-panel.component.html',
  styleUrls: ['./indicator-panel.component.scss'],
  changeDetection: ChangeDetectionStrategy.Default
})
export class IndicatorPanelComponent implements OnChanges {
  @Input() catalog: IndicatorDefinition[] | null = null;
  @Input() catalogLoading = false;
  @Input() catalogError = '';
  @Input() instances: IndicatorInstance[] = [];
  readonly maxIndicators = 8;
  readonly maxPanes = 3;

  @Output() add = new EventEmitter<IndicatorAdd>();
  @Output() update = new EventEmitter<IndicatorUpdate>();
  @Output() remove = new EventEmitter<string>();
  @Output() toggle = new EventEmitter<string>();
  @Output() recolor = new EventEmitter<{ id: string; color: string }>();
  @Output() hideAll = new EventEmitter<void>();
  @Output() showAll = new EventEmitter<void>();
  @Output() clear = new EventEmitter<void>();
  @Output() preset = new EventEmitter<PresetName>();
  @Output() retryCatalog = new EventEmitter<void>();

  readonly presets: PresetName[] = ['Trend', 'Momentum', 'Volatility'];
  picking = false;
  query = '';
  selected: IndicatorDefinition | null = null;
  editingId: string | null = null;
  form = new FormGroup<Record<string, FormControl<string>>>({});

  ngOnChanges(c: SimpleChanges): void {
    if (c['instances'] && this.editingId && !this.instances.some(i => i.id === this.editingId)) this.cancel();
  }

  get paneCount(): number { return this.instances.filter(i => this.def(i.code)?.placement === 'PANE').length; }
  get atLimit(): boolean { return this.instances.length >= this.maxIndicators; }
  get anyVisible(): boolean { return this.instances.some(i => i.visible); }
  get anyHidden(): boolean { return this.instances.some(i => !i.visible); }

  def(code: string): IndicatorDefinition | undefined { return this.catalog?.find(d => d.code === code); }
  label(i: IndicatorInstance): string { return indicatorLabel(this.def(i.code), i.code, i.params); }

  get filtered(): IndicatorDefinition[] {
    const q = this.query.trim().toLowerCase();
    return (this.catalog ?? []).filter(d => !q || `${d.name} ${d.code} ${d.description}`.toLowerCase().includes(q));
  }
  get overlays(): IndicatorDefinition[] { return this.filtered.filter(d => d.placement === 'OVERLAY'); }
  get panes(): IndicatorDefinition[] { return this.filtered.filter(d => d.placement === 'PANE'); }
  blocked(d: IndicatorDefinition): boolean { return d.placement === 'PANE' && this.paneCount >= this.maxPanes; }

  startPick(): void { this.picking = true; this.selected = null; this.editingId = null; this.query = ''; }

  choose(d: IndicatorDefinition): void {
    if (this.blocked(d)) return;
    this.selected = d;
    this.buildForm(d, {});
  }

  edit(i: IndicatorInstance): void {
    const d = this.def(i.code);
    if (!d) return;
    this.picking = false;
    this.selected = d;
    this.editingId = i.id;
    this.buildForm(d, i.params);
  }

  cancel(): void { this.picking = false; this.selected = null; this.editingId = null; }

  private buildForm(d: IndicatorDefinition, values: Record<string, number>): void {
    const g = new FormGroup<Record<string, FormControl<string>>>({});
    for (const p of d.params) {
      g.addControl(p.name, new FormControl<string>(String(values[p.name] ?? p.defaultValue), { nonNullable: true, validators: [paramValidator(p)] }));
    }
    this.form = g;
  }

  step(p: IndicatorParam): number { return p.type === 'INT' ? 1 : 0.1; }
  err(p: IndicatorParam): string {
    const c = this.form.controls[p.name];
    const e = c?.errors?.['msg'];
    return c && c.dirty && e ? String(e) : '';
  }

  /** The cross-parameter rules (e.g. MACD fast < slow) come back from the backend as a 400 on the row. */
  submit(): void {
    if (!this.selected || this.form.invalid) { this.form.markAllAsTouched(); return; }
    const params: Record<string, number> = {};
    for (const p of this.selected.params) params[p.name] = Number(String(this.form.controls[p.name].value).replace(',', '.'));
    if (this.editingId) this.update.emit({ id: this.editingId, params });
    else this.add.emit({ code: this.selected.code, params });
    this.cancel();
  }

  trackId(_: number, i: IndicatorInstance): string { return i.id; }
}
