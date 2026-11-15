// Dates are always `YYYY-MM-DD` strings (never Date objects), computed in Africa/Tunis.

const ISO = /^\d{4}-\d{2}-\d{2}$/;

export function isIsoDate(s: string | null | undefined): s is string {
  return !!s && ISO.test(s) && !isNaN(Date.parse(s + 'T00:00:00Z'));
}

export function todayTunis(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Africa/Tunis', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date());
}

function utc(s: string): Date {
  return new Date(s + 'T00:00:00Z');
}
function iso(d: Date): string {
  return d.toISOString().slice(0, 10);
}

export function addDays(s: string, n: number): string {
  const d = utc(s);
  d.setUTCDate(d.getUTCDate() + n);
  return iso(d);
}

export function addMonths(s: string, n: number): string {
  const d = utc(s);
  const day = d.getUTCDate();
  d.setUTCDate(1);
  d.setUTCMonth(d.getUTCMonth() + n);
  const last = new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth() + 1, 0)).getUTCDate();
  d.setUTCDate(Math.min(day, last));
  return iso(d);
}

export function startOfYear(s: string): string {
  return s.slice(0, 4) + '-01-01';
}

export function startOfWeek(s: string): string {
  const d = utc(s);
  const dow = (d.getUTCDay() + 6) % 7; // Monday = 0
  return addDays(s, -dow);
}

export function startOfMonth(s: string): string {
  return s.slice(0, 8) + '01';
}

export function daysBetween(a: string, b: string): number {
  return Math.round((utc(b).getTime() - utc(a).getTime()) / 86400000);
}
