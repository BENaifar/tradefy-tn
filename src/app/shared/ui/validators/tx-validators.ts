import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Accepts "12,5" or "12.5". Returns null when empty or not a plain decimal number. */
export function parseDecimal(value: unknown): number | null {
  if (value === null || value === undefined) return null;
  const s = String(value).trim().replace(/\s/g, '').replace(',', '.');
  if (s === '' || !/^-?\d*\.?\d+$|^-?\d+\.$/.test(s)) return null;
  const n = Number(s);
  return Number.isFinite(n) ? n : null;
}

/** Number of decimals typed (after normalising the comma). */
export function decimalsOf(value: unknown): number {
  const s = String(value ?? '').trim().replace(',', '.');
  const i = s.indexOf('.');
  return i < 0 ? 0 : s.length - i - 1;
}

export interface DecimalRules { min?: number; max?: number; maxDecimals?: number; exclusiveMin?: boolean; required?: boolean; }

/** Text-input decimal validator (money, price, percent). Works on the raw string. */
export function decimalValidator(rules: DecimalRules): ValidatorFn {
  return (c: AbstractControl): ValidationErrors | null => {
    const raw = c.value;
    if (raw === null || raw === undefined || String(raw).trim() === '') {
      return rules.required ? { required: { message: 'This field is required.' } } : null;
    }
    const n = parseDecimal(raw);
    if (n === null) return { decimal: { message: 'Enter a valid number.' } };
    if (rules.maxDecimals !== undefined && decimalsOf(raw) > rules.maxDecimals) {
      return { decimals: { message: `At most ${rules.maxDecimals} decimals.` } };
    }
    if (rules.min !== undefined && (rules.exclusiveMin ? n <= rules.min : n < rules.min)) {
      return { min: { message: rules.exclusiveMin ? `Must be greater than ${rules.min}.` : `Must be at least ${rules.min}.` } };
    }
    if (rules.max !== undefined && n > rules.max) return { max: { message: `Must be at most ${rules.max}.` } };
    return null;
  };
}

export function integerValidator(min = 1, max?: number): ValidatorFn {
  return (c: AbstractControl): ValidationErrors | null => {
    const raw = c.value;
    if (raw === null || raw === undefined || String(raw).trim() === '') return { required: { message: 'This field is required.' } };
    const s = String(raw).trim();
    if (!/^\d+$/.test(s)) return { integer: { message: 'Enter a whole number.' } };
    const n = Number(s);
    if (n < min) return { min: { message: `Must be at least ${min}.` } };
    if (max !== undefined && n > max) return { max: { message: `Must be at most ${max}.` } };
    return null;
  };
}

export function exactTextValidator(text: string): ValidatorFn {
  return (c: AbstractControl): ValidationErrors | null =>
    c.value === text ? null : { exact: { message: `Type ${text} exactly to confirm.` } };
}

export function maxLengthMsg(n: number): ValidatorFn {
  return (c: AbstractControl): ValidationErrors | null =>
    String(c.value ?? '').length <= n ? null : { maxlen: { message: `At most ${n} characters.` } };
}

/** from ≤ to, neither in the future (YYYY-MM-DD strings compare lexicographically). */
export function dateRangeValidator(fromKey: string, toKey: string, today: () => string): ValidatorFn {
  return (g: AbstractControl): ValidationErrors | null => {
    const from = g.get(fromKey)?.value as string | null;
    const to = g.get(toKey)?.value as string | null;
    if (from && to && from > to) return { range: { message: '"From" must be on or before "To".' } };
    const t = today();
    if ((from && from > t) || (to && to > t)) return { future: { message: 'Dates cannot be in the future.' } };
    return null;
  };
}

/** Block characters that make no sense in a whole-number input (e + - . ,). */
export function blockNonDigits(ev: KeyboardEvent): void {
  if (['e', 'E', '+', '-', '.', ','].includes(ev.key)) ev.preventDefault();
}

export interface CsvCheck { ok: boolean; message: string; preview: string[][]; }
export const CSV_HEADER = 'date,ticker,open,high,low,close,volume';

/** CSV import checks: `.csv`, ≤ 5 MB, not empty, expected header. Returns a 5-row preview. */
export async function checkCsvFile(file: File): Promise<CsvCheck> {
  if (!file.name.toLowerCase().endsWith('.csv')) return { ok: false, message: 'Only .csv files are accepted.', preview: [] };
  if (file.size === 0) return { ok: false, message: 'The file is empty.', preview: [] };
  if (file.size > 5 * 1024 * 1024) return { ok: false, message: 'The file is larger than 5 MB.', preview: [] };
  const text = await file.text();
  const lines = text.split(/\r?\n/).filter(l => l.trim() !== '');
  if (!lines.length) return { ok: false, message: 'The file is empty.', preview: [] };
  const header = lines[0].replace(/^﻿/, '').replace(/\s/g, '').toLowerCase();
  if (header !== CSV_HEADER) return { ok: false, message: `Header must be exactly: ${CSV_HEADER}`, preview: [] };
  return { ok: true, message: `${lines.length - 1} data rows`, preview: lines.slice(0, 6).map(l => l.split(',')) };
}
