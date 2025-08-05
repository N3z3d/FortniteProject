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
export const FILTER_OPTIONS = {
  ALL_REGIONS: 'ALL' as const,
  ALL_POSITIONS: 'ALL' as const,
  ALL_TRANCHES: 'ALL' as const,
  DEFAULT_SEARCH_TERM: '',
  
  REGIONS: [
    { value: 'ALL', label: 'Toutes les régions', color: '#6c757d' },
    { value: 'NAE', label: 'Nord-Amérique Est', color: '#007bff' },
    { value: 'NAW', label: 'Nord-Amérique Ouest', color: '#17a2b8' },
    { value: 'EU', label: 'Europe', color: '#28a745' },
    { value: 'ASIA', label: 'Asie', color: '#ffc107' },
    { value: 'OCE', label: 'Océanie', color: '#dc3545' },
    { value: 'BRAZIL', label: 'Brésil', color: '#fd7e14' },
    { value: 'MENA', label: 'Moyen-Orient/Afrique', color: '#6f42c1' }
  ] as const,
  
  PERFORMANCE_LEVELS: [
    { value: 'ALL', label: 'Tous les niveaux', min: 0, max: Infinity },
    { value: 'BEGINNER', label: 'Débutant', min: 0, max: 100 },
    { value: 'INTERMEDIATE', label: 'Intermédiaire', min: 100, max: 500 },
    { value: 'ADVANCED', label: 'Avancé', min: 500, max: 1000 },
    { value: 'EXPERT', label: 'Expert', min: 1000, max: Infinity }
  ] as const,
  
  SORT_OPTIONS: [
    { value: 'points_desc', label: 'Points (décroissant)', field: 'totalPoints', order: 'desc' },
    { value: 'points_asc', label: 'Points (croissant)', field: 'totalPoints', order: 'asc' },
    { value: 'name_asc', label: 'Nom (A-Z)', field: 'name', order: 'asc' },
    { value: 'name_desc', label: 'Nom (Z-A)', field: 'name', order: 'desc' },
    { value: 'region_asc', label: 'Région (A-Z)', field: 'region', order: 'asc' },
    { value: 'kd_desc', label: 'K/D (décroissant)', field: 'kdRatio', order: 'desc' },
    { value: 'winrate_desc', label: 'Winrate (décroissant)', field: 'winRate', order: 'desc' }
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
  
  // Labels de performance
  PERFORMANCE_LABELS: {
    EXCELLENT: 'Excellent',
    GOOD: 'Bon',
    AVERAGE: 'Moyen',
    POOR: 'Faible',
    UNKNOWN: 'Inconnu'
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

// Messages d'erreur du draft (avec alias pour compatibilité)
export const DRAFT_ERROR_MESSAGES = {
  PLAYER_ALREADY_SELECTED: 'Ce joueur a déjà été sélectionné',
  NOT_YOUR_TURN: 'Ce n\'est pas votre tour de drafter',
  REGION_LIMIT_EXCEEDED: 'Limite de joueurs par région atteinte',
  TEAM_FULL: 'Votre équipe est complète',
  TIME_EXPIRED: 'Le temps de sélection a expiré',
  INVALID_SELECTION: 'Sélection invalide',
  DRAFT_NOT_ACTIVE: 'Le draft n\'est pas actif',
  CONNECTION_ERROR: 'Erreur de connexion au serveur',
  UNAUTHORIZED: 'Vous n\'êtes pas autorisé à participer à ce draft',
  GAME_NOT_FOUND: 'Jeu non trouvé',
  PLAYER_NOT_FOUND: 'Joueur non trouvé',
  INVALID_ROUND: 'Round de draft invalide',
  DRAFT_COMPLETED: 'Le draft est déjà terminé',
  SERVER_ERROR: 'Erreur serveur, veuillez réessayer'
};

// Messages de succès du draft (avec alias pour compatibilité)
export const DRAFT_SUCCESS_MESSAGES = {
  PLAYER_SELECTED: 'Joueur sélectionné avec succès',
  DRAFT_STARTED: 'Le draft a commencé',
  DRAFT_COMPLETED: 'Le draft est terminé',
  TURN_UPDATED: 'C\'est votre tour de drafter',
  TEAM_UPDATED: 'Équipe mise à jour',
  DRAFT_PAUSED: 'Le draft a été mis en pause',
  DRAFT_RESUMED: 'Le draft a repris'
};

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

// Configuration de l'accessibilité
export const ACCESSIBILITY_CONFIG = {
  // Labels ARIA
  ARIA_LABELS: {
    PLAYER_CARD: 'Carte joueur',
    SELECT_PLAYER: 'Sélectionner ce joueur',
    TEAM_OVERVIEW: 'Vue d\'ensemble de l\'équipe',
    DRAFT_TIMER: 'Temps restant pour sélectionner',
    CURRENT_TURN: 'Tour actuel',
    DRAFT_STATUS: 'Statut du draft'
  },
  
  // Descriptions pour lecteurs d'écran
  SCREEN_READER_DESCRIPTIONS: {
    PLAYER_AVAILABLE: 'Joueur disponible pour sélection',
    PLAYER_SELECTED: 'Joueur déjà sélectionné',
    YOUR_TURN: 'C\'est votre tour de sélectionner un joueur',
    WAITING_TURN: 'En attente de votre tour',
    DRAFT_COMPLETED: 'Draft terminé, toutes les équipes sont complètes'
  },
  
  // Raccourcis clavier
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
export const ERROR_MESSAGES = DRAFT_ERROR_MESSAGES;
export const SUCCESS_MESSAGES = DRAFT_SUCCESS_MESSAGES;

// Labels pour les régions
export const REGION_LABELS = FILTER_OPTIONS.REGIONS.reduce((acc, region) => {
  acc[region.value] = region.label;
  return acc;
}, {} as Record<string, string>);

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
} as any;

// Labels pour les statuts
export const STATUS_LABELS = {
  NOT_STARTED: 'Non démarré',
  IN_PROGRESS: 'En cours',
  PAUSED: 'En pause',
  COMPLETED: 'Terminé',
  CANCELLED: 'Annulé',
  ERROR: 'Erreur',
  CREATED: 'Créé',
  ACTIVE: 'Actif'
} as any;