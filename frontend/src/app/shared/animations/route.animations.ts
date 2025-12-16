import {
  trigger,
  transition,
  style,
  query,
  animate,
  group,
  animateChild
} from '@angular/animations';

// Animation de fondu avec glissement subtil
export const fadeSlideAnimation = trigger('routeAnimations', [
  transition('* <=> *', [
    style({ position: 'relative' }),
    query(':enter, :leave', [
      style({
        position: 'absolute',
        top: 0,
        left: 0,
        width: '100%',
        opacity: 1
      })
    ], { optional: true }),
    query(':enter', [
      style({ opacity: 0, transform: 'translateY(20px)' })
    ], { optional: true }),
    group([
      query(':leave', [
        animate('200ms ease-out', style({ opacity: 0, transform: 'translateY(-10px)' }))
      ], { optional: true }),
      query(':enter', [
        animate('300ms 100ms ease-out', style({ opacity: 1, transform: 'translateY(0)' }))
      ], { optional: true }),
      query('@*', animateChild(), { optional: true })
    ])
  ])
]);

// Animation de glissement horizontal (pour navigation latérale)
export const slideAnimation = trigger('slideRouteAnimations', [
  transition('* => forward', [
    style({ position: 'relative' }),
    query(':enter, :leave', [
      style({
        position: 'absolute',
        top: 0,
        width: '100%'
      })
    ], { optional: true }),
    query(':enter', [
      style({ left: '100%', opacity: 0 })
    ], { optional: true }),
    group([
      query(':leave', [
        animate('300ms ease-out', style({ left: '-100%', opacity: 0 }))
      ], { optional: true }),
      query(':enter', [
        animate('300ms ease-out', style({ left: '0%', opacity: 1 }))
      ], { optional: true })
    ])
  ]),
  transition('* => back', [
    style({ position: 'relative' }),
    query(':enter, :leave', [
      style({
        position: 'absolute',
        top: 0,
        width: '100%'
      })
    ], { optional: true }),
    query(':enter', [
      style({ left: '-100%', opacity: 0 })
    ], { optional: true }),
    group([
      query(':leave', [
        animate('300ms ease-out', style({ left: '100%', opacity: 0 }))
      ], { optional: true }),
      query(':enter', [
        animate('300ms ease-out', style({ left: '0%', opacity: 1 }))
      ], { optional: true })
    ])
  ])
]);

// Animation de zoom subtil (pour dialogs et modals)
export const zoomAnimation = trigger('zoomRouteAnimations', [
  transition(':enter', [
    style({ opacity: 0, transform: 'scale(0.95)' }),
    animate('250ms ease-out', style({ opacity: 1, transform: 'scale(1)' }))
  ]),
  transition(':leave', [
    animate('200ms ease-in', style({ opacity: 0, transform: 'scale(0.95)' }))
  ])
]);

// Animation simple de fondu
export const fadeAnimation = trigger('fadeRouteAnimations', [
  transition('* <=> *', [
    query(':enter', [
      style({ opacity: 0 })
    ], { optional: true }),
    query(':leave', [
      animate('150ms ease-out', style({ opacity: 0 }))
    ], { optional: true }),
    query(':enter', [
      animate('200ms ease-in', style({ opacity: 1 }))
    ], { optional: true })
  ])
]);

// Animation gaming premium avec effet de glitch subtil
export const gamingAnimation = trigger('gamingRouteAnimations', [
  transition('* <=> *', [
    style({ position: 'relative', overflow: 'hidden' }),
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
        transform: 'translateX(30px) skewX(-2deg)',
        filter: 'blur(4px)'
      })
    ], { optional: true }),
    group([
      query(':leave', [
        animate('200ms cubic-bezier(0.4, 0, 0.2, 1)', style({
          opacity: 0,
          transform: 'translateX(-30px) skewX(2deg)',
          filter: 'blur(4px)'
        }))
      ], { optional: true }),
      query(':enter', [
        animate('350ms 50ms cubic-bezier(0.4, 0, 0.2, 1)', style({
          opacity: 1,
          transform: 'translateX(0) skewX(0)',
          filter: 'blur(0)'
        }))
      ], { optional: true })
    ])
  ])
]);
