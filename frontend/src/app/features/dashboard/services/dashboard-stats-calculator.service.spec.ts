import { DashboardStatsCalculatorService } from './dashboard-stats-calculator.service';

describe('DashboardStatsCalculatorService', () => {
  let service: DashboardStatsCalculatorService;

  beforeEach(() => {
    service = new DashboardStatsCalculatorService();
  });

  it('returns null when entries are empty', () => {
    expect(service.buildStatsFromLeaderboard([], 0)).toBeNull();
  });

  it('builds stats and premium values from leaderboard entries', () => {
    const entries: any[] = [
      {
        teamName: 'Alpha',
        totalPoints: 100,
        players: [
          { region: 'EU', tranche: 1 },
          { region: { name: 'NAW' }, tranche: 2 }
        ]
      },
      {
        teamName: 'Beta',
        totalPoints: 300,
        players: [{ region: 'EU', tranche: 1 }]
      }
    ];

    const result = service.buildStatsFromLeaderboard(entries, 2, new Date(2025, 0, 1));

    expect(result).not.toBeNull();
    expect(result?.stats.totalTeams).toBe(2);
    expect(result?.stats.totalPlayers).toBe(3);
    expect(result?.stats.totalPoints).toBe(400);
    expect(result?.stats.averagePointsPerTeam).toBe(200);
    expect(result?.stats.mostActiveTeam).toBe('Beta');
    expect(result?.stats.seasonProgress).toBe(0);
    expect(result?.stats.teamComposition?.regions).toEqual({ EU: 2, NAW: 1 });
    expect(result?.stats.teamComposition?.tranches).toEqual({
      'Tranche 1': 2,
      'Tranche 2': 1
    });
    expect(result?.premium).toEqual({
      totalScore: 400,
      activeGames: 2,
      weeklyBest: 300,
      ranking: 2
    });
  });

  it('calculates season progress with a provided date', () => {
    const result = service.calculateSeasonProgress(new Date(2025, 0, 1));

    expect(result).toBe(0);
  });

  it('applies stat updates and recalculates competition stats', () => {
    const now = new Date(2025, 0, 1);
    const current = {
      totalTeams: 1,
      totalPlayers: 2,
      totalPoints: 3,
      averagePointsPerTeam: 3,
      mostActiveTeam: 'Alpha',
      seasonProgress: 0
    };

    const result = service.applyStatsUpdate(current, { totalPlayers: 5 }, now);

    expect(result.stats.totalPlayers).toBe(5);
    expect(result.stats.lastUpdate).toBe(now.toISOString());
    expect(result.competitionStats.totalPlayers).toBe(5);
    expect(result.competitionStats.seasonProgress).toBe(0);
  });

  it('applies competition stats from api data', () => {
    const now = new Date(2025, 0, 1);
    const current = {
      totalTeams: 4,
      totalPlayers: 8,
      totalPoints: 400,
      averagePointsPerTeam: 100,
      mostActiveTeam: 'Alpha',
      seasonProgress: 0
    };

    const result = service.applyCompetitionStats(
      current,
      { totalTeams: 10, totalPlayers: 20, totalPoints: 1000, averagePoints: 120 },
      now
    );

    expect(result.competitionStats.totalTeams).toBe(10);
    expect(result.competitionStats.totalPlayers).toBe(20);
    expect(result.competitionStats.averagePointsPerTeam).toBe(120);
    expect(result.stats.totalPlayers).toBe(20);
    expect(result.stats.totalPoints).toBe(1000);
  });
});
