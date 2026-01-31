import { Injectable, ElementRef, Renderer2, RendererFactory2 } from '@angular/core';
import { ButtonEffectsService } from './effects/button-effects.service';
import { ParticleEffectsService } from './effects/particle-effects.service';
import { UiEffectsService } from './effects/ui-effects.service';
import { AnimationEffectsService } from './effects/animation-effects.service';

export interface FortniteButtonOptions {
  type?: 'primary' | 'secondary' | 'danger' | 'success';
  size?: 'small' | 'medium' | 'large';
  effects?: boolean;
  importance?: 'low' | 'medium' | 'high' | 'critical';
}

@Injectable({
  providedIn: 'root'
})
export class PremiumInteractionsService {
  private renderer: Renderer2;

  constructor(
    private rendererFactory: RendererFactory2,
    private buttonEffects: ButtonEffectsService,
    private particleEffects: ParticleEffectsService,
    private uiEffects: UiEffectsService,
    private animationEffects: AnimationEffectsService
  ) {
    this.renderer = this.rendererFactory.createRenderer(null, null);
  }

  initMagneticButton(element: ElementRef, strength: number = 0.5): void {
    this.buttonEffects.initMagneticButton(element, strength);
  }

  createRipple(element: ElementRef, event: MouseEvent): void {
    this.buttonEffects.createRipple(element, event);
  }

  initSpringButton(element: ElementRef): void {
    this.buttonEffects.initSpringButton(element);
  }

  initParallaxCard(element: ElementRef): void {
    this.uiEffects.initParallaxCard(element);
  }

  initGlowEffect(element: ElementRef, color: string = '#00d4ff'): void {
    this.buttonEffects.initGlowEffect(element, color);
  }

  smoothScrollTo(target: string, duration: number = 800): void {
    this.animationEffects.smoothScrollTo(target, duration);
  }

  typewriterEffect(element: ElementRef, text: string, speed: number = 50): Promise<void> {
    return this.animationEffects.typewriterEffect(element, text, speed);
  }

  createParticleExplosion(element: ElementRef, particleCount: number = 30): void {
    this.particleEffects.createParticleExplosion(element, particleCount);
    this.uiEffects.addScreenShake(200);
  }

  showGamingNotification(message: string, type: 'success' | 'error' | 'info' = 'info'): void {
    this.uiEffects.showGamingNotification(message, type);
  }

  addShimmerEffect(element: ElementRef): void {
    this.uiEffects.addShimmerEffect(element);
  }

  removeShimmerEffect(element: ElementRef): void {
    this.uiEffects.removeShimmerEffect(element);
  }

  addContextualFeedback(element: ElementRef, importance: 'low' | 'medium' | 'high' | 'critical'): void {
    const btn = element.nativeElement;

    const effects = {
      low: { particles: 8, magneticStrength: 0.2, glowIntensity: 0.3 },
      medium: { particles: 15, magneticStrength: 0.4, glowIntensity: 0.5 },
      high: { particles: 25, magneticStrength: 0.6, glowIntensity: 0.7 },
      critical: { particles: 40, magneticStrength: 0.8, glowIntensity: 1.0 }
    };

    const config = effects[importance];

    this.renderer.setStyle(btn, '--importance-glow', `${config.glowIntensity}`);
    this.renderer.addClass(btn, `importance-${importance}`);

    this.renderer.listen(btn, 'click', () => {
      this.createParticleExplosion(element, config.particles);
    });
  }

  initFortniteButton(element: ElementRef, options: FortniteButtonOptions = {}): void {
    const btn = element.nativeElement;
    const { type = 'primary', size = 'medium', effects = true } = options;

    this.renderer.addClass(btn, 'fortnite-btn');
    this.renderer.addClass(btn, `fortnite-btn-${type}`);
    this.renderer.addClass(btn, `fortnite-btn-${size}`);

    if (effects) {
      if (type === 'primary' || type === 'danger') {
        this.initMagneticButton(element, 0.6);
        this.addContextualFeedback(element, 'high');
      } else {
        this.initMagneticButton(element, 0.3);
        this.addContextualFeedback(element, 'medium');
      }

      this.initSpringButton(element);
      this.initGlowEffect(element);
    }
  }
}