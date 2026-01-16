/**
 * Constantes pour le système de draft
 * Définit les configurations, limites et options du système de draft
 */

// Configuration principale du draft
export const DRAFT_CONSTANTS = {
  // Limites temporelles
  TURN_TIMEOUT_SECONDS: 120, // 2 minutes par tour
  DRAFT_ROUND_TIME_LIMIT: 300, // 5 minutes par round
  MAX_DRAFT_DURATION_MINUTES: 60, // 1 heure max pour tout le draft
  
  // Limites de joueurs
  MIN_PLAYERS_PER_TEAM: 3,
  MAX_PLAYERS_PER_TEAM: 6,
  DEFAULT_PLAYERS_PER_TEAM: 5,
  
  // Limites par région
  MAX_PLAYERS_PER_REGION: 3,
  MIN_REGIONS_REQUIRED: 2,
  
  // Configuration des rounds
  TOTAL_DRAFT_ROUNDS: 5,
  SNAKE_DRAFT_ENABLED: true,
  
  // Timeouts et retry
  API_TIMEOUT_MS: 5000,
  MAX_RETRY_ATTEMPTS: 3,
  RETRY_DELAY_MS: 1000,
  
  // Auto-refresh
  AUTO_REFRESH_INTERVAL_MS: 2000,
  AUTO_REFRESH_INTERVAL: 2000, // Alias pour compatibilité
  REFRESH_ON_FOCUS: true,
  
  // Messages de notification
  NOTIFICATION_DURATION_MS: 4000,
  ERROR_NOTIFICATION_DURATION_MS: 6000,
  SNACKBAR_DURATION: 4000
};

// Options de filtrage pour les joueurs
// Labels use translation keys - resolve with TranslationService.t(labelKey)
export const FILTER_OPTIONS = {
  ALL_REGIONS: 'ALL' as const,
  ALL_POSITIONS: 'ALL' as const,
  ALL_TRANCHES: 'ALL' as const,
  DEFAULT_SEARCH_TERM: '',

  REGIONS: [
    { value: 'ALL', labelKey: 'draft.filters.allRegions', color: '#6c757d' },
    { value: 'NAE', labelKey: 'draft.filters.naeRegion', color: '#007bff' },
    { value: 'NAW', labelKey: 'draft.filters.nawRegion', color: '#17a2b8' },
    { value: 'EU', labelKey: 'draft.filters.euRegion', color: '#28a745' },
    { value: 'ASIA', labelKey: 'draft.filters.asiaRegion', color: '#ffc107' },
    { value: 'OCE', labelKey: 'draft.filters.oceRegion', color: '#dc3545' },
    { value: 'BRAZIL', labelKey: 'draft.filters.brazilRegion', color: '#fd7e14' },
    { value: 'MENA', labelKey: 'draft.filters.menaRegion', color: '#6f42c1' }
  ] as const,

  PERFORMANCE_LEVELS: [
    { value: 'ALL', labelKey: 'draft.filters.allLevels', min: 0, max: Infinity },
    { value: 'BEGINNER', labelKey: 'draft.filters.beginner', min: 0, max: 100 },
    { value: 'INTERMEDIATE', labelKey: 'draft.filters.intermediate', min: 100, max: 500 },
    { value: 'ADVANCED', labelKey: 'draft.filters.advanced', min: 500, max: 1000 },
    { value: 'EXPERT', labelKey: 'draft.filters.expert', min: 1000, max: Infinity }
  ] as const,

  SORT_OPTIONS: [
    { value: 'points_desc', labelKey: 'draft.sort.pointsDesc', field: 'totalPoints', order: 'desc' },
    { value: 'points_asc', labelKey: 'draft.sort.pointsAsc', field: 'totalPoints', order: 'asc' },
    { value: 'name_asc', labelKey: 'draft.sort.nameAsc', field: 'name', order: 'asc' },
    { value: 'name_desc', labelKey: 'draft.sort.nameDesc', field: 'name', order: 'desc' },
    { value: 'region_asc', labelKey: 'draft.sort.regionAsc', field: 'region', order: 'asc' },
    { value: 'kd_desc', labelKey: 'draft.sort.kdDesc', field: 'kdRatio', order: 'desc' },
    { value: 'winrate_desc', labelKey: 'draft.sort.winrateDesc', field: 'winRate', order: 'desc' }
  ] as const
};

// Configuration des performances
export const PERFORMANCE_CONFIG = {
  // Seuils de performance
  EXCELLENT_THRESHOLD: 1000,
  GOOD_THRESHOLD: 500,
  AVERAGE_THRESHOLD: 200,
  POOR_THRESHOLD: 50,
  
  // Délai de recherche
  SEARCH_DEBOUNCE_TIME: 300,
  
  // Couleurs associées aux performances
  PERFORMANCE_COLORS: {
    EXCELLENT: '#28a745', // Vert
    GOOD: '#17a2b8',      // Bleu clair
    AVERAGE: '#ffc107',   // Jaune
    POOR: '#dc3545',      // Rouge
    UNKNOWN: '#6c757d'    // Gris
  },
  
  // Labels de performance - use translation keys
  PERFORMANCE_LABEL_KEYS: {
    EXCELLENT: 'draft.performance.excellent',
    GOOD: 'draft.performance.good',
    AVERAGE: 'draft.performance.average',
    POOR: 'draft.performance.poor',
    UNKNOWN: 'draft.performance.unknown'
  }
};

