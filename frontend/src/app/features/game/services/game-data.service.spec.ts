import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { GameDataService } from './game-data.service';
import { TranslationService } from '../../../core/services/translation.service';
import { LoggerService } from '../../../core/services/logger.service';
import { environment } from '../../../../environments/environment';
import { MOCK_GAME_PARTICIPANTS, MOCK_GAMES } from '../../../core/data/mock-game-data';

describe('GameDataService', () => {
  let service: GameDataService;
  let httpMock: HttpTestingController;
  let translationSpy: jasmine.SpyObj<TranslationService>;
  let loggerSpy: jasmine.SpyObj<LoggerService>;

  const apiBaseUrl = `${environment.apiUrl}/api`;
  let originalFallbackEnabled: boolean;

  beforeEach(() => {
    originalFallbackEnabled = environment.enableFallbackData;
    environment.enableFallbackData = true;

    translationSpy = jasmine.createSpyObj('TranslationService', ['t']);
    loggerSpy = jasmine.createSpyObj('LoggerService', ['debug', 'info', 'warn', 'error']);
    translationSpy.t.and.callFake((key: string) => {
      const values: Record<string, string> = {
        'errors.validation': 'Validation error',
        'errors.generic': 'Generic error',
        'errors.notFound': 'Resource not found',
        'errors.unauthorized': 'Unauthorized',
        'errors.handler.forbiddenMessage': 'Forbidden',
        'errors.handler.serverErrorMessage': 'Server error',
        'games.validation.idMissing': 'Game identifier is missing',
        'games.validation.nameRequired': 'Game name is required',
        'games.validation.creatorMissing': 'Creator name is missing',
        'games.validation.maxParticipantsInvalid': 'Max participants must be greater than zero',
        'games.validation.participantCountNegative': 'Participant count cannot be negative',
        'games.validation.participantCountExceeded': 'Participant count exceeds maximum'
      };
      return values[key] || key;
    });

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        GameDataService,
        { provide: TranslationService, useValue: translationSpy },
        { provide: LoggerService, useValue: loggerSpy }
      ]
    });

    service = TestBed.inject(GameDataService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    environment.enableFallbackData = originalFallbackEnabled;
    httpMock.verify();
  });

  it('maps game details from API response', () => {
    const apiGame = {
      id: 'game-1',
      name: 'Test Game',
      creatorUsername: 'TestUser',
      maxParticipants: 10,
      status: 'CREATING',
      createdAt: '2025-01-01T00:00:00Z',
      currentParticipantCount: 2
    };

    service.getGameById(apiGame.id).subscribe(game => {
      expect(game.id).toBe(apiGame.id);
      expect(game.name).toBe(apiGame.name);
      expect(game.creatorName).toBe('TestUser');
      expect(game.participantCount).toBe(2);
    });

    const req = httpMock.expectOne(`${apiBaseUrl}/games/${apiGame.id}`);
    expect(req.request.method).toBe('GET');
    req.flush(apiGame);
  });

  it('falls back to mock game on server error when fallback is enabled', () => {
    const fallbackGame = MOCK_GAMES[0];

    service.getGameById(fallbackGame.id).subscribe(game => {
      expect(game.id).toBe(fallbackGame.id);
      expect(game.name).toBe(fallbackGame.name);
    });

    const req = httpMock.expectOne(`${apiBaseUrl}/games/${fallbackGame.id}`);
    req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
  });

  it('surfaces translated not found error', () => {
    const missingId = 'missing-id';

    service.getGameById(missingId).subscribe({
      next: () => fail('expected getGameById to fail'),
      error: error => {
        expect(error.message).toContain('Resource not found');
        expect((error as Error & { status?: number }).status).toBe(404);
      }
    });

    const req = httpMock.expectOne(`${apiBaseUrl}/games/${missingId}`);
    req.flush('Not Found', { status: 404, statusText: 'Not Found' });
  });

  it('falls back to mock participants on server error when enabled', () => {
    const gameId = MOCK_GAMES[0].id;
    const fallbackParticipants = MOCK_GAME_PARTICIPANTS[gameId];

    service.getGameParticipants(gameId).subscribe(participants => {
      expect(participants.length).toBe(fallbackParticipants.length);
      expect(participants[0].username).toBe(fallbackParticipants[0].username);
    });

    const req = httpMock.expectOne(`${apiBaseUrl}/games/${gameId}/participants`);
    req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
  });

  it('returns translated validation errors instead of hardcoded strings', () => {
    const result = service.validateGameData({
      id: '',
      name: '',
      creatorName: '',
      maxParticipants: 0,
      participantCount: -1
    } as any);

    expect(result.isValid).toBeFalse();
    expect(result.errors).toContain('Game identifier is missing');
    expect(result.errors).toContain('Game name is required');
    expect(result.errors).toContain('Creator name is missing');
    expect(result.errors).toContain('Max participants must be greater than zero');
    expect(result.errors).toContain('Participant count cannot be negative');
  });

  it('returns translated exceeded participants error', () => {
    const result = service.validateGameData({
      id: 'game-1',
      name: 'Game',
      creatorName: 'Creator',
      maxParticipants: 2,
      participantCount: 3
    } as any);

    expect(result.isValid).toBeFalse();
    expect(result.errors).toEqual(['Participant count exceeds maximum']);
  });

  describe('getGameById edge cases', () => {
    it('rejects empty gameId with validation error', () => {
      service.getGameById('').subscribe({
        error: (err) => {
          expect(err.message).toBe('Validation error');
        }
      });
    });

    it('rejects whitespace-only gameId', () => {
      service.getGameById('   ').subscribe({
        error: (err) => {
          expect(err.message).toBe('Validation error');
        }
      });
    });

    it('maps 401 to unauthorized message', () => {
      service.getGameById('g-1').subscribe({
        error: (err) => {
          expect(err.message).toBe('Unauthorized');
          expect(err.status).toBe(401);
        }
      });

      httpMock.expectOne(`${apiBaseUrl}/games/g-1`)
        .flush(null, { status: 401, statusText: 'Unauthorized' });
    });

    it('maps 403 to forbidden message', () => {
      service.getGameById('g-1').subscribe({
        error: (err) => {
          expect(err.message).toBe('Forbidden');
          expect(err.status).toBe(403);
        }
      });

      httpMock.expectOne(`${apiBaseUrl}/games/g-1`)
        .flush(null, { status: 403, statusText: 'Forbidden' });
    });

    it('uses backend error message when provided', () => {
      service.getGameById('g-1').subscribe({
        error: (err) => {
          expect(err.message).toBe('Game is archived');
        }
      });

      httpMock.expectOne(`${apiBaseUrl}/games/g-1`)
        .flush({ message: 'Game is archived' }, { status: 400, statusText: 'Bad Request' });
    });

    it('does not use fallback for 404 errors', () => {
      service.getGameById('nonexistent').subscribe({
        error: (err) => {
          expect(err.message).toBe('Resource not found');
        }
      });

      httpMock.expectOne(`${apiBaseUrl}/games/nonexistent`)
        .flush(null, { status: 404, statusText: 'Not Found' });
    });
  });

  describe('getGameParticipants', () => {
    it('rejects empty gameId with validation error', () => {
      service.getGameParticipants('').subscribe({
        error: (err) => {
          expect(err.message).toBe('Validation error');
        }
      });
    });

    it('maps API participants correctly', () => {
      const apiParticipants = [
        { id: 'p1', username: 'User1', joinedAt: '2025-01-01T00:00:00Z', isCreator: true },
        { id: 'p2', username: 'User2', joinedAt: '2025-01-01T01:00:00Z', isCreator: false }
      ];

      service.getGameParticipants('game-1').subscribe(participants => {
        expect(participants.length).toBe(2);
        expect(participants[0].username).toBe('User1');
        expect(participants[1].isCreator).toBeFalse();
      });

      httpMock.expectOne(`${apiBaseUrl}/games/game-1/participants`)
        .flush(apiParticipants);
    });

    it('returns empty array for non-array response', () => {
      service.getGameParticipants('game-1').subscribe(participants => {
        expect(participants).toEqual([]);
        expect(loggerSpy.warn).toHaveBeenCalledWith(
          'GameDataService: participants payload is not an array',
          jasmine.objectContaining({ gameId: 'game-1' })
        );
      });

      httpMock.expectOne(`${apiBaseUrl}/games/game-1/participants`)
        .flush({ data: [] });
    });

    it('surfaces error for 404 when fallback disabled', () => {
      environment.enableFallbackData = false;

      service.getGameParticipants('game-1').subscribe({
        error: (err) => {
          expect(err.message).toBe('Resource not found');
        }
      });

      httpMock.expectOne(`${apiBaseUrl}/games/game-1/participants`)
        .flush(null, { status: 404, statusText: 'Not Found' });
    });
  });

  describe('getUserGames', () => {
    it('maps array of API games', () => {
      const apiGames = [
        { id: 'g1', name: 'Game1', creatorUsername: 'U1', maxParticipants: 4, status: 'CREATING', createdAt: '2025-01-01', currentParticipantCount: 1 },
        { id: 'g2', name: 'Game2', creatorUsername: 'U2', maxParticipants: 8, status: 'ACTIVE', createdAt: '2025-01-02', currentParticipantCount: 3 }
      ];

      service.getUserGames().subscribe(games => {
        expect(games.length).toBe(2);
        expect(games[0].creatorName).toBe('U1');
        expect(games[1].participantCount).toBe(3);
      });

      httpMock.expectOne(`${apiBaseUrl}/games/my-games`).flush(apiGames);
    });

    it('returns empty array for non-array response', () => {
      service.getUserGames().subscribe(games => {
        expect(games).toEqual([]);
      });

      httpMock.expectOne(`${apiBaseUrl}/games/my-games`).flush({ items: [] });
    });

    it('filters out games that fail to map', () => {
      const apiGames = [
        { id: 'g1', name: 'Good Game', creatorUsername: 'U1', maxParticipants: 4, status: 'CREATING', createdAt: '2025-01-01', currentParticipantCount: 1 },
        null
      ];

      service.getUserGames().subscribe(games => {
        expect(games.length).toBe(1);
        expect(games[0].name).toBe('Good Game');
      });

      httpMock.expectOne(`${apiBaseUrl}/games/my-games`).flush(apiGames);
    });

    it('handles HTTP error', () => {
      service.getUserGames().subscribe({
        error: (err) => {
          expect(err.message).toBe('Generic error');
          expect(loggerSpy.error).toHaveBeenCalledWith(
            'GameDataService: HTTP request failed',
            jasmine.objectContaining({ status: 418, message: 'Generic error' })
          );
        }
      });

      httpMock.expectOne(`${apiBaseUrl}/games/my-games`)
        .flush(null, { status: 418, statusText: 'Teapot' });
    });
  });

  describe('verifyGameExists', () => {
    it('returns true when game exists', () => {
      const apiGame = { id: 'g1', name: 'Game', creatorUsername: 'U', maxParticipants: 4, status: 'CREATING', createdAt: '2025-01-01', currentParticipantCount: 1 };

      service.verifyGameExists('g1').subscribe(exists => {
        expect(exists).toBeTrue();
      });

      httpMock.expectOne(`${apiBaseUrl}/games/g1`).flush(apiGame);
    });

    it('throws not found error when game is missing', () => {
      service.verifyGameExists('missing').subscribe({
        error: (err) => {
          expect(err.message).toBe('Resource not found');
        }
      });

      httpMock.expectOne(`${apiBaseUrl}/games/missing`)
        .flush(null, { status: 404, statusText: 'Not Found' });
    });

    it('rejects empty gameId', () => {
      service.verifyGameExists('').subscribe({
        error: (err) => {
          expect(err.message).toBe('Validation error');
        }
      });
    });
  });

  describe('calculateGameStatistics', () => {
    it('calculates fill percentage and available slots', () => {
      const game = { maxParticipants: 10, participantCount: 3, canJoin: true } as any;
      const stats = service.calculateGameStatistics(game);

      expect(stats.fillPercentage).toBe(30);
      expect(stats.availableSlots).toBe(7);
      expect(stats.isNearlyFull).toBeFalse();
      expect(stats.canAcceptMoreParticipants).toBeTrue();
    });

    it('detects nearly full games (>=80%)', () => {
      const game = { maxParticipants: 10, participantCount: 8, canJoin: true } as any;
      const stats = service.calculateGameStatistics(game);

      expect(stats.isNearlyFull).toBeTrue();
    });

    it('returns defaults for null game', () => {
      const stats = service.calculateGameStatistics(null as any);

      expect(stats.fillPercentage).toBe(0);
      expect(stats.availableSlots).toBe(0);
      expect(stats.canAcceptMoreParticipants).toBeFalse();
    });

    it('returns canAcceptMoreParticipants false when canJoin is false', () => {
      const game = { maxParticipants: 10, participantCount: 3, canJoin: false } as any;
      const stats = service.calculateGameStatistics(game);

      expect(stats.canAcceptMoreParticipants).toBeFalse();
    });
  });

  describe('validateGameData', () => {
    it('passes valid game data', () => {
      const result = service.validateGameData({
        id: 'game-1',
        name: 'Valid Game',
        creatorName: 'Creator',
        maxParticipants: 10,
        participantCount: 3
      } as any);

      expect(result.isValid).toBeTrue();
      expect(result.errors).toEqual([]);
    });
  });
});
