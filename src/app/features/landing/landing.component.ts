import { AfterViewInit, Component, ElementRef, NgZone, OnDestroy } from '@angular/core';
import { gsap } from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';
import { LenisService } from '../../core/services/lenis.service';

gsap.registerPlugin(ScrollTrigger);

@Component({
  selector: 'app-landing',
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.scss']
})
export class LandingComponent implements AfterViewInit, OnDestroy {
  activeStage = 0;
  motionPaused = false;
  activeChapter = 'hero';
  readonly chapters = [
    { id: 'hero', name: 'Overview' },
    { id: 'ai-system', name: 'Intelligence' },
    { id: 'features', name: 'Platform' },
    { id: 'stats', name: 'Our approach' },
    { id: 'cta', name: 'Get started' }
  ];
  readonly markets = [
    { symbol: 'BTC', name: 'Bitcoin', price: '89,420.50', change: '+4.82%', points: '0,200 24,185 44,194 66,158 86,174 108,147 130,161 152,120 176,140 198,108 220,126 244,84 266,102 288,66 310,83 336,40 360,55 384,24 410,37 440,12' },
    { symbol: 'ETH', name: 'Ethereum', price: '3,412.10', change: '+2.14%', points: '0,185 24,169 44,180 66,150 86,160 108,170 130,139 152,145 176,110 198,123 220,98 244,110 266,80 288,95 310,62 336,80 360,48 384,60 410,30 440,22' },
    { symbol: 'SOL', name: 'Solana', price: '198.75', change: '+8.45%', points: '0,210 24,195 44,205 66,178 86,190 108,145 130,168 152,135 176,152 198,115 220,130 244,90 266,104 288,70 310,95 336,45 360,64 384,20 410,39 440,8' }
  ];
  readonly stages = [
    { name: 'Observe', title: 'Find the signal. Filter the noise.', text: 'Bring price movements, order flow, and market context into one clear view. Start with a better understanding of what is moving.', label: 'MARKET INPUT', result: 'Signals organized', nodes: ['Price', 'Volume', 'Order flow'] },
    { name: 'Interpret', title: 'More context. Clearer decisions.', text: 'Explore connections between market signals and your strategy. AI-assisted analysis brings the important details into focus.', label: 'CONTEXT ENGINE', result: 'Patterns connected', nodes: ['Trend', 'Liquidity', 'Volatility'] },
    { name: 'Act', title: 'Your strategy. Your final call.', text: 'Turn your analysis into a considered next step. Review the context, assess your risk, and stay in control of every decision.', label: 'STRATEGY OUTPUT', result: 'Ready for review', nodes: ['Review', 'Risk', 'Decision'] }
  ];
  readonly capabilities = [
    { number: '01', title: 'A wider market view.', text: 'Keep digital assets and market movements in perspective with a workspace built around clarity.', tag: 'MARKET INTELLIGENCE', kind: 'bars' },
    { number: '02', title: 'Less friction. More focus.', text: 'Move from an idea to your next decision with a direct, considered trading workflow.', tag: 'CONNECTED WORKFLOW', kind: 'route' },
    { number: '03', title: 'Control at every step.', text: 'Put your strategy, account, and risk awareness at the center of your trading experience.', tag: 'BUILT AROUND YOU', kind: 'shield' }
  ];
  private context?: gsap.Context;
  private media?: gsap.MatchMedia;
  private chapterObserver?: IntersectionObserver;

  constructor(private host: ElementRef<HTMLElement>, private zone: NgZone, private lenis: LenisService) {}

  scrollTo(id: string): void { this.lenis.scrollTo(id, -90); }

  toggleMotion(): void {
    this.motionPaused = !this.motionPaused;
    this.setupMotion();
  }

  ngAfterViewInit(): void {
    this.setupMotion();
    this.zone.runOutsideAngular(() => {
      this.chapterObserver = new IntersectionObserver(entries => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            this.zone.run(() => { this.activeChapter = entry.target.id; });
          }
        }
      }, { rootMargin: '-15% 0px -65% 0px', threshold: 0 });
      this.chapters.forEach(chapter => {
        const section = this.host.nativeElement.querySelector('#' + chapter.id);
        if (section) { this.chapterObserver?.observe(section); }
      });
    });
  }

  private setupMotion(): void {
    this.media?.revert();
    this.context?.revert();
    if (this.motionPaused) { return; }

    this.zone.runOutsideAngular(() => {
      this.context = gsap.context(() => {
        this.media = gsap.matchMedia();
        this.media.add('(prefers-reduced-motion: no-preference)', () => {
          gsap.from('.hero-enter', { y: 28, opacity: 0, duration: 0.85, stagger: 0.1, ease: 'power3.out', clearProps: 'all' });
          gsap.to('.orbit-satellite', {
            rotation: 360, svgOrigin: '300 210', duration: 32, repeat: -1, ease: 'none',
            scrollTrigger: { trigger: '#hero', start: 'top bottom', end: 'bottom top', toggleActions: 'play pause play pause' }
          });
          gsap.utils.toArray<HTMLElement>('[data-reveal]').forEach(element => {
            gsap.from(element, { y: 28, opacity: 0, duration: 0.8, ease: 'power2.out', clearProps: 'all', scrollTrigger: { trigger: element, start: 'top 92%', once: true } });
          });
          gsap.to('.signal-dot', { strokeDashoffset: -1, duration: 4, repeat: -1, ease: 'none', scrollTrigger: { trigger: '#ai-system', start: 'top bottom', end: 'bottom top', toggleActions: 'play pause play pause' } });
          gsap.from('.closing-line', { scaleX: 0, transformOrigin: 'left', scrollTrigger: { trigger: '#cta', start: 'top 90%', end: 'top 35%', scrub: 1 } });
        });
      }, this.host.nativeElement);
    });
  }

  ngOnDestroy(): void {
    this.chapterObserver?.disconnect();
    this.media?.revert();
    this.context?.revert();
  }
}

