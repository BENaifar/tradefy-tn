import { IndicatorDefinition } from '../../models/market-data.model';
import { MAX_INDICATORS, validateSavedIndicators } from './chart-prefs';

const def = (code: string, placement: 'OVERLAY' | 'PANE', params: IndicatorDefinition['params'] = []): IndicatorDefinition => ({
  code, name: code, description: '', placement, params, outputs: [{ key: 'value', label: code, style: 'LINE' }], referenceLines: []
});
const catalog: IndicatorDefinition[] = [
  def('SMA', 'OVERLAY', [{ name: 'period', label: 'Period', type: 'INT', defaultValue: 20, min: 2, max: 500 }]),
  def('BOLL', 'OVERLAY', [
    { name: 'period', label: 'Period', type: 'INT', defaultValue: 20, min: 2, max: 500 },
    { name: 'stdDev', label: 'Std dev', type: 'DECIMAL', defaultValue: 2, min: 0.5, max: 5 }
  ]),
  def('RSI', 'PANE', [{ name: 'period', label: 'Period', type: 'INT', defaultValue: 14, min: 2, max: 100 }])
];

describe('validateSavedIndicators', () => {
  it('returns nothing for garbage', () => {
    expect(validateSavedIndicators(null, catalog)).toEqual([]);
    expect(validateSavedIndicators('x', catalog)).toEqual([]);
  });
  it('drops unknown indicator codes', () => {
    const r = validateSavedIndicators([{ code: 'NOPE', params: {}, color: '#ffffff', visible: true }, { code: 'SMA', params: { period: 50 }, color: '#8fb3ff', visible: true }], catalog);
    expect(r.length).toBe(1);
    expect(r[0].code).toBe('SMA');
    expect(r[0].params['period']).toBe(50);
  });
  it('drops out-of-range and non-integer params (the default is used)', () => {
    const r = validateSavedIndicators([{ code: 'SMA', params: { period: 9999 }, color: '#8fb3ff', visible: true }, { code: 'RSI', params: { period: 2.5 }, color: '#8fb3ff', visible: true }], catalog);
    expect(r[0].params['period']).toBe(20);
    expect(r[1].params['period']).toBe(14);
  });
  it('keeps a decimal param inside its range and resets one outside', () => {
    const r = validateSavedIndicators([{ code: 'BOLL', params: { period: 20, stdDev: 2.5 }, color: '#8fb3ff', visible: true }, { code: 'BOLL', params: { period: 20, stdDev: 9 }, color: '#d0a2f7', visible: true }], catalog);
    expect(r[0].params['stdDev']).toBe(2.5);
    expect(r[1].params['stdDev']).toBe(2);
  });
  it('fixes invalid colours and keeps the visible flag', () => {
    const r = validateSavedIndicators([{ code: 'SMA', params: { period: 20 }, color: 'red', visible: false }], catalog);
    expect(r[0].color).toMatch(/^#[0-9a-f]{6}$/i);
    expect(r[0].visible).toBeFalse();
  });
  it('applies the 8 indicator and 3 pane limits', () => {
    const many = Array.from({ length: 12 }, () => ({ code: 'SMA', params: { period: 20 }, color: '#ffffff', visible: true }));
    expect(validateSavedIndicators(many, catalog).length).toBe(MAX_INDICATORS);
    const panes = Array.from({ length: 5 }, () => ({ code: 'RSI', params: { period: 14 }, color: '#ffffff', visible: true }));
    expect(validateSavedIndicators(panes, catalog).length).toBe(3);
  });
});
