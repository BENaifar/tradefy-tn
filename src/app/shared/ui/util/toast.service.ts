import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export type ToastKind = 'info' | 'success' | 'error';
export interface Toast {
  id: number;
  kind: ToastKind;
  message: string;
  action?: { label: string; run: () => void };
  /** Called when the toast goes away without the action being used (e.g. to send a delayed DELETE). */
  onExpire?: () => void;
}

/** Bottom-right stack. Auto-dismiss after 4 s; errors stay until closed. Rendered by tx-module-header. */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private seq = 0;
  private readonly subject = new BehaviorSubject<Toast[]>([]);
  readonly toasts$ = this.subject.asObservable();

  success(message: string): void { this.push({ kind: 'success', message }, 4000); }
  info(message: string): void { this.push({ kind: 'info', message }, 4000); }
  error(message: string): void { this.push({ kind: 'error', message }, 0); }

  /** A toast with an action button (undo). `onExpire` runs when it times out without the action. */
  withAction(message: string, label: string, run: () => void, onExpire: () => void, ms = 5000): void {
    this.push({ kind: 'info', message, action: { label, run }, onExpire }, ms);
  }

  dismiss(id: number, expired = false): void {
    const t = this.subject.value.find(x => x.id === id);
    if (!t) return;
    this.subject.next(this.subject.value.filter(x => x.id !== id));
    if (expired) t.onExpire?.();
  }

  runAction(id: number): void {
    const t = this.subject.value.find(x => x.id === id);
    if (!t) return;
    this.subject.next(this.subject.value.filter(x => x.id !== id));
    t.action?.run();
  }

  private push(t: Omit<Toast, 'id'>, ms: number): void {
    const id = ++this.seq;
    this.subject.next([...this.subject.value, { ...t, id }].slice(-5));
    if (ms > 0) setTimeout(() => this.dismiss(id, true), ms);
  }

  /** Shared behaviour for HTTP failures: status 0 / 403 / 5xx toast, 401 nothing (JwtInterceptor handles it). */
  reportError(e: { kind: string; message: string }): void {
    if (e.kind === 'network' || e.kind === 'forbidden' || e.kind === 'server') this.error(e.message);
  }
}
