import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { LiveAnnouncer } from '@angular/cdk/a11y';
import { GameService } from './game.service';
import { LoggerService } from '../../../core/services/logger.service';
import { environment } from '../../../../environments/environment';
import { Game } from '../models/game.interface';

describe('GameService - Enhanced Logging (JIRA-4A)', () => {
  let service: GameService;
  let httpMock: HttpTestingController;
  let mockLogger: jasmine.SpyObj<LoggerService>;
  let mockAnnouncer: jasmine.SpyObj<LiveAnnouncer>;

  beforeEach(() => {
    mockLogger = jasmine.createSpyObj('LoggerService', ['debug', 'info', 'warn', 'error']);
    mockAnnouncer = jasmine.createSpyObj('LiveAnnouncer', ['announce']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, MatSnackBarModule],
      providers: [
        GameService,
        { provide: LoggerService, useValue: mockLogger },
        { provide: LiveAnnouncer, useValue: mockAnnouncer }
      ]
    });

    service = TestBed.inject(GameService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getGameById with logging', () => {
    it('should log request with requestId before API call', (done) => {
      // Arrange
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

      // Act
      service.getGameById(gameId).subscribe({
        next: () => {
          // Assert
          expect(mockLogger.info).toHaveBeenCalledWith(
            'GameService: fetching game by ID',
            jasmine.objectContaining({
              gameId,
              requestId: jasmine.stringMatching(/^req_\d+_[a-z0-9]+$/)
            })
          );
          done();
        }
      });

      // Respond to HTTP request
      const req = httpMock.expectOne(`${environment.apiUrl}/api/games/${gameId}`);
      req.flush(mockGame);
    });

    it('should log success with game details after API call', (done) => {
      // Arrange
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

      // Act
      service.getGameById(gameId).subscribe({
        next: () => {
          // Assert
          expect(mockLogger.debug).toHaveBeenCalledWith(
            'GameService: game fetched successfully',
            jasmine.objectContaining({
              gameId,
              gameName: 'Victory League',
              requestId: jasmine.any(String)
            })
          );
          done();
        }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/games/${gameId}`);
      req.flush(mockGame);
    });

    it('should log detailed error context on HTTP failure', (done) => {
      // Arrange
      const gameId = 'nonexistent-game';
      const errorMessage = 'Game not found';

      // Act
      service.getGameById(gameId).subscribe({
        error: () => {
          // Assert
          expect(mockLogger.error).toHaveBeenCalledWith(
            'GameService.getGameById: HTTP error',
            jasmine.objectContaining({
              gameId,
              status: 404,
              statusText: 'Not Found',
              errorMessage: 'Ressource non trouvée',
              requestId: jasmine.stringMatching(/^req_\d+_[a-z0-9]+$/),
              timestamp: jasmine.any(String)
            })
          );
          done();
        }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/games/${gameId}`);
      req.flush(errorMessage, { status: 404, statusText: 'Not Found' });
    });

    it('should log network error (status 0) with appropriate message', (done) => {
      // Arrange
      const gameId = 'game-network-fail';

      // Act
      service.getGameById(gameId).subscribe({
        error: () => {
          // Assert
          expect(mockLogger.error).toHaveBeenCalledWith(
            'GameService.getGameById: HTTP error',
            jasmine.objectContaining({
              status: 0,
              errorMessage: 'Erreur de communication avec le serveur (vérifiez votre connexion)'
            })
          );
          done();
        }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/games/${gameId}`);
      req.error(new ProgressEvent('Network error'), { status: 0 });
    });

    it('should include URL in error logs for debugging', (done) => {
      // Arrange
      const gameId = 'game-500';

      // Act
      service.getGameById(gameId).subscribe({
        error: () => {
          // Assert
          expect(mockLogger.error).toHaveBeenCalledWith(
            'GameService.getGameById: HTTP error',
            jasmine.objectContaining({
              url: `${environment.apiUrl}/api/games/${gameId}`
            })
          );
          done();
        }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/games/${gameId}`);
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    });
  });

  describe('getGameParticipants with logging', () => {
    it('should log request with correlation ID', (done) => {
      // Arrange
      const gameId = 'game-789';

      // Act
      service.getGameParticipants(gameId).subscribe({
        next: () => {
          // Assert
          expect(mockLogger.info).toHaveBeenCalledWith(
            'GameService: fetching game participants',
            jasmine.objectContaining({
              gameId,
              requestId: jasmine.stringMatching(/^req_/)
            })
          );
          done();
        }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/games/${gameId}/participants`);
      req.flush([]);
    });

    it('should log participant count on success', (done) => {
      // Arrange
      const gameId = 'game-participants';
      const mockParticipants = [
        { id: 'p1', username: 'user1', isCreator: true },
        { id: 'p2', username: 'user2', isCreator: false },
        { id: 'p3', username: 'user3', isCreator: false }
      ];

      // Act
      service.getGameParticipants(gameId).subscribe({
        next: () => {
          // Assert
          expect(mockLogger.debug).toHaveBeenCalledWith(
            'GameService: participants fetched',
            jasmine.objectContaining({
              gameId,
              count: 3,
              requestId: jasmine.any(String)
            })
          );
          done();
        }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/games/${gameId}/participants`);
      req.flush(mockParticipants);
    });

    it('should log error with context on participant fetch failure', (done) => {
      // Arrange
      const gameId = 'game-error';

      // Act
      service.getGameParticipants(gameId).subscribe({
        error: () => {
          // Assert
          expect(mockLogger.error).toHaveBeenCalledWith(
            'GameService.getGameParticipants: HTTP error',
            jasmine.objectContaining({
              gameId,
              status: jasmine.any(Number),
              requestId: jasmine.any(String),
              timestamp: jasmine.any(String)
            })
          );
          done();
        }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/games/${gameId}/participants`);
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });
  });

  describe('Request ID generation', () => {
    it('should generate unique request IDs for concurrent requests', (done) => {
      // Arrange
      const gameIds = ['game-1', 'game-2', 'game-3'];
      const requestIds: string[] = [];

      // Act - Make concurrent requests
      gameIds.forEach((gameId, index) => {
        service.getGameById(gameId).subscribe();

        const req = httpMock.match(`${environment.apiUrl}/api/games/${gameId}`)[0];
        req.flush({ id: gameId, name: `Game ${index}` });
      });

      // Assert - Collect all request IDs from log calls
      mockLogger.info.calls.all().forEach(call => {
        const context = call.args[1] as { requestId: string };
        requestIds.push(context.requestId);
      });

      // All request IDs should be unique
      const uniqueIds = new Set(requestIds);
      expect(uniqueIds.size).toBe(requestIds.length);
      expect(requestIds.length).toBe(gameIds.length);
      done();
    });
  });

  describe('Error message mapping', () => {
    const errorStatusCases = [
      { status: 400, expected: 'Requête invalide' },
      { status: 401, expected: 'Non autorisé - Veuillez vous reconnecter' },
      { status: 403, expected: 'Accès refusé' },
      { status: 404, expected: 'Ressource non trouvée' },
      { status: 500, expected: 'Erreur serveur interne' },
      { status: 502, expected: 'Service temporairement indisponible' },
      { status: 503, expected: 'Service temporairement indisponible' }
    ];

    errorStatusCases.forEach(({ status, expected }) => {
      it(`should map HTTP ${status} to user-friendly message: "${expected}"`, (done) => {
        // Act
        service.getGameById('test-game').subscribe({
          error: () => {
            // Assert
            expect(mockLogger.error).toHaveBeenCalledWith(
              jasmine.any(String),
              jasmine.objectContaining({
                errorMessage: expected,
                status
              })
            );
            done();
          }
        });

        const req = httpMock.expectOne(`${environment.apiUrl}/api/games/test-game`);
        req.flush('Error', { status, statusText: 'Error' });
      });
    });
  });
});
