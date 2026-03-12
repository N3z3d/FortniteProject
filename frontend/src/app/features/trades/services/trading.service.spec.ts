import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TradingService, Player, TradeOffer, TradeStats, Team } from './trading.service';
import { LoggerService } from '../../../core/services/logger.service';
import { environment } from '../../../../environments/environment';

describe('TradingService', () => {
  let service: TradingService;
  let httpMock: HttpTestingController;
  let logger: jasmine.SpyObj<LoggerService>;

  const makePlayer = (overrides: Partial<Player> = {}): Player => ({
    id: 'p1',
    name: 'Alpha',
    region: 'EU',
    team: 'Team A',
    position: 'Support',
    averageScore: 10,
    totalScore: 100,
    gamesPlayed: 12,
    marketValue: 50,
    ...overrides
  });

  const makeTradeResponse = (overrides: Partial<TradeOffer> = {}) => ({
    id: 't1',
    fromTeamId: 'team-1',
    fromTeamName: 'Team One',
    fromUserId: 'user-1',
    fromUserName: 'User One',
    toTeamId: 'team-2',
    toTeamName: 'Team Two',
    toUserId: 'user-2',
    toUserName: 'User Two',
    offeredPlayers: [makePlayer({ id: 'p1' })],
    requestedPlayers: [makePlayer({ id: 'p2', region: 'NA' })],
    status: 'pending' as const,
    createdAt: '2026-01-01T00:00:00.000Z',
    updatedAt: '2026-01-01T00:00:00.000Z',
    expiresAt: '2026-01-05T00:00:00.000Z',
    message: 'Offer',
    valueBalance: 10,
    ...overrides
  });

  const makeDraftDetailResponse = () => ({
    participants: [
      {
        participantId: 'participant-thibaut',
        username: 'thibaut',
        selectedPlayers: [{ playerId: 'player-out', nickname: 'Bugha' }]
      },
      {
        participantId: 'participant-teddy',
        username: 'teddy',
        selectedPlayers: [{ playerId: 'player-in', nickname: 'Mero' }]
      }
    ]
  });

  const makeDraftParticipantUsersResponse = () => [
    { userId: 'user-thibaut', username: 'thibaut' },
    { userId: 'user-teddy', username: 'teddy' }
  ];

  beforeEach(() => {
    logger = jasmine.createSpyObj<LoggerService>('LoggerService', ['debug', 'info', 'warn', 'error']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        TradingService,
        { provide: LoggerService, useValue: logger }
      ]
    });

    service = TestBed.inject(TradingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('getTrades caches results and maps date fields', () => {
    const rawTrade = makeTradeResponse();
    let response: TradeOffer[] | undefined;

    service.getTrades('game-1').subscribe(trades => {
      response = trades;
    });

    const legacyReq = httpMock.expectOne(`${environment.apiBaseUrl}/api/trades/game/game-1`);
    const detailReq = httpMock.expectOne(`${environment.apiBaseUrl}/api/games/game-1/details`);
    const participantsReq = httpMock.expectOne(
      `${environment.apiBaseUrl}/api/games/game-1/participants`
    );
    const auditReq = httpMock.expectOne(`${environment.apiBaseUrl}/api/games/game-1/draft/audit`);
    expect(legacyReq.request.method).toBe('GET');
    expect(detailReq.request.method).toBe('GET');
    expect(participantsReq.request.method).toBe('GET');
    expect(auditReq.request.method).toBe('GET');

    legacyReq.flush([rawTrade]);
    detailReq.flush({ participants: [] });
    participantsReq.flush([]);
    auditReq.flush([]);

    expect(response?.length).toBe(1);
    expect(response?.[0].createdAt instanceof Date).toBeTrue();
    expect((service as any).tradesSubject.value.length).toBe(1);

    service.getTrades('game-1').subscribe();
    httpMock.expectNone(`${environment.apiBaseUrl}/api/trades/game/game-1`);
    httpMock.expectNone(`${environment.apiBaseUrl}/api/games/game-1/details`);
    httpMock.expectNone(`${environment.apiBaseUrl}/api/games/game-1/participants`);
    httpMock.expectNone(`${environment.apiBaseUrl}/api/games/game-1/draft/audit`);
  });

  it('getTrades merges draft audit trades into the dashboard feed', () => {
    let response: TradeOffer[] | undefined;

    service.getTrades('game-1').subscribe(trades => {
      response = trades;
    });

    httpMock.expectOne(`${environment.apiBaseUrl}/api/trades/game/game-1`).flush([]);
    httpMock.expectOne(`${environment.apiBaseUrl}/api/games/game-1/details`).flush(
      makeDraftDetailResponse()
    );
    httpMock.expectOne(`${environment.apiBaseUrl}/api/games/game-1/participants`).flush(
      makeDraftParticipantUsersResponse()
    );
    httpMock.expectOne(`${environment.apiBaseUrl}/api/games/game-1/draft/audit`).flush([
      {
        id: 'proposal-1',
        type: 'TRADE_PROPOSED',
        occurredAt: '2026-03-09T10:00:00.000Z',
        proposerParticipantId: 'participant-thibaut',
        targetParticipantId: 'participant-teddy',
        playerOutId: 'player-out',
        playerInId: 'player-in'
      },
      {
        id: 'terminal-1',
        type: 'TRADE_ACCEPTED',
        occurredAt: '2026-03-09T10:05:00.000Z',
        proposerParticipantId: 'participant-thibaut',
        targetParticipantId: 'participant-teddy',
        playerOutId: 'player-out',
        playerInId: 'player-in'
      }
    ]);

    expect(response).toEqual([
      jasmine.objectContaining({
        id: 'proposal-1',
        status: 'accepted',
        fromUserId: 'user-thibaut',
        fromUserName: 'thibaut',
        toUserId: 'user-teddy',
        toUserName: 'teddy',
        offeredPlayers: [jasmine.objectContaining({ name: 'Bugha' })],
        requestedPlayers: [jasmine.objectContaining({ name: 'Mero' })]
      })
    ]);
  });

  it('getTrades without gameId uses base endpoint', () => {
    service.getTrades().subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/trades`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('getTrades surfaces the legacy endpoint failure instead of silently returning an empty list', () => {
    let receivedError: unknown;

    service.getTrades('game-1').subscribe({
      error: error => {
        receivedError = error;
      }
    });

    for (let attempt = 0; attempt < 3; attempt += 1) {
      httpMock.expectOne(`${environment.apiBaseUrl}/api/games/game-1/details`).flush({
        participants: []
      });
      httpMock.expectOne(`${environment.apiBaseUrl}/api/games/game-1/participants`).flush([]);
      httpMock.expectOne(`${environment.apiBaseUrl}/api/games/game-1/draft/audit`).flush([]);
      httpMock.expectOne(`${environment.apiBaseUrl}/api/trades/game/game-1`).flush(
        { message: 'boom' },
        { status: 500, statusText: 'Server Error' }
      );
    }

    expect(receivedError).toBeTruthy();
    expect((service as any).errorSubject.value).toBe('Failed to load trades');
  });

  it('getTeams caches results and updates subject from the real teams endpoint', () => {
    const teams: Team[] = [
      {
        id: 'team-1',
        name: 'Team One',
        ownerId: 'user-1',
        ownerName: 'User One',
        players: [makePlayer()],
        totalValue: 100,
        currentScore: 50,
        gameId: 'game-1'
      }
    ];

    service.getTeams('game-1').subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/teams/game/game-1`);
    expect(req.request.method).toBe('GET');
    req.flush(teams);

    expect((service as any).teamsSubject.value.length).toBe(1);

    service.getTeams('game-1').subscribe();
    httpMock.expectNone(`${environment.apiBaseUrl}/api/teams/game/game-1`);
  });

  it('createTradeOffer updates trades and clears errors', () => {
    const rawTrade = makeTradeResponse({ id: 't2' });
    let created: TradeOffer | undefined;

    service.createTradeOffer({ message: 'New' }).subscribe(trade => {
      created = trade;
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/trades`);
    expect(req.request.method).toBe('POST');
    req.flush(rawTrade);

    expect(created).toBeTruthy();
    expect((service as any).tradesSubject.value.length).toBe(1);
    expect((service as any).errorSubject.value).toBeNull();
  });

  it('calculateTradeBalance and isTradeBalanced handle edge cases', () => {
    const offered = [makePlayer({ marketValue: 100 })];
    const requested = [makePlayer({ id: 'p2', marketValue: 50 })];

    expect(service.calculateTradeBalance(offered, requested)).toBe(50);
    expect(service.isTradeBalanced(offered, requested, 0.6)).toBeTrue();
    expect(service.isTradeBalanced(offered, requested, 0.1)).toBeFalse();
    expect(service.isTradeBalanced([], [], 0.1)).toBeTrue();
  });

  it('getPendingTradesForUser filters pending trades', () => {
    const trades = [
      makeTradeResponse({ id: 't1', status: 'pending', toUserId: 'user-9' }) as TradeOffer,
      makeTradeResponse({ id: 't2', status: 'accepted', toUserId: 'user-9' }) as TradeOffer,
      makeTradeResponse({ id: 't3', status: 'pending', fromUserId: 'user-9' }) as TradeOffer
    ];

    (service as any).tradesSubject.next(trades);

    let pending: TradeOffer[] = [];
    service.getPendingTradesForUser('user-9').subscribe(result => {
      pending = result;
    });

    expect(pending.length).toBe(2);
  });

  it('getTradeHistory maps completedAt to Date', () => {
    const history = [
      {
        id: 'h1',
        fromTeamName: 'Team A',
        toTeamName: 'Team B',
        tradedPlayers: { offered: [makePlayer()], received: [makePlayer({ id: 'p2' })] },
        completedAt: '2026-01-02T00:00:00.000Z',
        success: true
      }
    ];

    let resultDate: Date | undefined;

    service.getTradeHistory('game-1').subscribe(items => {
      resultDate = items[0].completedAt;
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/trades/history?gameId=game-1`);
    expect(req.request.method).toBe('GET');
    req.flush(history);

    expect(resultDate instanceof Date).toBeTrue();
  });

  it('getTradingStats aggregates draft trades with legacy stats', () => {
    let stats: TradeStats | undefined;

    service.getTradingStats('game-1').subscribe(result => {
      stats = result;
    });

    httpMock.expectOne(`${environment.apiBaseUrl}/api/trades/game/game-1/statistics`).flush({
      totalTrades: 2,
      successfulTrades: 1,
      pendingOffers: 1,
      receivedOffers: 0
    });
    httpMock.expectOne(`${environment.apiBaseUrl}/api/games/game-1/details`).flush(
      makeDraftDetailResponse()
    );
    httpMock.expectOne(`${environment.apiBaseUrl}/api/games/game-1/participants`).flush(
      makeDraftParticipantUsersResponse()
    );
    httpMock.expectOne(`${environment.apiBaseUrl}/api/games/game-1/draft/audit`).flush([
      {
        id: 'proposal-1',
        type: 'TRADE_PROPOSED',
        occurredAt: '2026-03-09T10:00:00.000Z',
        proposerParticipantId: 'participant-thibaut',
        targetParticipantId: 'participant-teddy',
        playerOutId: 'player-out',
        playerInId: 'player-in'
      },
      {
        id: 'proposal-2',
        type: 'TRADE_PROPOSED',
        occurredAt: '2026-03-09T11:00:00.000Z',
        proposerParticipantId: 'participant-thibaut',
        targetParticipantId: 'participant-teddy',
        playerOutId: 'player-out',
        playerInId: 'player-in'
      },
      {
        id: 'terminal-1',
        type: 'TRADE_ACCEPTED',
        occurredAt: '2026-03-09T10:05:00.000Z',
        proposerParticipantId: 'participant-thibaut',
        targetParticipantId: 'participant-teddy',
        playerOutId: 'player-out',
        playerInId: 'player-in'
      }
    ]);

    expect(stats).toEqual({
      totalTrades: 3,
      successfulTrades: 2,
      pendingOffers: 1,
      receivedOffers: 0
    });
  });

  it('getTradingStats surfaces the legacy statistics failure instead of silently returning zeros', () => {
    let receivedError: unknown;

    service.getTradingStats('game-1').subscribe({
      error: error => {
        receivedError = error;
      }
    });

    for (let attempt = 0; attempt < 3; attempt += 1) {
      httpMock.expectOne(`${environment.apiBaseUrl}/api/games/game-1/details`).flush(
        makeDraftDetailResponse()
      );
      httpMock.expectOne(`${environment.apiBaseUrl}/api/games/game-1/participants`).flush(
        makeDraftParticipantUsersResponse()
      );
      httpMock.expectOne(`${environment.apiBaseUrl}/api/games/game-1/draft/audit`).flush([]);
      httpMock.expectOne(`${environment.apiBaseUrl}/api/trades/game/game-1/statistics`).flush(
        { message: 'boom' },
        { status: 500, statusText: 'Server Error' }
      );
    }

    expect(receivedError).toBeTruthy();
    expect((service as any).errorSubject.value).toBe('Failed to load trading statistics');
  });
});
