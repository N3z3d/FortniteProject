import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { LeaderboardService } from './leaderboard.service';
import { LoggerService } from './logger.service';
import { environment } from '../../../environments/environment';

describe('LeaderboardService', () => {
  let service: LeaderboardService;
  let httpMock: HttpTestingController;
  let logger: jasmine.SpyObj<LoggerService>;
  const endpoint = `${environment.apiUrl}/api/leaderboard/joueurs`;

  beforeEach(() => {
    logger = jasmine.createSpyObj('LoggerService', ['debug', 'error', 'warn', 'info']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        LeaderboardService,
        { provide: LoggerService, useValue: logger }
      ]
    });

    service = TestBed.inject(LeaderboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('maps wrapped player leaderboard payloads with region normalization', () => {
    const payload = [
      {
        playerId: 'p1',
        nickname: 'Alpha',
        username: 'alpha',
        region: 'EU',
        rank: 2,
        totalPoints: 120,
        teamsCount: 1
      }
    ];

    service.getPlayerLeaderboard(2025).subscribe(entries => {
      expect(entries.length).toBe(1);
      expect(entries[0].region).toBe('EU');
      expect(entries[0].rank).toBe(2);
      expect(entries[0].totalPoints).toBe(120);
    });

    const req = httpMock.expectOne(request => {
      return request.url === endpoint && request.params.get('season') === '2025';
    });
    expect(req.request.method).toBe('GET');
    req.flush(payload);
  });

  it('requests leaderboard with filters and returns data', () => {
    const payload = [
      {
        rank: 1,
        userId: 'u1',
        username: 'Team A',
        isSpecial: false,
        totalPoints: 100,
        pointsByRegion: {},
        regionsWon: 0,
        firstPlacePlayers: 0,
        worldChampions: 0,
        team: { id: 't1', name: 'Team A', season: 2025, tradesRemaining: 1, players: [] },
        recentMovements: []
      }
    ];

    service.getLeaderboard({ season: 2025, region: 'EU', gameId: 'game-1' }).subscribe(entries => {
      expect(entries.length).toBe(1);
      expect(entries[0].userId).toBe('u1');
    });

    const req = httpMock.expectOne(request => {
      return request.url === `${environment.apiUrl}/api/leaderboard`
        && request.params.get('season') === '2025'
        && request.params.get('region') === 'EU'
        && request.params.get('gameId') === 'game-1';
    });
    expect(req.request.method).toBe('GET');
    req.flush(payload);
  });

  it('returns empty list and logs error when leaderboard request fails', () => {
    service.getLeaderboard({ season: 2025 }).subscribe(entries => {
      expect(entries).toEqual([]);
    });

    const req = httpMock.expectOne(request => {
      return request.url === `${environment.apiUrl}/api/leaderboard`
        && request.params.get('season') === '2025';
    });
    req.error(new ProgressEvent('Network error'));
    expect(logger.error).toHaveBeenCalled();
  });

  it('returns empty list for unexpected payloads', () => {
    service.getPlayerLeaderboard(2025).subscribe(entries => {
      expect(entries).toEqual([]);
    });

    const req = httpMock.expectOne(request => {
      return request.url === endpoint && request.params.get('season') === '2025';
    });
    req.flush([]);
  });

  it('returns entries even when required player fields are missing', () => {
    const payload = [
      {
        playerId: '',
        nickname: '',
        region: null,
        totalPoints: 10
      }
    ];

    service.getPlayerLeaderboard(2025).subscribe(entries => {
      expect(entries.length).toBe(1);
    });

    const req = httpMock.expectOne(request => {
      return request.url === endpoint && request.params.get('season') === '2025';
    });
    req.flush(payload);
  });

  describe('canTrade', () => {
    it('validates trade successfully when all conditions are met', () => {
      const team = {
        id: 't1',
        name: 'Team A',
        season: 2025,
        tradesRemaining: 3,
        players: [
          { id: 'p1', nickname: 'Player1', region: 'EU' as const, tranche: '3', points: 100, isActive: true, rank: 5, isWorldChampion: false, lastUpdate: '2025-01-01' }
        ]
      };
      const playerIn = {
        id: 'p2',
        nickname: 'Player2',
        region: 'EU' as const,
        tranche: '3',
        points: 120,
        isActive: true,
        rank: 8,
        isWorldChampion: false,
        lastUpdate: '2025-01-01',
        isAvailable: true,
        stats: { totalPoints: 120, tournamentsPlayed: 5, averagePlacement: 10, winRate: 0.2 }
      };

      service.canTrade('t1', 'p1', 'p2').subscribe(validation => {
        expect(validation.isValid).toBe(true);
        expect(validation.newTeamState).toBeDefined();
        expect(validation.newTeamState?.tradesRemaining).toBe(2);
      });

      const teamReq = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/teams/t1`);
      teamReq.flush(team);

      const playerReq = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/players/p2`);
      playerReq.flush(playerIn);
    });

    it('rejects trade when no trades remaining', () => {
      const team = {
        id: 't1',
        name: 'Team A',
        season: 2025,
        tradesRemaining: 0,
        players: [
          { id: 'p1', nickname: 'Player1', region: 'EU' as const, tranche: '3', points: 100, isActive: true, rank: 5, isWorldChampion: false, lastUpdate: '2025-01-01' }
        ]
      };
      const playerIn = {
        id: 'p2',
        nickname: 'Player2',
        region: 'EU' as const,
        tranche: '3',
        points: 120,
        isActive: true,
        rank: 8,
        isWorldChampion: false,
        lastUpdate: '2025-01-01',
        isAvailable: true,
        stats: { totalPoints: 120, tournamentsPlayed: 5, averagePlacement: 10, winRate: 0.2 }
      };

      service.canTrade('t1', 'p1', 'p2').subscribe(validation => {
        expect(validation.isValid).toBe(false);
        expect(validation.reason).toBe('Plus de trades disponibles');
      });

      const teamReq = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/teams/t1`);
      teamReq.flush(team);

      const playerReq = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/players/p2`);
      playerReq.flush(playerIn);
    });

    it('rejects trade when incoming player rank is above 10', () => {
      const team = {
        id: 't1',
        name: 'Team A',
        season: 2025,
        tradesRemaining: 3,
        players: [
          { id: 'p1', nickname: 'Player1', region: 'EU' as const, tranche: '3', points: 100, isActive: true, rank: 5, isWorldChampion: false, lastUpdate: '2025-01-01' }
        ]
      };
      const playerIn = {
        id: 'p2',
        nickname: 'Player2',
        region: 'EU' as const,
        tranche: '3',
        points: 50,
        isActive: true,
        rank: 15,
        isWorldChampion: false,
        lastUpdate: '2025-01-01',
        isAvailable: true,
        stats: { totalPoints: 50, tournamentsPlayed: 2, averagePlacement: 20, winRate: 0.1 }
      };

      service.canTrade('t1', 'p1', 'p2').subscribe(validation => {
        expect(validation.isValid).toBe(false);
        expect(validation.reason).toBe('Le joueur entrant doit être top 10 de sa région');
      });

      const teamReq = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/teams/t1`);
      teamReq.flush(team);

      const playerReq = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/players/p2`);
      playerReq.flush(playerIn);
    });

    it('throws error when player not found in team', () => {
      const team = {
        id: 't1',
        name: 'Team A',
        season: 2025,
        tradesRemaining: 3,
        players: [
          { id: 'p1', nickname: 'Player1', region: 'EU' as const, tranche: '3', points: 100, isActive: true, rank: 5, isWorldChampion: false, lastUpdate: '2025-01-01' }
        ]
      };

      service.canTrade('t1', 'p999', 'p2').subscribe({
        error: (err) => {
          expect(err.message).toBe("Joueur sortant non trouvé dans l'équipe");
        }
      });

      const teamReq = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/teams/t1`);
      teamReq.flush(team);
    });
  });

  describe('executeTrade', () => {
    it('executes valid trade successfully', () => {
      const team = {
        id: 't1',
        name: 'Team A',
        season: 2025,
        tradesRemaining: 3,
        players: [
          { id: 'p1', nickname: 'Player1', region: 'EU' as const, tranche: '3', points: 100, isActive: true, rank: 5, isWorldChampion: false, lastUpdate: '2025-01-01' }
        ]
      };
      const playerIn = {
        id: 'p2',
        nickname: 'Player2',
        region: 'EU' as const,
        tranche: '3',
        points: 120,
        isActive: true,
        rank: 8,
        isWorldChampion: false,
        lastUpdate: '2025-01-01',
        isAvailable: true,
        stats: { totalPoints: 120, tournamentsPlayed: 5, averagePlacement: 10, winRate: 0.2 }
      };
      const updatedTeam = { ...team, tradesRemaining: 2 };

      service.executeTrade('t1', 'p1', 'p2').subscribe(result => {
        expect(result.tradesRemaining).toBe(2);
      });

      const teamReq = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/teams/t1`);
      teamReq.flush(team);

      const playerReq = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/players/p2`);
      playerReq.flush(playerIn);

      const tradeReq = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/teams/t1/trade`);
      expect(tradeReq.request.method).toBe('POST');
      expect(tradeReq.request.body.playerOutId).toBe('p1');
      expect(tradeReq.request.body.playerInId).toBe('p2');
      tradeReq.flush(updatedTeam);
    });

    it('throws error when validation fails', () => {
      const team = {
        id: 't1',
        name: 'Team A',
        season: 2025,
        tradesRemaining: 0,
        players: [
          { id: 'p1', nickname: 'Player1', region: 'EU' as const, tranche: '3', points: 100, isActive: true, rank: 5, isWorldChampion: false, lastUpdate: '2025-01-01' }
        ]
      };
      const playerIn = {
        id: 'p2',
        nickname: 'Player2',
        region: 'EU' as const,
        tranche: '3',
        points: 120,
        isActive: true,
        rank: 8,
        isWorldChampion: false,
        lastUpdate: '2025-01-01',
        isAvailable: true,
        stats: { totalPoints: 120, tournamentsPlayed: 5, averagePlacement: 10, winRate: 0.2 }
      };

      service.executeTrade('t1', 'p1', 'p2').subscribe({
        error: (err) => {
          expect(err.message).toBe('Plus de trades disponibles');
        }
      });

      const teamReq = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/teams/t1`);
      teamReq.flush(team);

      const playerReq = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/players/p2`);
      playerReq.flush(playerIn);
    });
  });

  describe('getTeamMovements', () => {
    it('fetches and sorts team movements by timestamp descending', () => {
      const movements = [
        { id: 'm1', type: 'TRADE' as const, description: 'Trade 1', timestamp: '2025-01-01' },
        { id: 'm2', type: 'SCORE_UPDATE' as const, description: 'Score', timestamp: '2025-01-03' },
        { id: 'm3', type: 'TEAM_UPDATE' as const, description: 'Update', timestamp: '2025-01-02' }
      ];

      service.getTeamMovements('t1').subscribe(result => {
        expect(result.length).toBe(3);
        expect(result[0].id).toBe('m2');
        expect(result[1].id).toBe('m3');
        expect(result[2].id).toBe('m1');
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/teams/t1/movements`);
      req.flush(movements);
    });
  });

  describe('updatePlayerPoints', () => {
    it('posts point update with correct payload', () => {
      const movement = { id: 'm1', type: 'SCORE_UPDATE' as const, description: 'Points added', timestamp: '2025-01-01' };

      service.updatePlayerPoints('p1', 50, 'Tournament win').subscribe(result => {
        expect(result.id).toBe('m1');
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/players/p1/points`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body.points).toBe(50);
      expect(req.request.body.reason).toBe('Tournament win');
      expect(req.request.body.timestamp).toBeDefined();
      req.flush(movement);
    });
  });

  describe('searchAvailablePlayers', () => {
    it('searches players with all criteria', () => {
      const players = [
        {
          id: 'p1',
          nickname: 'Player1',
          region: 'EU' as const,
          tranche: '3',
          points: 600,
          rank: 5,
          isActive: true,
          isWorldChampion: false,
          lastUpdate: '2025-01-01',
          isAvailable: true,
          stats: { totalPoints: 600, tournamentsPlayed: 5, averagePlacement: 10, winRate: 0.2 }
        }
      ];

      service.searchAvailablePlayers({ region: 'EU', tranche: '3', minRank: 10, minPoints: 500 }).subscribe(result => {
        expect(result.length).toBe(1);
        expect(result[0].id).toBe('p1');
      });

      const req = httpMock.expectOne(request => {
        return request.url === `${environment.apiUrl}/api/leaderboard/players/search`
          && request.params.get('available') === 'true'
          && request.params.get('region') === 'EU'
          && request.params.get('tranche') === '3'
          && request.params.get('minRank') === '10'
          && request.params.get('minPoints') === '500';
      });
      req.flush(players);
    });

    it('filters out unavailable players', () => {
      const players = [
        {
          id: 'p1',
          nickname: 'Player1',
          region: 'EU' as const,
          tranche: '3',
          points: 600,
          rank: 5,
          isActive: true,
          isWorldChampion: false,
          lastUpdate: '2025-01-01',
          isAvailable: true,
          stats: { totalPoints: 600, tournamentsPlayed: 5, averagePlacement: 10, winRate: 0.2 }
        },
        {
          id: 'p2',
          nickname: 'Player2',
          region: 'EU' as const,
          tranche: '3',
          points: 700,
          rank: 3,
          isActive: true,
          isWorldChampion: false,
          lastUpdate: '2025-01-01',
          isAvailable: false,
          stats: { totalPoints: 700, tournamentsPlayed: 6, averagePlacement: 8, winRate: 0.3 }
        }
      ];

      service.searchAvailablePlayers({ region: 'EU' }).subscribe(result => {
        expect(result.length).toBe(1);
        expect(result[0].id).toBe('p1');
      });

      const req = httpMock.expectOne(request => {
        return request.url === `${environment.apiUrl}/api/leaderboard/players/search`
          && request.params.get('available') === 'true';
      });
      req.flush(players);
    });
  });

  describe('getRegionRankings', () => {
    it('fetches and sorts region rankings by rank ascending', () => {
      const players = [
        {
          id: 'p3',
          nickname: 'Player3',
          region: 'EU' as const,
          tranche: '3',
          points: 500,
          rank: 10,
          isActive: true,
          isWorldChampion: false,
          lastUpdate: '2025-01-01',
          isAvailable: true,
          stats: { totalPoints: 500, tournamentsPlayed: 4, averagePlacement: 12, winRate: 0.15 }
        },
        {
          id: 'p1',
          nickname: 'Player1',
          region: 'EU' as const,
          tranche: '3',
          points: 800,
          rank: 2,
          isActive: true,
          isWorldChampion: false,
          lastUpdate: '2025-01-01',
          isAvailable: true,
          stats: { totalPoints: 800, tournamentsPlayed: 6, averagePlacement: 5, winRate: 0.4 }
        }
      ];

      service.getRegionRankings('EU').subscribe(result => {
        expect(result.length).toBe(2);
        expect(result[0].rank).toBe(2);
        expect(result[1].rank).toBe(10);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/rankings/EU`);
      req.flush(players);
    });
  });

  describe('isPlayerEligibleForSpecial', () => {
    it('returns true when player meets all eligibility criteria', () => {
      const player = {
        id: 'p1',
        nickname: 'Player1',
        region: 'EU' as const,
        tranche: '3',
        points: 600,
        rank: 5,
        isActive: true,
        isWorldChampion: false,
        lastUpdate: '2025-01-01',
        isAvailable: true,
        stats: { totalPoints: 600, tournamentsPlayed: 5, averagePlacement: 10, winRate: 0.2 }
      };

      service.isPlayerEligibleForSpecial('p1').subscribe(result => {
        expect(result).toBe(true);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/players/p1`);
      req.flush(player);
    });

    it('returns false when player rank is above 10', () => {
      const player = {
        id: 'p1',
        nickname: 'Player1',
        region: 'EU' as const,
        tranche: '3',
        points: 600,
        rank: 15,
        isActive: true,
        isWorldChampion: false,
        lastUpdate: '2025-01-01',
        isAvailable: true,
        stats: { totalPoints: 600, tournamentsPlayed: 5, averagePlacement: 10, winRate: 0.2 }
      };

      service.isPlayerEligibleForSpecial('p1').subscribe(result => {
        expect(result).toBe(false);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/players/p1`);
      req.flush(player);
    });

    it('returns false when player request fails', () => {
      service.isPlayerEligibleForSpecial('p1').subscribe(result => {
        expect(result).toBe(false);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/players/p1`);
      req.error(new ProgressEvent('Network error'));
    });
  });

  describe('updatePlayerAvailability', () => {
    it('patches player availability', () => {
      const updatedPlayer = {
        id: 'p1',
        nickname: 'Player1',
        region: 'EU' as const,
        tranche: '3',
        points: 600,
        rank: 5,
        isActive: true,
        isWorldChampion: false,
        lastUpdate: '2025-01-01',
        isAvailable: false,
        stats: { totalPoints: 600, tournamentsPlayed: 5, averagePlacement: 10, winRate: 0.2 }
      };

      service.updatePlayerAvailability('p1', false).subscribe(result => {
        expect(result.isAvailable).toBe(false);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/players/p1/availability`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body.isAvailable).toBe(false);
      expect(req.request.body.timestamp).toBeDefined();
      req.flush(updatedPlayer);
    });
  });

  describe('getPlayerStats', () => {
    it('fetches player stats', () => {
      const stats = { totalPoints: 600, tournamentsPlayed: 5, averagePlacement: 10, winRate: 0.2 };

      service.getPlayerStats('p1').subscribe(result => {
        expect(result.totalPoints).toBe(600);
        expect(result.tournamentsPlayed).toBe(5);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/players/p1/stats`);
      req.flush(stats);
    });
  });

  describe('getTopPerformersByRegion', () => {
    it('fetches top 10 performers by default', () => {
      const players = Array.from({ length: 15 }, (_, i) => ({
        id: `p${i}`,
        nickname: `Player${i}`,
        region: 'EU' as const,
        tranche: '3',
        points: 1000 - i * 10,
        rank: i + 1,
        isActive: true,
        isWorldChampion: false,
        lastUpdate: '2025-01-01',
        isAvailable: true,
        stats: { totalPoints: 1000 - i * 10, tournamentsPlayed: 5, averagePlacement: 10, winRate: 0.2 }
      }));

      service.getTopPerformersByRegion('EU').subscribe(result => {
        expect(result.length).toBe(10);
        expect(result[0].id).toBe('p0');
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/rankings/EU`);
      req.flush(players);
    });

    it('fetches custom number of top performers', () => {
      const players = Array.from({ length: 10 }, (_, i) => ({
        id: `p${i}`,
        nickname: `Player${i}`,
        region: 'NAW' as const,
        tranche: '3',
        points: 1000 - i * 10,
        rank: i + 1,
        isActive: true,
        isWorldChampion: false,
        lastUpdate: '2025-01-01',
        isAvailable: true,
        stats: { totalPoints: 1000 - i * 10, tournamentsPlayed: 5, averagePlacement: 10, winRate: 0.2 }
      }));

      service.getTopPerformersByRegion('NAW', 5).subscribe(result => {
        expect(result.length).toBe(5);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/rankings/NAW`);
      req.flush(players);
    });
  });

  describe('updatePlayerPointsWithValidation', () => {
    it('updates points after validation', () => {
      const player = {
        id: 'p1',
        nickname: 'Player1',
        region: 'EU' as const,
        tranche: '3',
        points: 600,
        rank: 5,
        isActive: true,
        isWorldChampion: false,
        lastUpdate: '2025-01-01',
        isAvailable: true,
        stats: { totalPoints: 600, tournamentsPlayed: 5, averagePlacement: 10, winRate: 0.2 }
      };
      const movement = { id: 'm1', type: 'SCORE_UPDATE' as const, description: 'Points added', timestamp: '2025-01-01' };

      service.updatePlayerPointsWithValidation('p1', 50, 'Tournament win').subscribe(result => {
        expect(result.id).toBe('m1');
      });

      const playerReq = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/players/p1`);
      playerReq.flush(player);

      const updateReq = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/players/p1/points`);
      updateReq.flush(movement);
    });

    it('throws error when points would become negative', () => {
      const player = {
        id: 'p1',
        nickname: 'Player1',
        region: 'EU' as const,
        tranche: '3',
        points: 50,
        rank: 5,
        isActive: true,
        isWorldChampion: false,
        lastUpdate: '2025-01-01',
        isAvailable: true,
        stats: { totalPoints: 50, tournamentsPlayed: 5, averagePlacement: 10, winRate: 0.2 }
      };

      service.updatePlayerPointsWithValidation('p1', -100, 'Penalty').subscribe({
        error: (err) => {
          expect(err.message).toBe('Les points ne peuvent pas être négatifs');
        }
      });

      const playerReq = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/players/p1`);
      playerReq.flush(player);
    });
  });

  describe('getTeamDetails', () => {
    it('fetches team details', () => {
      const team = {
        id: 't1',
        name: 'Team A',
        season: 2025,
        tradesRemaining: 3,
        players: [
          { id: 'p1', nickname: 'Player1', region: 'EU' as const, tranche: '3', points: 100, isActive: true, rank: 5, isWorldChampion: false, lastUpdate: '2025-01-01' }
        ]
      };

      service.getTeamDetails('t1').subscribe(result => {
        expect(result.id).toBe('t1');
        expect(result.players.length).toBe(1);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/teams/t1`);
      req.flush(team);
    });
  });

  describe('getPronostiqueurLeaderboard', () => {
    it('fetches pronostiqueur leaderboard with season filter', () => {
      const entries = [
        {
          userId: 'u1',
          username: 'user1',
          email: 'user1@test.com',
          rank: 1,
          totalPoints: 1000,
          totalTeams: 5,
          avgPointsPerTeam: 200,
          bestTeamPoints: 300,
          bestTeamName: 'Team A',
          victories: 3,
          winRate: 0.6
        }
      ];

      service.getPronostiqueurLeaderboard(2025).subscribe(result => {
        expect(result.length).toBe(1);
        expect(result[0].userId).toBe('u1');
      });

      const req = httpMock.expectOne(request => {
        return request.url === `${environment.apiUrl}/api/leaderboard/pronostiqueurs`
          && request.params.get('season') === '2025';
      });
      req.flush(entries);
    });

    it('returns empty array on error', () => {
      service.getPronostiqueurLeaderboard(2025).subscribe(result => {
        expect(result).toEqual([]);
      });

      const req = httpMock.expectOne(request => {
        return request.url === `${environment.apiUrl}/api/leaderboard/pronostiqueurs`;
      });
      req.error(new ProgressEvent('Network error'));
      expect(logger.error).toHaveBeenCalled();
    });
  });

  describe('getGameDeltaLeaderboard', () => {
    it('fetches game delta leaderboard for a given gameId', () => {
      const entries = [
        {
          rank: 1,
          participantId: 'p1',
          username: 'alice',
          deltaPr: 500,
          periodStart: '2025-01-01',
          periodEnd: '2025-12-31',
          computedAt: '2025-12-31T08:00:00'
        }
      ];

      service.getGameDeltaLeaderboard('game-42').subscribe(result => {
        expect(result.length).toBe(1);
        expect(result[0].username).toBe('alice');
        expect(result[0].deltaPr).toBe(500);
        expect(result[0].rank).toBe(1);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/games/game-42/leaderboard`);
      expect(req.request.method).toBe('GET');
      req.flush(entries);
    });

    it('propagates error when game delta leaderboard fails', () => {
      service.getGameDeltaLeaderboard('game-42').subscribe({
        error: (err) => {
          expect(err).toBeDefined();
          expect(logger.error).toHaveBeenCalled();
        }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/games/game-42/leaderboard`);
      req.error(new ProgressEvent('Network error'));
    });
  });

  describe('getTeamLeaderboard', () => {
    it('fetches team leaderboard successfully', () => {
      const teams = [
        { id: 't1', name: 'Team A', totalPoints: 500 },
        { id: 't2', name: 'Team B', totalPoints: 400 }
      ];

      service.getTeamLeaderboard().subscribe(result => {
        expect(result.length).toBe(2);
        expect(result[0].id).toBe('t1');
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard`);
      req.flush(teams);
      expect(logger.info).toHaveBeenCalled();
      expect(logger.debug).toHaveBeenCalled();
    });

    it('logs error and throws on failure', () => {
      service.getTeamLeaderboard().subscribe({
        error: (err) => {
          expect(err).toBeDefined();
          expect(logger.error).toHaveBeenCalled();
        }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard`);
      req.error(new ProgressEvent('Network error'));
    });
  });
});
