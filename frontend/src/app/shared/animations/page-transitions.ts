import { trigger, transition, style, query, group, animate, animateChild } from '@angular/animations';

// ===== PREMIUM PAGE TRANSITIONS =====

export const slideInAnimation = trigger('routeAnimations', [
  // Slide in from right
  transition('* <=> *', [
    style({ position: 'relative' }),
    query(':enter, :leave', [
      style({
        position: 'absolute',
        top: 0,
        left: 0,
        width: '100%'
      })
    ], { optional: true }),
    query(':enter', [
      style({ transform: 'translateX(100%)', opacity: 0 })
    ], { optional: true }),
    query(':leave', animateChild(), { optional: true }),
    group([
      query(':leave', [
        animate('0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94)', 
                style({ transform: 'translateX(-100%)', opacity: 0 }))
      ], { optional: true }),
      query(':enter', [
        animate('0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94)', 
                style({ transform: 'translateX(0%)', opacity: 1 }))
      ], { optional: true })
    ]),
    query(':enter', animateChild(), { optional: true }),
  ])
]);

// Gaming fade transition with scale
export const gamingFadeAnimation = trigger('gamingRoute', [
  transition('* <=> *', [
    style({ position: 'relative' }),
    query(':enter, :leave', [
      style({
        position: 'absolute',
        top: 0,
        left: 0,
        width: '100%',
        height: '100%'
      })
    ], { optional: true }),
    query(':enter', [
      style({ 
        opacity: 0, 
        transform: 'scale(0.95) translateY(20px)',
        filter: 'blur(5px)'
      })
    ], { optional: true }),
    query(':leave', animateChild(), { optional: true }),
    group([
      query(':leave', [
        animate('0.3s ease-out', 
                style({ 
                  opacity: 0, 
                  transform: 'scale(1.05) translateY(-20px)',
                  filter: 'blur(5px)'
                }))
      ], { optional: true }),
      query(':enter', [
        animate('0.5s 0.1s cubic-bezier(0.175, 0.885, 0.32, 1.275)', 
                style({ 
                  opacity: 1, 
                  transform: 'scale(1) translateY(0)',
                  filter: 'blur(0px)'
                }))
      ], { optional: true })
    ]),
    query(':enter', animateChild(), { optional: true }),
  ])
]);

// Premium slide up animation
export const slideUpAnimation = trigger('slideUp', [
  transition('* <=> *', [
    style({ position: 'relative' }),
    query(':enter, :leave', [
      style({
        position: 'absolute',
        top: 0,
        left: 0,
        width: '100%'
      })
    ], { optional: true }),
    query(':enter', [
      style({ transform: 'translateY(100%)', opacity: 0 })
    ], { optional: true }),
    query(':leave', animateChild(), { optional: true }),
    group([
      query(':leave', [
        animate('0.3s ease-in', 
                style({ transform: 'translateY(-50%)', opacity: 0 }))
      ], { optional: true }),
      query(':enter', [
        animate('0.5s cubic-bezier(0.68, -0.55, 0.265, 1.55)', 
                style({ transform: 'translateY(0%)', opacity: 1 }))
      ], { optional: true })
    ]),
    query(':enter', animateChild(), { optional: true }),
  ])
]);

// Gaming glitch transition effect
export const glitchTransition = trigger('glitchRoute', [
  transition('* <=> *', [
    style({ position: 'relative' }),
    query(':enter, :leave', [
      style({
        position: 'absolute',
        top: 0,
        left: 0,
        width: '100%'
      })
    ], { optional: true }),
    query(':enter', [
      style({ 
        opacity: 0,
        transform: 'translateX(50px) skewX(5deg)',
        filter: 'hue-rotate(90deg) saturate(1.5)'
      })
    ], { optional: true }),
    query(':leave', animateChild(), { optional: true }),
    group([
      query(':leave', [
        animate('0.2s ease-in', 
                style({ 
                  opacity: 0,
                  transform: 'translateX(-50px) skewX(-5deg)',
                  filter: 'hue-rotate(-90deg) saturate(0.5)'
                }))
      ], { optional: true }),
      query(':enter', [
        animate('0.4s 0.1s cubic-bezier(0.25, 0.46, 0.45, 0.94)', 
                style({ 
                  opacity: 1,
                  transform: 'translateX(0) skewX(0deg)',
                  filter: 'hue-rotate(0deg) saturate(1)'
                }))
      ], { optional: true })
    ]),
    query(':enter', animateChild(), { optional: true }),
  ])
]);

