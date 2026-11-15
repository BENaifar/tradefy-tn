import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, catchError, map, retry, throwError, timer } from 'rxjs';

export type Params = Record<string, string | number | boolean | null | undefined>;

/** Builds HttpParams and drops null / undefined / '' values (the backend would receive "null"). */
export function cleanParams(params?: Params): HttpParams {
  let p = new HttpParams();
  for (const [k, v] of Object.entries(params ?? {})) {
    if (v === null || v === undefined || v === '') continue;
    p = p.set(k, String(v));
  }
  return p;
}

/**
 * Thin typed wrapper used by each module's services. It is NOT an interceptor: it unwraps `.data`,
 * retries only GETs on status 0 / 503 (max 2, backoff) and maps errors with the module's own handler.
 * The bearer token is added by the existing JwtInterceptor.
 */
export class ApiClient<E> {
  constructor(
    private readonly http: HttpClient,
    private readonly base: string,
    private readonly mapError: (e: unknown) => E
  ) {}

  get<T>(path: string, params?: Params): Observable<T> {
    return this.http.get<{ data: T }>(this.base + path, { params: cleanParams(params) }).pipe(
      retry({
        count: 2,
        delay: (err: unknown, n: number) =>
          err instanceof HttpErrorResponse && (err.status === 0 || err.status === 503)
            ? timer(400 * 2 ** (n - 1))
            : throwError(() => err)
      }),
      map(r => r.data),
      catchError(e => throwError(() => this.mapError(e)))
    );
  }

  post<T>(path: string, body?: unknown, params?: Params): Observable<T> {
    return this.http.post<{ data: T }>(this.base + path, body ?? null, { params: cleanParams(params) }).pipe(
      map(r => r.data),
      catchError(e => throwError(() => this.mapError(e)))
    );
  }

  put<T>(path: string, body?: unknown): Observable<T> {
    return this.http.put<{ data: T }>(this.base + path, body ?? null).pipe(
      map(r => r.data),
      catchError(e => throwError(() => this.mapError(e)))
    );
  }

  delete<T>(path: string): Observable<T> {
    return this.http.delete<{ data: T }>(this.base + path).pipe(
      map(r => r.data),
      catchError(e => throwError(() => this.mapError(e)))
    );
  }

  /** Multipart upload. Never sets Content-Type: the browser adds the boundary. */
  upload<T>(path: string, file: File): Observable<T> {
    const fd = new FormData();
    fd.append('file', file, file.name);
    return this.http.post<{ data: T }>(this.base + path, fd).pipe(
      map(r => r.data),
      catchError(e => throwError(() => this.mapError(e)))
    );
  }
}
