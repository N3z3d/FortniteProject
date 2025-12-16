/**
 * Interface pour représenter l'état et la structure d'une draft
 * Basé sur le modèle backend Draft.java
 */

export interface Draft {
  id: string;
  gameId: string;
  status: DraftStatus | string;
  currentRound: number;
  currentTurn?: number;
  totalRounds?: number;
  timePerPick?: number; // en secondes
  currentPick?: number;
  createdAt?: Date | string;
  updatedAt?: Date | string;
  startedAt?: Date | string;
  completedAt?: Date | string | null;
  finishedAt?: Date | string | null;
  rules?: DraftRules;
  participants?: DraftParticipant[];
  picks?: DraftPick[];
  currentPickId?: string;
  isReversed?: boolean; // Snake draft - direction change each round
}

export interface DraftRules {
  maxPlayersPerTeam?: number;
  regionLimits?: { [region: string]: number };
  regionQuotas?: { [region: string]: number };
  timePerPick?: number; // en secondes
  timeLimitPerPick?: number; // alternative nomenclature
  allowTrades?: boolean;
  snakeDraft?: boolean; // true = snake draft, false = linear
  autopickEnabled?: boolean;
  autoPickEnabled?: boolean;
  autopickTimeLimit?: number; // temps avant autopick en secondes
  autoPickDelay?: number;
}

export interface DraftParticipant {
  id: string;
  userId?: string;
  gameId?: string;
  username: string;
  teamName?: string;
  draftOrder?: number;
  isCreator?: boolean;
  isActive?: boolean;
  selectedPlayers?: DraftedPlayer[];
  timeRemaining?: number; // temps restant pour le pick actuel
  lastPickTime?: Date;
  isCurrentTurn?: boolean;
  autopickPreferences?: AutopickPreference[];
}

export interface DraftPick {
  id: string;
  draftId: string;
  participantId: string;
  playerId: string;
  playerName?: string;
  playerRegion?: string;
  round: number;
  pickNumber: number; // Position globale dans la draft (1, 2, 3...)
  turnNumber?: number; // Position dans le tour (1-n participants)
  pickedAt?: Date | string;
  timeToDecide?: number; // temps pris pour prendre la décision en secondes
  timeTakenSeconds?: number;
  isAutopick?: boolean;
  previousOwner?: string; // Si c'est un trade
  selectionTime?: string | Date;
  autoPick?: boolean;
}

export interface DraftedPlayer {
  id: string;
  username: string;
  region: string;
  pickedInRound: number;
  pickOrder: number;
  stats?: PlayerStats;
  isStarter?: boolean;
  position?: string; // IGL, Fragger, Support, etc.
}

export interface PlayerStats {
  kills: number;
  deaths: number;
  wins: number;
  kd: number;
  winRate: number;
  averagePlacement: number;
  recentForm: number; // Score de forme récente (0-100)
}

export interface AutopickPreference {
  playerId: string;
  playerName: string;
  priority: number; // 1 = highest priority
  onlyIfAvailable: boolean;
}

export interface DraftTimer {
  currentPickTimeRemaining: number; // temps restant en secondes
  totalPickTime: number; // temps total alloué
  isActive: boolean;
  warningThreshold: number; // seuil d'alerte en secondes
}

export interface DraftState {
  draft: Draft;
  availablePlayers: AvailablePlayer[];
  timer: DraftTimer;
  currentParticipant?: DraftParticipant;
  userParticipant?: DraftParticipant;
  isUserTurn: boolean;
  canMakePick: boolean;
  errors: string[];
  warnings: string[];
}

export interface AvailablePlayer {
  id: string;
  username: string;
  nickname: string; // Alias pour username pour compatibilité
  region: any; // Simplifié pour éviter les erreurs de types
  tranche: any; // Niveau de jeu (débutant, intermédiaire, expert, etc.)
  stats?: PlayerStats | any;
  totalPoints?: number; // Points totaux calculés
  isRecommended?: boolean;
  recommendationReason?: string;
  conflictsWith?: string[]; // Liste des joueurs avec qui il y a conflit (région, etc.)
  currentSeason: number;
  selected?: boolean;
  available?: boolean;
}

export interface DraftHistory {
  draftId: string;
  picks: DraftPick[];
  participants: DraftParticipant[];
  completedAt: Date;
  winner?: string;
  statistics: DraftStatistics;
}

export interface DraftStatistics {
  totalPicks: number;
  averagePickTime?: number;
  averageSelectionTime?: number;
  fastestPick?: number | {
    playerId: string;
    time: number;
    participant: string;
  };
  slowestPick?: number | {
    playerId: string;
    time: number;
    participant: string;
  };
  autoPicks?: number;
  autopicks?: number;
  regionDistribution?: { [region: string]: number };
  participantStats?: ParticipantDraftStats[];
}

export interface ParticipantDraftStats {
  participantId: string;
  username: string;
  averagePickTime: number;
  autopicks: number;
  regionPreference: string;
  pickQuality: number; // Score de qualité des picks (0-100)
}

