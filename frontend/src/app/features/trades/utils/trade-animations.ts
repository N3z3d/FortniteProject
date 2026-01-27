import { trigger, transition, style, animate, keyframes, query, stagger } from '@angular/animations';

/**
 * Reusable animations for trade components
 * Extracted to reduce component file sizes and follow DRY principles
 */

export const slideInFromSide = trigger('slideInFromSide', [
  transition(':enter', [
    style({ transform: 'translateX(-100%)', opacity: 0 }),
    animate(
      '0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94)',
      style({ transform: 'translateX(0)', opacity: 1 })
    )
  ])
]);

export const playerCardDrag = trigger('playerCardDrag', [
  transition('idle => dragging', [
    animate(
      '0.3s ease-out',
      style({
        transform: 'scale(1.1) rotate(8deg)',
        zIndex: 1000,
        boxShadow: '0 20px 60px rgba(var(--gaming-primary-rgb), 0.4)'
      })
    )
  ]),
  transition('dragging => idle', [
    animate(
      '0.3s cubic-bezier(0.68, -0.55, 0.265, 1.55)',
      style({
        transform: 'scale(1) rotate(0deg)',
        zIndex: 'auto',
        boxShadow: 'var(--shadow-trading-card)'
      })
    )
  ])
]);

export const tradeBalanceChange = trigger('tradeBalanceChange', [
  transition('* => positive', [
    animate(
      '0.6s cubic-bezier(0.68, -0.55, 0.265, 1.55)',
      keyframes([
        style({ transform: 'scale(1)', color: 'var(--gaming-light)', offset: 0 }),
        style({ transform: 'scale(1.2)', color: 'var(--value-positive)', offset: 0.5 }),
        style({ transform: 'scale(1)', color: 'var(--value-positive)', offset: 1 })
      ])
    )
  ]),
  transition('* => negative', [
    animate(
      '0.6s cubic-bezier(0.68, -0.55, 0.265, 1.55)',
      keyframes([
        style({ transform: 'scale(1)', color: 'var(--gaming-light)', offset: 0 }),
        style({ transform: 'scale(1.2)', color: 'var(--value-negative)', offset: 0.5 }),
        style({ transform: 'scale(1)', color: 'var(--value-negative)', offset: 1 })
      ])
    )
  ]),
  transition('* => neutral', [
    animate('0.4s ease-out', style({ transform: 'scale(1)', color: 'var(--gaming-light)' }))
  ])
]);

export const dropZoneActive = trigger('dropZoneActive', [
  transition(':enter', [
    style({ opacity: 0, transform: 'scale(0.9)' }),
    animate('0.3s ease-out', style({ opacity: 1, transform: 'scale(1)' }))
  ])
]);

export const slideInFromBottom = trigger('slideInFromBottom', [
  transition(':enter', [
    style({ transform: 'translateY(100%)', opacity: 0 }),
    animate(
      '0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94)',
      style({ transform: 'translateY(0)', opacity: 1 })
    )
  ]),
  transition(':leave', [
    animate('0.3s ease-in', style({ transform: 'translateY(100%)', opacity: 0 }))
  ])
]);

export const playerStagger = trigger('playerStagger', [
  transition('* => *', [
    query(
      ':enter',
      [
        style({ opacity: 0, transform: 'translateX(-30px)' }),
        stagger(100, [
          animate(
            '0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94)',
            style({ opacity: 1, transform: 'translateX(0)' })
          )
        ])
      ],
      { optional: true }
    )
  ])
]);

export const actionButtonHover = trigger('actionButtonHover', [
  transition('idle => hover', [
    animate(
      '0.3s cubic-bezier(0.68, -0.55, 0.265, 1.55)',
      style({ transform: 'scale(1.05) translateY(-2px)' })
    )
  ]),
  transition('hover => idle', [
    animate('0.2s ease-out', style({ transform: 'scale(1) translateY(0)' }))
  ])
]);

export const tradeStatusChange = trigger('tradeStatusChange', [
  transition('pending => accepted', [
    animate(
      '1s cubic-bezier(0.68, -0.55, 0.265, 1.55)',
      keyframes([
        style({ transform: 'scale(1)', background: 'var(--card-pending)', offset: 0 }),
        style({ transform: 'scale(1.02)', background: 'var(--card-receive)', offset: 0.5 }),
        style({ transform: 'scale(1)', background: 'var(--card-receive)', offset: 1 })
      ])
    )
  ]),
  transition('pending => rejected', [
    animate(
      '0.8s ease-in-out',
      keyframes([
        style({ transform: 'scale(1) rotateZ(0deg)', offset: 0 }),
        style({ transform: 'scale(0.95) rotateZ(-2deg)', offset: 0.3 }),
        style({ transform: 'scale(1.02) rotateZ(1deg)', offset: 0.7 }),
        style({ transform: 'scale(1) rotateZ(0deg)', offset: 1 })
      ])
    )
  ])
]);

export const timelineProgress = trigger('timelineProgress', [
  transition(':enter', [
    query(
      '.timeline-connector',
      [style({ height: '0%' }), animate('0.8s ease-out', style({ height: '100%' }))],
      { optional: true }
    ),
    query(
      '.timeline-item',
      [
        style({ opacity: 0, transform: 'scale(0.8)' }),
        stagger(200, [
          animate(
            '0.5s cubic-bezier(0.68, -0.55, 0.265, 1.55)',
            style({ opacity: 1, transform: 'scale(1)' })
          )
        ])
      ],
      { optional: true }
    )
  ])
]);
