/**
 * Shared Team models for the Fortnite Pronos application
 */

import { Player, TeamPlayer } from './player.model';

/** Team owner information */
export interface TeamOwner {
  id: string;
  username: string;
}

/** Base team interface */
export interface BaseTeam {
  id: string;
  name: string;
  season: number;
}

/** Team with owner information */
export interface Team extends BaseTeam {
  owner?: TeamOwner;
  ownerName?: string;
  ownerId?: string;
  ownerUsername?: string;
  players: TeamPlayer[];
  totalPoints?: number;
  totalScore?: number;
}

/** Team with required owner (for strict typing) */
export interface TeamWithOwner extends Team {
  owner: TeamOwner;
}

/** Team statistics */
export interface TeamStats {
  totalPoints: number;
  averagePoints: number;
  topPlayerPoints: number;
  playersCount: number;
  regionDistribution: Record<string, number>;
  regionPointsDistribution: Record<string, number>;
}

/** Team composition for dashboard */
export interface TeamComposition {
  totalPlayers: number;
  byRegion: Record<string, number>;
  byTranche: Record<string, number>;
}

/** Team for editing purposes */
export interface TeamEditData {
  id: string;
  name: string;
  season: number;
  ownerUsername: string;
  totalScore: number;
  players: Array<{
    playerId: string;
    nickname: string;
    region: string;
    tranche: string;
  }>;
}
