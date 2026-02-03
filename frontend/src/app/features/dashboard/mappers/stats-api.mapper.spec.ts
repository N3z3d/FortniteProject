import { StatsApiMapper } from './stats-api.mapper';

describe('StatsApiMapper', () => {
  describe('mapApiStatsToDisplayStats', () => {
    it('should map valid API response to display stats', () => {
      const apiResponse = {
        stats: {
          totalTeams: 10,
          totalPlayers: 50,
          totalPoints: 5000,
          mostActiveTeam: 'Team Alpha',
          seasonProgress: 45.5
        }
      };

      const result = StatsApiMapper.mapApiStatsToDisplayStats(apiResponse);

      expect(result.totalTeams).toBe(10);
      expect(result.totalPlayers).toBe(50);
      expect(result.totalPoints).toBe(5000);
      expect(result.averagePointsPerTeam).toBe(500);
      expect(result.mostActiveTeam).toBe('Team Alpha');
      expect(result.seasonProgress).toBe(45.5);
    });

    it('should handle statistics property instead of stats', () => {
      const apiResponse = {
        statistics: {
          totalTeams: 5,
          totalPlayers: 25,
          totalPoints: 1000
        }
      };

      const result = StatsApiMapper.mapApiStatsToDisplayStats(apiResponse);

      expect(result.totalTeams).toBe(5);
      expect(result.totalPlayers).toBe(25);
      expect(result.totalPoints).toBe(1000);
    });

    it('should handle flat API response without nested stats', () => {
      const apiResponse = {
        totalTeams: 8,
        totalPlayers: 40,
        totalPoints: 3200
      };

      const result = StatsApiMapper.mapApiStatsToDisplayStats(apiResponse);

      expect(result.totalTeams).toBe(8);
      expect(result.totalPlayers).toBe(40);
      expect(result.totalPoints).toBe(3200);
    });

    it('should handle alternative property names (teams, players, points)', () => {
      const apiResponse = {
        teams: 6,
        players: 30,
        points: 2400
      };

      const result = StatsApiMapper.mapApiStatsToDisplayStats(apiResponse);

      expect(result.totalTeams).toBe(6);
      expect(result.totalPlayers).toBe(30);
      expect(result.totalPoints).toBe(2400);
    });

    it('should calculate season progress when not provided', () => {
      const apiResponse = {
        totalTeams: 5,
        totalPlayers: 25,
        totalPoints: 1000
      };

      const result = StatsApiMapper.mapApiStatsToDisplayStats(apiResponse);

      expect(result.seasonProgress).toBeGreaterThanOrEqual(0);
      expect(result.seasonProgress).toBeLessThanOrEqual(100);
    });

    it('should extract mostActiveTeam from topTeam.name', () => {
      const apiResponse = {
        stats: {
          totalTeams: 5,
          topTeam: { name: 'Team Beta' }
        }
      };

      const result = StatsApiMapper.mapApiStatsToDisplayStats(apiResponse);

      expect(result.mostActiveTeam).toBe('Team Beta');
    });

    it('should extract mostActiveTeam from leadingTeam', () => {
      const apiResponse = {
        stats: {
          totalTeams: 5,
          leadingTeam: 'Team Gamma'
        }
      };

      const result = StatsApiMapper.mapApiStatsToDisplayStats(apiResponse);

      expect(result.mostActiveTeam).toBe('Team Gamma');
    });

    it('should return empty stats when API response is null', () => {
      const result = StatsApiMapper.mapApiStatsToDisplayStats(null);

      expect(result.totalTeams).toBe(0);
      expect(result.totalPlayers).toBe(0);
      expect(result.totalPoints).toBe(0);
      expect(result.averagePointsPerTeam).toBe(0);
      expect(result.mostActiveTeam).toBe('');
    });

    it('should return empty stats when API response is undefined', () => {
      const result = StatsApiMapper.mapApiStatsToDisplayStats(undefined);

      expect(result.totalTeams).toBe(0);
      expect(result.totalPlayers).toBe(0);
    });

    it('should calculate average points per team correctly', () => {
      const apiResponse = {
        totalTeams: 4,
        totalPoints: 1250
      };

      const result = StatsApiMapper.mapApiStatsToDisplayStats(apiResponse);

      expect(result.averagePointsPerTeam).toBe(313); // Math.round(1250/4)
    });

    it('should return 0 average points when no teams', () => {
      const apiResponse = {
        totalTeams: 0,
        totalPoints: 1000
      };

      const result = StatsApiMapper.mapApiStatsToDisplayStats(apiResponse);

      expect(result.averagePointsPerTeam).toBe(0);
    });

    it('should include additional stats when available', () => {
      const apiResponse = {
        stats: {
          totalTeams: 5,
          averageKD: 1.5,
          totalKills: 1000,
          totalWins: 50,
          topPerformer: 'PlayerOne',
          winRate: 0.65
        }
      };

      const result = StatsApiMapper.mapApiStatsToDisplayStats(apiResponse);

      expect(result.additionalStats).toBeDefined();
      expect(result.additionalStats?.averageKD).toBe(1.5);
      expect(result.additionalStats?.totalKills).toBe(1000);
      expect(result.additionalStats?.totalWins).toBe(50);
      expect(result.additionalStats?.topPerformer).toBe('PlayerOne');
      expect(result.additionalStats?.winRate).toBe(0.65);
    });

    it('should map additional stats with alternative property names', () => {
      const apiResponse = {
        stats: {
          totalTeams: 5,
          avgKD: 2.0,
          kills: 2000,
          wins: 100,
          bestPlayer: 'PlayerTwo',
          globalWinRate: 0.75
        }
      };

      const result = StatsApiMapper.mapApiStatsToDisplayStats(apiResponse);

      expect(result.additionalStats?.averageKD).toBe(2.0);
      expect(result.additionalStats?.totalKills).toBe(2000);
      expect(result.additionalStats?.totalWins).toBe(100);
      expect(result.additionalStats?.topPerformer).toBe('PlayerTwo');
      expect(result.additionalStats?.winRate).toBe(0.75);
    });
  });

  describe('validateMappedStats', () => {
    it('should validate correct stats', () => {
      const stats = {
        totalTeams: 10,
        totalPlayers: 50,
        totalPoints: 5000,
        averagePointsPerTeam: 500,
        seasonProgress: 50
      };

      const result = StatsApiMapper.validateMappedStats(stats);

      expect(result).toBe(true);
    });

    it('should return false when stats is null', () => {
      const result = StatsApiMapper.validateMappedStats(null);

      expect(result).toBe(false);
    });

    it('should return false when stats is undefined', () => {
      const result = StatsApiMapper.validateMappedStats(undefined);

      expect(result).toBe(false);
    });

    it('should return false when required field is missing', () => {
      const stats = {
        totalTeams: 10,
        totalPlayers: 50,
        // totalPoints missing
        averagePointsPerTeam: 500
      };

      const result = StatsApiMapper.validateMappedStats(stats);

      expect(result).toBe(false);
    });

    it('should return false when totalTeams is negative', () => {
      const stats = {
        totalTeams: -5,
        totalPlayers: 50,
        totalPoints: 5000,
        averagePointsPerTeam: 500
      };

      const result = StatsApiMapper.validateMappedStats(stats);

      expect(result).toBe(false);
    });

    it('should return false when totalPlayers is negative', () => {
      const stats = {
        totalTeams: 10,
        totalPlayers: -10,
        totalPoints: 5000,
        averagePointsPerTeam: 500
      };

      const result = StatsApiMapper.validateMappedStats(stats);

      expect(result).toBe(false);
    });

    it('should return false when players exist but no teams', () => {
      const stats = {
        totalTeams: 0,
        totalPlayers: 50,
        totalPoints: 5000,
        averagePointsPerTeam: 0,
        seasonProgress: 50
      };

      const result = StatsApiMapper.validateMappedStats(stats);

      expect(result).toBe(false);
    });

    it('should return false when season progress is negative', () => {
      const stats = {
        totalTeams: 10,
        totalPlayers: 50,
        totalPoints: 5000,
        averagePointsPerTeam: 500,
        seasonProgress: -10
      };

      const result = StatsApiMapper.validateMappedStats(stats);

      expect(result).toBe(false);
    });

    it('should return false when season progress exceeds 100', () => {
      const stats = {
        totalTeams: 10,
        totalPlayers: 50,
        totalPoints: 5000,
        averagePointsPerTeam: 500,
        seasonProgress: 150
      };

      const result = StatsApiMapper.validateMappedStats(stats);

      expect(result).toBe(false);
    });

    it('should accept stats with 0 teams and 0 players', () => {
      const stats = {
        totalTeams: 0,
        totalPlayers: 0,
        totalPoints: 0,
        averagePointsPerTeam: 0,
        seasonProgress: 50
      };

      const result = StatsApiMapper.validateMappedStats(stats);

      expect(result).toBe(true);
    });
  });

  describe('mapLeaderboardToTeamStats', () => {
    it('should map leaderboard entries to team stats', () => {
      const leaderboard = [
        { teamName: 'Team A', totalPoints: 1000, players: [{}, {}, {}], rank: 1 },
        { teamName: 'Team B', totalPoints: 800, players: [{}, {}], rank: 2 },
        { teamName: 'Team C', totalPoints: 600, players: [{}, {}, {}, {}], rank: 3 }
      ];

      const result = StatsApiMapper.mapLeaderboardToTeamStats(leaderboard);

      expect(result.totalTeams).toBe(3);
      expect(result.averageTeamSize).toBe(3); // (3+2+4)/3 = 3
      expect(result.topTeams.length).toBe(3);
      expect(result.topTeams[0].name).toBe('Team A');
      expect(result.topTeams[0].points).toBe(1000);
    });

    it('should return empty stats when leaderboard is empty', () => {
      const result = StatsApiMapper.mapLeaderboardToTeamStats([]);

      expect(result.totalTeams).toBe(0);
      expect(result.averageTeamSize).toBe(0);
      expect(result.topTeams.length).toBe(0);
    });

    it('should return empty stats when leaderboard is null', () => {
      const result = StatsApiMapper.mapLeaderboardToTeamStats(null as any);

      expect(result.totalTeams).toBe(0);
    });

    it('should handle alternative property names (team.name, points)', () => {
      const leaderboard = [
        { team: { name: 'Team X' }, points: 500 }
      ];

      const result = StatsApiMapper.mapLeaderboardToTeamStats(leaderboard);

      expect(result.topTeams[0].name).toBe('Team X');
      expect(result.topTeams[0].points).toBe(500);
    });

    it('should aggregate duplicate team entries', () => {
      const leaderboard = [
        { teamName: 'Team A', totalPoints: 500, players: [{}] },
        { teamName: 'Team A', totalPoints: 300, players: [{}] }
      ];

      const result = StatsApiMapper.mapLeaderboardToTeamStats(leaderboard);

      expect(result.totalTeams).toBe(1);
      expect(result.topTeams[0].points).toBe(800); // 500 + 300
      expect(result.topTeams[0].playerCount).toBe(2); // 1 + 1
    });

    it('should sort teams by points descending', () => {
      const leaderboard = [
        { teamName: 'Team C', totalPoints: 300 },
        { teamName: 'Team A', totalPoints: 1000 },
        { teamName: 'Team B', totalPoints: 600 }
      ];

      const result = StatsApiMapper.mapLeaderboardToTeamStats(leaderboard);

      expect(result.topTeams[0].name).toBe('Team A');
      expect(result.topTeams[1].name).toBe('Team B');
      expect(result.topTeams[2].name).toBe('Team C');
    });

    it('should limit to top 10 teams', () => {
      const leaderboard = Array.from({ length: 15 }, (_, i) => ({
        teamName: `Team ${i}`,
        totalPoints: 1000 - (i * 50)
      }));

      const result = StatsApiMapper.mapLeaderboardToTeamStats(leaderboard);

      expect(result.totalTeams).toBe(15);
      expect(result.topTeams.length).toBe(10);
    });

    it('should assign correct ranks to top teams', () => {
      const leaderboard = [
        { teamName: 'Team A', totalPoints: 1000 },
        { teamName: 'Team B', totalPoints: 800 },
        { teamName: 'Team C', totalPoints: 600 }
      ];

      const result = StatsApiMapper.mapLeaderboardToTeamStats(leaderboard);

      expect(result.topTeams[0].rank).toBe(1);
      expect(result.topTeams[1].rank).toBe(2);
      expect(result.topTeams[2].rank).toBe(3);
    });

    it('should default to 1 player when players array is missing', () => {
      const leaderboard = [
        { teamName: 'Team A', totalPoints: 1000 }
      ];

      const result = StatsApiMapper.mapLeaderboardToTeamStats(leaderboard);

      expect(result.topTeams[0].playerCount).toBe(1);
    });
  });

  describe('calculatePerformanceMetrics', () => {
    it('should calculate all performance metrics', () => {
      const rawData = {
        totalTeams: 20,
        totalPlayers: 100,
        totalPoints: 50000
      };

      const result = StatsApiMapper.calculatePerformanceMetrics(rawData);

      expect(result.efficiency).toBeGreaterThanOrEqual(0);
      expect(result.efficiency).toBeLessThanOrEqual(100);
      expect(result.competition).toBeGreaterThanOrEqual(0);
      expect(result.activity).toBeGreaterThanOrEqual(0);
      expect(result.growth).toBeGreaterThanOrEqual(0);
    });

    it('should return zero metrics when rawData is null', () => {
      const result = StatsApiMapper.calculatePerformanceMetrics(null);

      expect(result.efficiency).toBe(0);
      expect(result.competition).toBe(0);
      expect(result.activity).toBe(0);
      expect(result.growth).toBe(0);
    });

    it('should cap competition at 100', () => {
      const rawData = {
        totalTeams: 100, // 100 * 2 = 200, should cap at 100
        totalPlayers: 500,
        totalPoints: 10000
      };

      const result = StatsApiMapper.calculatePerformanceMetrics(rawData);

      expect(result.competition).toBe(100);
    });

    it('should cap activity at 100', () => {
      const rawData = {
        totalTeams: 10,
        totalPlayers: 1000, // (1000/10)*10 = 1000, should cap at 100
        totalPoints: 10000
      };

      const result = StatsApiMapper.calculatePerformanceMetrics(rawData);

      expect(result.activity).toBe(100);
    });

    it('should return neutral growth when no historical data', () => {
      const rawData = {
        totalTeams: 10,
        totalPlayers: 50,
        totalPoints: 5000
      };

      const result = StatsApiMapper.calculatePerformanceMetrics(rawData);

      expect(result.growth).toBe(50); // Neutral value
    });

    it('should calculate growth from historical data', () => {
      const rawData = {
        totalTeams: 10,
        totalPlayers: 60,
        totalPoints: 5000,
        historicalData: [
          { totalPlayers: 50 },
          { totalPlayers: 60 }
        ]
      };

      const result = StatsApiMapper.calculatePerformanceMetrics(rawData);

      expect(result.growth).toBeGreaterThan(50); // Positive growth
    });

    it('should handle zero teams without division error', () => {
      const rawData = {
        totalTeams: 0,
        totalPlayers: 0,
        totalPoints: 0
      };

      const result = StatsApiMapper.calculatePerformanceMetrics(rawData);

      expect(result.efficiency).toBe(0);
      expect(result.competition).toBe(0);
    });

    it('should ensure all metrics are non-negative', () => {
      const rawData = {
        totalTeams: -5,
        totalPlayers: -10,
        totalPoints: -1000
      };

      const result = StatsApiMapper.calculatePerformanceMetrics(rawData);

      expect(result.efficiency).toBeGreaterThanOrEqual(0);
      expect(result.competition).toBeGreaterThanOrEqual(0);
      expect(result.activity).toBeGreaterThanOrEqual(0);
      expect(result.growth).toBeGreaterThanOrEqual(0);
    });
  });
});
