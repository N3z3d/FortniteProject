import { TestBed } from '@angular/core/testing';
import { take } from 'rxjs';
import { WebSocketService, TradeNotification } from './websocket.service';
import { UserContextService } from './user-context.service';
import { LoggerService } from './logger.service';
import { environment } from '../../../environments/environment';

describe('WebSocketService', () => {
  let service: WebSocketService;
  let userContext: jasmine.SpyObj<UserContextService>;
  let logger: jasmine.SpyObj<LoggerService>;

  beforeEach(() => {
    userContext = jasmine.createSpyObj('UserContextService', ['getCurrentUser', 'getLastUser']);
    logger = jasmine.createSpyObj('LoggerService', ['debug', 'info', 'warn', 'error']);
    userContext.getCurrentUser.and.returnValue({ id: 'u1', username: 'TestUser', email: 'test@example.com' });
    userContext.getLastUser.and.returnValue(null);

    TestBed.configureTestingModule({
      providers: [
        WebSocketService,
        { provide: UserContextService, useValue: userContext },
        { provide: LoggerService, useValue: logger }
      ]
    });
    service = TestBed.inject(WebSocketService);
  });

  afterEach(() => {
    service.disconnect();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should have isConnected$ observable', () => {
    expect(service.isConnected$).toBeDefined();
  });

  it('should have tradeNotifications observable', () => {
    expect(service.tradeNotifications).toBeDefined();
  });

  it('should initially be disconnected', () => {
    service.isConnected$.pipe(take(1)).subscribe(connected => {
      expect(connected).toBeFalse();
    });
  });

  it('should disconnect gracefully when not connected', () => {
    expect(() => service.disconnect()).not.toThrow();
  });

  it('should not throw when subscribing to game trades while disconnected', () => {
    expect(() => service.subscribeToGameTrades('test-game-id')).not.toThrow();
  });

  it('logs warning when subscribing to game trades while disconnected', () => {
    service.subscribeToGameTrades('test-game-id');

    expect(logger.warn).toHaveBeenCalledWith(
      'WebSocketService: cannot subscribe to game trades while disconnected',
      jasmine.objectContaining({ gameId: 'test-game-id' })
    );
  });

  it('logs error when trade notification payload is invalid JSON', () => {
    (service as any).handleTradeMessage({ body: 'invalid-json' });

    expect(logger.error).toHaveBeenCalledWith(
      'WebSocketService: failed to parse trade notification',
      jasmine.objectContaining({ body: 'invalid-json' })
    );
  });

  it('builds connect headers with bearer token when provided', () => {
    const headers = (service as any).buildConnectHeaders('token-123');

    expect(headers).toEqual({ Authorization: 'Bearer token-123' });
  });

  it('uses X-Test-User header in dev mode when no token is provided', () => {
    const headers = (service as any).buildConnectHeaders();

    expect(headers['X-Test-User']).toBe('TestUser');
  });

  it('builds websocket URL from apiUrl', () => {
    const url = (service as any).getWebSocketUrl();

    expect(url).toBe(`${environment.apiUrl}/ws`);
  });

  describe('TradeNotification interface', () => {
    it('should accept valid notification types', () => {
      const notification: TradeNotification = {
        type: 'TRADE_PROPOSED',
        tradeId: '123',
        fromTeamId: 'team1',
        fromTeamName: 'Team Alpha',
        toTeamId: 'team2',
        toTeamName: 'Team Beta',
        status: 'PENDING'
      };

      expect(notification.type).toBe('TRADE_PROPOSED');
      expect(notification.counterTradeId).toBeUndefined();
    });

    it('should accept notification with counterTradeId', () => {
      const notification: TradeNotification = {
        type: 'TRADE_COUNTERED',
        tradeId: '123',
        fromTeamId: 'team1',
        fromTeamName: 'Team Alpha',
        toTeamId: 'team2',
        toTeamName: 'Team Beta',
        status: 'COUNTERED',
        counterTradeId: '456'
      };

      expect(notification.counterTradeId).toBe('456');
    });
  });
});
