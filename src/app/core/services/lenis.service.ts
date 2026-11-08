import { Injectable, NgZone } from '@angular/core';
import Lenis from 'lenis';
import { gsap } from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';

gsap.registerPlugin(ScrollTrigger);

@Injectable({
  providedIn: 'root'
})
export class LenisService {
  private lenis?: Lenis;
  private motionPreference?: MediaQueryList;
  private readonly tick = (time: number) => this.lenis?.raf(time * 1000);
  private readonly onMotionChange = () => this.configure();

  constructor(private zone: NgZone) {}

  init(): void {
    if (this.motionPreference) { return; }
    this.motionPreference = window.matchMedia('(prefers-reduced-motion: reduce)');
    this.motionPreference.addEventListener('change', this.onMotionChange);
    this.configure();
  }

  private configure(): void {
    gsap.ticker.remove(this.tick);
    this.lenis?.destroy();
    this.lenis = undefined;
    if (this.motionPreference?.matches) { return; }
    this.zone.runOutsideAngular(() => {
      this.lenis = new Lenis({
        duration: 1.05,
        easing: (t: number) => Math.min(1, 1.001 - Math.pow(2, -10 * t)),
        orientation: 'vertical',
        gestureOrientation: 'vertical',
        smoothWheel: true,
      });
      // Keep a stable callback so teardown removes the same listener.
      gsap.ticker.add(this.tick);
      gsap.ticker.lagSmoothing(0);
      this.lenis.on('scroll', ScrollTrigger.update);
    });
  }

  scrollTo(target: string | HTMLElement, offset = 0): void {
    const element = typeof target === 'string' ? document.querySelector<HTMLElement>(target) : target;
    if (!element) { return; }
    if (this.lenis) {
      this.lenis.scrollTo(element, { offset });
    } else {
      window.scrollTo({ top: element.getBoundingClientRect().top + window.scrollY + offset, behavior: 'instant' });
    }
  }

  destroy(): void {
    this.motionPreference?.removeEventListener('change', this.onMotionChange);
    this.motionPreference = undefined;
    gsap.ticker.remove(this.tick);
    this.lenis?.destroy();
    this.lenis = undefined;
  }
}

