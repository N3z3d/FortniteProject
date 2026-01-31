import { Injectable, ElementRef, Renderer2, RendererFactory2 } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ButtonEffectsService {
  private renderer: Renderer2;

  constructor(private rendererFactory: RendererFactory2) {
    this.renderer = this.rendererFactory.createRenderer(null, null);
  }

  initMagneticButton(element: ElementRef, strength: number = 0.5): void {
    const btn = element.nativeElement;
    let isHovering = false;
    let magneticZone = 150;

    const onMouseMove = (e: MouseEvent) => {
      if (!isHovering) return;

      const rect = btn.getBoundingClientRect();
      const centerX = rect.left + rect.width / 2;
      const centerY = rect.top + rect.height / 2;
      const x = e.clientX - centerX;
      const y = e.clientY - centerY;

      const distance = Math.sqrt(x * x + y * y);
      const normalizedDistance = Math.min(distance / magneticZone, 1);
      const magneticStrength = (1 - normalizedDistance) * strength;

      const magneticX = x * magneticStrength * 0.8;
      const magneticY = y * magneticStrength * 0.8;

      const rotateX = (y / rect.height) * 10;
      const rotateY = -(x / rect.width) * 10;
      const scale = 1 + magneticStrength * 0.1;

      this.renderer.setStyle(btn, 'transform',
        `perspective(1000px) translateX(${magneticX}px) translateY(${magneticY}px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) scale(${scale})`);

      const glowIntensity = magneticStrength;
      this.renderer.setStyle(btn, '--glow-intensity', `${glowIntensity}`);
      this.renderer.addClass(btn, 'magnetic-hover');
    };

    const onMouseEnter = () => {
      isHovering = true;
      this.renderer.addClass(btn, 'magnetic-active');
      this.renderer.setStyle(btn, 'transition', 'all 0.15s cubic-bezier(0.25, 0.46, 0.45, 0.94)');
    };

    const onMouseLeave = () => {
      isHovering = false;
      this.renderer.removeClass(btn, 'magnetic-active');
      this.renderer.removeClass(btn, 'magnetic-hover');
      this.renderer.setStyle(btn, 'transition', 'all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275)');
      this.renderer.setStyle(btn, 'transform', 'perspective(1000px) translateX(0) translateY(0) rotateX(0deg) rotateY(0deg) scale(1)');
      this.renderer.removeStyle(btn, '--glow-intensity');
    };

    this.renderer.listen(btn, 'mousemove', onMouseMove);
    this.renderer.listen(btn, 'mouseenter', onMouseEnter);
    this.renderer.listen(btn, 'mouseleave', onMouseLeave);
  }

  createRipple(element: ElementRef, event: MouseEvent): void {
    const btn = element.nativeElement;
    const rect = btn.getBoundingClientRect();

    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;

    const ripple = this.renderer.createElement('span');
    this.renderer.addClass(ripple, 'ripple');

    this.renderer.setStyle(ripple, 'left', `${x}px`);
    this.renderer.setStyle(ripple, 'top', `${y}px`);
    this.renderer.setStyle(ripple, 'position', 'absolute');
    this.renderer.setStyle(ripple, 'border-radius', '50%');
    this.renderer.setStyle(ripple, 'background', 'rgba(255, 255, 255, 0.6)');
    this.renderer.setStyle(ripple, 'transform', 'scale(0)');
    this.renderer.setStyle(ripple, 'animation', 'rippleEffect 0.6s ease-out');
    this.renderer.setStyle(ripple, 'pointer-events', 'none');
    this.renderer.setStyle(ripple, 'z-index', '1');

    this.renderer.appendChild(btn, ripple);

    setTimeout(() => {
      if (ripple.parentNode) {
        this.renderer.removeChild(btn, ripple);
      }
    }, 600);
  }

  initSpringButton(element: ElementRef): void {
    const btn = element.nativeElement;
    let isPressed = false;

    const onMouseDown = () => {
      isPressed = true;

      this.renderer.setStyle(btn, 'transform',
        'perspective(1000px) translateZ(-8px) scale(0.95) rotateX(5deg)');
      this.renderer.setStyle(btn, 'box-shadow',
        'inset 0 4px 8px rgba(0, 0, 0, 0.3), 0 2px 4px rgba(0, 0, 0, 0.2)');
      this.renderer.setStyle(btn, 'filter', 'brightness(0.9)');
    };

    const onMouseUp = () => {
      if (!isPressed) return;
      isPressed = false;

      this.renderer.setStyle(btn, 'transition', 'all 0.3s cubic-bezier(0.68, -0.55, 0.265, 1.55)');
      this.renderer.setStyle(btn, 'transform',
        'perspective(1000px) translateZ(0px) scale(1.05) rotateX(0deg)');
      this.renderer.removeStyle(btn, 'box-shadow');
      this.renderer.removeStyle(btn, 'filter');

      setTimeout(() => {
        this.renderer.setStyle(btn, 'transform',
          'perspective(1000px) translateZ(0px) scale(1) rotateX(0deg)');
      }, 300);
    };

    const onMouseLeave = () => {
      if (!isPressed) return;
      isPressed = false;

      this.renderer.setStyle(btn, 'transition', 'all 0.2s ease-out');
      this.renderer.setStyle(btn, 'transform',
        'perspective(1000px) translateZ(0px) scale(1) rotateX(0deg)');
      this.renderer.removeStyle(btn, 'box-shadow');
      this.renderer.removeStyle(btn, 'filter');
    };

    this.renderer.listen(btn, 'mousedown', onMouseDown);
    this.renderer.listen(btn, 'mouseup', onMouseUp);
    this.renderer.listen(btn, 'mouseleave', onMouseLeave);
  }

  initGlowEffect(element: ElementRef, color: string = '#00d4ff'): void {
    const el = element.nativeElement;

    const onMouseEnter = () => {
      this.renderer.setStyle(el, 'box-shadow',
        `0 0 30px ${color}40, 0 0 60px ${color}20, inset 0 0 20px ${color}10`);
      this.renderer.setStyle(el, 'border-color', `${color}80`);
    };

    const onMouseLeave = () => {
      this.renderer.removeStyle(el, 'box-shadow');
      this.renderer.removeStyle(el, 'border-color');
    };

    this.renderer.listen(el, 'mouseenter', onMouseEnter);
    this.renderer.listen(el, 'mouseleave', onMouseLeave);
  }
}
