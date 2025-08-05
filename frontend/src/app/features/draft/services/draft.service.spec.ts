import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { DraftService } from './draft.service';
import { environment } from '../../../../environments/environment';
import {
  Draft,
  DraftStatus,
  DraftPick,
  Player,
  GameParticipant,
  DraftStatusInfo,
  DraftStatistics,
  PlayerSelectionRequest,
  DraftInitializeRequest,
  DraftActionResponse,
  DraftHistoryEntry,
  DraftProgress,
  DraftRules,
  DraftParticipantInfo,
  DraftBoardState
} from '../models/draft.interface';

describe('DraftService', () => {
  let service: DraftService;
  let httpMock: HttpTestingController;
  let apiUrl: string;

  const mockDraft: Draft = {
    id: 'draft-123',
    gameId: 'game-123',
    status: 'ACTIVE',
    currentRound: 1,
    currentPick: 1,
    totalRounds: 3,
    createdAt: '2025-01-15T10:30:00',
    updatedAt: '2025-01-15T10:35:00',
    startedAt: '2025-01-15T10:30:00',
    finishedAt: null
  };

  const mockPlayer: Player = {
    id: 'player-1',
    nickname: 'pixie',
    username: 'pixie',
    region: 'EU',
    tranche: '1',
    currentSeason: 2025
  };

  const mockParticipant: GameParticipant = {
    id: 'participant-1',
    username: 'user1',
    joinedAt: '2025-01-15T10:30:00',
    isCreator: true,
    draftOrder: 1
  };

  const mockDraftPick: DraftPick = {
    id: 'pick-1',
    draftId: 'draft-123',
    participantId: 'participant-1',
    playerId: 'player-1',
    round: 1,
    pickNumber: 1,
    selectionTime: '2025-01-15T10:35:00',
    timeTakenSeconds: 30,
    autoPick: false
  };

  const mockDraftBoardState: DraftBoardState = {
    draft: mockDraft,
    participants: [
      {
        participant: mockParticipant,
        selections: [],
        isCurrentTurn: true,
        timeRemaining: 300,
        hasTimedOut: false
      }
    ],
    availablePlayers: [mockPlayer],
    selectedPlayers: [],
    currentParticipant: mockParticipant,
    progress: {
      currentRound: 1,
      currentPick: 1,
      totalRounds: 3,
      totalPicks: 6,
      completedPicks: 0,
      progressPercentage: 0,
      estimatedTimeRemaining: null
    },
    rules: {
      maxPlayersPerTeam: 3,
      regionQuotas: {
        EU: 2,
        NAC: 2,
        BR: 1,
        ASIA: 1,
        OCE: 1,
        ME: 1
      },
      timeLimitPerPick: 300,
      autoPickEnabled: true,
      autoPickDelay: 43200
    }
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DraftService]
    });

    service = TestBed.inject(DraftService);
    httpMock = TestBed.inject(HttpTestingController);
    apiUrl = environment.apiUrl;
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('initializeDraft', () => {
    it('should initialize a draft for a game', () => {
      const gameId = 'game-123';
      const request: DraftInitializeRequest = { gameId };

      service.initializeDraft(gameId).subscribe(draft => {
        expect(draft).toEqual(mockDraft);
        expect(draft.status).toBe('ACTIVE');
      });

      const req = httpMock.expectOne(`${apiUrl}/drafts/initialize`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockDraft);
    });

    it('should handle error when initialization fails', () => {
      const gameId = 'game-123';
      const errorMessage = 'Game not found';

      service.initializeDraft(gameId).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(404);
          expect(error.error.message).toBe(errorMessage);
        }
      });

      const req = httpMock.expectOne(`${apiUrl}/drafts/initialize`);
      req.flush({ message: errorMessage }, { status: 404, statusText: 'Not Found' });
    });
  });

  describe('getDraftStatus', () => {
    it('should get draft status for a game', () => {
      const gameId = 'game-123';
      const mockStatus: DraftStatusInfo = {
        status: 'ACTIVE',
        currentRound: 1,
        currentPick: 1,
        totalRounds: 3,
        totalParticipants: 2
      };

      service.getDraftStatus(gameId).subscribe(status => {
        expect(status).toEqual(mockStatus);
        expect(status.status).toBe('ACTIVE');
      });

      const req = httpMock.expectOne(`${apiUrl}/drafts/${gameId}/status`);
      expect(req.request.method).toBe('GET');
      req.flush(mockStatus);
    });
  });

  describe('getParticipantSelectionOrder', () => {
    it('should get participant selection order for a game', () => {
      const gameId = 'game-123';
      const mockParticipants = [mockParticipant];

      service.getParticipantSelectionOrder(gameId).subscribe(participants => {
        expect(participants).toEqual(mockParticipants);
        expect(participants[0].draftOrder).toBe(1);
      });

      const req = httpMock.expectOne(`${apiUrl}/drafts/${gameId}/participants/order`);
      expect(req.request.method).toBe('GET');
      req.flush(mockParticipants);
    });
  });

  describe('getCurrentParticipant', () => {
    it('should get current participant', () => {
      const gameId = 'game-123';

      service.getCurrentParticipant(gameId).subscribe(participant => {
        expect(participant).toEqual(mockParticipant);
      });

      const req = httpMock.expectOne(`${apiUrl}/drafts/${gameId}/current-participant`);
      expect(req.request.method).toBe('GET');
      req.flush(mockParticipant);
    });
  });

  describe('getAvailablePlayers', () => {
    it('should get available players for a game', () => {
      const gameId = 'game-123';
      const mockPlayers = [mockPlayer];

      service.getAvailablePlayers(gameId).subscribe(players => {
        expect(players).toEqual(mockPlayers);
        expect(players[0].nickname).toBe('pixie');
      });

      const req = httpMock.expectOne(`${apiUrl}/drafts/${gameId}/available-players`);
      expect(req.request.method).toBe('GET');
      req.flush(mockPlayers);
    });

    it('should get available players filtered by region', () => {
      const gameId = 'game-123';
      const region = 'EU';
      const mockPlayers = [mockPlayer];

      service.getAvailablePlayers(gameId, region).subscribe(players => {
        expect(players).toEqual(mockPlayers);
        expect(players[0].region).toBe('EU');
      });

      const req = httpMock.expectOne(`${apiUrl}/drafts/${gameId}/available-players?region=${region}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockPlayers);
    });
  });

  describe('makePlayerSelection', () => {
    it('should make a player selection', () => {
      const gameId = 'game-123';
      const playerId = 'player-1';
      const request: PlayerSelectionRequest = { playerId };

      service.makePlayerSelection(gameId, playerId).subscribe(pick => {
        expect(pick).toEqual(mockDraftPick);
        expect(pick.playerId).toBe(playerId);
        expect(pick.autoPick).toBe(false);
      });

      const req = httpMock.expectOne(`${apiUrl}/drafts/${gameId}/select`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockDraftPick);
    });

    it('should handle error when player is already selected', () => {
      const gameId = 'game-123';
      const playerId = 'player-1';

      service.makePlayerSelection(gameId, playerId).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(400);
          expect(error.error.message).toBe('Player already selected');
        }
      });

      const req = httpMock.expectOne(`${apiUrl}/drafts/${gameId}/select`);
      req.flush({ message: 'Player already selected' }, { status: 400, statusText: 'Bad Request' });
    });
  });

  describe('getParticipantSelections', () => {
    it('should get participant selections', () => {
      const gameId = 'game-123';
      const participantId = 'participant-1';
      const mockPicks = [mockDraftPick];

      service.getParticipantSelections(gameId, participantId).subscribe(picks => {
        expect(picks).toEqual(mockPicks);
      });

      const req = httpMock.expectOne(`${apiUrl}/drafts/${gameId}/participants/${participantId}/selections`);
      expect(req.request.method).toBe('GET');
      req.flush(mockPicks);
    });
  });

  describe('getAllDraftPicks', () => {
    it('should get all draft picks for a game', () => {
      const gameId = 'game-123';
      const mockPicks = [mockDraftPick];

      service.getAllDraftPicks(gameId).subscribe(picks => {
        expect(picks).toEqual(mockPicks);
      });

      const req = httpMock.expectOne(`${apiUrl}/drafts/${gameId}/picks`);
      expect(req.request.method).toBe('GET');
      req.flush(mockPicks);
    });
  });

  describe('handleTimeouts', () => {
    it('should handle timeouts for a game', () => {
      const gameId = 'game-123';
      const mockAutoPicks = [mockDraftPick];

      service.handleTimeouts(gameId).subscribe(picks => {
        expect(picks).toEqual(mockAutoPicks);
      });

      const req = httpMock.expectOne(`${apiUrl}/drafts/${gameId}/handle-timeouts`);
      expect(req.request.method).toBe('POST');
      req.flush(mockAutoPicks);
    });
  });

  describe('pauseDraft', () => {
    it('should pause a draft', () => {
      const gameId = 'game-123';

      service.pauseDraft(gameId).subscribe(success => {
        expect(success).toBe(true);
      });

      const req = httpMock.expectOne(`${apiUrl}/drafts/${gameId}/pause`);
      expect(req.request.method).toBe('POST');
      req.flush(true);
    });
  });

  describe('resumeDraft', () => {
    it('should resume a draft', () => {
      const gameId = 'game-123';

      service.resumeDraft(gameId).subscribe(success => {
        expect(success).toBe(true);
      });

      const req = httpMock.expectOne(`${apiUrl}/drafts/${gameId}/resume`);
      expect(req.request.method).toBe('POST');
      req.flush(true);
    });
  });

  describe('cancelDraft', () => {
    it('should cancel a draft', () => {
      const gameId = 'game-123';

      service.cancelDraft(gameId).subscribe(success => {
        expect(success).toBe(true);
      });

      const req = httpMock.expectOne(`${apiUrl}/drafts/${gameId}/cancel`);
      expect(req.request.method).toBe('POST');
      req.flush(true);
    });
  });

  describe('getDraftHistory', () => {
    it('should get draft history', () => {
      const gameId = 'game-123';
      const mockHistory: DraftHistoryEntry[] = [
        {
          pick: mockDraftPick,
          player: mockPlayer,
          participant: mockParticipant,
          round: 1,
          pickNumber: 1,
          selectionTime: '2025-01-15T10:35:00',
          timeTakenSeconds: 30,
          autoPick: false
        }
      ];

      service.getDraftHistory(gameId).subscribe(history => {
        expect(history).toEqual(mockHistory);
      });

      const req = httpMock.expectOne(`${apiUrl}/drafts/${gameId}/history`);
      expect(req.request.method).toBe('GET');
      req.flush(mockHistory);
    });
  });

  describe('getDraftStatistics', () => {
    it('should get draft statistics', () => {
      const gameId = 'game-123';
      const mockStats: DraftStatistics = {
        totalPicks: 4,
        averageSelectionTime: 45.5,
        fastestPick: 10,
        slowestPick: 120,
        autoPicks: 1
      };

      service.getDraftStatistics(gameId).subscribe(statistics => {
        expect(statistics.totalPicks).toBe(4);
        expect(statistics.autoPicks).toBe(1);
      });

      const req = httpMock.expectOne(`${apiUrl}/drafts/${gameId}/statistics`);
      expect(req.request.method).toBe('GET');
      req.flush(mockStats);
    });
  });

  describe('getDraftBoardState', () => {
    it('should get complete draft board state', () => {
      const gameId = 'game-123';

      service.getDraftBoardState(gameId).subscribe(state => {
        expect(state).toEqual(mockDraftBoardState);
        expect(state.draft.status).toBe('ACTIVE');
        expect(state.participants.length).toBe(1);
        expect(state.availablePlayers.length).toBe(1);
      });

      const req = httpMock.expectOne(`${apiUrl}/drafts/${gameId}/board-state`);
      expect(req.request.method).toBe('GET');
      req.flush(mockDraftBoardState);
    });
  });

  describe('State Management', () => {
    it('should emit draft state changes', () => {
      const gameId = 'game-123';
      let emittedState: DraftBoardState | null = null;

      service.draftState$.subscribe(state => {
        emittedState = state;
      });

      service.getDraftBoardState(gameId).subscribe();

      const req = httpMock.expectOne(`${apiUrl}/drafts/${gameId}/board-state`);
      req.flush(mockDraftBoardState);

      expect(emittedState).toEqual(mockDraftBoardState);
    });

    it('should emit current game ID changes', () => {
      const gameId = 'game-123';
      let emittedGameId: string | null = null;

      service.currentGameId$.subscribe(id => {
        emittedGameId = id;
      });

      service.initializeDraft(gameId).subscribe();

      const req = httpMock.expectOne(`${apiUrl}/drafts/initialize`);
      req.flush(mockDraft);

      expect(emittedGameId).toBe(gameId);
    });
  });

  describe('Auto Refresh', () => {
    it('should start auto refresh when initializing draft', () => {
      const gameId = 'game-123';

      service.initializeDraft(gameId).subscribe();

      const req = httpMock.expectOne(`${apiUrl}/drafts/initialize`);
      req.flush(mockDraft);

      // Vérifier que l'auto-refresh est démarré
      expect(service['refreshTimer']).toBeDefined();
    });

    it('should refresh draft state automatically', () => {
      const gameId = 'game-123';

      service.refreshDraftState(gameId);

      const req = httpMock.expectOne(`${apiUrl}/drafts/${gameId}/board-state`);
      expect(req.request.method).toBe('GET');
      req.flush(mockDraftBoardState);
    });
  });

  describe('Error Handling', () => {
    it('should handle HTTP errors gracefully', () => {
      const gameId = 'game-123';

      service.getDraftBoardState(gameId).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error).toBeTruthy();
        }
      });

      const req = httpMock.expectOne(`${apiUrl}/drafts/${gameId}/board-state`);
      req.error(new ErrorEvent('Network error'));
    });

    it('should handle server errors', () => {
      const gameId = 'game-123';

      service.getDraftBoardState(gameId).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(500);
        }
      });

      const req = httpMock.expectOne(`${apiUrl}/drafts/${gameId}/board-state`);
      req.flush({ error: 'Internal server error' }, { status: 500, statusText: 'Internal Server Error' });
    });
  });
}); 