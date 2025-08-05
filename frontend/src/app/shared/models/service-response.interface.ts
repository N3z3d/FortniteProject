/**
 * Premium Service Response Pattern Interfaces
 * Designed for sophisticated UI integration and user experience
 */

export interface ServiceResponse<T = any> {
  success: boolean;
  data?: T;
  error?: ServiceError;
  metadata?: ResponseMetadata;
}

export interface ServiceError {
  code: string;
  message: string;
  userMessage?: string;
  field?: string;
  severity: 'error' | 'warning' | 'info';
}

export interface ResponseMetadata {
  timestamp: string;
  duration?: number;
  requestId?: string;
  performanceHints?: PerformanceHint[];
}

export interface PerformanceHint {
  type: 'cache' | 'retry' | 'offline' | 'optimization';
  message: string;
  action?: string;
}

// Enhanced Draft Operation Responses
export interface DraftOperationResponse extends ServiceResponse {
  data: {
    draft: DraftState;
    game: GameState;
    affectedParticipants?: string[];
    nextActions?: DraftAction[];
    stateTransition?: StateTransition;
  };
}

export interface GameJoinResponse extends ServiceResponse {
  data: {
    game: GameState;
    participant: ParticipantInfo;
    onboardingRequired?: boolean;
    welcomeMessage?: string;
    nextSteps?: ActionSuggestion[];
  };
}

export interface PlayerSelectionResponse extends ServiceResponse {
  data: {
    selectedPlayer: Player;
    updatedTeam: Player[];
    draftProgress: DraftProgress;
    nextPicker?: ParticipantInfo;
    regionQuotaUpdate?: RegionQuota[];
    teamCompletion?: TeamCompletion;
  };
}

// Supporting Interfaces
export interface DraftState {
  id: string;
  status: 'ACTIVE' | 'PAUSED' | 'FINISHED' | 'CANCELLED';
  currentRound: number;
  currentPick: number;
  totalRounds: number;
  timeRemaining?: number;
  participants: ParticipantInfo[];
}

export interface GameState {
  id: string;
  name: string;
  status: string;
  participantCount: number;
  maxParticipants: number;
  canJoin: boolean;
  draft?: DraftState;
}

export interface ParticipantInfo {
  id: string;
  username: string;
  avatar?: string;
  isCurrentTurn: boolean;
  draftOrder: number;
  selectedPlayers: Player[];
  isReady: boolean;
}

export interface DraftAction {
  type: 'select_player' | 'pause_draft' | 'resume_draft' | 'timeout_warning';
  label: string;
  enabled: boolean;
  timeoutSeconds?: number;
}

export interface StateTransition {
  from: string;
  to: string;
  reason: string;
  affectedUsers: string[];
  requiresRefresh: boolean;
}

export interface ActionSuggestion {
  type: 'navigate' | 'action' | 'info';
  label: string;
  icon?: string;
  route?: string;
  action?: () => void;
  priority: 'high' | 'medium' | 'low';
}

export interface DraftProgress {
  current: number;
  total: number;
  percentage: number;
  roundProgress: {
    current: number;
    total: number;
  };
}

export interface RegionQuota {
  region: string;
  current: number;
  maximum: number;
  remaining: number;
}

export interface TeamCompletion {
  isComplete: boolean;
  missingPositions: string[];
  score: number;
  recommendations?: string[];
}

export interface Player {
  id: string;
  nickname: string;
  username: string;
  region: string;
  tranche: string;
  avatar?: string;
  stats?: PlayerStats;
}

export interface PlayerStats {
  rank: number;
  score: number;
  recentPerformance: number[];
}