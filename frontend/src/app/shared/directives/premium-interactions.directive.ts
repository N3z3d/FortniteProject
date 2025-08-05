import { Directive, ElementRef, Input, OnInit, OnDestroy, HostListener } from '@angular/core';
import { PremiumInteractionsService } from '../services/premium-interactions.service';

@Directive({
  selector: '[appPremiumInteractions]',
  standalone: true
})
export class PremiumInteractionsDirective implements OnInit, OnDestroy {
  @Input() interactionType: 'magnetic' | 'ripple' | 'spring' | 'parallax' | 'glow' = 'magnetic';
  @Input() glowColor: string = '#00d4ff';
  @Input() magneticStrength: number = 0.3;

  constructor(
    private el: ElementRef,
    private interactionsService: PremiumInteractionsService
  ) {}

  ngOnInit(): void {
    this.initInteraction();
  }

  ngOnDestroy(): void {
    // Cleanup will be handled by the service
  }

  private initInteraction(): void {
    switch (this.interactionType) {
      case 'magnetic':
        this.interactionsService.initMagneticButton(this.el);
        break;
      case 'spring':
        this.interactionsService.initSpringButton(this.el);
        break;
      case 'parallax':
        this.interactionsService.initParallaxCard(this.el);
        break;
      case 'glow':
        this.interactionsService.initGlowEffect(this.el, this.glowColor);
        break;
    }
  }

  @HostListener('click', ['$event'])
  onClick(event: MouseEvent): void {
    if (this.interactionType === 'ripple') {
      this.interactionsService.createRipple(this.el, event);
    }
  }
}

@Directive({
  selector: '[appTooltip]',
  standalone: true
})
export class TooltipDirective implements OnInit {
  @Input('appTooltip') tooltipText: string = '';

  constructor(private el: ElementRef) {}

  ngOnInit(): void {
    this.el.nativeElement.classList.add('premium-tooltip');
    this.el.nativeElement.setAttribute('data-tooltip', this.tooltipText);
  }
}

@Directive({
  selector: '[appRevealOnScroll]',
  standalone: true
})
export class RevealOnScrollDirective implements OnInit, OnDestroy {
  @Input() revealThreshold: number = 0.1;
  @Input() revealDelay: number = 0;

  private observer: IntersectionObserver | null = null;

  constructor(private el: ElementRef) {}

  ngOnInit(): void {
    this.el.nativeElement.classList.add('reveal-animation');
    this.setupIntersectionObserver();
  }

  ngOnDestroy(): void {
    if (this.observer) {
      this.observer.disconnect();
    }
  }

  private setupIntersectionObserver(): void {
    this.observer = new IntersectionObserver(
      (entries) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            setTimeout(() => {
              entry.target.classList.add('revealed');
            }, this.revealDelay);
            this.observer?.unobserve(entry.target);
          }
        });
      },
      {
        threshold: this.revealThreshold,
        rootMargin: '0px 0px -10% 0px'
      }
    );

    this.observer.observe(this.el.nativeElement);
  }
}

@Directive({
  selector: '[appPulse]',
  standalone: true
})
export class PulseDirective implements OnInit {
  @Input() pulseColor: string = '#00d4ff';

  constructor(private el: ElementRef) {}

  ngOnInit(): void {
    this.el.nativeElement.classList.add('gaming-pulse');
    if (this.pulseColor !== '#00d4ff') {
      this.el.nativeElement.style.setProperty('--pulse-color', this.pulseColor);
    }
  }
}

@Directive({
  selector: '[appFlipCard]',
  standalone: true
})
export class FlipCardDirective implements OnInit {
  @Input() flipTrigger: 'hover' | 'click' = 'hover';
  private isFlipped = false;

  constructor(private el: ElementRef) {}

  ngOnInit(): void {
    this.el.nativeElement.classList.add('flip-card');
    this.setupFlipTrigger();
  }

  private setupFlipTrigger(): void {
    if (this.flipTrigger === 'hover') {
      this.el.nativeElement.addEventListener('mouseenter', () => this.flip());
      this.el.nativeElement.addEventListener('mouseleave', () => this.flip());
    } else {
      this.el.nativeElement.addEventListener('click', () => this.flip());
    }
  }

  private flip(): void {
    this.isFlipped = !this.isFlipped;
    if (this.isFlipped) {
      this.el.nativeElement.classList.add('flipped');
    } else {
      this.el.nativeElement.classList.remove('flipped');
    }
  }
}

@Directive({
  selector: '[appLoadingShimmer]',
  standalone: true
})
export class LoadingShimmerDirective implements OnInit {
  @Input() isLoading: boolean = false;

  constructor(
    private el: ElementRef,
    private interactionsService: PremiumInteractionsService
  ) {}

  ngOnInit(): void {
    this.updateShimmer();
  }

  ngOnChanges(): void {
    this.updateShimmer();
  }

  private updateShimmer(): void {
    if (this.isLoading) {
      this.interactionsService.addShimmerEffect(this.el);
    } else {
      this.interactionsService.removeShimmerEffect(this.el);
    }
  }
}

@Directive({
  selector: '[appTypewriter]',
  standalone: true
})
export class TypewriterDirective implements OnInit {
  @Input() typewriterText: string = '';
  @Input() typewriterSpeed: number = 50;
  @Input() showCursor: boolean = true;

  constructor(
    private el: ElementRef,
    private interactionsService: PremiumInteractionsService
  ) {}

  ngOnInit(): void {
    if (this.showCursor) {
      this.el.nativeElement.classList.add('typewriter-cursor');
    }
    
    if (this.typewriterText) {
      this.interactionsService.typewriterEffect(
        this.el, 
        this.typewriterText, 
        this.typewriterSpeed
      );
    }
  }
}

@Directive({
  selector: '[appAnimatedBorder]',
  standalone: true
})
export class AnimatedBorderDirective implements OnInit {
  constructor(private el: ElementRef) {}

  ngOnInit(): void {
    this.el.nativeElement.classList.add('animated-border');
  }
}

@Directive({
  selector: '[appParticleClick]',
  standalone: true
})
export class ParticleClickDirective {
  @Input() particleCount: number = 20;
  @Input() particleColor: string = '#00d4ff';

  constructor(
    private el: ElementRef,
    private interactionsService: PremiumInteractionsService
  ) {}

  @HostListener('click', ['$event'])
  onClick(event: MouseEvent): void {
    this.interactionsService.createParticleExplosion(this.el, this.particleCount);
  }
}