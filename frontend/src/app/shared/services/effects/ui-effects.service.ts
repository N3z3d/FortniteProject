import { Injectable, ElementRef, Renderer2, RendererFactory2 } from '@angular/core';
import { secureRandomFloat } from '../../utils/secure-random.util';

@Injectable({
  providedIn: 'root'
})
export class UiEffectsService {
  private renderer: Renderer2;

  constructor(private rendererFactory: RendererFactory2) {
    this.renderer = this.rendererFactory.createRenderer(null, null);
  }

  initParallaxCard(element: ElementRef): void {
    const card = element.nativeElement;

    const onMouseMove = (e: MouseEvent) => {
      const rect = card.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;

      const centerX = rect.width / 2;
      const centerY = rect.height / 2;

      const rotateX = (y - centerY) / 10;
      const rotateY = (centerX - x) / 10;

      this.renderer.setStyle(card, 'transform',
        `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) scale3d(1.02, 1.02, 1.02)`);
    };

    const onMouseLeave = () => {
      this.renderer.setStyle(card, 'transform',
        'perspective(1000px) rotateX(0deg) rotateY(0deg) scale3d(1, 1, 1)');
    };

    this.renderer.listen(card, 'mousemove', onMouseMove);
    this.renderer.listen(card, 'mouseleave', onMouseLeave);
  }

  showGamingNotification(message: string, type: 'success' | 'error' | 'info' = 'info'): void {
    const notification = this.renderer.createElement('div');
    this.renderer.addClass(notification, 'gaming-notification');
    this.renderer.addClass(notification, `notification-${type}`);

    this.renderer.setProperty(notification, 'textContent', message);
    this.renderer.setStyle(notification, 'position', 'fixed');
    this.renderer.setStyle(notification, 'top', '20px');
    this.renderer.setStyle(notification, 'right', '20px');
    this.renderer.setStyle(notification, 'z-index', '10000');
    this.renderer.setStyle(notification, 'padding', '16px 24px');
    this.renderer.setStyle(notification, 'border-radius', '12px');
    this.renderer.setStyle(notification, 'font-family', "'Inter', sans-serif");
    this.renderer.setStyle(notification, 'font-weight', '600');
    this.renderer.setStyle(notification, 'color', 'white');
    this.renderer.setStyle(notification, 'backdrop-filter', 'blur(20px)');
    this.renderer.setStyle(notification, 'border', '1px solid rgba(255, 255, 255, 0.2)');
    this.renderer.setStyle(notification, 'transform', 'translateX(100%)');
    this.renderer.setStyle(notification, 'transition', 'transform 0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55)');

    const backgrounds = {
      success: 'rgba(0, 255, 136, 0.9)',
      error: 'rgba(255, 51, 102, 0.9)',
      info: 'rgba(0, 212, 255, 0.9)'
    };

    this.renderer.setStyle(notification, 'background', backgrounds[type]);
    this.renderer.appendChild(document.body, notification);

    setTimeout(() => {
      this.renderer.setStyle(notification, 'transform', 'translateX(0)');
    }, 100);

    setTimeout(() => {
      this.renderer.setStyle(notification, 'transform', 'translateX(100%)');
      setTimeout(() => {
        if (notification.parentNode) {
          this.renderer.removeChild(document.body, notification);
        }
      }, 400);
    }, 3000);
  }

  addShimmerEffect(element: ElementRef): void {
    const el = element.nativeElement;

    const shimmer = this.renderer.createElement('div');
    this.renderer.addClass(shimmer, 'shimmer-overlay');
    this.renderer.setStyle(shimmer, 'position', 'absolute');
    this.renderer.setStyle(shimmer, 'top', '0');
    this.renderer.setStyle(shimmer, 'left', '-100%');
    this.renderer.setStyle(shimmer, 'width', '100%');
    this.renderer.setStyle(shimmer, 'height', '100%');
    this.renderer.setStyle(shimmer, 'background',
      'linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent)');
    this.renderer.setStyle(shimmer, 'animation', 'shimmer 2s infinite');
    this.renderer.setStyle(shimmer, 'pointer-events', 'none');

    this.renderer.setStyle(el, 'position', 'relative');
    this.renderer.setStyle(el, 'overflow', 'hidden');

    this.renderer.appendChild(el, shimmer);
  }

  removeShimmerEffect(element: ElementRef): void {
    const el = element.nativeElement;
    const shimmer = el.querySelector('.shimmer-overlay');

    if (shimmer) {
      this.renderer.removeChild(el, shimmer);
    }
  }

  addScreenShake(duration: number): void {
    const intensity = 2;
    const startTime = Date.now();

    const shake = () => {
      const elapsed = Date.now() - startTime;
      if (elapsed < duration) {
        const x = (secureRandomFloat() - 0.5) * intensity;
        const y = (secureRandomFloat() - 0.5) * intensity;

        this.renderer.setStyle(document.body, 'transform',
          `translateX(${x}px) translateY(${y}px)`);

        requestAnimationFrame(shake);
      } else {
        this.renderer.setStyle(document.body, 'transform', 'none');
      }
    };

    shake();
  }
}
