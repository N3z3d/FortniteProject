// Interfaces de base pour les games
export interface Game {
  id: string;
  name: string;
  creatorName: string;
  maxParticipants: number;
  status: GameStatus;
  createdAt: string | Date;
  participantCount: number;
  canJoin: boolean;
  invitationCode?: string;
  invitationCodeExpiresAt?: string;
  isInvitationCodeExpired?: boolean;
  draftRules?: DraftRules;
  regionRules?: { [region: string]: number };
  // Champs optionnels pour compatibilité
  startDate?: string;
  endDate?: string;
  description?: string;
  participants?: GameParticipant[];
  teams?: Team[];
  /** Total count of available Fortnite players for draft selection */
  fortnitePlayerCount?: number;
}

// Interface pour les équipes
export interface Team {
  id: string;
  name: string;
  userId: string;
  userName: string;
  gameId: string;
  players: string[];
  totalScore: number;
  lastUpdated: string;
}

export interface CreateGameRequest {
  name: string;
  maxParticipants: number;
  description?: string;
  isPrivate?: boolean;
  autoStartDraft?: boolean;
  draftTimeLimit?: number;
  autoPickDelay?: number;
  currentSeason?: number;
  regionRules?: { [region: string]: number };
}

export interface JoinGameRequest {
  gameId: string;
  invitationCode?: string;
}

export interface GameResponse {
  success: boolean;
  message: string;
  data?: any;
}

export type GameStatus = 'CREATING' | 'DRAFTING' | 'ACTIVE' | 'FINISHED' | 'CANCELLED' | 'COMPLETED' | 'DRAFT' | 'RECRUITING';

export type InvitationCodeDuration = '24h' | '48h' | '7d' | 'permanent';

// Interfaces pour les participants
export interface GameParticipant {
  id: string;
  username: string;
  joinedAt: string;
  isCreator?: boolean;
  draftOrder?: number;
  selectedPlayers?: Player[];
  lastSelectionTime?: string;
  isCurrentTurn?: boolean;
  timeRemaining?: number;
  // Champs optionnels pour compatibilité
  role?: 'CREATOR' | 'PARTICIPANT';
  teamName?: string;
  playersCount?: number;
  totalScore?: number;
  isReady?: boolean;
}

// Interfaces pour le système de draft
export interface DraftState {
  gameId: string;
  status: DraftStatus;
  currentRound: number;
  totalRounds: number;
  currentPick: number;
  participants: DraftParticipant[];
  availablePlayers: Player[];
  rules: DraftRules;
  lastUpdated: string;
}

export interface DraftParticipant {
  id: string;
  username: string;
  draftOrder: number;
  isCurrentTurn: boolean;
  timeRemaining?: number;
  selections: DraftSelection[];
  isCreator: boolean;
}

export interface DraftSelection {
  playerId: string;
  playerName: string;
  selectionTime: string;
  round: number;
  pick: number;
  autoPick: boolean;
}

export interface DraftRules {
  maxPlayersPerTeam: number;
  timeLimitPerPick: number; // en secondes
  autoPickEnabled: boolean;
  regionQuotas: { [region: string]: number };
}

export type DraftStatus = 'NOT_STARTED' | 'ACTIVE' | 'PAUSED' | 'FINISHED' | 'CANCELLED';

// Interfaces pour les joueurs
export interface Player {
  id: string;
  username: string;
  nickname: string;
  region: string;
  tranche: string;
  currentSeason: number;
  selected?: boolean;
  available?: boolean;
}

// Interfaces pour les requêtes de draft
export interface DraftSelectionRequest {
  gameId: string;
  playerId: string;
}

export interface DraftActionRequest {
  gameId: string;
  action: 'pause' | 'resume' | 'cancel';
}

// Interfaces pour les statistiques et historique
export interface DraftStatistics {
  totalPicks: number;
  autoPicks: number;
  manualPicks: number;
  picksByRegion: { [region: string]: number };
  averagePickTime: number;
  fastestPick: number;
  slowestPick: number;
}

export interface DraftHistoryEntry {
  playerId: string;
  playerName: string;
  selectedBy: string;
  selectionTime: string;
  round: number;
  pick: number;
  autoPick: boolean;
  timeTaken: number; // en secondes
}

// Interfaces pour les notifications
export interface GameNotification {
  id: string;
  type: 'INFO' | 'WARNING' | 'ERROR' | 'SUCCESS';
  message: string;
  timestamp: string;
  gameId?: string;
  participantId?: string;
}

// Interfaces pour les codes d'invitation
export interface InvitationCode {
  code: string;
  gameId: string;
  gameName: string;
  creatorName: string;
  expiresAt?: string;
  maxUses?: number;
  currentUses: number;
}

// Interfaces pour les validations
export interface ValidationError {
  field: string;
  message: string;
  code: string;
}

export interface ValidationResult {
  isValid: boolean;
  errors: ValidationError[];
} 