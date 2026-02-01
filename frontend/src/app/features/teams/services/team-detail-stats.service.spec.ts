import { TestBed } from '@angular/core/testing';
import { TeamDetailStatsService, TeamStats } from './team-detail-stats.service';
import { Team, TeamPlayer, Player } from './team-detail-data.service';

describe('TeamDetailStatsService', () => {
  let service: TeamDetailStatsService;

  const mockPlayer1: Player = {
    accountId: 'player1',
    epicAccountId: 'epic1',
    userName: 'Player1',
    region: 'EU',
    points: 1000,
    tranche: 'TRANCHE_1'
  };

  const mockPlayer2: Player = {
    accountId: 'player2',
    epicAccountId: 'epic2',
    userName: 'Player2',
    region: 'NAW',
    points: 2000,
    tranche: 'TRANCHE_2'
  };

  const mockPlayer3: Player = {
    accountId: 'player3',
    epicAccountId: 'epic3',
    userName: 'Player3',
    region: 'EU',
    points: 1500,
    tranche: 'NEW'
  };

  const mockTeamPlayers: TeamPlayer[] = [
    { player: mockPlayer1, joinedAt: new Date().toISOString() },
    { player: mockPlayer2, joinedAt: new Date().toISOString() },
    { player: mockPlayer3, joinedAt: new Date().toISOString() }
  ];

  const mockTeam: Team = {
    id: 'team1',
    ownerUsername: 'owner',
    players: mockTeamPlayers,
    totalPoints: 4500,
    rank: 1
  };

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TeamDetailStatsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('calculateStats', () => {
    it('should calculate team stats correctly', () => {
      const stats = service.calculateStats(mockTeam);

      expect(stats.totalPoints).toBe(4500);
      expect(stats.playersCount).toBe(3);
      expect(stats.averagePoints).toBe(1500);
      expect(stats.topPlayerPoints).toBe(2000);
    });

    it('should calculate region distribution correctly', () => {
      const stats = service.calculateStats(mockTeam);

      expect(stats.regionDistribution['EU']).toBe(2);
      expect(stats.regionDistribution['NAW']).toBe(1);
    });

    it('should calculate region points distribution correctly', () => {
      const stats = service.calculateStats(mockTeam);

      expect(stats.regionPointsDistribution['EU']).toBe(2500);
      expect(stats.regionPointsDistribution['NAW']).toBe(2000);
    });

    it('should handle team with no players', () => {
      const emptyTeam: Team = {
        id: 'empty',
        ownerUsername: 'owner',
        players: [],
        totalPoints: 0,
        rank: 1
      };

      const stats = service.calculateStats(emptyTeam);

      expect(stats.totalPoints).toBe(0);
      expect(stats.playersCount).toBe(0);
      expect(stats.averagePoints).toBe(0);
      expect(stats.topPlayerPoints).toBe(0);
    });
  });

  describe('getRegionColor', () => {
    it('should return correct color for EU', () => {
      expect(service.getRegionColor('EU')).toBe('#3b82f6');
    });

    it('should return correct color for NAW', () => {
      expect(service.getRegionColor('NAW')).toBe('#10b981');
    });

    it('should return correct color for NAE', () => {
      expect(service.getRegionColor('NAE')).toBe('#8b5cf6');
    });

    it('should return correct color for BR', () => {
      expect(service.getRegionColor('BR')).toBe('#f59e0b');
    });

    it('should return gray for unknown region', () => {
      expect(service.getRegionColor('UNKNOWN')).toBe('#6b7280');
    });
  });

  describe('getRegionFlag', () => {
    it('should return correct flag for EU', () => {
      expect(service.getRegionFlag('EU')).toBe('🇪🇺');
    });

    it('should return correct flag for NAW', () => {
      expect(service.getRegionFlag('NAW')).toBe('🇺🇸');
    });

    it('should return correct flag for NAE', () => {
      expect(service.getRegionFlag('NAE')).toBe('🇺🇸');
    });

    it('should return correct flag for BR', () => {
      expect(service.getRegionFlag('BR')).toBe('🇧🇷');
    });

    it('should return globe for unknown region', () => {
      expect(service.getRegionFlag('UNKNOWN')).toBe('🌐');
    });
  });

  describe('getTrancheColor', () => {
    it('should return correct color for tranche 1', () => {
      expect(service.getTrancheColor(1)).toBe('#ef4444');
    });

    it('should return correct color for tranche 2', () => {
      expect(service.getTrancheColor(2)).toBe('#f97316');
    });

    it('should return correct color for tranche 3', () => {
      expect(service.getTrancheColor(3)).toBe('#eab308');
    });

    it('should return gray for unknown tranche', () => {
      expect(service.getTrancheColor(99)).toBe('#6b7280');
    });
  });

  describe('getTrancheNumber', () => {
    it('should extract tranche number from string', () => {
      expect(service.getTrancheNumber('TRANCHE_1')).toBe(1);
      expect(service.getTrancheNumber('TRANCHE_2')).toBe(2);
      expect(service.getTrancheNumber('TRANCHE_3')).toBe(3);
    });

    it('should return 0 for NEW tranche', () => {
      expect(service.getTrancheNumber('NEW')).toBe(0);
    });

    it('should return 0 for invalid tranche', () => {
      expect(service.getTrancheNumber('INVALID')).toBe(0);
    });
  });

  describe('formatPoints', () => {
    it('should format points with k suffix for thousands', () => {
      expect(service.formatPoints(1500)).toBe('1.5k');
      expect(service.formatPoints(2000)).toBe('2k');
      expect(service.formatPoints(10500)).toBe('10.5k');
    });

    it('should return plain number for values less than 1000', () => {
      expect(service.formatPoints(500)).toBe('500');
      expect(service.formatPoints(999)).toBe('999');
    });
  });

  describe('getTopPlayers', () => {
    it('should return top 3 players sorted by points', () => {
      const topPlayers = service.getTopPlayers(mockTeam);

      expect(topPlayers.length).toBe(3);
      expect(topPlayers[0].player.points).toBe(2000);
      expect(topPlayers[1].player.points).toBe(1500);
      expect(topPlayers[2].player.points).toBe(1000);
    });

    it('should return fewer players if team has less than 3', () => {
      const smallTeam: Team = {
        id: 'small',
        ownerUsername: 'owner',
        players: [mockTeamPlayers[0]],
        totalPoints: 1000,
        rank: 1
      };

      const topPlayers = service.getTopPlayers(smallTeam);
      expect(topPlayers.length).toBe(1);
    });
  });

  describe('getSortedPlayers', () => {
    it('should return all players sorted by points descending', () => {
      const sorted = service.getSortedPlayers(mockTeam);

      expect(sorted.length).toBe(3);
      expect(sorted[0].player.points).toBe(2000);
      expect(sorted[1].player.points).toBe(1500);
      expect(sorted[2].player.points).toBe(1000);
    });
  });

  describe('getProgressPercentage', () => {
    it('should calculate percentage relative to top player', () => {
      const stats = service.calculateStats(mockTeam);

      expect(service.getProgressPercentage(2000, stats)).toBe(100);
      expect(service.getProgressPercentage(1500, stats)).toBe(75);
      expect(service.getProgressPercentage(1000, stats)).toBe(50);
    });

    it('should return 0 if stats is null', () => {
      expect(service.getProgressPercentage(1000, null)).toBe(0);
    });

    it('should return 0 if top player has 0 points', () => {
      const stats: TeamStats = {
        totalPoints: 0,
        averagePoints: 0,
        topPlayerPoints: 0,
        playersCount: 0,
        regionDistribution: {},
        regionPointsDistribution: {}
      };

      expect(service.getProgressPercentage(1000, stats)).toBe(0);
    });
  });

  describe('getRegionPercentage', () => {
    it('should calculate region percentage of total points', () => {
      const stats = service.calculateStats(mockTeam);

      const euPercentage = service.getRegionPercentage('EU', stats);
      const nawPercentage = service.getRegionPercentage('NAW', stats);

      expect(euPercentage).toBeCloseTo(55.56, 1); // 2500/4500
      expect(nawPercentage).toBeCloseTo(44.44, 1); // 2000/4500
    });

    it('should return 0 if stats is null', () => {
      expect(service.getRegionPercentage('EU', null)).toBe(0);
    });

    it('should return 0 if region has no points', () => {
      const stats = service.calculateStats(mockTeam);
      expect(service.getRegionPercentage('ASIA', stats)).toBe(0);
    });
  });

  describe('getSortedRegionsByPoints', () => {
    it('should return regions sorted by points descending', () => {
      const stats = service.calculateStats(mockTeam);
      const sorted = service.getSortedRegionsByPoints(stats);

      expect(sorted.length).toBe(2);
      expect(sorted[0]).toBe('EU'); // 2500 points
      expect(sorted[1]).toBe('NAW'); // 2000 points
    });

    it('should return empty array if stats is null', () => {
      expect(service.getSortedRegionsByPoints(null)).toEqual([]);
    });
  });

  describe('getPlayerRank', () => {
    it('should return correct rank for players', () => {
      expect(service.getPlayerRank(mockPlayer2, mockTeam)).toBe(1); // 2000 points
      expect(service.getPlayerRank(mockPlayer3, mockTeam)).toBe(2); // 1500 points
      expect(service.getPlayerRank(mockPlayer1, mockTeam)).toBe(3); // 1000 points
    });
  });

  describe('getRegionArcLength', () => {
    it('should calculate arc length for donut chart', () => {
      const stats = service.calculateStats(mockTeam);

      const euArc = service.getRegionArcLength('EU', stats);
      const nawArc = service.getRegionArcLength('NAW', stats);

      expect(euArc).toBeCloseTo(55.56, 1);
      expect(nawArc).toBeCloseTo(44.44, 1);
    });
  });

  describe('getRegionArcOffset', () => {
    it('should calculate arc offset for donut chart', () => {
      const stats = service.calculateStats(mockTeam);

      const euOffset = service.getRegionArcOffset('EU', stats);
      expect(euOffset).toBe(25); // First region starts at 25

      const nawOffset = service.getRegionArcOffset('NAW', stats);
      expect(nawOffset).toBeGreaterThan(25); // Second region starts after EU
    });

    it('should return 0 if stats is null', () => {
      expect(service.getRegionArcOffset('EU', null)).toBe(0);
    });
  });
});
