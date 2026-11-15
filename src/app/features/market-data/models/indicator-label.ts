import { IndicatorDefinition } from './market-data.model';

/** `SMA(20)` / `MACD(12, 26, 9)`: values follow the order of the definition's params. Nothing indicator-specific. */
export function indicatorLabel(def: Pick<IndicatorDefinition, 'code' | 'params'> | undefined, code: string, params: Record<string, number>): string {
  const values = (def?.params ?? []).map(p => params[p.name] ?? p.defaultValue);
  return values.length ? `${def?.code ?? code}(${values.join(', ')})` : (def?.code ?? code);
}
