// One place for number/time formatting. `null` renders "—". Display only, never used for money arithmetic.

const MINUS = '−';
const ZONE = 'Africa/Tunis';
const cache = new Map<string, Intl.NumberFormat>();

function nf(min: number, max: number): Intl.NumberFormat {
  const k = `${min}-${max}`;
  let f = cache.get(k);
  if (!f) {
    f = new Intl.NumberFormat('en-US', { minimumFractionDigits: min, maximumFractionDigits: max });
    cache.set(k, f);
  }
  return f;
}

export function isNum(v: unknown): v is number {
  return typeof v === 'number' && Number.isFinite(v);
}

/** 12,345.678 (negative with a true minus sign). */
export function fmtNumber(v: number | null | undefined, decimals = 3): string {
  if (!isNum(v)) return '—';
  const s = nf(decimals, decimals).format(Math.abs(v));
  return v < 0 && Number(s.replace(/,/g, '')) !== 0 ? MINUS + s : s;
}

/** 12,345.678 TND */
export function fmtMoney(v: number | null | undefined, unit = true, signed = false, decimals = 3): string {
  if (!isNum(v)) return '—';
  const base = fmtNumber(v, decimals);
  const sign = signed && v > 0 ? '+' : '';
  return `${sign}${base}${unit ? ' TND' : ''}`;
}

/** +1.23 % */
export function fmtPct(v: number | null | undefined, signed = true, decimals = 2): string {
  if (!isNum(v)) return '—';
  const sign = signed && v > 0 && Number(Math.abs(v).toFixed(decimals)) !== 0 ? '+' : '';
  return `${sign}${fmtNumber(v, decimals)} %`;
}

export function fmtQty(v: number | null | undefined): string {
  if (!isNum(v)) return '—';
  return nf(0, 0).format(v).replace(/,/g, ' ');
}

export function fmtCompact(v: number | null | undefined): string {
  if (!isNum(v)) return '—';
  const a = Math.abs(v);
  if (a >= 1e9) return fmtNumber(v / 1e9, 2) + 'B';
  if (a >= 1e6) return fmtNumber(v / 1e6, 2) + 'M';
  if (a >= 1e3) return fmtNumber(v / 1e3, 1) + 'K';
  return fmtNumber(v, 0);
}

export function signClass(v: number | null | undefined): '' | 'pos' | 'neg' {
  if (!isNum(v) || v === 0) return '';
  return v > 0 ? 'pos' : 'neg';
}

const dtf = new Map<string, Intl.DateTimeFormat>();
function dateFormatter(mode: 'datetime' | 'time' | 'date'): Intl.DateTimeFormat {
  let f = dtf.get(mode);
  if (!f) {
    const o: Intl.DateTimeFormatOptions = { timeZone: ZONE, hour12: false };
    if (mode !== 'time') Object.assign(o, { day: '2-digit', month: '2-digit', year: 'numeric' });
    if (mode !== 'date') Object.assign(o, { hour: '2-digit', minute: '2-digit' });
    f = new Intl.DateTimeFormat('en-GB', o);
    dtf.set(mode, f);
  }
  return f;
}

/**
 * Formats a date in Africa/Tunis. Accepts an ISO instant, a zone-less LocalDateTime (shown as written),
 * a `YYYY-MM-DD` date, or epoch SECONDS (number).
 */
export function fmtTunis(v: string | number | null | undefined, mode: 'datetime' | 'time' | 'date' = 'datetime'): string {
  if (v === null || v === undefined || v === '') return '—';
  if (typeof v === 'string') {
    if (/^\d{4}-\d{2}-\d{2}$/.test(v)) return mode === 'time' ? '—' : `${v.slice(8, 10)}/${v.slice(5, 7)}/${v.slice(0, 4)}`;
    if (/^\d{2}:\d{2}/.test(v)) return v.slice(0, 5); // LocalTime
    if (!/(Z|[+-]\d{2}:?\d{2})$/.test(v)) {
      // LocalDateTime: no zone, show the wall-clock as written
      const m = v.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/);
      if (!m) return '—';
      const d = `${m[3]}/${m[2]}/${m[1]}`;
      const t = `${m[4]}:${m[5]}`;
      return mode === 'date' ? d : mode === 'time' ? t : `${d}, ${t}`;
    }
  }
  const date = typeof v === 'number' ? new Date(v * 1000) : new Date(v);
  if (isNaN(date.getTime())) return '—';
  return dateFormatter(mode).format(date);
}
