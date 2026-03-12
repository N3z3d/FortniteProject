/** Represents a single team entry in a game leaderboard ordered by delta PR descending. */
export interface TeamDeltaLeaderboardEntry {
  rank: number;
  participantId: string;
  username: string;
  deltaPr: number;
  periodStart: string; // ISO date "YYYY-MM-DD"
  periodEnd: string; // ISO date "YYYY-MM-DD"
  computedAt: string; // ISO datetime
}
