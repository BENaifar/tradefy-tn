import { Component, ElementRef, HostListener, OnDestroy, ViewChild } from '@angular/core';
import { Subscription } from 'rxjs';
import { ConfirmRequest, ConfirmService } from '../../util/confirm.service';

/** Replaces `confirm()`. Supports "type RESET to confirm". Rendered by tx-module-header. */
@Component({
  selector: 'tx-confirm-dialog',
  templateUrl: './tx-confirm-dialog.component.html',
  styleUrls: ['./tx-confirm-dialog.component.scss']
})
export class TxConfirmDialogComponent implements OnDestroy {
  req: ConfirmRequest | null = null;
  typed = '';
  @ViewChild('dialog') dialog?: ElementRef<HTMLElement>;
  private sub: Subscription;

  constructor(private confirm: ConfirmService) {
    this.sub = confirm.request$.subscribe(r => {
      this.req = r;
      this.typed = '';
      if (r) setTimeout(() => this.dialog?.nativeElement.querySelector<HTMLElement>('input, button.primary, button')?.focus());
    });
  }

  get canConfirm(): boolean { return !this.req?.requireText || this.typed === this.req.requireText; }

  ok(): void { if (this.canConfirm) this.confirm.resolve(true); }
  cancel(): void { this.confirm.resolve(false); }

  @HostListener('document:keydown.escape')
  onEsc(): void { if (this.req) this.cancel(); }

  ngOnDestroy(): void { this.sub.unsubscribe(); if (this.req) this.confirm.resolve(false); }
}
