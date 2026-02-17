import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { LiveAnnouncer } from '@angular/cdk/a11y';

import { GameCommandService } from './game-command.service';
import { LoggerService } from '../../../core/services/logger.service';
import { TranslationService } from '../../../core/services/translation.service';
import { UiErrorFeedbackService } from '../../../core/services/ui-error-feedback.service';
import { environment } from '../../../../environments/environment';
import {
  CreateGameRequest,
  DraftState,
  Game,
  GameResponse,
  InvitationCode
} from '../models/game.interface';

describe('GameCommandService', () => {
  let service: GameCommandService;
  let httpMock: HttpTestingController;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
  let uiFeedbackSpy: jasmine.SpyObj<UiErrorFeedbackService>;
  let announcerSpy: jasmine.SpyObj<LiveAnnouncer>;
  let translationSpy: jasmine.SpyObj<TranslationService>;

  const apiBaseUrl = `${environment.apiUrl}/api`;

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

  beforeEach(() => {
    loggerSpy = jasmine.createSpyObj('LoggerService', ['debug', 'info', 'warn', 'error']);
    uiFeedbackSpy = jasmine.createSpyObj('UiErrorFeedbackService', ['showSuccessMessage', 'showError']);
    announcerSpy = jasmine.createSpyObj('LiveAnnouncer', ['announce']);
    translationSpy = jasmine.createSpyObj('TranslationService', ['t']);

    translationSpy.t.and.callFake((key: string) => {
      const values: Record<string, string> = {
        'common.close': 'Close',
        'common.error': 'Error',
        'errors.generic': 'Generic error',
        'errors.validation': 'Validation error',
        'errors.unauthorized': 'Unauthorized',
        'errors.notFound': 'Not found',
        'errors.handler.forbiddenMessage': 'Forbidden',
        'errors.handler.serverErrorMessage': 'Server error',
        'games.detail.actions.joinSuccess': 'Join success',
        'games.detail.actions.joinError': 'Join error',
        'games.detail.actions.invitationCodeRegeneratedAnnounce': 'Invitation code regenerated',
        'games.detail.actions.renameAnnounce': 'Game renamed to {name}',
        'games.detail.actions.deleteSuccessAnnounce': 'Game deleted',
        'games.detail.actions.deleteError': 'Delete error',
        'games.detail.actions.draftFinishedAnnounce': 'Draft finished',
        'games.detail.actions.draftFinishError': 'Draft finish error',
        'games.detail.actions.archiveError': 'Archive failed',
        'games.detail.actions.draftPausedAnnounce': 'Draft paused',
        'games.detail.actions.draftResumedAnnounce': 'Draft resumed'
      };
      return values[key] || key;
    });

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        GameCommandService,
        { provide: LoggerService, useValue: loggerSpy },
        { provide: UiErrorFeedbackService, useValue: uiFeedbackSpy },
        { provide: LiveAnnouncer, useValue: announcerSpy },
        { provide: TranslationService, useValue: translationSpy }
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

  it('sends command requests and maps responses', () => {
    const createRequest: CreateGameRequest = {
      name: 'New Game',
      maxParticipants: 8
    };

    service.createGame(createRequest).subscribe(game => expect(game).toEqual(mockGame));
    service.joinGameWithCode(' invite ').subscribe(game => expect(game).toEqual(mockGame));
    service.generateInvitationCode('game-1').subscribe(code => expect(code).toEqual(invitationCode));
    service.startDraft('game-1').subscribe(success => expect(success).toBeTrue());
    service.leaveGame('game-1').subscribe(success => expect(success).toBeTrue());
    service.initializeDraft('game-1').subscribe(state => expect(state).toEqual(draftState));
    service.makePlayerSelection('game-1', 'player-1').subscribe(success => expect(success).toBeTrue());
    service.cancelDraft('game-1').subscribe(success => expect(success).toBeTrue());

    const createReq = httpMock.expectOne(`${apiBaseUrl}/games`);
    createReq.flush(mockGame);

    const joinWithCodeReq = httpMock.expectOne(`${apiBaseUrl}/games/join-with-code`);
    expect(joinWithCodeReq.request.body).toEqual({ code: 'INVITE' });
    joinWithCodeReq.flush(mockGame);

    httpMock.expectOne(`${apiBaseUrl}/games/game-1/invitation-code`).flush(invitationCode);
    httpMock.expectOne(`${apiBaseUrl}/games/game-1/start-draft`).flush(successResponse);
    httpMock.expectOne(`${apiBaseUrl}/games/game-1/leave`).flush(successResponse);
    httpMock.expectOne(`${apiBaseUrl}/games/game-1/draft/initialize`).flush(draftState);
    httpMock.expectOne(`${apiBaseUrl}/games/game-1/draft/select`).flush(successResponse);
    httpMock.expectOne(`${apiBaseUrl}/games/game-1/draft/cancel`).flush(successResponse);
  });

  it('shows translated snackbar and announcer message on join success', () => {
    service.joinGame('game-1').subscribe(success => expect(success).toBeTrue());

    const req = httpMock.expectOne(`${apiBaseUrl}/games/join`);
    req.flush(successResponse);

    expect(uiFeedbackSpy.showSuccessMessage).toHaveBeenCalledWith('Join success', 3000);
    expect(announcerSpy.announce).toHaveBeenCalledWith('Join success', 'polite');
  });

  it('shows backend message on join conflict', done => {
    service.joinGame('game-1').subscribe({
      next: () => fail('expected error'),
      error: () => {
        expect(uiFeedbackSpy.showError).toHaveBeenCalledWith(
          jasmine.any(Object),
          'games.detail.actions.joinError',
          { duration: 5000 }
        );
        done();
      }
    });

    const req = httpMock.expectOne(`${apiBaseUrl}/games/join`);
    req.flush(
      { message: 'User is already participating in this game', code: 'USER_ALREADY_IN_GAME' },
      { status: 409, statusText: 'Conflict' }
    );
  });

  it('announces regenerate and rename actions with translated messages', () => {
    service.regenerateInvitationCode('game-1', '24h').subscribe();
    service.renameGame('game-1', 'Renamed').subscribe();

    httpMock.expectOne(`${apiBaseUrl}/games/game-1/regenerate-code?duration=24h`).flush(mockGame);
    httpMock.expectOne(`${apiBaseUrl}/games/game-1/rename`).flush(mockGame);

    expect(announcerSpy.announce).toHaveBeenCalledWith('Invitation code regenerated', 'polite');
    expect(announcerSpy.announce).toHaveBeenCalledWith('Game renamed to Renamed', 'polite');
  });

  it('deletes game and announces success', () => {
    service.deleteGame('game-1').subscribe(success => expect(success).toBeTrue());

    httpMock.expectOne(`${apiBaseUrl}/games/game-1`).flush(successResponse);
    expect(announcerSpy.announce).toHaveBeenCalledWith('Game deleted', 'polite');
  });

  it('shows translated delete error snackbar on failure', done => {
    service.deleteGame('game-1').subscribe({
      next: () => fail('expected error'),
      error: () => {
        expect(loggerSpy.error).toHaveBeenCalled();
        expect(uiFeedbackSpy.showError).toHaveBeenCalledWith(
          jasmine.any(Object),
          'games.detail.actions.deleteError',
          { duration: 5000 }
        );
        done();
      }
    });

    httpMock.expectOne(`${apiBaseUrl}/games/game-1`).flush('Error', {
      status: 500,
      statusText: 'Internal Server Error'
    });
  });

  it('finishes draft and announces success', () => {
    service.finishDraft('game-1').subscribe(game => expect(game).toEqual(mockGame));

    httpMock.expectOne(`${apiBaseUrl}/games/game-1/draft/finish`).flush(mockGame);
    expect(announcerSpy.announce).toHaveBeenCalledWith('Draft finished', 'polite');
  });

  it('archives game and stores archived id', () => {
    service.archiveGame('game-1').subscribe(success => expect(success).toBeTrue());

    const archived = JSON.parse(localStorage.getItem('archived_games') || '[]') as string[];
    expect(archived).toEqual(['game-1']);

    httpMock.expectOne(`${apiBaseUrl}/games/game-1/archive`).flush(successResponse);
  });

  it('returns translated archive error', done => {
    service.archiveGame('game-1').subscribe({
      next: () => fail('expected error'),
      error: error => {
        expect(error.message).toBe('Archive failed');
        done();
      }
    });

    httpMock.expectOne(`${apiBaseUrl}/games/game-1/archive`).flush('Error', {
      status: 500,
      statusText: 'Internal Server Error'
    });
  });

  it('announces pause and resume actions', () => {
    service.pauseDraft('game-1').subscribe(success => expect(success).toBeTrue());
    service.resumeDraft('game-1').subscribe(success => expect(success).toBeTrue());

    httpMock.expectOne(`${apiBaseUrl}/games/game-1/draft/pause`).flush(successResponse);
    httpMock.expectOne(`${apiBaseUrl}/games/game-1/draft/resume`).flush(successResponse);

    expect(announcerSpy.announce).toHaveBeenCalledWith('Draft paused', 'polite');
    expect(announcerSpy.announce).toHaveBeenCalledWith('Draft resumed', 'polite');
  });

  it('maps 401 with translated message in shared error handler', done => {
    const createRequest: CreateGameRequest = { name: 'Error Game', maxParticipants: 8 };

    service.createGame(createRequest).subscribe({
      next: () => fail('expected error'),
      error: error => {
        expect(error.message).toBe('Unauthorized');
        expect(loggerSpy.error).toHaveBeenCalled();
        done();
      }
    });

    httpMock.expectOne(`${apiBaseUrl}/games`).flush('Unauthorized', {
      status: 401,
      statusText: 'Unauthorized'
    });
  });

  it('uses generic translated message for HTTP 500 error payloads', done => {
    const createRequest: CreateGameRequest = { name: 'Error Game', maxParticipants: 8 };

    service.createGame(createRequest).subscribe({
      next: () => fail('expected error'),
      error: error => {
        expect(error.message).toBe('Server error');
        done();
      }
    });

    httpMock.expectOne(`${apiBaseUrl}/games`).flush(
      { message: 'User cannot have more than 5 active games. Current: 5' },
      { status: 500, statusText: 'Internal Server Error' }
    );
  });

  it('keeps user-safe backend message for HTTP 400 validation error', done => {
    const createRequest: CreateGameRequest = { name: 'Error Game', maxParticipants: 8 };

    service.createGame(createRequest).subscribe({
      next: () => fail('expected error'),
      error: error => {
        expect(error.message).toBe('Game name already exists');
        done();
      }
    });

    httpMock.expectOne(`${apiBaseUrl}/games`).flush(
      { message: 'Game name already exists' },
      { status: 400, statusText: 'Bad Request' }
    );
  });
});
