import { Observable, Subscription } from 'rxjs';
import { ToastService } from './toast.service';

export interface SectionError { kind?: string; status?: number; message: string; fieldErrors?: Record<string, string>; }

/**
 * Loading / error / data state of one page section (each section owns its state: one failing
 * request never blanks a page). `load` cancels the previous request (switchMap behaviour).
 */
export class Section<T> {
  data: T | null = null;
  loading = false;
  loaded = false;
  error: SectionError | null = null;
  private sub?: Subscription;
  private factory?: () => Observable<T>;

  constructor(private readonly toast?: ToastService, private readonly onData?: (d: T) => void) {}

  load(factory: () => Observable<T>, keepData = false): void {
    this.factory = factory;
    this.sub?.unsubscribe();
    this.loading = true;
    this.error = null;
    if (!keepData) this.data = null;
    this.sub = factory().subscribe({
      next: d => { this.data = d; this.loading = false; this.loaded = true; this.onData?.(d); },
      error: (e: SectionError) => {
        this.loading = false;
        this.loaded = true;
        this.error = e;
        if (e && this.toast) this.toast.reportError({ kind: e.kind ?? '', message: e.message });
      }
    });
  }

  reload(keepData = true): void { if (this.factory) this.load(this.factory, keepData); }
  get isNotFound(): boolean { return this.error?.kind === 'not-found'; }
  destroy(): void { this.sub?.unsubscribe(); }
}
