/**
 * Shared Player models for the Fortnite Pronos application
 */

/** Base player interface with minimal required fields */
export interface BasePlayer {
  id?: string;
  playerId?: string;
  nickname: string;
  region: string;
}

/** Player with points information */
export interface PlayerWithPoints extends BasePlayer {
  points?: number;
  totalPoints?: number;
}

/** Player with tranche/tier information */
export interface PlayerWithTranche extends BasePlayer {
  tranche: string;
}

/** Full player interface combining all fields */
export interface Player extends BasePlayer {
  tranche?: string;
  points?: number;
  totalPoints?: number;
  username?: string;
  rank?: number;
}

/** Player in a team context */
export interface TeamPlayer {
  player: Player;
  position: number;
  playerId?: string;
  active?: boolean;
}

/** Leaderboard player entry */
export interface LeaderboardPlayer {
  playerId: string;
  nickname: string;
  username: string;
  region: string;
  totalPoints: number;
  rank: number;
}
