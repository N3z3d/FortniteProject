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

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/trades/game/game-1`);
    expect(req.request.method).toBe('GET');
    req.flush([rawTrade]);

    expect(response?.length).toBe(1);
    expect(response?.[0].createdAt instanceof Date).toBeTrue();
    expect((service as any).tradesSubject.value.length).toBe(1);

    service.getTrades('game-1').subscribe();
    httpMock.expectNone(`${environment.apiBaseUrl}/api/trades/game/game-1`);
  });

  it('getTrades without gameId uses base endpoint', () => {
    service.getTrades().subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/trades`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('getTeams caches results and updates subject', () => {
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

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/trades/teams?gameId=game-1`);
    expect(req.request.method).toBe('GET');
    req.flush(teams);

    expect((service as any).teamsSubject.value.length).toBe(1);

    service.getTeams('game-1').subscribe();
    httpMock.expectNone(`${environment.apiBaseUrl}/api/trades/teams?gameId=game-1`);
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

  it('getTradingStats emits error and sets errorSubject on failure', (done) => {
    const url = `${environment.apiBaseUrl}/api/trades/game/game-1/statistics`;

    service.getTradingStats('game-1').subscribe({
      error: err => {
        expect(err).toBeTruthy();
        expect((service as any).errorSubject.value).toBe('Failed to load trading statistics');
        done();
      }
    });

    const req1 = httpMock.expectOne(url);
    req1.flush('Boom', { status: 500, statusText: 'Server Error' });
    const req2 = httpMock.expectOne(url);
    req2.flush('Boom', { status: 500, statusText: 'Server Error' });
    const req3 = httpMock.expectOne(url);
    req3.flush('Boom', { status: 500, statusText: 'Server Error' });
  });
});
