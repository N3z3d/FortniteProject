import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { DraftService } from './draft.service';
import { LoggerService } from '../../../core/services/logger.service';
import { TranslationService } from '../../../core/services/translation.service';
import { environment } from '../../../../environments/environment';
import { DraftBoardState } from '../models/draft.interface';

describe('DraftService snake runtime contract', () => {
  let service: DraftService;
  let httpMock: HttpTestingController;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
  let translationSpy: jasmine.SpyObj<TranslationService>;

  const apiBaseUrl = `${environment.apiUrl}`;

  const detailResponse = {
    gameId: 'game-1',
    regions: ['EU', 'NAW'],
    tranchesEnabled: true,
    draftInfo: {
      draftId: 'draft-1',
      status: 'ACTIVE',
      currentRound: 1,
      currentPick: 1,
      totalRounds: 3,
    },
    participants: [
      {
        participantId: 'participant-1',
        username: 'marcel',
        totalPlayers: 1,
        selectedPlayers: [
          {
            playerId: 'player-1',
            nickname: 'Bugha',
            region: 'NAE',
            tranche: 'expert',
            currentScore: 150,
          },
        ],
      },
      {
        participantId: 'participant-2',
        username: 'teddy',
        totalPlayers: 0,
        selectedPlayers: [],
      },
    ],
  };

  const catalogueResponse = [
    {
      id: 'player-1',
      nickname: 'Bugha',
      region: 'NAE',
      tranche: 'expert',
      locked: false,
      currentSeason: 2025,
    },
    {
      id: 'player-2',
      nickname: 'Queasy',
      region: 'EU',
      tranche: 'expert',
      locked: false,
      currentSeason: 2025,
    },
  ];

  const turnEnvelope = {
    success: true,
    data: {
      draftId: 'draft-1',
      region: 'EU',
      participantId: 'user-2',
      round: 1,
      pickNumber: 2,
      reversed: false,
      expiresAt: '2026-04-03T10:01:00Z',
    },
  };

  const recommendEnvelope = {
    success: true,
    data: {
      id: 'player-2',
      nickname: 'Queasy',
      region: 'EU',
      tranche: 'expert',
      trancheFloor: 6,
    },
  };

  const participantUsersResponse = [
    { userId: 'user-1', username: 'marcel' },
    { userId: 'user-2', username: 'teddy' },
  ];

  beforeEach(() => {
    loggerSpy = jasmine.createSpyObj('LoggerService', ['debug', 'info', 'warn', 'error']);
    translationSpy = jasmine.createSpyObj('TranslationService', ['t']);
    translationSpy.t.and.callFake((key: string) => key);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        DraftService,
        { provide: LoggerService, useValue: loggerSpy },
        { provide: TranslationService, useValue: translationSpy },
      ],
    });

    service = TestBed.inject(DraftService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    service.stopAutoRefresh();
    sessionStorage.clear();
    httpMock.verify();
  });

  it('maps snake board state from the runtime endpoints', () => {
    let emittedState: DraftBoardState | undefined;

    service.getSnakeBoardState('game-1').subscribe(state => {
      emittedState = state;
    });

    httpMock.expectOne(`${apiBaseUrl}/api/games/game-1/details`).flush(detailResponse);
    httpMock
      .expectOne(`${apiBaseUrl}/api/games/game-1/draft/snake/turn?region=EU`)
      .flush(turnEnvelope);
    httpMock.expectOne(`${apiBaseUrl}/api/games/game-1/participants`).flush(participantUsersResponse);
    httpMock.expectOne(`${apiBaseUrl}/players/catalogue`).flush(catalogueResponse);
    httpMock
      .expectOne(`${apiBaseUrl}/api/games/game-1/draft/snake/recommend?region=EU`)
      .flush(recommendEnvelope);

    expect(emittedState).toBeDefined();
    expect(emittedState!.draft.id).toBe('draft-1');
    expect(emittedState!.draft.totalRounds).toBe(3);
    expect(emittedState!.currentParticipant?.username).toBe('teddy');
    expect(emittedState!.pickExpiresAt).toBe('2026-04-03T10:01:00Z');
    expect(emittedState!.recommendedPlayerId).toBe('player-2');
    expect(emittedState!.tranchesEnabled).toBeTrue();
    expect(emittedState!.participants.length).toBe(2);
    expect(emittedState!.availablePlayers[0].selected).toBeTrue();
    expect(emittedState!.availablePlayers[0].available).toBeFalse();
    expect(emittedState!.availablePlayers[1].available).toBeTrue();
    expect(emittedState!.availablePlayers[1].totalPoints).toBeUndefined();
    expect(emittedState!.progress?.totalRounds).toBe(3);
    expect(emittedState!.progress?.totalPicks).toBe(6);
    expect(emittedState!.progress?.completedPicks).toBe(1);
  });

  it('initializes snake cursors when the current turn is not ready yet', () => {
    let emittedState: DraftBoardState | undefined;

    service.getSnakeBoardState('game-1').subscribe(state => {
      emittedState = state;
    });

    httpMock.expectOne(`${apiBaseUrl}/api/games/game-1/details`).flush(detailResponse);
    httpMock
      .expectOne(`${apiBaseUrl}/api/games/game-1/draft/snake/turn?region=EU`)
      .flush(null, { status: 404, statusText: 'Not Found' });
    httpMock
      .expectOne(`${apiBaseUrl}/api/games/game-1/draft/snake/initialize`)
      .flush(turnEnvelope);
    httpMock.expectOne(`${apiBaseUrl}/api/games/game-1/participants`).flush(participantUsersResponse);
    httpMock.expectOne(`${apiBaseUrl}/players/catalogue`).flush(catalogueResponse);
    httpMock
      .expectOne(`${apiBaseUrl}/api/games/game-1/draft/snake/recommend?region=EU`)
      .flush(recommendEnvelope);

    expect(emittedState).toBeDefined();
    expect(emittedState!.currentParticipant?.id).toBe('participant-2');
    expect(emittedState!.currentPick).toBe(2);
  });

  it('matches the current participant even when usernames differ only by case', () => {
    let emittedState: DraftBoardState | undefined;

    service.getSnakeBoardState('game-1').subscribe(state => {
      emittedState = state;
    });

    httpMock.expectOne(`${apiBaseUrl}/api/games/game-1/details`).flush({
      ...detailResponse,
      participants: [
        detailResponse.participants[0],
        { ...detailResponse.participants[1], username: 'TEDDY' },
      ],
    });
    httpMock
      .expectOne(`${apiBaseUrl}/api/games/game-1/draft/snake/turn?region=EU`)
      .flush(turnEnvelope);
    httpMock.expectOne(`${apiBaseUrl}/api/games/game-1/participants`).flush(participantUsersResponse);
    httpMock.expectOne(`${apiBaseUrl}/players/catalogue`).flush(catalogueResponse);
    httpMock
      .expectOne(`${apiBaseUrl}/api/games/game-1/draft/snake/recommend?region=EU`)
      .flush(recommendEnvelope);

    expect(emittedState).toBeDefined();
    expect(emittedState!.currentParticipant?.id).toBe('participant-2');
    expect(emittedState!.currentParticipant?.username).toBe('TEDDY');
  });

  it('reuses the persisted snake region on reload before falling back to the first configured region', () => {
    sessionStorage.setItem('draft:snake:region:game-1', 'NAW');

    service.getSnakeBoardState('game-1').subscribe();

    httpMock.expectOne(`${apiBaseUrl}/api/games/game-1/details`).flush(detailResponse);
    httpMock
      .expectOne(`${apiBaseUrl}/api/games/game-1/draft/snake/turn?region=NAW`)
      .flush({ ...turnEnvelope, data: { ...turnEnvelope.data, region: 'NAW' } });
    httpMock.expectOne(`${apiBaseUrl}/api/games/game-1/participants`).flush(participantUsersResponse);
    httpMock.expectOne(`${apiBaseUrl}/players/catalogue`).flush(catalogueResponse);
    httpMock
      .expectOne(`${apiBaseUrl}/api/games/game-1/draft/snake/recommend?region=NAW`)
      .flush({ ...recommendEnvelope, data: { ...recommendEnvelope.data, region: 'NAW' } });
  });

  it('degrades gracefully when snake recommendation is unavailable', () => {
    let emittedState: DraftBoardState | undefined;

    service.getSnakeBoardState('game-1').subscribe(state => {
      emittedState = state;
    });

    httpMock.expectOne(`${apiBaseUrl}/api/games/game-1/details`).flush(detailResponse);
    httpMock
      .expectOne(`${apiBaseUrl}/api/games/game-1/draft/snake/turn?region=EU`)
      .flush({ ...turnEnvelope, data: { ...turnEnvelope.data, region: 'EU' } });
    httpMock.expectOne(`${apiBaseUrl}/api/games/game-1/participants`).flush(participantUsersResponse);
    httpMock.expectOne(`${apiBaseUrl}/players/catalogue`).flush(catalogueResponse);
    httpMock
      .expectOne(`${apiBaseUrl}/api/games/game-1/draft/snake/recommend?region=EU`)
      .flush(null, { status: 500, statusText: 'Server Error' });

    expect(emittedState).toBeDefined();
    expect(emittedState!.recommendedPlayerId).toBeNull();
  });

  it('propagates tranchesEnabled from the game detail payload', () => {
    let emittedState: DraftBoardState | undefined;

    service.getSnakeBoardState('game-1').subscribe(state => {
      emittedState = state;
    });

    httpMock
      .expectOne(`${apiBaseUrl}/api/games/game-1/details`)
      .flush({ ...detailResponse, tranchesEnabled: false });
    httpMock
      .expectOne(`${apiBaseUrl}/api/games/game-1/draft/snake/turn?region=EU`)
      .flush({ ...turnEnvelope, data: { ...turnEnvelope.data, region: 'EU' } });
    httpMock.expectOne(`${apiBaseUrl}/api/games/game-1/participants`).flush(participantUsersResponse);
    httpMock.expectOne(`${apiBaseUrl}/players/catalogue`).flush(catalogueResponse);
    httpMock
      .expectOne(`${apiBaseUrl}/api/games/game-1/draft/snake/recommend?region=EU`)
      .flush({ ...recommendEnvelope, data: { ...recommendEnvelope.data, region: 'EU' } });

    expect(emittedState).toBeDefined();
    expect(emittedState!.tranchesEnabled).toBeFalse();
  });

  it('posts a snake pick then reloads the board state', () => {
    let emittedState: DraftBoardState | undefined;

    service.submitSnakePick('game-1', 'player-2').subscribe(state => {
      emittedState = state;
    });

    const pickRequest = httpMock.expectOne(`${apiBaseUrl}/api/games/game-1/draft/snake/pick`);
    expect(pickRequest.request.method).toBe('POST');
    expect(pickRequest.request.body).toEqual({
      playerId: 'player-2',
      region: 'GLOBAL',
    });
    pickRequest.flush(turnEnvelope);

    httpMock.expectOne(`${apiBaseUrl}/api/games/game-1/details`).flush(detailResponse);
    httpMock
      .expectOne(`${apiBaseUrl}/api/games/game-1/draft/snake/turn?region=EU`)
      .flush(turnEnvelope);
    httpMock.expectOne(`${apiBaseUrl}/api/games/game-1/participants`).flush(participantUsersResponse);
    httpMock.expectOne(`${apiBaseUrl}/players/catalogue`).flush(catalogueResponse);
    httpMock
      .expectOne(`${apiBaseUrl}/api/games/game-1/draft/snake/recommend?region=EU`)
      .flush(recommendEnvelope);

    expect(emittedState).toBeDefined();
    expect(emittedState!.currentParticipant?.username).toBe('teddy');
  });
});
