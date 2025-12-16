/**
 * Theme constants for the Fortnite Pronos application
 * Centralized color palette and design tokens
 */

// Primary accent colors
export const THEME_COLORS = {
  // Nexus theme primary colors
  primaryCyan: '#00d4ff',
  primaryGold: '#ffaa00',
  primaryOrange: '#ff6b35',
  primaryGreen: '#00ff88',

  // Status colors
  success: 'rgba(0, 255, 136, 0.9)',
  error: 'rgba(255, 51, 102, 0.9)',
  errorSolid: '#c53030',
  info: 'rgba(0, 212, 255, 0.9)',
  warning: '#ffaa00',

  // UI colors
  focusOutline: '#2563eb',
  focusOutlineHover: '#1d4ed8',
  lightBackground: '#f7f7f7',

  // Overlay colors
  overlayLight: 'rgba(255, 255, 255, 0.2)',
  overlayMedium: 'rgba(255, 255, 255, 0.4)',
  overlayStrong: 'rgba(255, 255, 255, 0.6)',
  shadowLight: 'rgba(0, 0, 0, 0.2)',
  shadowMedium: 'rgba(0, 0, 0, 0.3)',

  // Region colors for team statistics
  regions: {
    EU: '#4CAF50',
    NAW: '#2196F3',
    NAC: '#FF9800',
    BR: '#FFD700',
    ASIA: '#E91E63',
    OCE: '#9C27B0',
    ME: '#FF5722'
  } as Record<string, string>,

  // Tranche colors for player tiers
  tranches: ['#FFD700', '#C0C0C0', '#CD7F32', '#4CAF50', '#2196F3']
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
  return THEME_COLORS.regions[region] || '#757575';
}

// Helper function to get tranche color
export function getTrancheColor(trancheIndex: number): string {
  return THEME_COLORS.tranches[trancheIndex - 1] || '#757575';
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