// ===== COMPONENT ANIMATIONS =====

// Stagger animation for lists
export const staggerAnimation = trigger('stagger', [
  transition('* <=> *', [
    query(':enter', [
      style({ opacity: 0, transform: 'translateY(30px)' }),
      animate('0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94)', 
              style({ opacity: 1, transform: 'translateY(0)' }))
    ], { optional: true })
  ])
]);

// Card hover animations
export const cardHoverAnimation = trigger('cardHover', [
  transition(':enter', [
    style({ opacity: 0, transform: 'scale(0.8) rotateY(180deg)' }),
    animate('0.6s cubic-bezier(0.68, -0.55, 0.265, 1.55)',
            style({ opacity: 1, transform: 'scale(1) rotateY(0deg)' }))
  ]),
  transition(':leave', [
    animate('0.3s ease-in',
            style({ opacity: 0, transform: 'scale(0.8) rotateY(-180deg)' }))
  ])
]);

// Premium button press animation
export const buttonPressAnimation = trigger('buttonPress', [
  transition('idle => pressed', [
    animate('0.1s ease-out', style({ transform: 'scale(0.95)' }))
  ]),
  transition('pressed => idle', [
    animate('0.2s cubic-bezier(0.68, -0.55, 0.265, 1.55)', 
            style({ transform: 'scale(1)' }))
  ])
]);

// Modal/Dialog animations
export const modalAnimation = trigger('modal', [
  transition(':enter', [
    style({ 
      opacity: 0, 
      transform: 'scale(0.7) translateY(-50px)',
      backdropFilter: 'blur(0px)'
    }),
    animate('0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275)', 
            style({ 
              opacity: 1, 
              transform: 'scale(1) translateY(0)',
              backdropFilter: 'blur(20px)'
            }))
  ]),
  transition(':leave', [
    animate('0.2s ease-in', 
            style({ 
              opacity: 0, 
              transform: 'scale(0.7) translateY(-50px)',
              backdropFilter: 'blur(0px)'
            }))
  ])
]);

// Gaming notification animation
export const notificationAnimation = trigger('notification', [
  transition(':enter', [
    style({ 
      opacity: 0, 
      transform: 'translateX(100%) rotateZ(10deg)',
      filter: 'blur(5px)'
    }),
    animate('0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55)', 
            style({ 
              opacity: 1, 
              transform: 'translateX(0) rotateZ(0deg)',
              filter: 'blur(0px)'
            }))
  ]),
  transition(':leave', [
    animate('0.3s ease-in', 
            style({ 
              opacity: 0, 
              transform: 'translateX(100%) rotateZ(-10deg)',
              filter: 'blur(5px)'
            }))
  ])
]);

// Loading animation
export const loadingAnimation = trigger('loading', [
  transition('* <=> *', [
    query('.loading-element', [
      style({ opacity: 0, transform: 'scale(0.8)' }),
      animate('0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94)', 
              style({ opacity: 1, transform: 'scale(1)' }))
    ], { optional: true })
  ])
]);

// Gaming pulse effect
export const pulseAnimation = trigger('pulse', [
  transition('* <=> *', [
    animate('1s ease-in-out', style({ transform: 'scale(1.05)' })),
    animate('1s ease-in-out', style({ transform: 'scale(1)' }))
  ])
]);

// 3D flip animation for cards
export const flipAnimation = trigger('flip', [
  transition('front => back', [
    animate('0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94)', 
            style({ transform: 'rotateY(180deg)' }))
  ]),
  transition('back => front', [
    animate('0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94)', 
            style({ transform: 'rotateY(0deg)' }))
  ])
]);

// Premium slide reveal animation
export const slideRevealAnimation = trigger('slideReveal', [
  transition(':enter', [
    style({ 
      clipPath: 'inset(0 100% 0 0)',
      opacity: 0
    }),
    animate('0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94)', 
            style({ 
              clipPath: 'inset(0 0% 0 0)',
              opacity: 1
            }))
  ]),
  transition(':leave', [
    animate('0.4s ease-in', 
            style({ 
              clipPath: 'inset(0 0 0 100%)',
              opacity: 0
            }))
  ])
]);

// Gaming glow effect animation
export const glowAnimation = trigger('glow', [
  transition('* <=> *', [
    animate('2s ease-in-out infinite', style({
      filter: 'drop-shadow(0 0 20px rgba(0, 212, 255, 0.8))'
    }))
  ])
]);