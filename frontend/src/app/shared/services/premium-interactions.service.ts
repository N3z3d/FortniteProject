import { Injectable, ElementRef, Renderer2, RendererFactory2 } from '@angular/core';

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

  constructor(private rendererFactory: RendererFactory2) {
    this.renderer = this.rendererFactory.createRenderer(null, null);
  }

  // ===== ENHANCED MAGNETIC BUTTON EFFECT =====
  initMagneticButton(element: ElementRef, strength: number = 0.5): void {
    const btn = element.nativeElement;
    let isHovering = false;
    let magneticZone = 150; // Increased magnetic zone

    const onMouseMove = (e: MouseEvent) => {
      if (!isHovering) return;

      const rect = btn.getBoundingClientRect();
      const centerX = rect.left + rect.width / 2;
      const centerY = rect.top + rect.height / 2;
      const x = e.clientX - centerX;
      const y = e.clientY - centerY;

      // Enhanced magnetic calculation with stronger attraction
      const distance = Math.sqrt(x * x + y * y);
      const normalizedDistance = Math.min(distance / magneticZone, 1);
      const magneticStrength = (1 - normalizedDistance) * strength;
      
      // Stronger magnetic pull with easing
      const magneticX = x * magneticStrength * 0.8;
      const magneticY = y * magneticStrength * 0.8;
      
      // 3D transformation with perspective
      const rotateX = (y / rect.height) * 10;
      const rotateY = -(x / rect.width) * 10;
      const scale = 1 + magneticStrength * 0.1;

      this.renderer.setStyle(btn, 'transform', 
        `perspective(1000px) translateX(${magneticX}px) translateY(${magneticY}px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) scale(${scale})`);
      
      // Enhanced glow based on proximity
      const glowIntensity = magneticStrength;
      this.renderer.setStyle(btn, '--glow-intensity', `${glowIntensity}`);
      this.renderer.addClass(btn, 'magnetic-hover');
      
      // Particle trail effect
      if (Math.random() < 0.1 && magneticStrength > 0.3) {
        this.createMagneticParticle(btn, e.clientX - rect.left, e.clientY - rect.top);
      }
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

  // ===== RIPPLE EFFECT =====
  createRipple(element: ElementRef, event: MouseEvent): void {
    const btn = element.nativeElement;
    const rect = btn.getBoundingClientRect();
    
    // Calculate ripple position
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;
    
    // Create ripple element
    const ripple = this.renderer.createElement('span');
    this.renderer.addClass(ripple, 'ripple');
    
    // Set ripple styles
    this.renderer.setStyle(ripple, 'left', `${x}px`);
    this.renderer.setStyle(ripple, 'top', `${y}px`);
    this.renderer.setStyle(ripple, 'position', 'absolute');
    this.renderer.setStyle(ripple, 'border-radius', '50%');
    this.renderer.setStyle(ripple, 'background', 'rgba(255, 255, 255, 0.6)');
    this.renderer.setStyle(ripple, 'transform', 'scale(0)');
    this.renderer.setStyle(ripple, 'animation', 'rippleEffect 0.6s ease-out');
    this.renderer.setStyle(ripple, 'pointer-events', 'none');
    this.renderer.setStyle(ripple, 'z-index', '1');
    
    // Add ripple to button
    this.renderer.appendChild(btn, ripple);
    
    // Remove ripple after animation
    setTimeout(() => {
      if (ripple.parentNode) {
        this.renderer.removeChild(btn, ripple);
      }
    }, 600);
  }

  // ===== ENHANCED 3D SPRING BUTTON ANIMATION =====
  initSpringButton(element: ElementRef): void {
    const btn = element.nativeElement;
    let isPressed = false;
    
    const onMouseDown = (e: MouseEvent) => {
      isPressed = true;
      
      // 3D press effect with realistic depth
      this.renderer.setStyle(btn, 'transform', 
        'perspective(1000px) translateZ(-8px) scale(0.95) rotateX(5deg)');
      this.renderer.setStyle(btn, 'box-shadow', 
        'inset 0 4px 8px rgba(0, 0, 0, 0.3), 0 2px 4px rgba(0, 0, 0, 0.2)');
      this.renderer.setStyle(btn, 'filter', 'brightness(0.9)');
      
      // Create press ripple
      this.createPressRipple(btn, e);
    };
    
    const onMouseUp = () => {
      if (!isPressed) return;
      isPressed = false;
      
      // Spring back animation with bounce
      this.renderer.setStyle(btn, 'transition', 'all 0.3s cubic-bezier(0.68, -0.55, 0.265, 1.55)');
      this.renderer.setStyle(btn, 'transform', 
        'perspective(1000px) translateZ(0px) scale(1.05) rotateX(0deg)');
      this.renderer.removeStyle(btn, 'box-shadow');
      this.renderer.removeStyle(btn, 'filter');
      
      // Reset to normal after bounce
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

  // ===== PARALLAX CARDS =====
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

  // ===== PREMIUM HOVER GLOW =====
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

  // ===== SMOOTH SCROLL WITH EASING =====
  smoothScrollTo(target: string, duration: number = 800): void {
    const targetElement = document.querySelector(target) as HTMLElement;
    if (!targetElement) return;

    const targetPosition = targetElement.offsetTop - 80; // Account for fixed navbar
    const startPosition = window.pageYOffset;
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

  // ===== TYPEWRITER EFFECT =====
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

  // ===== ENHANCED PARTICLE EXPLOSION EFFECT =====
  createParticleExplosion(element: ElementRef, particleCount: number = 30): void {
    const container = element.nativeElement;
    const rect = container.getBoundingClientRect();
    
    // Create multiple waves of particles
    for (let wave = 0; wave < 3; wave++) {
      setTimeout(() => {
        for (let i = 0; i < particleCount / 3; i++) {
          const particle = this.renderer.createElement('div');
          this.renderer.addClass(particle, 'explosion-particle');
          
          // Enhanced particle properties
          const angle = (Math.PI * 2 * i) / (particleCount / 3) + (wave * 0.3);
          const velocity = 80 + Math.random() * 120 + (wave * 20);
          const size = 3 + Math.random() * 6;
          const lifetime = 0.8 + Math.random() * 0.4;
          
          // Random particle colors for variety
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
          
          // Remove particle after animation
          setTimeout(() => {
            if (particle.parentNode) {
              this.renderer.removeChild(container, particle);
            }
          }, lifetime * 1000);
        }
      }, wave * 100);
    }
    
    // Screen shake effect
    this.addScreenShake(200);
  }

  // ===== GAMING NOTIFICATION =====
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
    
    // Set background based on type
    const backgrounds = {
      success: 'rgba(0, 255, 136, 0.9)',
      error: 'rgba(255, 51, 102, 0.9)',
      info: 'rgba(0, 212, 255, 0.9)'
    };
    
    this.renderer.setStyle(notification, 'background', backgrounds[type]);
    this.renderer.appendChild(document.body, notification);
    
    // Animate in
    setTimeout(() => {
      this.renderer.setStyle(notification, 'transform', 'translateX(0)');
    }, 100);
    
    // Animate out and remove
    setTimeout(() => {
      this.renderer.setStyle(notification, 'transform', 'translateX(100%)');
      setTimeout(() => {
        if (notification.parentNode) {
          this.renderer.removeChild(document.body, notification);
        }
      }, 400);
    }, 3000);
  }

  // ===== LOADING SHIMMER EFFECT =====
  addShimmerEffect(element: ElementRef): void {
    const el = element.nativeElement;
    
    // Create shimmer overlay
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
    
    // Ensure parent has relative positioning
    this.renderer.setStyle(el, 'position', 'relative');
    this.renderer.setStyle(el, 'overflow', 'hidden');
    
    this.renderer.appendChild(el, shimmer);
  }

  // ===== REMOVE SHIMMER EFFECT =====
  removeShimmerEffect(element: ElementRef): void {
    const el = element.nativeElement;
    const shimmer = el.querySelector('.shimmer-overlay');
    
    if (shimmer) {
      this.renderer.removeChild(el, shimmer);
    }
  }

  // ===== MAGNETIC PARTICLE TRAIL =====
  private createMagneticParticle(btn: HTMLElement, x: number, y: number): void {
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

  // ===== PRESS RIPPLE EFFECT =====
  private createPressRipple(btn: HTMLElement, event: MouseEvent): void {
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

  // ===== SCREEN SHAKE EFFECT =====
  private addScreenShake(duration: number): void {
    const intensity = 2;
    const startTime = Date.now();
    
    const shake = () => {
      const elapsed = Date.now() - startTime;
      if (elapsed < duration) {
        const x = (Math.random() - 0.5) * intensity;
        const y = (Math.random() - 0.5) * intensity;
        
        this.renderer.setStyle(document.body, 'transform', 
          `translateX(${x}px) translateY(${y}px)`);
        
        requestAnimationFrame(shake);
      } else {
        this.renderer.setStyle(document.body, 'transform', 'none');
      }
    };
    
    shake();
  }

  // ===== CONTEXTUAL BUTTON FEEDBACK =====
  addContextualFeedback(element: ElementRef, importance: 'low' | 'medium' | 'high' | 'critical'): void {
    const btn = element.nativeElement;
    
    const effects = {
      low: {
        particles: 8,
        magneticStrength: 0.2,
        glowIntensity: 0.3
      },
      medium: {
        particles: 15,
        magneticStrength: 0.4,
        glowIntensity: 0.5
      },
      high: {
        particles: 25,
        magneticStrength: 0.6,
        glowIntensity: 0.7
      },
      critical: {
        particles: 40,
        magneticStrength: 0.8,
        glowIntensity: 1.0
      }
    };
    
    const config = effects[importance];
    
    // Apply importance-based styling
    this.renderer.setStyle(btn, '--importance-glow', `${config.glowIntensity}`);
    this.renderer.addClass(btn, `importance-${importance}`);
    
    // Override click handler for contextual particles
    this.renderer.listen(btn, 'click', () => {
      this.createParticleExplosion(element, config.particles);
    });
  }

  // ===== FORTNITE-STYLE BUTTON ENHANCEMENT =====
  initFortniteButton(element: ElementRef, options: {
    type?: 'primary' | 'secondary' | 'danger' | 'success';
    size?: 'small' | 'medium' | 'large';
    effects?: boolean;
  } = {}): void {
    const btn = element.nativeElement;
    const { type = 'primary', size = 'medium', effects = true } = options;
    
    // Add Fortnite-style classes
    this.renderer.addClass(btn, 'fortnite-btn');
    this.renderer.addClass(btn, `fortnite-btn-${type}`);
    this.renderer.addClass(btn, `fortnite-btn-${size}`);
    
    if (effects) {
      // Initialize all effects based on button type
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