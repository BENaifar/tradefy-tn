import { Component, ElementRef, EventEmitter, HostListener, Input, OnChanges, Output, SimpleChanges, ViewChild } from '@angular/core';

/** Right slide-over (trade ticket, position detail). Focus is trapped while open; Esc closes it. */
@Component({
  selector: 'tx-drawer',
  templateUrl: './tx-drawer.component.html',
  styleUrls: ['./tx-drawer.component.scss']
})
export class TxDrawerComponent implements OnChanges {
  @Input() open = false;
  @Input() title = '';
  @Input() kicker = '';
  @Output() closed = new EventEmitter<void>();
  @ViewChild('panel') panel?: ElementRef<HTMLElement>;
  private previous: HTMLElement | null = null;

  ngOnChanges(c: SimpleChanges): void {
    if (c['open']) {
      if (this.open) {
        this.previous = document.activeElement as HTMLElement | null;
        setTimeout(() => {
          const first = this.focusables()[0];
          if (first) first.focus(); else this.panel?.nativeElement.focus();
        });
      } else {
        this.previous?.focus?.();
        this.previous = null;
      }
    }
  }

  @HostListener('document:keydown.escape')
  onEsc(): void { if (this.open) this.closed.emit(); }

  onKeydown(ev: KeyboardEvent): void {
    if (ev.key !== 'Tab') return;
    const items = this.focusables();
    if (!items.length) { ev.preventDefault(); return; }
    const first = items[0], last = items[items.length - 1];
    const active = document.activeElement;
    if (ev.shiftKey && active === first) { ev.preventDefault(); last.focus(); }
    else if (!ev.shiftKey && active === last) { ev.preventDefault(); first.focus(); }
  }

  private focusables(): HTMLElement[] {
    const root = this.panel?.nativeElement;
    if (!root) return [];
    return Array.from(root.querySelectorAll<HTMLElement>('a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'))
      .filter(el => el.offsetParent !== null);
  }
}
