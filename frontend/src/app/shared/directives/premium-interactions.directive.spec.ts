import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import {
  PremiumInteractionsDirective,
  TooltipDirective,
  RevealOnScrollDirective,
  PulseDirective,
  FlipCardDirective,
  LoadingShimmerDirective,
  TypewriterDirective,
  AnimatedBorderDirective,
  ParticleClickDirective
} from './premium-interactions.directive';
import { PremiumInteractionsService } from '../services/premium-interactions.service';
import { ElementRef } from '@angular/core';

describe('PremiumInteractionsDirective', () => {
  let service: jasmine.SpyObj<PremiumInteractionsService>;

  beforeEach(() => {
    service = jasmine.createSpyObj('PremiumInteractionsService', [
      'initMagneticButton',
      'initSpringButton',
      'initParallaxCard',
      'initGlowEffect',
      'createRipple'
    ]);
  });

  @Component({
    template: `
      <button appPremiumInteractions
              [interactionType]="type"
              [magneticStrength]="strength"
              [glowColor]="color">Test</button>
    `,
    standalone: true,
    imports: [PremiumInteractionsDirective]
  })
  class TestComponent {
    type: 'magnetic' | 'ripple' | 'spring' | 'parallax' | 'glow' = 'magnetic';
    strength = 0.3;
    color = '#00d4ff';
  }

  it('should create with magnetic interaction by default', () => {
    TestBed.configureTestingModule({
      providers: [
        { provide: PremiumInteractionsService, useValue: service }
      ]
    });
    const fixture = TestBed.createComponent(TestComponent);
    fixture.detectChanges();

    expect(service.initMagneticButton).toHaveBeenCalled();
  });

  it('should init spring interaction when type is spring', () => {
    TestBed.configureTestingModule({
      providers: [
        { provide: PremiumInteractionsService, useValue: service }
      ]
    });
    const fixture = TestBed.createComponent(TestComponent);
    fixture.componentInstance.type = 'spring';
    fixture.detectChanges();

    expect(service.initSpringButton).toHaveBeenCalled();
  });

  it('should init parallax interaction when type is parallax', () => {
    TestBed.configureTestingModule({
      providers: [
        { provide: PremiumInteractionsService, useValue: service }
      ]
    });
    const fixture = TestBed.createComponent(TestComponent);
    fixture.componentInstance.type = 'parallax';
    fixture.detectChanges();

    expect(service.initParallaxCard).toHaveBeenCalled();
  });

  it('should init glow interaction with custom color', () => {
    TestBed.configureTestingModule({
      providers: [
        { provide: PremiumInteractionsService, useValue: service }
      ]
    });
    const fixture = TestBed.createComponent(TestComponent);
    fixture.componentInstance.type = 'glow';
    fixture.componentInstance.color = '#ff0000';
    fixture.detectChanges();

    expect(service.initGlowEffect).toHaveBeenCalledWith(jasmine.any(ElementRef), '#ff0000');
  });

  it('should create ripple on click when type is ripple', () => {
    TestBed.configureTestingModule({
      providers: [
        { provide: PremiumInteractionsService, useValue: service }
      ]
    });
    const fixture = TestBed.createComponent(TestComponent);
    fixture.componentInstance.type = 'ripple';
    fixture.detectChanges();

    const button = fixture.debugElement.query(By.css('button'));
    button.nativeElement.click();

    expect(service.createRipple).toHaveBeenCalled();
  });
});

describe('TooltipDirective', () => {
  @Component({
    template: `<div [appTooltip]="text"></div>`,
    standalone: true,
    imports: [TooltipDirective]
  })
  class TestComponent {
    text = 'Test Tooltip';
  }

  it('should add tooltip class and attribute', () => {
    const fixture = TestBed.createComponent(TestComponent);
    fixture.detectChanges();

    const div = fixture.debugElement.query(By.css('div'));
    expect(div.nativeElement.classList.contains('premium-tooltip')).toBe(true);
    expect(div.nativeElement.getAttribute('data-tooltip')).toBe('Test Tooltip');
  });

  it('should handle empty tooltip text', () => {
    const fixture = TestBed.createComponent(TestComponent);
    fixture.componentInstance.text = '';
    fixture.detectChanges();

    const div = fixture.debugElement.query(By.css('div'));
    expect(div.nativeElement.getAttribute('data-tooltip')).toBe('');
  });
});

