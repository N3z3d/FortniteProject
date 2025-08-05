import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { GameService } from './game.service';
import { Game, CreateGameRequest, JoinGameRequest } from '../models/game.interface';
import { environment } from '../../../../environments/environment';

describe('GameService', () => {
  let service: GameService;
  let httpMock: HttpTestingController;
  const apiUrl = environment.apiUrl;

  const mockGame: Game = {
    id: '1',
    name: 'Test Game',
    creatorName: 'Test User',
    maxParticipants: 10,
    status: 'CREATING',
    createdAt: '2024-01-15T10:30:00',
    participantCount: 1,
    canJoin: true
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [GameService]
    });
    service = TestBed.inject(GameService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getUserGames', () => {
    it('should return user games successfully', () => {
      const expectedGames: Game[] = [mockGame];

      service.getUserGames().subscribe(games => {
        expect(games).toEqual(expectedGames);
      });

      const req = httpMock.expectOne(`${apiUrl}/games`);
      expect(req.request.method).toBe('GET');
      req.flush(expectedGames);
    });

    it('should handle network errors', () => {
      service.getUserGames().subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(500);
        }
      });

      const req = httpMock.expectOne(`${apiUrl}/games`);
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    });

    it('should return empty array when no games (not an error)', () => {
      service.getUserGames().subscribe(games => {
        expect(games).toEqual([]);
      });

      const req = httpMock.expectOne(`${apiUrl}/games`);
      req.flush([]);
    });

    it('should handle empty response gracefully', () => {
      service.getUserGames().subscribe({
        next: (games) => {
          expect(games).toEqual([]);
        },
        error: (error) => {
          fail('should not have failed with empty array');
        }
      });

      const req = httpMock.expectOne(`${apiUrl}/games`);
      req.flush([]);
    });
  });

  describe('getUserGames - Empty Array Handling', () => {
    it('should handle empty array response as success, not error', () => {
      // Arrange
      const emptyGames: Game[] = [];
      
      // Act
      service.getUserGames().subscribe({
        next: (games) => {
          // Assert
          expect(games).toEqual([]);
        },
        error: (error) => {
          fail('should not have failed with empty array');
        }
      });
      
      const req = httpMock.expectOne(`${apiUrl}/games`);
      req.flush(emptyGames);
    });

    it('should return empty array when backend returns 200 with []', () => {
      // Arrange
      const emptyResponse: Game[] = [];
      
      // Act
      service.getUserGames().subscribe(games => {
        // Assert
        expect(games).toEqual([]);
        expect(games.length).toBe(0);
      });
      
      const req = httpMock.expectOne(`${apiUrl}/games`);
      req.flush(emptyResponse, { status: 200, statusText: 'OK' });
    });

    it('should handle 401 error as authentication issue', () => {
      // Act
      service.getUserGames().subscribe({
        next: (games) => {
          fail('should not succeed with 401');
        },
        error: (error) => {
          // Assert
          expect(error.message).toContain('Non autorisé');
        }
      });
      
      const req = httpMock.expectOne(`${apiUrl}/games`);
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });

    it('should handle 500 error as server issue', () => {
      // Act
      service.getUserGames().subscribe({
        next: (games) => {
          fail('should not succeed with 500');
        },
        error: (error) => {
          // Assert
          expect(error.message).toContain('Erreur serveur');
        }
      });
      
      const req = httpMock.expectOne(`${apiUrl}/games`);
      req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
    });
  });

  describe('getAvailableGames', () => {
    it('should return available games successfully', () => {
      const expectedGames: Game[] = [mockGame];

      service.getAvailableGames().subscribe(games => {
        expect(games).toEqual(expectedGames);
      });

      const req = httpMock.expectOne(`${apiUrl}/games/available`);
      expect(req.request.method).toBe('GET');
      req.flush(expectedGames);
    });

    it('should return empty array when no available games', () => {
      service.getAvailableGames().subscribe(games => {
        expect(games).toEqual([]);
      });

      const req = httpMock.expectOne(`${apiUrl}/games/available`);
      req.flush([]);
    });
  });

  describe('getAllGames', () => {
    it('should return all games successfully', () => {
      const expectedGames: Game[] = [mockGame];

      service.getAllGames().subscribe(games => {
        expect(games).toEqual(expectedGames);
      });

      const req = httpMock.expectOne(`${apiUrl}/games`);
      expect(req.request.method).toBe('GET');
      req.flush(expectedGames);
    });

    it('should return empty array when no games exist', () => {
      service.getAllGames().subscribe(games => {
        expect(games).toEqual([]);
      });

      const req = httpMock.expectOne(`${apiUrl}/games`);
      req.flush([]);
    });
  });

  describe('joinGame', () => {
    it('should join game successfully', () => {
      const gameId = 'game-1';
      const joinRequest: JoinGameRequest = { gameId };

      service.joinGame(gameId).subscribe(success => {
        expect(success).toBe(true);
      });

      const req = httpMock.expectOne(`${apiUrl}/games/join`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(joinRequest);
      req.flush({ success: true, message: 'Joined successfully' });
    });

    it('should handle join game errors', () => {
      const gameId = 'invalid-game';

      service.joinGame(gameId).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(404);
        }
      });

      const req = httpMock.expectOne(`${apiUrl}/games/join`);
      req.flush('Game not found', { status: 404, statusText: 'Not Found' });
    });
  });

  describe('getGameById', () => {
    it('should return game details successfully', () => {
      const gameId = 'game-1';

      service.getGameById(gameId).subscribe((game: Game) => {
        expect(game).toEqual(mockGame);
      });

      const req = httpMock.expectOne(`${apiUrl}/games/${gameId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockGame);
    });
  });

  describe('createGame', () => {
    it('should create game successfully', () => {
      const createRequest: CreateGameRequest = {
        name: 'New Game',
        maxParticipants: 10,
        regionRules: {
          'EU': 2,
          'NAW': 1,
          'BR': 1
        }
      };

      service.createGame(createRequest).subscribe(game => {
        expect(game).toBeDefined();
      });

      const req = httpMock.expectOne(`${apiUrl}/games`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(createRequest);
      req.flush(mockGame);
    });
  });

  describe('Error Handling', () => {
    it('should handle 401 unauthorized errors', () => {
      service.getUserGames().subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.message).toBe('Non autorisé');
        }
      });

      const req = httpMock.expectOne(`${apiUrl}/games`);
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });

    it('should handle 403 forbidden errors', () => {
      service.getUserGames().subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.message).toBe('Accès refusé');
        }
      });

      const req = httpMock.expectOne(`${apiUrl}/games`);
      req.flush('Forbidden', { status: 403, statusText: 'Forbidden' });
    });

    it('should handle 404 not found errors', () => {
      service.getUserGames().subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.message).toBe('Ressource non trouvée');
        }
      });

      const req = httpMock.expectOne(`${apiUrl}/games`);
      req.flush('Not Found', { status: 404, statusText: 'Not Found' });
    });

    it('should handle 500 server errors', () => {
      service.getUserGames().subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.message).toBe('Erreur serveur');
        }
      });

      const req = httpMock.expectOne(`${apiUrl}/games`);
      req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
    });
  });
}); 