// États du draft
export const DRAFT_STATES = {
  NOT_STARTED: 'NOT_STARTED' as const,
  IN_PROGRESS: 'IN_PROGRESS' as const,
  PAUSED: 'PAUSED' as const,
  COMPLETED: 'COMPLETED' as const,
  CANCELLED: 'CANCELLED' as const,
  ERROR: 'ERROR' as const
};

// Types de draft
export const DRAFT_TYPES = {
  SNAKE: 'SNAKE' as const,
  LINEAR: 'LINEAR' as const,
  AUCTION: 'AUCTION' as const,
  RANDOM: 'RANDOM' as const
};

// Error message translation keys (use with TranslationService.t())
export const DRAFT_ERROR_MESSAGE_KEYS = {
  PLAYER_ALREADY_SELECTED: 'draft.errors.playerAlreadySelected',
  NOT_YOUR_TURN: 'draft.errors.notYourTurn',
  REGION_LIMIT_EXCEEDED: 'draft.errors.regionLimitExceeded',
  TEAM_FULL: 'draft.errors.teamFull',
  TIME_EXPIRED: 'draft.errors.timeExpired',
  INVALID_SELECTION: 'draft.errors.invalidSelection',
  DRAFT_NOT_ACTIVE: 'draft.errors.draftNotActive',
  CONNECTION_ERROR: 'draft.errors.connectionError',
  UNAUTHORIZED: 'draft.errors.unauthorized',
  GAME_NOT_FOUND: 'draft.errors.gameNotFound',
  PLAYER_NOT_FOUND: 'draft.errors.playerNotFound',
  INVALID_ROUND: 'draft.errors.invalidRound',
  DRAFT_COMPLETED: 'draft.errors.draftCompleted',
  SERVER_ERROR: 'draft.errors.serverError'
};

// Success message translation keys (use with TranslationService.t())
export const DRAFT_SUCCESS_MESSAGE_KEYS = {
  PLAYER_SELECTED: 'draft.success.playerSelected',
  DRAFT_STARTED: 'draft.success.draftStarted',
  DRAFT_COMPLETED: 'draft.success.draftCompleted',
  TURN_UPDATED: 'draft.success.turnUpdated',
  TEAM_UPDATED: 'draft.success.teamUpdated',
  DRAFT_PAUSED: 'draft.success.draftPaused',
  DRAFT_RESUMED: 'draft.success.draftResumed'
};

// Legacy aliases - deprecated, use *_KEYS versions with TranslationService
/** @deprecated Use DRAFT_ERROR_MESSAGE_KEYS with TranslationService */
export const DRAFT_ERROR_MESSAGES = DRAFT_ERROR_MESSAGE_KEYS;
/** @deprecated Use DRAFT_SUCCESS_MESSAGE_KEYS with TranslationService */
export const DRAFT_SUCCESS_MESSAGES = DRAFT_SUCCESS_MESSAGE_KEYS;

// Configuration de l'interface utilisateur
export const UI_CONFIG = {
  // Animations
  TRANSITION_DURATION_MS: 300,
  CARD_HOVER_SCALE: 1.02,
  BUTTON_PRESS_SCALE: 0.98,
  
  // Dimensions
  PLAYER_CARD_HEIGHT: 120,
  TEAM_CARD_WIDTH: 280,
  SIDEBAR_WIDTH: 320,
  
  // Couleurs du thème
  THEME_COLORS: {
    PRIMARY: '#007bff',
    SECONDARY: '#6c757d',
    SUCCESS: '#28a745',
    WARNING: '#ffc107',
    DANGER: '#dc3545',
    INFO: '#17a2b8',
    LIGHT: '#f8f9fa',
    DARK: '#343a40'
  },
  
  // Breakpoints responsive
  BREAKPOINTS: {
    XS: 0,
    SM: 576,
    MD: 768,
    LG: 992,
    XL: 1200,
    XXL: 1400
  }
};

