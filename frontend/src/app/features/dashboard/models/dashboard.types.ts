export interface TeamComposition {
  regions: Record<string, number>;
  tranches: Record<string, number>;
}

export interface DashboardStats {
  totalTeams: number;
  totalPlayers: number;
  totalPoints: number;
  averagePointsPerTeam: number;
  mostActiveTeam: string;
  seasonProgress: number;
  proPlayersCount?: number;
  teamComposition?: TeamComposition;
  lastUpdate?: string;
}

export interface CompetitionStats {
  totalTeams: number;
  totalPlayers: number;
  totalPoints: number;
  averagePointsPerTeam: number;
  mostActiveTeam: string;
  seasonProgress: number;
}

export interface PremiumStats {
  totalScore: number;
  ranking: number;
  activeGames: number;
  weeklyBest: number;
}
