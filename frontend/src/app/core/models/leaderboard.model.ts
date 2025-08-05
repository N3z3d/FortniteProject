export interface Player {
  id: string;
  name: string;
  nickname?: string;
  avatar?: string;
  region: string;
  tranche: string;
  points: number;
  teamId?: string;
}

export interface LeaderboardEntryDTO {
  teamId: string;
  teamName: string;
  ownerName: string;
  totalPoints: number;
  rank: number;
  players: Player[];
  lastUpdate: string;
}

export interface LeaderboardResponse {
  entries: LeaderboardEntryDTO[];
  totalEntries: number;
  lastUpdate: string;
} 