// Configuration des statistiques affichées
export const STATS_CONFIG = {
  // Statistiques principales à afficher
  PRIMARY_STATS: [
    'totalPoints',
    'kdRatio',
    'winRate',
    'averagePlacement'
  ],
  
  // Statistiques secondaires
  SECONDARY_STATS: [
    'kills',
    'deaths', 
    'wins',
    'gamesPlayed',
    'minutesPlayed'
  ],
  
  // Format d'affichage des statistiques
  STAT_FORMATS: {
    totalPoints: { decimals: 0, suffix: ' pts' },
    kdRatio: { decimals: 2, suffix: '' },
    winRate: { decimals: 1, suffix: '%' },
    averagePlacement: { decimals: 1, suffix: '' },
    kills: { decimals: 0, suffix: '' },
    deaths: { decimals: 0, suffix: '' },
    wins: { decimals: 0, suffix: '' },
    gamesPlayed: { decimals: 0, suffix: ' parties' },
    minutesPlayed: { decimals: 0, suffix: ' min' }
  }
};

// Validation des données
export const VALIDATION_RULES = {
  PLAYER_NAME: {
    MIN_LENGTH: 2,
    MAX_LENGTH: 30,
    PATTERN: /^[a-zA-Z0-9_-]+$/
  },
  
  TEAM_NAME: {
    MIN_LENGTH: 3,
    MAX_LENGTH: 50,
    PATTERN: /^[a-zA-Z0-9\s_-]+$/
  },
  
  GAME_CODE: {
    LENGTH: 6,
    PATTERN: /^[A-Z0-9]{6}$/
  }
};

// Configuration de l'accessibilité - uses translation keys
export const ACCESSIBILITY_CONFIG = {
  // Labels ARIA - translation keys
  ARIA_LABEL_KEYS: {
    PLAYER_CARD: 'draft.accessibility.playerCard',
    SELECT_PLAYER: 'draft.accessibility.selectPlayer',
    TEAM_OVERVIEW: 'draft.accessibility.teamOverview',
    DRAFT_TIMER: 'draft.accessibility.draftTimer',
    CURRENT_TURN: 'draft.accessibility.currentTurn',
    DRAFT_STATUS: 'draft.accessibility.draftStatus'
  },

  // Screen reader description keys
  SCREEN_READER_DESCRIPTION_KEYS: {
    PLAYER_AVAILABLE: 'draft.accessibility.playerAvailable',
    PLAYER_SELECTED: 'draft.accessibility.playerSelected',
    YOUR_TURN: 'draft.accessibility.yourTurn',
    WAITING_TURN: 'draft.accessibility.waitingTurn',
    DRAFT_COMPLETED: 'draft.accessibility.draftCompletedMsg'
  },

  // Raccourcis clavier (no translation needed)
  KEYBOARD_SHORTCUTS: {
    SELECT_PLAYER: 'Enter',
    NEXT_PLAYER: 'ArrowRight',
    PREVIOUS_PLAYER: 'ArrowLeft',
    FILTER_TOGGLE: 'F',
    ESCAPE_MODAL: 'Escape'
  }
};

// Export des types pour TypeScript
export type DraftState = typeof DRAFT_STATES[keyof typeof DRAFT_STATES];
export type DraftType = typeof DRAFT_TYPES[keyof typeof DRAFT_TYPES];
export type RegionValue = typeof FILTER_OPTIONS.REGIONS[number]['value'];
export type PerformanceLevel = typeof FILTER_OPTIONS.PERFORMANCE_LEVELS[number]['value'];
export type SortOption = typeof FILTER_OPTIONS.SORT_OPTIONS[number]['value'];

// Alias pour compatibilité avec l'ancien code
export const ERROR_MESSAGES = DRAFT_ERROR_MESSAGE_KEYS;
export const SUCCESS_MESSAGES = DRAFT_SUCCESS_MESSAGE_KEYS;

// Region label keys mapping (use TranslationService.t(labelKey) to resolve)
export const REGION_LABEL_KEYS = FILTER_OPTIONS.REGIONS.reduce((acc, region) => {
  acc[region.value] = region.labelKey;
  return acc;
}, {} as Record<string, string>);

// Legacy alias for backwards compatibility
export const REGION_LABELS = REGION_LABEL_KEYS;

// Couleurs pour les statuts
export const STATUS_COLORS = {
  NOT_STARTED: '#6c757d',
  IN_PROGRESS: '#007bff',
  PAUSED: '#ffc107',
  COMPLETED: '#28a745',
  CANCELLED: '#dc3545',
  ERROR: '#dc3545',
  CREATED: '#6c757d', // Alias pour NOT_STARTED
  ACTIVE: '#007bff'   // Alias pour IN_PROGRESS
} as const;

// Status label translation keys
export const STATUS_LABEL_KEYS = {
  NOT_STARTED: 'draft.status.notStarted',
  IN_PROGRESS: 'draft.status.inProgress',
  PAUSED: 'draft.status.paused',
  COMPLETED: 'draft.status.completed',
  CANCELLED: 'draft.status.cancelled',
  ERROR: 'draft.status.error',
  CREATED: 'draft.status.created',
  ACTIVE: 'draft.status.active'
} as const;

// Legacy alias for backwards compatibility (with index signature for dynamic access)
export const STATUS_LABELS: Record<string, string> = STATUS_LABEL_KEYS;
