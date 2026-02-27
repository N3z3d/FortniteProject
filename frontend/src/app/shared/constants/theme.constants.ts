/**
 * Theme constants for the Fortnite Pronos application
 * Centralized color palette and design tokens
 */

// Primary accent colors
export const THEME_COLORS = {
  // Nexus theme primary colors
  primaryCyan: 'var(--color-info)',
  primaryGold: 'var(--color-action)',
  primaryOrange: 'var(--color-region-brazil)',
  primaryGreen: 'var(--color-success)',

  // Status colors
  success: 'rgba(0, 255, 136, 0.9)',
  error: 'rgba(255, 51, 102, 0.9)',
  errorSolid: 'var(--color-danger)',
  info: 'rgba(0, 212, 255, 0.9)',
  warning: 'var(--color-warning)',

  // UI colors
  focusOutline: 'var(--color-primary)',
  focusOutlineHover: 'var(--color-info)',
  lightBackground: 'var(--color-light)',

  // Overlay colors
  overlayLight: 'rgba(255, 255, 255, 0.2)',
  overlayMedium: 'rgba(255, 255, 255, 0.4)',
  overlayStrong: 'rgba(255, 255, 255, 0.6)',
  shadowLight: 'rgba(0, 0, 0, 0.2)',
  shadowMedium: 'rgba(0, 0, 0, 0.3)',

  // Region colors for team statistics
  regions: {
    EU: 'var(--color-region-eu)',
    NAW: 'var(--color-region-naw)',
    NAC: 'var(--color-admin-warning)',
    BR: 'var(--color-region-brazil)',
    ASIA: 'var(--color-region-asia)',
    OCE: 'var(--color-region-oce)',
    ME: 'var(--color-region-mena)'
  } as Record<string, string>,

  // Tranche colors for player tiers
  tranches: [
    'var(--color-achievement)',
    'rgb(192 192 192)',
    'rgb(205 127 50)',
    'var(--color-region-eu)',
    'var(--color-admin-info)'
  ]
} as const;

// Animation timing constants
export const ANIMATION_TIMING = {
  fast: 150,
  normal: 200,
  slow: 300,
  verySlow: 500,

  // Easing functions
  easeOut: 'ease-out',
  easeIn: 'ease-in',
  easeInOut: 'ease-in-out',
  cubicBezier: 'cubic-bezier(0.4, 0, 0.2, 1)'
} as const;

// Breakpoint constants (matching SCSS)
export const BREAKPOINTS = {
  mobileSmall: 480,
  mobile: 768,
  tablet: 1024,
  desktop: 1200,
  desktopLarge: 1440
} as const;

// Spacing constants
export const SPACING = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
  xxl: 48
} as const;

// Z-index layers
export const Z_INDEX = {
  dropdown: 1000,
  sticky: 1020,
  fixed: 1030,
  modalBackdrop: 1040,
  modal: 1050,
  popover: 1060,
  tooltip: 1070,
  toast: 1080
} as const;

// Number formatting thresholds
export const NUMBER_FORMAT = {
  millionThreshold: 1000000,
  thousandThreshold: 1000,
  millionSuffix: 'M',
  thousandSuffix: 'K'
} as const;

// Default particle effect colors
export const PARTICLE_COLORS = [
  THEME_COLORS.primaryCyan,
  THEME_COLORS.primaryOrange,
  THEME_COLORS.primaryGreen,
  THEME_COLORS.primaryGold
] as const;

// Helper function to get region color
export function getRegionColor(region: string): string {
  return THEME_COLORS.regions[region] || 'var(--color-neutral)';
}

// Helper function to get tranche color
export function getTrancheColor(trancheIndex: number): string {
  return THEME_COLORS.tranches[trancheIndex - 1] || 'var(--color-neutral)';
}

// Helper function to format points
export function formatPoints(points: number): string {
  if (points >= NUMBER_FORMAT.millionThreshold) {
    return (points / NUMBER_FORMAT.millionThreshold).toFixed(2) + NUMBER_FORMAT.millionSuffix;
  } else if (points >= NUMBER_FORMAT.thousandThreshold) {
    return (points / NUMBER_FORMAT.thousandThreshold).toFixed(1) + NUMBER_FORMAT.thousandSuffix;
  }
  return points.toString();
}
