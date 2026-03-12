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
      region: 'GLOBAL',
      participantId: 'user-2',
      round: 1,
      pickNumber: 2,
      reversed: false,
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
    httpMock.verify();
  });

  it('maps snake board state from the runtime endpoints', () => {
    let emittedState: DraftBoardState | undefined;

    service.getSnakeBoardState('game-1').subscribe(state => {
      emittedState = state;
    });

    httpMock.expectOne(`${apiBaseUrl}/api/games/game-1/details`).flush(detailResponse);
    httpMock.expectOne(`${apiBaseUrl}/api/games/game-1/participants`).flush(participantUsersResponse);
    httpMock.expectOne(`${apiBaseUrl}/players/catalogue`).flush(catalogueResponse);
    httpMock
      .expectOne(`${apiBaseUrl}/api/games/game-1/draft/snake/turn?region=GLOBAL`)
      .flush(turnEnvelope);

    expect(emittedState).toBeDefined();
    expect(emittedState!.draft.id).toBe('draft-1');
    expect(emittedState!.draft.totalRounds).toBe(3);
    expect(emittedState!.currentParticipant?.username).toBe('teddy');
    expect(emittedState!.participants.length).toBe(2);
    expect(emittedState!.availablePlayers[0].selected).toBeTrue();
    expect(emittedState!.availablePlayers[0].available).toBeFalse();
    expect(emittedState!.availablePlayers[1].available).toBeTrue();
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
    httpMock.expectOne(`${apiBaseUrl}/api/games/game-1/participants`).flush(participantUsersResponse);
    httpMock.expectOne(`${apiBaseUrl}/players/catalogue`).flush(catalogueResponse);
    httpMock
      .expectOne(`${apiBaseUrl}/api/games/game-1/draft/snake/turn?region=GLOBAL`)
      .flush(null, { status: 404, statusText: 'Not Found' });
    httpMock
      .expectOne(`${apiBaseUrl}/api/games/game-1/draft/snake/initialize`)
      .flush(turnEnvelope);

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
    httpMock.expectOne(`${apiBaseUrl}/api/games/game-1/participants`).flush(participantUsersResponse);
    httpMock.expectOne(`${apiBaseUrl}/players/catalogue`).flush(catalogueResponse);
    httpMock
      .expectOne(`${apiBaseUrl}/api/games/game-1/draft/snake/turn?region=GLOBAL`)
      .flush(turnEnvelope);

    expect(emittedState).toBeDefined();
    expect(emittedState!.currentParticipant?.id).toBe('participant-2');
    expect(emittedState!.currentParticipant?.username).toBe('TEDDY');
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
    httpMock.expectOne(`${apiBaseUrl}/api/games/game-1/participants`).flush(participantUsersResponse);
    httpMock.expectOne(`${apiBaseUrl}/players/catalogue`).flush(catalogueResponse);
    httpMock
      .expectOne(`${apiBaseUrl}/api/games/game-1/draft/snake/turn?region=GLOBAL`)
      .flush(turnEnvelope);

    expect(emittedState).toBeDefined();
    expect(emittedState!.currentParticipant?.username).toBe('teddy');
  });
});
