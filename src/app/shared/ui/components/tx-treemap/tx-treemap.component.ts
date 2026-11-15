import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, EventEmitter, Input, NgZone, Output } from '@angular/core';
import { fmtNumber, fmtPct } from '../../util/format';
import { ResizeAware } from '../svg/resize-aware';

export interface TreemapItem { id?: string; label: string; size: number; colorValue: number; }
interface Tile { item: TreemapItem; x: number; y: number; w: number; h: number; }
interface Cell { item: TreemapItem; area: number; }

/** Squarified treemap: area = `size`, colour = `colorValue` (percent: green up, red down). Labels hidden on small tiles. */
@Component({
  selector: 'tx-treemap',
  templateUrl: './tx-treemap.component.html',
  styleUrls: ['./tx-treemap.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TxTreemapComponent extends ResizeAware {
  @Input() items: TreemapItem[] = [];
  @Input() height = 280;
  @Output() select = new EventEmitter<string>();

  constructor(host: ElementRef<HTMLElement>, zone: NgZone, cdr: ChangeDetectorRef) { super(host, zone, cdr); }

  get tiles(): Tile[] {
    const items = this.items.filter(i => i.size > 0).sort((a, b) => b.size - a.size);
    const total = items.reduce((a, i) => a + i.size, 0);
    if (!total) return [];
    const W = this.width, H = this.height;
    const cells: Cell[] = items.map(item => ({ item, area: (item.size / total) * W * H }));
    const out: Tile[] = [];
    let x = 0, y = 0, w = W, h = H;
    let rest = cells;
    const worst = (row: Cell[], side: number): number => {
      const s = row.reduce((a, c) => a + c.area, 0);
      const max = Math.max(...row.map(c => c.area)), min = Math.min(...row.map(c => c.area));
      return Math.max((side * side * max) / (s * s), (s * s) / (side * side * min));
    };
    while (rest.length) {
      const side = Math.min(w, h);
      const row = [rest[0]];
      let i = 1;
      while (i < rest.length && worst([...row, rest[i]], side) <= worst(row, side)) { row.push(rest[i]); i++; }
      const area = row.reduce((a, c) => a + c.area, 0);
      if (w >= h) {
        const cw = area / h; let cy = y;
        for (const c of row) { const ch = c.area / cw; out.push({ item: c.item, x, y: cy, w: cw, h: ch }); cy += ch; }
        x += cw; w -= cw;
      } else {
        const rh = area / w; let cx = x;
        for (const c of row) { const cw = c.area / rh; out.push({ item: c.item, x: cx, y, w: cw, h: rh }); cx += cw; }
        y += rh; h -= rh;
      }
      rest = rest.slice(row.length);
    }
    return out;
  }

  fill(v: number): string {
    const a = Math.min(0.8, 0.18 + (Math.min(Math.abs(v), 20) / 20) * 0.6);
    return v >= 0 ? `rgba(74, 222, 128, ${a})` : `rgba(248, 113, 113, ${a})`;
  }
  max0(v: number): number { return Math.max(0, v); }
  pct(v: number): string { return fmtPct(v); }
  num(v: number): string { return fmtNumber(v, 0); }
  get label(): string { return this.items.length ? 'Positions treemap: ' + this.items.map(i => `${i.label} ${fmtPct(i.colorValue)}`).join(', ') : 'No positions'; }
}
