import { CommonModule } from '@angular/common';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterModule } from '@angular/router';
import { ScrollTrigger } from 'gsap/ScrollTrigger';
import { LenisService } from '../../core/services/lenis.service';
import { LandingComponent } from './landing.component';

describe('Landing page interactions', () => {
  let fixture: ComponentFixture<LandingComponent>;
  let root: HTMLElement;
  let scrolling: jasmine.SpyObj<LenisService>;

  beforeEach(async () => {
    scrolling = jasmine.createSpyObj('LenisService', ['scrollTo']);
    await TestBed.configureTestingModule({
      imports: [CommonModule, RouterModule.forRoot([])],
      declarations: [LandingComponent],
      providers: [{ provide: LenisService, useValue: scrolling }]
    }).compileComponents();
    fixture = TestBed.createComponent(LandingComponent);
    fixture.detectChanges();
    root = fixture.nativeElement;
  });

  afterEach(() => fixture.destroy());

  it('renders original SVG graphics without image or video assets', () => {
    expect(root.querySelectorAll('img, video, source, svg image').length).toBe(0);
    expect(root.querySelector('.market-chart')).not.toBeNull();
    expect(root.querySelector('.neural-map')).not.toBeNull();
    expect(root.querySelectorAll('h1').length).toBe(1);
  });

  it('updates the market name, price, chart, and selection together', () => {
    const previousPoints = root.querySelector('.market-line')?.getAttribute('points');
    const buttons = root.querySelectorAll<HTMLButtonElement>('.market-switcher button');
    buttons[1].click();
    fixture.detectChanges();
    expect(root.querySelector('.chart-heading')?.textContent).toContain('Ethereum');
    expect(root.querySelector('.chart-price')?.textContent).toContain('3,412.10');
    expect(root.querySelector('.market-line')?.getAttribute('points')).not.toBe(previousPoints);
    expect(buttons[1].getAttribute('aria-pressed')).toBe('true');
    expect(buttons[0].getAttribute('aria-pressed')).toBe('false');
  });

  it('updates the AI explanation and diagram labels when a stage is selected', () => {
    root.querySelectorAll<HTMLButtonElement>('.stage-controls button')[2].click();
    fixture.detectChanges();
    expect(root.querySelector('.stage-content h3')?.textContent).toContain('Your final call');
    expect(root.querySelector('.neural-map')?.textContent).toContain('Decision');
    expect(root.querySelector('.map-footer')?.textContent).toContain('Ready for review');
  });

  it('uses Lenis for the intelligence section shortcut with navbar clearance', () => {
    root.querySelector<HTMLButtonElement>('.text-action')!.click();
    expect(scrolling.scrollTo).toHaveBeenCalledWith('#ai-system', -90);
  });

  it('navigates to a selected chapter with the same smooth-scroll offset', () => {
    root.querySelectorAll<HTMLButtonElement>('.chapter-nav button')[2].click();
    expect(scrolling.scrollTo).toHaveBeenCalledWith('#features', -90);
  });

  it('keeps every landing section on a black background', () => {
    const sections = root.querySelectorAll<HTMLElement>('.landing-page > section');
    expect(sections.length).toBe(6);
    sections.forEach(section => {
      expect(getComputedStyle(section).backgroundColor).withContext(section.id).toBe('rgb(0, 0, 0)');
    });
  });

  it('lets the visitor pause motion without leaving hidden content', () => {
    root.querySelector<HTMLButtonElement>('.motion-control')!.click();
    fixture.detectChanges();
    expect(root.querySelector('.motion-control')?.getAttribute('aria-pressed')).toBe('true');
    expect(root.querySelector('.motion-control')?.textContent).toContain('Enable');
    root.querySelectorAll<HTMLElement>('[data-reveal], .hero-enter').forEach(element => {
      expect(element.style.opacity).not.toBe('0');
    });
  });

  it('cleans up its own animations without destroying unrelated scroll triggers', () => {
    const outside = ScrollTrigger.create({ id: 'outside-landing', start: 0, end: 100 });
    fixture.destroy();
    expect(ScrollTrigger.getById('outside-landing')).toBe(outside);
    outside.kill();
  });
});
