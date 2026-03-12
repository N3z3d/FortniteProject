import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError, Subject } from 'rxjs';
import { TradeListComponent, Trade } from './trade-list.component';
import { LoggerService } from '../../../core/services/logger.service';
import { TranslationService } from '../../../core/services/translation.service';
import { WebSocketService, TradeNotification } from '../../../core/services/websocket.service';
import { TradingService, TradeOffer } from '../services/trading.service';
import { GameSelectionService } from '../../../core/services/game-selection.service';

describe('TradeListComponent', () => {
  let component: TradeListComponent;
  let fixture: ComponentFixture<TradeListComponent>;
  let router: jasmine.SpyObj<Router>;
  let logger: jasmine.SpyObj<LoggerService>;
  let tradingService: jasmine.SpyObj<TradingService>;
  let translationService: jasmine.SpyObj<TranslationService>;
  let webSocketService: jasmine.SpyObj<WebSocketService>;
  let tradeNotificationsSubject: Subject<TradeNotification>;
  let gameSelectionService: jasmine.SpyObj<GameSelectionService>;

  beforeEach(async () => {
    router = jasmine.createSpyObj('Router', ['navigate']);
    logger = jasmine.createSpyObj('LoggerService', ['debug', 'error']);
    tradingService = jasmine.createSpyObj('TradingService', ['getTrades']);
    translationService = jasmine.createSpyObj('TranslationService', ['t']);
    translationService.t.and.callFake((key: string, fallback?: string) => fallback || key);
    gameSelectionService = jasmine.createSpyObj('GameSelectionService', ['getSelectedGame']);
    gameSelectionService.getSelectedGame.and.returnValue({ id: 'selected-game', name: 'Selected Game' } as any);

    tradeNotificationsSubject = new Subject<TradeNotification>();
    webSocketService = jasmine.createSpyObj('WebSocketService', ['connect'], {
      tradeNotifications: tradeNotificationsSubject.asObservable()
    });

    const mockTradeOffers: TradeOffer[] = [{
      id: '1',
      fromTeamId: 'team1',
      fromTeamName: 'Team Alpha',
      fromUserId: 'user1',
      fromUserName: 'User1',
      toTeamId: 'team2',
      toTeamName: 'Team Beta',
      toUserId: 'user2',
      toUserName: 'User2',
      offeredPlayers: [{ id: 'p1', name: 'Player1', region: 'EU', averageScore: 100, totalScore: 1000, gamesPlayed: 10, marketValue: 5000 }],
      requestedPlayers: [{ id: 'p2', name: 'Player2', region: 'NAW', averageScore: 90, totalScore: 900, gamesPlayed: 10, marketValue: 4500 }],
      status: 'pending',
      createdAt: new Date(),
      updatedAt: new Date(),
      expiresAt: new Date(),
      valueBalance: 500
    }];
    tradingService.getTrades.and.returnValue(of(mockTradeOffers));

    await TestBed.configureTestingModule({
      imports: [TradeListComponent],
      providers: [
        { provide: Router, useValue: router },
        {
          provide: ActivatedRoute,
          useValue: {
            parent: { snapshot: { paramMap: { get: (key: string) => key === 'id' ? 'game-1' : null } } },
            snapshot: { paramMap: { get: () => null } }
          }
        },
        { provide: LoggerService, useValue: logger },
        { provide: TradingService, useValue: tradingService },
        { provide: TranslationService, useValue: translationService },
        { provide: WebSocketService, useValue: webSocketService },
        { provide: GameSelectionService, useValue: gameSelectionService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TradeListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with loading state', () => {
    expect(component.isLoading).toBeTrue();
    expect(component.trades).toEqual([]);
    expect(component.activeTab).toBe('ALL');
  });

  it('should load trades on init', fakeAsync(() => {
    fixture.detectChanges();
    tick();

    expect(tradingService.getTrades).toHaveBeenCalledWith('game-1');
    expect(component.isLoading).toBeFalse();
    expect(component.trades.length).toBeGreaterThan(0);
  }));

  it('should connect to WebSocket on init', () => {
    fixture.detectChanges();

    expect(webSocketService.connect).toHaveBeenCalled();
  });

  it('should subscribe to trade notifications', fakeAsync(() => {
    spyOn<any>(component, 'handleTradeNotification');

    fixture.detectChanges();

    const notification: TradeNotification = {
      type: 'TRADE_PROPOSED',
      tradeId: '1',
      fromTeamId: 'team1',
      fromTeamName: 'Team Alpha',
      toTeamId: 'team2',
      toTeamName: 'Team Beta',
      status: 'pending'
    };

    tradeNotificationsSubject.next(notification);
    tick();

    expect(component['handleTradeNotification']).toHaveBeenCalledWith(notification);
  }));

  it('should reload trades on TRADE_PROPOSED notification', fakeAsync(() => {
    fixture.detectChanges();
    tick();

    tradingService.getTrades.calls.reset();

    const notification: TradeNotification = {
      type: 'TRADE_PROPOSED',
      tradeId: '2',
      fromTeamId: 'team1',
      fromTeamName: 'Team Alpha',
      toTeamId: 'team2',
      toTeamName: 'Team Beta',
      status: 'pending'
    };

    tradeNotificationsSubject.next(notification);
    tick();

    expect(tradingService.getTrades).toHaveBeenCalled();
  }));

  it('should update trade status on TRADE_ACCEPTED notification', fakeAsync(() => {
    fixture.detectChanges();
    tick();

    component.trades = [{
      id: '1',
      playerOut: { id: 'p1', username: 'Player1' },
      playerIn: { id: 'p2', username: 'Player2' },
      team: { id: 't1', name: 'Team', owner: 'Owner' },
      createdAt: new Date(),
      status: 'PENDING'
    }];

    const notification: TradeNotification = {
      type: 'TRADE_ACCEPTED',
      tradeId: '1',
      fromTeamId: 'team1',
      fromTeamName: 'Team Alpha',
      toTeamId: 'team2',
      toTeamName: 'Team Beta',
      status: 'accepted'
    };

    tradeNotificationsSubject.next(notification);
    tick();

    expect(component.trades[0].status).toBe('COMPLETED');
  }));

  it('should handle trade notification error', fakeAsync(() => {
    fixture.detectChanges();

    tradeNotificationsSubject.error(new Error('WebSocket error'));
    tick();

    expect(logger.error).toHaveBeenCalledWith('TradeList: WebSocket notification error', jasmine.any(Error));
  }));

  it('should expose an explicit error instead of falling back to mock data on trades loading error', fakeAsync(() => {
    tradingService.getTrades.and.returnValue(throwError(() => new Error('API error')));

    fixture.detectChanges();
    tick();

    expect(logger.error).toHaveBeenCalledWith('TradeList: failed to load trades', jasmine.any(Error));
    expect(component.trades).toEqual([]);
    expect(component.errorMessage).toBeTruthy();
    expect(component.isLoading).toBeFalse();
  }));

  it('should map trade offer status correctly', () => {
    const mockPlayer = { id: 'p1', name: 'P1', region: 'EU', averageScore: 100, totalScore: 1000, gamesPlayed: 10, marketValue: 5000 };
    const mockOffers: TradeOffer[] = [
      { id: '1', status: 'pending', offeredPlayers: [mockPlayer], requestedPlayers: [mockPlayer], fromTeamId: 't1', fromTeamName: 'Team1', fromUserId: 'u1', fromUserName: 'User1', toTeamId: 't2', toTeamName: 'Team2', toUserId: 'u2', toUserName: 'User2', createdAt: new Date(), updatedAt: new Date(), expiresAt: new Date(), valueBalance: 0 },
      { id: '2', status: 'accepted', offeredPlayers: [mockPlayer], requestedPlayers: [mockPlayer], fromTeamId: 't1', fromTeamName: 'Team1', fromUserId: 'u1', fromUserName: 'User1', toTeamId: 't2', toTeamName: 'Team2', toUserId: 'u2', toUserName: 'User2', createdAt: new Date(), updatedAt: new Date(), expiresAt: new Date(), valueBalance: 0 },
      { id: '3', status: 'rejected', offeredPlayers: [mockPlayer], requestedPlayers: [mockPlayer], fromTeamId: 't1', fromTeamName: 'Team1', fromUserId: 'u1', fromUserName: 'User1', toTeamId: 't2', toTeamName: 'Team2', toUserId: 'u2', toUserName: 'User2', createdAt: new Date(), updatedAt: new Date(), expiresAt: new Date(), valueBalance: 0 }
    ];
    tradingService.getTrades.and.returnValue(of(mockOffers));

    fixture.detectChanges();

    expect(component.trades[0].status).toBe('PENDING');
    expect(component.trades[1].status).toBe('COMPLETED');
    expect(component.trades[2].status).toBe('CANCELLED');
  });

  it('should filter trades by active tab', fakeAsync(() => {
    fixture.detectChanges();
    tick();

    component.trades = [
      { id: '1', status: 'PENDING', playerOut: { id: 'p1', username: 'P1' }, playerIn: { id: 'p2', username: 'P2' }, team: { id: 't1', name: 'T1', owner: 'O1' }, createdAt: new Date() },
      { id: '2', status: 'COMPLETED', playerOut: { id: 'p3', username: 'P3' }, playerIn: { id: 'p4', username: 'P4' }, team: { id: 't2', name: 'T2', owner: 'O2' }, createdAt: new Date() }
    ];

    component.setTab('PENDING');
    expect(component.filteredTrades.length).toBe(1);
    expect(component.filteredTrades[0].status).toBe('PENDING');

    component.setTab('HISTORY');
    expect(component.filteredTrades.length).toBe(1);
    expect(component.filteredTrades[0].status).toBe('COMPLETED');

    component.setTab('ALL');
    expect(component.filteredTrades.length).toBe(2);
  }));

  it('should filter trades by search term', fakeAsync(() => {
    fixture.detectChanges();
    tick();

    component.trades = [
      { id: '1', status: 'PENDING', playerOut: { id: 'p1', username: 'Ninja' }, playerIn: { id: 'p2', username: 'Tfue' }, team: { id: 't1', name: 'Team Alpha', owner: 'Owner' }, createdAt: new Date() },
      { id: '2', status: 'PENDING', playerOut: { id: 'p3', username: 'Bugha' }, playerIn: { id: 'p4', username: 'Aqua' }, team: { id: 't2', name: 'Team Beta', owner: 'Owner' }, createdAt: new Date() }
    ];

    component.searchTerm = 'Ninja';
    component.applyFilters();

    expect(component.filteredTrades.length).toBe(1);
    expect(component.filteredTrades[0].playerOut.username).toBe('Ninja');
  }));

  it('should get active trades count', fakeAsync(() => {
    fixture.detectChanges();
    tick();

    component.trades = [
      { id: '1', status: 'PENDING', playerOut: { id: 'p1', username: 'P1' }, playerIn: { id: 'p2', username: 'P2' }, team: { id: 't1', name: 'T1', owner: 'O' }, createdAt: new Date() },
      { id: '2', status: 'COMPLETED', playerOut: { id: 'p3', username: 'P3' }, playerIn: { id: 'p4', username: 'P4' }, team: { id: 't2', name: 'T2', owner: 'O' }, createdAt: new Date() },
      { id: '3', status: 'PENDING', playerOut: { id: 'p5', username: 'P5' }, playerIn: { id: 'p6', username: 'P6' }, team: { id: 't3', name: 'T3', owner: 'O' }, createdAt: new Date() }
    ];

    expect(component.getActiveTradesCount()).toBe(2);
    expect(component.getPendingCount()).toBe(2);
  }));

  it('should navigate to create trade', () => {
    (component as any).gameId = 'game-1';
    component.createNewTrade();

    expect(router.navigate).toHaveBeenCalledWith(['/games', 'game-1', 'trades', 'create']);
  });

  it('should navigate to trade detail', () => {
    (component as any).gameId = 'game-1';
    component.viewTrade('123');

    expect(router.navigate).toHaveBeenCalledWith(['/games', 'game-1', 'trades', '123']);
  });

  it('should cancel trade', fakeAsync(() => {
    fixture.detectChanges();
    tick();

    component.trades = [
      { id: '1', status: 'PENDING', playerOut: { id: 'p1', username: 'P1' }, playerIn: { id: 'p2', username: 'P2' }, team: { id: 't1', name: 'T1', owner: 'O' }, createdAt: new Date() }
    ];
    component.applyFilters();

    component.cancelTrade('1');

    expect(logger.debug).toHaveBeenCalledWith('TradeList: cancelling trade', { tradeId: '1' });
    expect(component.trades[0].status).toBe('CANCELLED');
  }));

  it('should get status label from translation', () => {
    translationService.t.and.returnValue('Pending');

    const label = component.getStatusLabel('PENDING');

    expect(label).toBe('Pending');
    expect(translationService.t).toHaveBeenCalledWith('trades.status.pending', 'PENDING');
  });

  it('should track trades by id', () => {
    const trade: Trade = {
      id: '123',
      status: 'PENDING',
      playerOut: { id: 'p1', username: 'P1' },
      playerIn: { id: 'p2', username: 'P2' },
      team: { id: 't1', name: 'T1', owner: 'O' },
      createdAt: new Date()
    };

    const trackId = component.trackByTradeId(0, trade);

    expect(trackId).toBe('123');
  });

  it('should unsubscribe on destroy', () => {
    fixture.detectChanges();

    spyOn(component['destroy$'], 'next');
    spyOn(component['destroy$'], 'complete');

    component.ngOnDestroy();

    expect(component['destroy$'].next).toHaveBeenCalled();
    expect(component['destroy$'].complete).toHaveBeenCalled();
  });
});
