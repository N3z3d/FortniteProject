import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { GameQueryService } from './game-query.service';
import { LoggerService } from '../../../core/services/logger.service';
import { environment } from '../../../../environments/environment';
import { MOCK_GAMES } from '../../../core/data/mock-game-data';
import {
  Game,
  DraftState,
  DraftHistoryEntry,
  DraftStatistics
} from '../models/game.interface';

describe('GameQueryService', () => {
  let service: GameQueryService;
  let httpMock: HttpTestingController;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
  let originalFallback: boolean;

  const apiBaseUrl = `${environment.apiUrl}/api`;
  type FlushResponse =
    | string
    | number
    | boolean
    | object
    | ArrayBuffer
    | Blob
    | null
    | (string | number | boolean | object | null)[];

  const mockGame: Game = {
    id: 'game-1',
    name: 'Test Game',
    creatorName: 'Tester',
    maxParticipants: 4,
    status: 'CREATING',
    createdAt: '2025-01-01T00:00:00Z',
    participantCount: 1,
    canJoin: true
  };

  const draftState: DraftState = {
    gameId: 'game-1',
    status: 'NOT_STARTED',
    currentRound: 0,
    totalRounds: 1,
    currentPick: 0,
    participants: [],
    availablePlayers: [],
    rules: {
      maxPlayersPerTeam: 1,
      timeLimitPerPick: 60,
      autoPickEnabled: false,
      regionQuotas: {}
    },
    lastUpdated: '2025-01-01T00:00:00Z'
  };

  const draftHistory: DraftHistoryEntry[] = [
    {
      playerId: 'player-1',
      playerName: 'Player One',
      selectedBy: 'user-1',
      selectionTime: '2025-01-01T00:00:00Z',
      round: 1,
      pick: 1,
      autoPick: false,
      timeTaken: 10
    }
  ];

  const draftStatistics: DraftStatistics = {
    totalPicks: 1,
    autoPicks: 0,
    manualPicks: 1,
    picksByRegion: { EU: 1 },
    averagePickTime: 10,
    fastestPick: 10,
    slowestPick: 10
  };

  const expectGet = (url: string, response: FlushResponse): void => {
    const req = httpMock.expectOne(url);
    expect(req.request.method).toBe('GET');
    req.flush(response);
  };

  beforeEach(() => {
    originalFallback = environment.enableFallbackData;
    loggerSpy = jasmine.createSpyObj('LoggerService', ['debug', 'info', 'warn', 'error']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        GameQueryService,
        { provide: LoggerService, useValue: loggerSpy }
      ]
    });

    service = TestBed.inject(GameQueryService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    environment.enableFallbackData = originalFallback;
    httpMock.verify();
    localStorage.clear();
  });

  it('fetches all games', () => {
    service.getAllGames().subscribe(games => {
      expect(games).toEqual([mockGame]);
    });

    expectGet(`${apiBaseUrl}/games`, [mockGame]);
  });

  it('fetches available games (alias for all games)', () => {
    service.getAvailableGames().subscribe(games => {
      expect(games).toEqual([mockGame]);
    });

    expectGet(`${apiBaseUrl}/games`, [mockGame]);
  });

  it('fetches game details (alias for getGameById)', () => {
    service.getGameDetails('game-1').subscribe(game => {
      expect(game).toEqual(mockGame);
    });

    expectGet(`${apiBaseUrl}/games/game-1`, mockGame);
  });

  it('validates invitation codes', () => {
    service.validateInvitationCode('INVITE').subscribe(isValid => {
      expect(isValid).toBe(true);
    });

    expectGet(`${apiBaseUrl}/games/validate-code/INVITE`, true);
  });

  it('falls back user games on server error when enabled', () => {
    environment.enableFallbackData = true;

    service.getUserGames().subscribe(games => {
      expect(games).toEqual(MOCK_GAMES);
    });

    const req = httpMock.expectOne(`${apiBaseUrl}/games/my-games`);
    req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    expect(loggerSpy.warn).toHaveBeenCalled();
  });

  it('surfaces errors when fallback is disabled', (done) => {
    environment.enableFallbackData = false;

    service.getUserGames().subscribe({
      next: () => fail('expected getUserGames to error'),
      error: (error) => {
        expect(error).toBeTruthy();
        expect(loggerSpy.error).toHaveBeenCalled();
        done();
      }
    });

    const req = httpMock.expectOne(`${apiBaseUrl}/games/my-games`);
    req.flush('Forbidden', { status: 403, statusText: 'Forbidden' });
  });

  it('fetches draft state, history, and statistics', () => {
    service.getDraftState('game-1').subscribe(state => {
      expect(state).toEqual(draftState);
    });

    service.getDraftHistory('game-1').subscribe(history => {
      expect(history).toEqual(draftHistory);
    });

    service.getDraftStatistics('game-1').subscribe(stats => {
      expect(stats).toEqual(draftStatistics);
    });

    expectGet(`${apiBaseUrl}/games/game-1/draft/state`, draftState);
    expectGet(`${apiBaseUrl}/games/game-1/draft/history`, draftHistory);
    expectGet(`${apiBaseUrl}/games/game-1/draft/statistics`, draftStatistics);
  });

  it('maps canJoinGame response to boolean', () => {
    service.canJoinGame('game-1').subscribe(canJoin => {
      expect(canJoin).toBe(false);
    });

    const req = httpMock.expectOne(`${apiBaseUrl}/games/game-1/can-join`);
    expect(req.request.method).toBe('GET');
    req.flush({ canJoin: false });
  });

  it('handles archived game ids in localStorage', () => {
    localStorage.setItem('archived_games', JSON.stringify(['game-1']));

    expect(service.getArchivedGameIds()).toEqual(['game-1']);
    expect(service.filterArchivedGames([mockGame])).toEqual([]);
  });

  it('evaluates host ownership', () => {
    const gameWithCreator = { ...mockGame, creatorId: 'user-1' } as Game & { creatorId: string };

    expect(service.isGameHost(gameWithCreator, 'user-1')).toBe(true);
    expect(service.isGameHost(gameWithCreator, 'user-2')).toBe(false);
  });
});
