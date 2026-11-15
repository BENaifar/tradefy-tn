import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject, take } from 'rxjs';

export interface ConfirmOptions {
  title: string;
  message: string;
  confirmLabel?: string;
  danger?: boolean;
  /** When set, the user must type this exact text (e.g. "RESET") to enable the confirm button. */
  requireText?: string;
}
export interface ConfirmRequest extends ConfirmOptions { result: Subject<boolean>; }

/** Replaces window.confirm(). Rendered by tx-module-header via <tx-confirm-dialog>. */
@Injectable({ providedIn: 'root' })
export class ConfirmService {
  private readonly subject = new BehaviorSubject<ConfirmRequest | null>(null);
  readonly request$ = this.subject.asObservable();

  confirm(options: ConfirmOptions): Observable<boolean> {
    const result = new Subject<boolean>();
    this.subject.next({ ...options, result });
    return result.pipe(take(1));
  }

  resolve(value: boolean): void {
    const r = this.subject.value;
    this.subject.next(null);
    if (r) { r.result.next(value); r.result.complete(); }
  }
}
