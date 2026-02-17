import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { TeamDetailDataService, TeamLoadResult } from './team-detail-data.service';
import { LeaderboardService } from '../../../core/services/leaderboard.service';
import { LoggerService } from '../../../core/services/logger.service';
import { TranslationService } from '../../../core/services/translation.service';

describe('TeamDetailDataService', () => {
  let service: TeamDetailDataService;
  let leaderboardServiceSpy: jasmine.SpyObj<LeaderboardService>;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
  let translationServiceSpy: jasmine.SpyObj<TranslationService>;

  const mockLeaderboardTeam = {
    teamId: 'team1',
    teamName: 'Team Alpha',
    ownerUsername: 'testuser',
    ownerId: 'owner1',
    totalPoints: 5000,
    rank: 1,
    players: [
      {
        playerId: 'player1',
        nickname: 'Player1',
        region: 'EU',
        points: 2000,
        tranche: 'TRANCHE_1'
      },
      {
        playerId: 'player2',
        nickname: 'Player2',
        region: 'NAW',
        points: 3000,
        tranche: 'TRANCHE_2'
      }
    ]
  };

  const mockAllTeams = [
    mockLeaderboardTeam,
    {
      teamId: 'team2',
      teamName: 'Team Beta',
      ownerUsername: 'otheruser',
      ownerId: 'owner2',
      totalPoints: 3000,
      rank: 2,
      players: []
    }
  ];

  beforeEach(() => {
    leaderboardServiceSpy = jasmine.createSpyObj('LeaderboardService', ['getTeamLeaderboard']);
    loggerSpy = jasmine.createSpyObj('LoggerService', ['debug', 'info', 'warn', 'error']);
    translationServiceSpy = jasmine.createSpyObj('TranslationService', ['t']);

    translationServiceSpy.t.and.callFake((key: string) => key);

    TestBed.configureTestingModule({
      providers: [
        TeamDetailDataService,
        { provide: LeaderboardService, useValue: leaderboardServiceSpy },
        { provide: LoggerService, useValue: loggerSpy },
        { provide: TranslationService, useValue: translationServiceSpy }
      ]
    });

    service = TestBed.inject(TeamDetailDataService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('loadMyTeam', () => {
    it('should load user team successfully', (done) => {
      leaderboardServiceSpy.getTeamLeaderboard.and.returnValue(of(mockAllTeams));

      service.loadMyTeam('testuser').subscribe((result: TeamLoadResult) => {
        expect(result.team).toBeTruthy();
        expect(result.team?.id).toBe('team1');
        expect(result.team?.owner?.username).toBe('testuser');
        expect(result.team?.totalPoints).toBe(5000);
        expect(result.team?.players.length).toBe(2);
        expect(result.allTeams.length).toBe(2);
        expect(result.error).toBeNull();
        done();
      });
    });

    it('should handle case-insensitive username matching', (done) => {
      leaderboardServiceSpy.getTeamLeaderboard.and.returnValue(of(mockAllTeams));

      service.loadMyTeam('TESTUSER').subscribe((result: TeamLoadResult) => {
        expect(result.team).toBeTruthy();
        expect(result.team?.owner?.username).toBe('testuser');
        expect(result.error).toBeNull();
        done();
      });
    });

    it('should return error when user has no team', (done) => {
      leaderboardServiceSpy.getTeamLeaderboard.and.returnValue(of(mockAllTeams));

      service.loadMyTeam('nonexistent').subscribe((result: TeamLoadResult) => {
        expect(result.team).toBeNull();
        expect(result.allTeams.length).toBe(2);
        expect(result.error).toBe('teams.detail.notFoundForUser');
        expect(translationServiceSpy.t).toHaveBeenCalledWith('teams.detail.notFoundForUser');
        done();
      });
    });

    it('should handle error when loading teams fails', (done) => {
      leaderboardServiceSpy.getTeamLeaderboard.and.returnValue(
        throwError(() => new Error('API Error'))
      );

      service.loadMyTeam('testuser').subscribe((result: TeamLoadResult) => {
        expect(result.team).toBeNull();
        expect(result.allTeams).toEqual([]);
        expect(result.error).toBeTruthy();
        expect(loggerSpy.warn).toHaveBeenCalledWith(
          'TeamDetailDataService: failed to load teams for user',
          jasmine.objectContaining({ username: 'testuser' })
        );
        done();
      });
    });
  });

  describe('loadTeamById', () => {
    it('should load team by ID successfully', (done) => {
      leaderboardServiceSpy.getTeamLeaderboard.and.returnValue(of(mockAllTeams));

      service.loadTeamById('team1').subscribe((result: TeamLoadResult) => {
        expect(result.team).toBeTruthy();
        expect(result.team?.id).toBe('team1');
        expect(result.team?.owner?.username).toBe('testuser');
        expect(result.allTeams.length).toBe(2);
        expect(result.error).toBeNull();
        done();
      });
    });

    it('should return error when team not found', (done) => {
      leaderboardServiceSpy.getTeamLeaderboard.and.returnValue(of(mockAllTeams));

      service.loadTeamById('nonexistent').subscribe((result: TeamLoadResult) => {
        expect(result.team).toBeNull();
        expect(result.allTeams.length).toBe(2);
        expect(result.error).toBe('teams.detail.notFound');
        done();
      });
    });

    it('should handle error when loading teams fails', (done) => {
      leaderboardServiceSpy.getTeamLeaderboard.and.returnValue(
        throwError(() => new Error('API Error'))
      );

      service.loadTeamById('team1').subscribe((result: TeamLoadResult) => {
        expect(result.team).toBeNull();
        expect(result.allTeams).toEqual([]);
        expect(result.error).toBeTruthy();
        expect(loggerSpy.warn).toHaveBeenCalledWith(
          'TeamDetailDataService: failed to load team by id',
          jasmine.objectContaining({ teamId: 'team1' })
        );
        done();
      });
    });
  });

  describe('convertLeaderboardToTeam', () => {
    it('should convert leaderboard team to Team interface', () => {
      leaderboardServiceSpy.getTeamLeaderboard.and.returnValue(of(mockAllTeams));

      service.loadMyTeam('testuser').subscribe((result: TeamLoadResult) => {
        const team = result.team!;

        expect(team.id).toBe('team1');
        expect(team.owner?.username).toBe('testuser');
        expect(team.totalPoints).toBe(5000);
        expect(team.players.length).toBe(2);

        // Check first player conversion
        const firstPlayer = team.players[0];
        expect(firstPlayer.player.id).toBe('player1');
        expect(firstPlayer.player.nickname).toBe('Player1');
        expect(firstPlayer.player.region).toBe('EU');
        expect(firstPlayer.player.points).toBe(2000);
        expect(firstPlayer.player.tranche).toBe('TRANCHE_1');
      });
    });

    it('should handle teams with empty player list', () => {
      const emptyTeam = {
        ...mockLeaderboardTeam,
        players: []
      };
      leaderboardServiceSpy.getTeamLeaderboard.and.returnValue(of([emptyTeam]));

      service.loadMyTeam('testuser').subscribe((result: TeamLoadResult) => {
        expect(result.team).toBeTruthy();
        expect(result.team?.players).toEqual([]);
      });
    });

    it('should handle teams with missing optional fields', () => {
      const minimalTeam = {
        teamId: 'team1',
        teamName: 'Minimal Team',
        ownerUsername: 'testuser',
        players: []
      };
      leaderboardServiceSpy.getTeamLeaderboard.and.returnValue(of([minimalTeam]));

      service.loadMyTeam('testuser').subscribe((result: TeamLoadResult) => {
        const team = result.team!;
        expect(team.id).toBe('team1');
        expect(team.owner?.username).toBe('testuser');
        expect(team.totalPoints).toBe(0);
      });
    });
  });

  describe('data mapping', () => {
    it('should correctly map all player fields', (done) => {
      const detailedPlayer = {
        playerId: 'p1',
        nickname: 'TestPlayer',
        region: 'BR',
        points: 1500,
        tranche: 'TRANCHE_3',
        additionalField: 'ignored'
      };

      const teamWithDetailedPlayer = {
        ...mockLeaderboardTeam,
        players: [detailedPlayer]
      };

      leaderboardServiceSpy.getTeamLeaderboard.and.returnValue(of([teamWithDetailedPlayer]));

      service.loadMyTeam('testuser').subscribe((result: TeamLoadResult) => {
        const player = result.team!.players[0].player;

        expect(player.id).toBe('p1');
        expect(player.nickname).toBe('TestPlayer');
        expect(player.region).toBe('BR');
        expect(player.points).toBe(1500);
        expect(player.tranche).toBe('TRANCHE_3');
        done();
      });
    });

    it('should preserve all teams in allTeams array', (done) => {
      leaderboardServiceSpy.getTeamLeaderboard.and.returnValue(of(mockAllTeams));

      service.loadMyTeam('testuser').subscribe((result: TeamLoadResult) => {
        expect(result.allTeams).toEqual(mockAllTeams);
        expect(result.allTeams.length).toBe(2);
        done();
      });
    });
  });

  describe('error handling', () => {
    it('should provide user-friendly error message on network failure', (done) => {
      leaderboardServiceSpy.getTeamLeaderboard.and.returnValue(
        throwError(() => ({ status: 0, message: 'Network Error' }))
      );

      service.loadMyTeam('testuser').subscribe((result: TeamLoadResult) => {
        expect(result.team).toBeNull();
        expect(result.error).toBe('teams.detail.dataUnavailable');
        done();
      });
    });

    it('should handle null or undefined username gracefully', (done) => {
      leaderboardServiceSpy.getTeamLeaderboard.and.returnValue(of(mockAllTeams));

      service.loadMyTeam('').subscribe((result: TeamLoadResult) => {
        expect(result.team).toBeNull();
        expect(result.error).toBeTruthy();
        done();
      });
    });

    it('should handle empty teams array', (done) => {
      leaderboardServiceSpy.getTeamLeaderboard.and.returnValue(of([]));

      service.loadMyTeam('testuser').subscribe((result: TeamLoadResult) => {
        expect(result.team).toBeNull();
        expect(result.allTeams).toEqual([]);
        expect(result.error).toBe('teams.detail.notFoundForUser');
        done();
      });
    });
  });
});
