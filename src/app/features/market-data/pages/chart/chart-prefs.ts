import { IndicatorDefinition } from '../../models/market-data.model';

export interface SavedIndicator { code: string; params: Record<string, number>; color: string; visible: boolean; }

export const MAX_INDICATORS = 8;
export const MAX_PANES = 3;
export const PALETTE = ['#f4f6f8', '#8fb3ff', '#d0a2f7', '#c7cbd1', '#9aa0a8', '#666c75', '#e5c07b', '#4ade80'];

/**
 * Validates saved indicators against the catalog on load: unknown codes are dropped, out-of-range / non-integer / non-numeric
 * params are dropped (the catalog default is used instead), colours must be #rrggbb, and the 8-indicator / 3-pane limits apply.
 */
export function validateSavedIndicators(saved: unknown, catalog: IndicatorDefinition[]): SavedIndicator[] {
  if (!Array.isArray(saved)) return [];
  const out: SavedIndicator[] = [];
  let panes = 0;
  for (const raw of saved) {
    const s = (raw ?? {}) as Partial<SavedIndicator>;
    const def = catalog.find(d => d.code === s.code);
    if (!def || out.length >= MAX_INDICATORS) continue;
    if (def.placement === 'PANE' && panes >= MAX_PANES) continue;
    const params: Record<string, number> = {};
    for (const p of def.params) {
      const v = s.params?.[p.name];
      const ok = typeof v === 'number' && Number.isFinite(v) && v >= p.min && v <= p.max && (p.type !== 'INT' || Number.isInteger(v));
      params[p.name] = ok ? v : p.defaultValue;
    }
    const used = new Set(out.map(o => o.color.toLowerCase()));
    const color = typeof s.color === 'string' && /^#[0-9a-f]{6}$/i.test(s.color)
      ? s.color
      : (PALETTE.find(c => !used.has(c)) ?? PALETTE[out.length % PALETTE.length]);
    if (def.placement === 'PANE') panes++;
    out.push({ code: def.code, params, color, visible: s.visible !== false });
  }
  return out;
}
