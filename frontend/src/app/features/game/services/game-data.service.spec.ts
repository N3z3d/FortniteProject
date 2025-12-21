import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { GameDataService } from './game-data.service';
import { environment } from '../../../../environments/environment';
import { MOCK_GAMES, MOCK_GAME_PARTICIPANTS } from '../../../core/data/mock-game-data';

describe('GameDataService', () => {
  let service: GameDataService;
  let httpMock: HttpTestingController;
  const apiBaseUrl = `${environment.apiUrl}/api`;
  let originalFallbackEnabled: boolean;

  beforeEach(() => {
    originalFallbackEnabled = environment.enableFallbackData;
    environment.enableFallbackData = true;

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [GameDataService]
    });

    service = TestBed.inject(GameDataService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    environment.enableFallbackData = originalFallbackEnabled;
    httpMock.verify();
  });

  it('should map game details from the API response', () => {
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

  it('should fallback to mock game on server error when enabled', () => {
    const fallbackGame = MOCK_GAMES[0];

    service.getGameById(fallbackGame.id).subscribe(game => {
      expect(game.id).toBe(fallbackGame.id);
      expect(game.name).toBe(fallbackGame.name);
    });

    const req = httpMock.expectOne(`${apiBaseUrl}/games/${fallbackGame.id}`);
    req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
  });

  it('should surface not-found errors', () => {
    const missingId = 'missing-id';

    service.getGameById(missingId).subscribe({
      next: () => fail('expected getGameById to fail'),
      error: error => {
        expect(error.message).toContain('Ressource non trouv');
      }
    });

    const req = httpMock.expectOne(`${apiBaseUrl}/games/${missingId}`);
    req.flush('Not Found', { status: 404, statusText: 'Not Found' });
  });

  it('should fallback participants on server error when enabled', () => {
    const gameId = MOCK_GAMES[0].id;
    const fallbackParticipants = MOCK_GAME_PARTICIPANTS[gameId];

    service.getGameParticipants(gameId).subscribe(participants => {
      expect(participants.length).toBe(fallbackParticipants.length);
      expect(participants[0].username).toBe(fallbackParticipants[0].username);
    });

    const req = httpMock.expectOne(`${apiBaseUrl}/games/${gameId}/participants`);
    req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
  });
});