describe('RevealOnScrollDirective', () => {
  @Component({
    template: `<div appRevealOnScroll [revealThreshold]="threshold" [revealDelay]="delay"></div>`,
    standalone: true,
    imports: [RevealOnScrollDirective]
  })
  class TestComponent {
    threshold = 0.1;
    delay = 0;
  }

  it('should add reveal-animation class on init', () => {
    const fixture = TestBed.createComponent(TestComponent);
    fixture.detectChanges();

    const div = fixture.debugElement.query(By.css('div'));
    expect(div.nativeElement.classList.contains('reveal-animation')).toBe(true);
  });

  it('should setup IntersectionObserver on init', () => {
    const observeSpy = jasmine.createSpy('observe');
    const mockObserver = {
      observe: observeSpy,
      disconnect: jasmine.createSpy('disconnect'),
      unobserve: jasmine.createSpy('unobserve')
    };
    spyOn(window, 'IntersectionObserver').and.returnValue(mockObserver as any);

    const fixture = TestBed.createComponent(TestComponent);
    fixture.detectChanges();

    expect(window.IntersectionObserver).toHaveBeenCalled();
    expect(observeSpy).toHaveBeenCalled();
  });

  it('should disconnect observer on destroy', () => {
    const disconnectSpy = jasmine.createSpy('disconnect');
    const mockObserver = {
      observe: jasmine.createSpy('observe'),
      disconnect: disconnectSpy,
      unobserve: jasmine.createSpy('unobserve')
    };
    spyOn(window, 'IntersectionObserver').and.returnValue(mockObserver as any);

    const fixture = TestBed.createComponent(TestComponent);
    fixture.detectChanges();
    fixture.destroy();

    expect(disconnectSpy).toHaveBeenCalled();
  });
});

describe('PulseDirective', () => {
  @Component({
    template: `<div appPulse [pulseColor]="color"></div>`,
    standalone: true,
    imports: [PulseDirective]
  })
  class TestComponent {
    color = '#00d4ff';
  }

  it('should add gaming-pulse class', () => {
    const fixture = TestBed.createComponent(TestComponent);
    fixture.detectChanges();

    const div = fixture.debugElement.query(By.css('div'));
    expect(div.nativeElement.classList.contains('gaming-pulse')).toBe(true);
  });

  it('should set custom pulse color when not default', () => {
    const fixture = TestBed.createComponent(TestComponent);
    fixture.componentInstance.color = '#ff0000';
    fixture.detectChanges();

    const div = fixture.debugElement.query(By.css('div'));
    expect(div.nativeElement.style.getPropertyValue('--pulse-color')).toBe('#ff0000');
  });

  it('should not set pulse color when default', () => {
    const fixture = TestBed.createComponent(TestComponent);
    fixture.componentInstance.color = '#00d4ff';
    fixture.detectChanges();

    const div = fixture.debugElement.query(By.css('div'));
    expect(div.nativeElement.style.getPropertyValue('--pulse-color')).toBe('');
  });
});

describe('FlipCardDirective', () => {
  @Component({
    template: `<div appFlipCard [flipTrigger]="trigger"></div>`,
    standalone: true,
    imports: [FlipCardDirective]
  })
  class TestComponent {
    trigger: 'hover' | 'click' = 'hover';
  }

  it('should add flip-card class', () => {
    const fixture = TestBed.createComponent(TestComponent);
    fixture.detectChanges();

    const div = fixture.debugElement.query(By.css('div'));
    expect(div.nativeElement.classList.contains('flip-card')).toBe(true);
  });

  it('should toggle flipped class on click when trigger is click', () => {
    const fixture = TestBed.createComponent(TestComponent);
    fixture.componentInstance.trigger = 'click';
    fixture.detectChanges();

    const div = fixture.debugElement.query(By.css('div'));
    div.nativeElement.click();
    expect(div.nativeElement.classList.contains('flipped')).toBe(true);

    div.nativeElement.click();
    expect(div.nativeElement.classList.contains('flipped')).toBe(false);
  });

  it('should setup hover listeners when trigger is hover', () => {
    const fixture = TestBed.createComponent(TestComponent);
    fixture.componentInstance.trigger = 'hover';
    fixture.detectChanges();

    const div = fixture.debugElement.query(By.css('div'));
    const mouseEnterEvent = new Event('mouseenter');
    div.nativeElement.dispatchEvent(mouseEnterEvent);
    expect(div.nativeElement.classList.contains('flipped')).toBe(true);

    const mouseLeaveEvent = new Event('mouseleave');
    div.nativeElement.dispatchEvent(mouseLeaveEvent);
    expect(div.nativeElement.classList.contains('flipped')).toBe(false);
  });
});

describe('LoadingShimmerDirective', () => {
  let service: jasmine.SpyObj<PremiumInteractionsService>;

  beforeEach(() => {
    service = jasmine.createSpyObj('PremiumInteractionsService', [
      'addShimmerEffect',
      'removeShimmerEffect'
    ]);
  });

  @Component({
    template: `<div appLoadingShimmer [isLoading]="loading"></div>`,
    standalone: true,
    imports: [LoadingShimmerDirective]
  })
  class TestComponent {
    loading = false;
  }

  it('should add shimmer effect when isLoading is true', () => {
    TestBed.configureTestingModule({
      providers: [
        { provide: PremiumInteractionsService, useValue: service }
      ]
    });
    const fixture = TestBed.createComponent(TestComponent);
    fixture.componentInstance.loading = true;
    fixture.detectChanges();

    expect(service.addShimmerEffect).toHaveBeenCalled();
  });

  it('should remove shimmer effect when isLoading is false', () => {
    TestBed.configureTestingModule({
      providers: [
        { provide: PremiumInteractionsService, useValue: service }
      ]
    });
    const fixture = TestBed.createComponent(TestComponent);
    fixture.componentInstance.loading = false;
    fixture.detectChanges();

    expect(service.removeShimmerEffect).toHaveBeenCalled();
  });
});

