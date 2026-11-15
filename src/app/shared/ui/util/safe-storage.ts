// Preferences only (never market or portfolio data). Keys are prefixed `tradex_mp_` and never touch tradex_token / tradex_user.
// Every access is wrapped in try/catch: storage can be blocked or full.

const PREFIX = 'tradex_mp_';

export function lsGet<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(PREFIX + key);
    return raw === null ? fallback : (JSON.parse(raw) as T);
  } catch {
    return fallback;
  }
}

export function lsSet(key: string, value: unknown): void {
  try {
    localStorage.setItem(PREFIX + key, JSON.stringify(value));
  } catch {
    /* ignore */
  }
}

export function lsRemove(key: string): void {
  try {
    localStorage.removeItem(PREFIX + key);
  } catch {
    /* ignore */
  }
}
