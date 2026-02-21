import { Injectable, ElementRef, Renderer2, RendererFactory2 } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AnimationEffectsService {
  private renderer: Renderer2;

  constructor(private rendererFactory: RendererFactory2) {
    this.renderer = this.rendererFactory.createRenderer(null, null);
  }

  smoothScrollTo(target: string, duration: number = 800): void {
    const targetElement = document.querySelector<HTMLElement>(target);
    if (!targetElement) return;

    const targetPosition = targetElement.offsetTop - 80;
    const startPosition = window.scrollY;
    const distance = targetPosition - startPosition;
    let startTime: number | null = null;

    const easeInOutCubic = (t: number): number => {
      return t < 0.5 ? 4 * t * t * t : (t - 1) * (2 * t - 2) * (2 * t - 2) + 1;
    };

    const animation = (currentTime: number) => {
      if (startTime === null) startTime = currentTime;
      const timeElapsed = currentTime - startTime;
      const progress = Math.min(timeElapsed / duration, 1);
      const ease = easeInOutCubic(progress);

      window.scrollTo(0, startPosition + distance * ease);

      if (timeElapsed < duration) {
        requestAnimationFrame(animation);
      }
    };

    requestAnimationFrame(animation);
  }

  typewriterEffect(element: ElementRef, text: string, speed: number = 50): Promise<void> {
    return new Promise((resolve) => {
      const el = element.nativeElement;
      let i = 0;

      this.renderer.setProperty(el, 'textContent', '');

      const typing = () => {
        if (i < text.length) {
          this.renderer.setProperty(el, 'textContent',
            el.textContent + text.charAt(i));
          i++;
          setTimeout(typing, speed);
        } else {
          resolve();
        }
      };

      typing();
    });
  }
}