export enum DraftStatus {
  CREATED = 'CREATED',
  WAITING_FOR_PLAYERS = 'WAITING_FOR_PLAYERS',
  READY_TO_START = 'READY_TO_START',
  IN_PROGRESS = 'IN_PROGRESS',
  PAUSED = 'PAUSED',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

export enum DraftEvent {
  DRAFT_CREATED = 'DRAFT_CREATED',
  PARTICIPANT_JOINED = 'PARTICIPANT_JOINED',
  PARTICIPANT_LEFT = 'PARTICIPANT_LEFT',
  DRAFT_STARTED = 'DRAFT_STARTED',
  PICK_MADE = 'PICK_MADE',
  TURN_CHANGED = 'TURN_CHANGED',
  ROUND_COMPLETED = 'ROUND_COMPLETED',
  DRAFT_COMPLETED = 'DRAFT_COMPLETED',
  DRAFT_PAUSED = 'DRAFT_PAUSED',
  DRAFT_RESUMED = 'DRAFT_RESUMED',
  AUTOPICK_TRIGGERED = 'AUTOPICK_TRIGGERED',
  TIME_WARNING = 'TIME_WARNING',
  ERROR_OCCURRED = 'ERROR_OCCURRED'
}

export interface DraftEventData {
  event: DraftEvent;
  timestamp: Date;
  draftId: string;
  participantId?: string;
  playerId?: string;
  data?: any;
  message?: string;
}

// Types utilitaires pour la validation
export type DraftValidationResult = {
  isValid: boolean;
  errors: string[];
  warnings: string[];
};

export type PickValidationResult = {
  isValid: boolean;
  canPick: boolean;
  errors: string[];
  warnings: string[];
  conflicts: string[];
};

// Types pour les requêtes API
export interface CreateDraftRequest {
  gameId: string;
  rules: DraftRules;
  participants?: string[]; // User IDs
}

export interface JoinDraftRequest {
  draftId: string;
  userId: string;
  teamName?: string;
}

export interface MakePickRequest {
  draftId: string;
  participantId: string;
  playerId: string;
}

export interface UpdateDraftRulesRequest {
  draftId: string;
  rules: Partial<DraftRules>;
}

// Types pour les réponses API
export interface DraftResponse {
  draft: Draft;
  message?: string;
  errors?: string[];
}

export interface DraftPickResponse {
  pick: DraftPick;
  updatedDraft: Draft;
  nextParticipant?: DraftParticipant;
  message?: string;
}

// Types complémentaires utilisés par les tests et services
export interface DraftStatusInfo {
  status: DraftStatus | string;
  currentRound?: number;
  currentPick?: number;
  totalRounds?: number;
  totalParticipants?: number;
}

export interface DraftProgress {
  currentRound: number;
  currentPick: number;
  totalRounds: number;
  totalPicks?: number;
  completedPicks?: number;
  progressPercentage?: number;
  estimatedTimeRemaining?: number | null;
}

export interface DraftHistoryEntry {
  pick: DraftPick;
  player: Player;
  participant: GameParticipant;
  round: number;
  pickNumber: number;
  selectionTime: string | Date;
  timeTakenSeconds?: number;
  autoPick?: boolean;
}

export interface PlayerSelectionRequest {
  playerId: string;
}

export interface DraftInitializeRequest {
  gameId: string;
}

export interface DraftActionResponse {
  success: boolean;
  message?: string;
}

export interface DraftParticipantInfo {
  participant: GameParticipant;
  selections: DraftPick[];
  isCurrentTurn?: boolean;
  timeRemaining?: number | null;
  hasTimedOut?: boolean;
}

// Aliases pour compatibilité avec les composants existants
export type Player = AvailablePlayer;

export type PlayerRegion = string;

// Aliases attendus par certains tests legacy
export interface GameParticipant {
  id: string;
  gameId?: string;
  userId?: string;
  username: string;
  joinedAt?: string | Date;
  isCreator?: boolean;
  draftOrder?: number;
  selections?: DraftPick[];
  lastSelectionTime?: string | Date;
  timeRemaining?: number | null;
  selectedPlayers?: Player[];
  isCurrentTurn?: boolean;
}

export interface DraftBoardState {
  draft: {
    id: string;
    gameId: string;
    status: DraftStatus | string;
    currentRound: number;
    currentPick?: number;
    totalRounds?: number;
    createdAt?: string | Date;
    updatedAt?: string | Date;
    startedAt?: string | Date;
    finishedAt?: string | null | Date;
  };
  // Forme aplatie acceptée par certains tests legacy
  gameId?: string;
  status?: DraftStatus | string;
  currentRound?: number;
  currentPick?: number;
  totalRounds?: number;
  lastUpdated?: string | Date;
  participants: (
    {
      participant: GameParticipant;
      selections: DraftPick[];
      isCurrentTurn?: boolean;
      timeRemaining?: number | null;
      hasTimedOut?: boolean;
    } | GameParticipant
  )[];
  availablePlayers: AvailablePlayer[];
  picks?: DraftPick[];
  selectedPlayers?: Player[];
  currentParticipant?: GameParticipant;
  progress?: DraftProgress;
  rules?: DraftRules;
}
