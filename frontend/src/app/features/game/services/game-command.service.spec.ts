import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LiveAnnouncer } from '@angular/cdk/a11y';
import { GameCommandService } from './game-command.service';
import { LoggerService } from '../../../core/services/logger.service';
import { environment } from '../../../../environments/environment';
import {
  Game,
  CreateGameRequest,
  DraftState,
  InvitationCode,
  GameResponse
} from '../models/game.interface';

describe('GameCommandService', () => {
  let service: GameCommandService;
  let httpMock: HttpTestingController;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let announcerSpy: jasmine.SpyObj<LiveAnnouncer>;

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

  const invitationCode: InvitationCode = {
    code: 'INVITE',
    gameId: 'game-1',
    gameName: 'Test Game',
    creatorName: 'Tester',
    currentUses: 0
  };

  const successResponse: GameResponse = {
    success: true,
    message: 'ok'
  };

  const expectPost = (url: string, body: unknown, response: FlushResponse): void => {
    const req = httpMock.expectOne(url);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush(response);
  };

  const expectPatch = (url: string, body: unknown, response: FlushResponse): void => {
    const req = httpMock.expectOne(url);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual(body);
    req.flush(response);
  };

  const expectDelete = (url: string, response: FlushResponse): void => {
    const req = httpMock.expectOne(url);
    expect(req.request.method).toBe('DELETE');
    req.flush(response);
  };

  beforeEach(() => {
    loggerSpy = jasmine.createSpyObj('LoggerService', ['debug', 'info', 'warn', 'error']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    announcerSpy = jasmine.createSpyObj('LiveAnnouncer', ['announce']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        GameCommandService,
        { provide: LoggerService, useValue: loggerSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: LiveAnnouncer, useValue: announcerSpy }
      ]
    });

    service = TestBed.inject(GameCommandService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('sends basic command requests and maps responses', () => {
    const createRequest: CreateGameRequest = {
      name: 'New Game',
      maxParticipants: 8
    };

    service.createGame(createRequest).subscribe(game => {
      expect(game).toEqual(mockGame);
    });

    service.joinGameWithCode('INVITE').subscribe(game => {
      expect(game).toEqual(mockGame);
    });

    service.generateInvitationCode('game-1').subscribe(code => {
      expect(code).toEqual(invitationCode);
    });

    service.startDraft('game-1').subscribe(success => {
      expect(success).toBe(true);
    });

    service.leaveGame('game-1').subscribe(success => {
      expect(success).toBe(true);
    });

    service.initializeDraft('game-1').subscribe(state => {
      expect(state).toEqual(draftState);
    });

    service.makePlayerSelection('game-1', 'player-1').subscribe(success => {
      expect(success).toBe(true);
    });

    service.cancelDraft('game-1').subscribe(success => {
      expect(success).toBe(true);
    });

    expectPost(`${apiBaseUrl}/games`, createRequest, mockGame);
    expectPost(`${apiBaseUrl}/games/join-with-code`, { code: 'INVITE' }, mockGame);
    expectPost(`${apiBaseUrl}/games/game-1/invitation-code`, {}, invitationCode);
    expectPost(`${apiBaseUrl}/games/game-1/draft/start`, {}, successResponse);
    expectPost(`${apiBaseUrl}/games/game-1/leave`, {}, successResponse);
    expectPost(`${apiBaseUrl}/games/game-1/draft/initialize`, {}, draftState);
    expectPost(`${apiBaseUrl}/games/game-1/draft/select`, { gameId: 'game-1', playerId: 'player-1' }, successResponse);
    expectPost(`${apiBaseUrl}/games/game-1/draft/cancel`, { gameId: 'game-1', action: 'cancel' }, successResponse);
  });

  it('shows snackbar and announces on joinGame success', () => {
    service.joinGame('game-1').subscribe(success => {
      expect(success).toBe(true);
    });

    const req = httpMock.expectOne(`${apiBaseUrl}/games/game-1/join`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ gameId: 'game-1', userId: 'current-user-id' });
    req.flush(successResponse);

    expect(snackBarSpy.open).toHaveBeenCalled();
    expect(announcerSpy.announce).toHaveBeenCalled();
  });

  it('shows error snackbar on joinGame failure', (done) => {
    service.joinGame('game-1').subscribe({
      next: () => fail('expected joinGame to fail'),
      error: () => {
        expect(snackBarSpy.open).toHaveBeenCalled();
        done();
      }
    });

    const req = httpMock.expectOne(`${apiBaseUrl}/games/game-1/join`);
    req.flush('Error', { status: 500, statusText: 'Internal Server Error' });
  });

  it('announces regenerateInvitationCode and renameGame', () => {
    service.regenerateInvitationCode('game-1', '24h').subscribe();
    service.renameGame('game-1', 'Renamed').subscribe();

    expectPost(`${apiBaseUrl}/games/game-1/regenerate-code`, { duration: '24h' }, mockGame);
    expectPatch(`${apiBaseUrl}/games/game-1/rename`, { name: 'Renamed' }, mockGame);

    expect(announcerSpy.announce).toHaveBeenCalledWith(jasmine.stringMatching(/invitation/), 'polite');
    expect(announcerSpy.announce).toHaveBeenCalledWith(jasmine.stringMatching(/Renamed/), 'polite');
  });

  it('logs errors when regenerateInvitationCode fails', (done) => {
    service.regenerateInvitationCode('game-1', '24h').subscribe({
      next: () => fail('expected regenerateInvitationCode to fail'),
      error: () => {
        expect(loggerSpy.error).toHaveBeenCalled();
        done();
      }
    });

    const req = httpMock.expectOne(`${apiBaseUrl}/games/game-1/regenerate-code`);
    req.flush('Error', { status: 500, statusText: 'Internal Server Error' });
  });

  it('deletes game and announces on success', () => {
    service.deleteGame('game-1').subscribe(success => {
      expect(success).toBe(true);
    });

    expectDelete(`${apiBaseUrl}/games/game-1`, successResponse);
    expect(announcerSpy.announce).toHaveBeenCalled();
  });

  it('shows error snackbar on deleteGame failure', (done) => {
    service.deleteGame('game-1').subscribe({
      next: () => fail('expected deleteGame to fail'),
      error: () => {
        expect(loggerSpy.error).toHaveBeenCalled();
        expect(snackBarSpy.open).toHaveBeenCalled();
        done();
      }
    });

    const req = httpMock.expectOne(`${apiBaseUrl}/games/game-1`);
    req.flush('Error', { status: 500, statusText: 'Internal Server Error' });
  });

  it('finishes draft and announces on success', () => {
    service.finishDraft('game-1').subscribe(game => {
      expect(game).toEqual(mockGame);
    });

    expectPost(`${apiBaseUrl}/games/game-1/draft/finish`, {}, mockGame);
    expect(announcerSpy.announce).toHaveBeenCalled();
  });

  it('archives game and persists localStorage', () => {
    service.archiveGame('game-1').subscribe(success => {
      expect(success).toBe(true);
    });

    const archived = JSON.parse(localStorage.getItem('archived_games') || '[]') as string[];
    expect(archived).toEqual(['game-1']);

    expectPost(`${apiBaseUrl}/games/game-1/archive`, {}, successResponse);
  });

  it('emits error when archive fails', (done) => {
    service.archiveGame('game-1').subscribe({
      next: () => fail('expected archiveGame to fail'),
      error: (error) => {
        expect(error.message).toContain('archiv');
        done();
      }
    });

    const req = httpMock.expectOne(`${apiBaseUrl}/games/game-1/archive`);
    req.flush('Error', { status: 500, statusText: 'Internal Server Error' });
  });

  it('announces pause and resume on success', () => {
    service.pauseDraft('game-1').subscribe(success => {
      expect(success).toBe(true);
    });

    service.resumeDraft('game-1').subscribe(success => {
      expect(success).toBe(true);
    });

    expectPost(`${apiBaseUrl}/games/game-1/draft/pause`, { gameId: 'game-1', action: 'pause' }, successResponse);
    expectPost(`${apiBaseUrl}/games/game-1/draft/resume`, { gameId: 'game-1', action: 'resume' }, successResponse);

    expect(announcerSpy.announce).toHaveBeenCalledWith(jasmine.stringMatching(/pause/), 'polite');
    expect(announcerSpy.announce).toHaveBeenCalledWith(jasmine.stringMatching(/repris/), 'polite');
  });

  it('handles errors via the shared handler', (done) => {
    const createRequest: CreateGameRequest = {
      name: 'Error Game',
      maxParticipants: 8
    };

    service.createGame(createRequest).subscribe({
      next: () => fail('expected createGame to fail'),
      error: (error) => {
        expect(error.message).toContain('Non autor');
        expect(loggerSpy.error).toHaveBeenCalled();
        done();
      }
    });

    const req = httpMock.expectOne(`${apiBaseUrl}/games`);
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
  });
});
