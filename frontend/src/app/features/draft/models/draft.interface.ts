/**
 * Interface pour représenter l'état et la structure d'une draft
 * Basé sur le modèle backend Draft.java
 */

export interface Draft {
  id: string;
  gameId: string;
  status: DraftStatus;
  currentRound: number;
  currentTurn: number;
  totalRounds: number;
  timePerPick: number; // en secondes
  createdAt: Date;
  startedAt?: Date;
  completedAt?: Date;
  rules: DraftRules;
  participants: DraftParticipant[];
  picks: DraftPick[];
  currentPickId?: string;
  isReversed: boolean; // Snake draft - direction change each round
}

export interface DraftRules {
  maxPlayersPerTeam: number;
  regionLimits: { [region: string]: number };
  timePerPick: number; // en secondes
  allowTrades: boolean;
  snakeDraft: boolean; // true = snake draft, false = linear
  autopickEnabled: boolean;
  autopickTimeLimit: number; // temps avant autopick en secondes
}

export interface DraftParticipant {
  id: string;
  userId: string;
  username: string;
  teamName: string;
  draftOrder: number;
  isCreator: boolean;
  isActive: boolean;
  selectedPlayers: DraftedPlayer[];
  timeRemaining?: number; // temps restant pour le pick actuel
  lastPickTime?: Date;
  isCurrentTurn: boolean;
  autopickPreferences?: AutopickPreference[];
}

export interface DraftPick {
  id: string;
  draftId: string;
  participantId: string;
  playerId: string;
  playerName: string;
  playerRegion: string;
  round: number;
  pickNumber: number; // Position globale dans la draft (1, 2, 3...)
  turnNumber: number; // Position dans le tour (1-n participants)
  pickedAt: Date;
  timeToDecide: number; // temps pris pour prendre la décision en secondes
  isAutopick: boolean;
  previousOwner?: string; // Si c'est un trade
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
  stats: PlayerStats | any;
  totalPoints?: number; // Points totaux calculés
  isRecommended: boolean;
  recommendationReason?: string;
  conflictsWith?: string[]; // Liste des joueurs avec qui il y a conflit (région, etc.)
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
  averagePickTime: number;
  fastestPick: {
    playerId: string;
    time: number;
    participant: string;
  };
  slowestPick: {
    playerId: string;
    time: number;
    participant: string;
  };
  autopicks: number;
  regionDistribution: { [region: string]: number };
  participantStats: ParticipantDraftStats[];
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

// Aliases pour compatibilité avec les composants existants
export type Player = AvailablePlayer;

export enum PlayerRegion {
  NAE = 'NAE',
  NAW = 'NAW',
  EU = 'EU',
  ASIA = 'ASIA',
  OCE = 'OCE',
  BRAZIL = 'BRAZIL',
  MENA = 'MENA'
}