import { AfterViewInit, ChangeDetectorRef, Directive, ElementRef, NgZone, OnDestroy } from '@angular/core';

/** Base for the SVG charts: measures the host width with a ResizeObserver so the drawing fits its container. */
@Directive()
export abstract class ResizeAware implements AfterViewInit, OnDestroy {
  width = 320;
  private ro?: ResizeObserver;

  protected constructor(protected host: ElementRef<HTMLElement>, protected zone: NgZone, protected cdr: ChangeDetectorRef) {}

  ngAfterViewInit(): void {
    const w0 = Math.round(this.host.nativeElement.getBoundingClientRect().width);
    if (w0 > 0) { this.width = w0; this.cdr.detectChanges(); }
    if (typeof ResizeObserver === 'undefined') return;
    this.ro = new ResizeObserver(entries => this.zone.run(() => {
      const w = Math.round(entries[0].contentRect.width);
      if (w > 0 && w !== this.width) { this.width = w; this.cdr.markForCheck(); }
    }));
    this.ro.observe(this.host.nativeElement);
  }

  ngOnDestroy(): void { this.ro?.disconnect(); }
}
