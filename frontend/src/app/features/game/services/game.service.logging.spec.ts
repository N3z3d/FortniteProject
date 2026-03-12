import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { GameQueryService } from './game-query.service';
import { LoggerService } from '../../../core/services/logger.service';
import { environment } from '../../../../environments/environment';
import { Game } from '../models/game.interface';

describe('GameQueryService - Enhanced Logging (JIRA-4A)', () => {
  let service: GameQueryService;
  let httpMock: HttpTestingController;
  let mockLogger: jasmine.SpyObj<LoggerService>;

  beforeEach(() => {
    mockLogger = jasmine.createSpyObj('LoggerService', ['debug', 'info', 'warn', 'error']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        GameQueryService,
        { provide: LoggerService, useValue: mockLogger }
      ]
    });

    service = TestBed.inject(GameQueryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    flushI18nRequests();
    httpMock.verify();
  });

  function flushI18nRequests(): void {
    const i18nRequests = httpMock.match((request) => request.url.startsWith('assets/i18n/'));
    i18nRequests.forEach((request) => request.flush({}));
  }

  describe('getGameById with logging', () => {
    it('logs request with requestId before API call', () => {
      const gameId = 'game-123';
      const mockGame: Game = {
        id: gameId,
        name: 'Test Game',
        status: 'ACTIVE',
        participantCount: 4,
        maxParticipants: 8,
        createdAt: new Date(),
        creatorName: 'Test User',
        canJoin: true
      };

      service.getGameById(gameId).subscribe({
        next: () => {
          expect(mockLogger.info).toHaveBeenCalledWith(
            'GameQueryService: fetching game by ID',
            jasmine.objectContaining({
              gameId,
              requestId: jasmine.stringMatching(/^req_\d+_[a-z0-9]+$/)
            })
          );
        }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/games/${gameId}`);
      req.flush(mockGame);
    });

    it('logs success with game details after API call', () => {
      const gameId = 'game-456';
      const mockGame: Game = {
        id: gameId,
        name: 'Victory League',
        status: 'DRAFTING',
        participantCount: 6,
        maxParticipants: 10,
        createdAt: new Date(),
        creatorName: 'Champion',
        canJoin: false
      };

      service.getGameById(gameId).subscribe({
        next: () => {
          expect(mockLogger.debug).toHaveBeenCalledWith(
            'GameQueryService: game fetched successfully',
            jasmine.objectContaining({
              gameId,
              gameName: 'Victory League',
              requestId: jasmine.any(String)
            })
          );
        }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/games/${gameId}`);
      req.flush(mockGame);
    });

    it('logs detailed error context on HTTP failure', () => {
      const gameId = 'nonexistent-game';
      const errorMessage = 'Game not found';

      service.getGameById(gameId).subscribe({
        error: () => {
          expect(mockLogger.error).toHaveBeenCalledWith(
            'GameQueryService.getGameById: HTTP error',
            jasmine.objectContaining({
              gameId,
              status: 404,
              statusText: 'Not Found',
              errorMessage: jasmine.stringMatching(/Game not found|errors\.notFound|Ressource non trouv/),
              requestId: jasmine.stringMatching(/^req_\d+_[a-z0-9]+$/),
              timestamp: jasmine.any(String)
            })
          );
        }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/games/${gameId}`);
      req.flush(errorMessage, { status: 404, statusText: 'Not Found' });
    });

    it('logs network error (status 0) with appropriate message', () => {
      const gameId = 'game-network-fail';

      service.getGameById(gameId).subscribe({
        error: () => {
          expect(mockLogger.error).toHaveBeenCalledWith(
            'GameQueryService.getGameById: HTTP error',
            jasmine.objectContaining({
              status: 0,
              errorMessage: jasmine.stringMatching(/errors\.network|Erreur de connexion|communication/)
            })
          );
        }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/games/${gameId}`);
      req.error(new ProgressEvent('Network error'), { status: 0 });
    });

    it('includes URL in error logs for debugging', () => {
      const gameId = 'game-500';

      service.getGameById(gameId).subscribe({
        error: () => {
          expect(mockLogger.error).toHaveBeenCalledWith(
            'GameQueryService.getGameById: HTTP error',
            jasmine.objectContaining({
              url: `${environment.apiUrl}/api/games/${gameId}`
            })
          );
        }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/games/${gameId}`);
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    });
  });

  describe('getGameParticipants with logging', () => {
    it('logs request with correlation ID', () => {
      const gameId = 'game-789';

      service.getGameParticipants(gameId).subscribe({
        next: () => {
          expect(mockLogger.info).toHaveBeenCalledWith(
            'GameQueryService: fetching game participants',
            jasmine.objectContaining({
              gameId,
              requestId: jasmine.stringMatching(/^req_/)
            })
          );
        }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/games/${gameId}/participants`);
      req.flush([]);
    });

    it('logs participant count on success', () => {
      const gameId = 'game-participants';
      const mockParticipants = [
        { id: 'p1', username: 'user1', isCreator: true },
        { id: 'p2', username: 'user2', isCreator: false },
        { id: 'p3', username: 'user3', isCreator: false }
      ];

      service.getGameParticipants(gameId).subscribe({
        next: () => {
          expect(mockLogger.debug).toHaveBeenCalledWith(
            'GameQueryService: participants fetched',
            jasmine.objectContaining({
              gameId,
              count: 3,
              requestId: jasmine.any(String)
            })
          );
        }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/games/${gameId}/participants`);
      req.flush(mockParticipants);
    });

    it('logs error with context on participant fetch failure', () => {
      const gameId = 'game-error';

      service.getGameParticipants(gameId).subscribe({
        error: () => {
          expect(mockLogger.error).toHaveBeenCalledWith(
            'GameQueryService.getGameParticipants: HTTP error',
            jasmine.objectContaining({
              gameId,
              status: jasmine.any(Number),
              requestId: jasmine.any(String),
              timestamp: jasmine.any(String)
            })
          );
        }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/games/${gameId}/participants`);
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });
  });

  describe('Request ID generation', () => {
    it('generates unique request IDs for concurrent requests', () => {
      const gameIds = ['game-1', 'game-2', 'game-3'];
      const requestIds: string[] = [];

      gameIds.forEach((gameId, index) => {
        service.getGameById(gameId).subscribe();

        const req = httpMock.match(`${environment.apiUrl}/api/games/${gameId}`)[0];
        req.flush({ id: gameId, name: `Game ${index}` });
      });

      mockLogger.info.calls.all().forEach(call => {
        const context = call.args[1] as { requestId: string };
        requestIds.push(context.requestId);
      });

      const uniqueIds = new Set(requestIds);
      expect(uniqueIds.size).toBe(requestIds.length);
      expect(requestIds.length).toBe(gameIds.length);
    });
  });

  describe('Error message mapping', () => {
    const errorStatusCases = [
      { status: 400, expectedFragment: 'Error' },
      { status: 401, expectedFragment: 'errors.unauthorized' },
      { status: 403, expectedFragment: 'Error' },
      { status: 404, expectedFragment: 'Error' },
      { status: 500, expectedFragment: 'Error' },
      { status: 502, expectedFragment: 'errors.network' },
      { status: 503, expectedFragment: 'errors.network' }
    ];

    errorStatusCases.forEach(({ status, expectedFragment }) => {
      it(`maps HTTP ${status} to a user-friendly message`, () => {
        service.getGameById('test-game').subscribe({
          error: () => {
            expect(mockLogger.error).toHaveBeenCalledWith(
              jasmine.any(String),
              jasmine.objectContaining({
                errorMessage: jasmine.stringMatching(expectedFragment),
                status
              })
            );
          }
        });

        const req = httpMock.expectOne(`${environment.apiUrl}/api/games/test-game`);
        req.flush('Error', { status, statusText: 'Error' });
      });
    });
  });
});
