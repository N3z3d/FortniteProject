import { Injectable, ElementRef, Renderer2, RendererFactory2 } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ParticleEffectsService {
  private renderer: Renderer2;

  constructor(private rendererFactory: RendererFactory2) {
    this.renderer = this.rendererFactory.createRenderer(null, null);
  }

  createParticleExplosion(element: ElementRef, particleCount: number = 30): void {
    const container = element.nativeElement;
    const rect = container.getBoundingClientRect();

    for (let wave = 0; wave < 3; wave++) {
      setTimeout(() => {
        for (let i = 0; i < particleCount / 3; i++) {
          const particle = this.renderer.createElement('div');
          this.renderer.addClass(particle, 'explosion-particle');

          const angle = (Math.PI * 2 * i) / (particleCount / 3) + (wave * 0.3);
          const velocity = 80 + Math.random() * 120 + (wave * 20);
          const size = 3 + Math.random() * 6;
          const lifetime = 0.8 + Math.random() * 0.4;

          const colors = ['#00d4ff', '#ff6b35', '#00ff88', '#ffaa00'];
          const color = colors[Math.floor(Math.random() * colors.length)];

          this.renderer.setStyle(particle, 'position', 'absolute');
          this.renderer.setStyle(particle, 'width', `${size}px`);
          this.renderer.setStyle(particle, 'height', `${size}px`);
          this.renderer.setStyle(particle, 'background', color);
          this.renderer.setStyle(particle, 'border-radius', '50%');
          this.renderer.setStyle(particle, 'pointer-events', 'none');
          this.renderer.setStyle(particle, 'left', `${rect.width / 2}px`);
          this.renderer.setStyle(particle, 'top', `${rect.height / 2}px`);
          this.renderer.setStyle(particle, 'box-shadow', `0 0 ${size * 2}px ${color}`);
          this.renderer.setStyle(particle, 'z-index', '1000');

          const deltaX = Math.cos(angle) * velocity;
          const deltaY = Math.sin(angle) * velocity;

          this.renderer.setStyle(particle, 'animation',
            `particleExplode ${lifetime}s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards`);
          this.renderer.setStyle(particle, '--deltaX', `${deltaX}px`);
          this.renderer.setStyle(particle, '--deltaY', `${deltaY}px`);

          this.renderer.appendChild(container, particle);

          setTimeout(() => {
            if (particle.parentNode) {
              this.renderer.removeChild(container, particle);
            }
          }, lifetime * 1000);
        }
      }, wave * 100);
    }
  }

  createMagneticParticle(btn: HTMLElement, x: number, y: number): void {
    const particle = this.renderer.createElement('div');
    this.renderer.setStyle(particle, 'position', 'absolute');
    this.renderer.setStyle(particle, 'width', '3px');
    this.renderer.setStyle(particle, 'height', '3px');
    this.renderer.setStyle(particle, 'background', 'var(--gaming-primary)');
    this.renderer.setStyle(particle, 'border-radius', '50%');
    this.renderer.setStyle(particle, 'pointer-events', 'none');
    this.renderer.setStyle(particle, 'left', `${x}px`);
    this.renderer.setStyle(particle, 'top', `${y}px`);
    this.renderer.setStyle(particle, 'box-shadow', '0 0 6px var(--gaming-primary)');
    this.renderer.setStyle(particle, 'opacity', '0.8');
    this.renderer.setStyle(particle, 'z-index', '1000');
    this.renderer.setStyle(particle, 'animation', 'magneticParticleFade 0.5s ease-out forwards');

    this.renderer.appendChild(btn, particle);

    setTimeout(() => {
      if (particle.parentNode) {
        this.renderer.removeChild(btn, particle);
      }
    }, 500);
  }

  createPressRipple(btn: HTMLElement, event: MouseEvent): void {
    const rect = btn.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;

    const ripple = this.renderer.createElement('div');
    this.renderer.setStyle(ripple, 'position', 'absolute');
    this.renderer.setStyle(ripple, 'left', `${x}px`);
    this.renderer.setStyle(ripple, 'top', `${y}px`);
    this.renderer.setStyle(ripple, 'width', '0');
    this.renderer.setStyle(ripple, 'height', '0');
    this.renderer.setStyle(ripple, 'border-radius', '50%');
    this.renderer.setStyle(ripple, 'background', 'rgba(255, 255, 255, 0.4)');
    this.renderer.setStyle(ripple, 'pointer-events', 'none');
    this.renderer.setStyle(ripple, 'transform', 'translate(-50%, -50%)');
    this.renderer.setStyle(ripple, 'animation', 'pressRipple 0.4s ease-out forwards');

    this.renderer.appendChild(btn, ripple);

    setTimeout(() => {
      if (ripple.parentNode) {
        this.renderer.removeChild(btn, ripple);
      }
    }, 400);
  }
}