describe('TypewriterDirective', () => {
  let service: jasmine.SpyObj<PremiumInteractionsService>;

  beforeEach(() => {
    service = jasmine.createSpyObj('PremiumInteractionsService', ['typewriterEffect']);
  });

  @Component({
    template: `<div appTypewriter [typewriterText]="text" [typewriterSpeed]="speed" [showCursor]="cursor"></div>`,
    standalone: true,
    imports: [TypewriterDirective]
  })
  class TestComponent {
    text = 'Hello World';
    speed = 50;
    cursor = true;
  }

  it('should add typewriter-cursor class when showCursor is true', () => {
    TestBed.configureTestingModule({
      providers: [
        { provide: PremiumInteractionsService, useValue: service }
      ]
    });
    const fixture = TestBed.createComponent(TestComponent);
    fixture.componentInstance.cursor = true;
    fixture.detectChanges();

    const div = fixture.debugElement.query(By.css('div'));
    expect(div.nativeElement.classList.contains('typewriter-cursor')).toBe(true);
  });

  it('should not add typewriter-cursor class when showCursor is false', () => {
    TestBed.configureTestingModule({
      providers: [
        { provide: PremiumInteractionsService, useValue: service }
      ]
    });
    const fixture = TestBed.createComponent(TestComponent);
    fixture.componentInstance.cursor = false;
    fixture.detectChanges();

    const div = fixture.debugElement.query(By.css('div'));
    expect(div.nativeElement.classList.contains('typewriter-cursor')).toBe(false);
  });

  it('should call typewriter effect when text is provided', () => {
    TestBed.configureTestingModule({
      providers: [
        { provide: PremiumInteractionsService, useValue: service }
      ]
    });
    const fixture = TestBed.createComponent(TestComponent);
    fixture.componentInstance.text = 'Test Text';
    fixture.componentInstance.speed = 100;
    fixture.detectChanges();

    expect(service.typewriterEffect).toHaveBeenCalledWith(
      jasmine.any(ElementRef),
      'Test Text',
      100
    );
  });

  it('should not call typewriter effect when text is empty', () => {
    TestBed.configureTestingModule({
      providers: [
        { provide: PremiumInteractionsService, useValue: service }
      ]
    });
    const fixture = TestBed.createComponent(TestComponent);
    fixture.componentInstance.text = '';
    fixture.detectChanges();

    expect(service.typewriterEffect).not.toHaveBeenCalled();
  });
});

describe('AnimatedBorderDirective', () => {
  @Component({
    template: `<div appAnimatedBorder></div>`,
    standalone: true,
    imports: [AnimatedBorderDirective]
  })
  class TestComponent {}

  it('should add animated-border class', () => {
    const fixture = TestBed.createComponent(TestComponent);
    fixture.detectChanges();

    const div = fixture.debugElement.query(By.css('div'));
    expect(div.nativeElement.classList.contains('animated-border')).toBe(true);
  });
});

describe('ParticleClickDirective', () => {
  let service: jasmine.SpyObj<PremiumInteractionsService>;

  beforeEach(() => {
    service = jasmine.createSpyObj('PremiumInteractionsService', ['createParticleExplosion']);
  });

  @Component({
    template: `<button appParticleClick [particleCount]="count" [particleColor]="color">Click me</button>`,
    standalone: true,
    imports: [ParticleClickDirective]
  })
  class TestComponent {
    count = 20;
    color = '#00d4ff';
  }

  it('should create particle explosion on click', () => {
    TestBed.configureTestingModule({
      providers: [
        { provide: PremiumInteractionsService, useValue: service }
      ]
    });
    const fixture = TestBed.createComponent(TestComponent);
    fixture.detectChanges();

    const button = fixture.debugElement.query(By.css('button'));
    button.nativeElement.click();

    expect(service.createParticleExplosion).toHaveBeenCalledWith(
      jasmine.any(ElementRef),
      20
    );
  });

  it('should use custom particle count', () => {
    TestBed.configureTestingModule({
      providers: [
        { provide: PremiumInteractionsService, useValue: service }
      ]
    });
    const fixture = TestBed.createComponent(TestComponent);
    fixture.componentInstance.count = 50;
    fixture.detectChanges();

    const button = fixture.debugElement.query(By.css('button'));
    button.nativeElement.click();

    expect(service.createParticleExplosion).toHaveBeenCalledWith(
      jasmine.any(ElementRef),
      50
    );
  });
});
