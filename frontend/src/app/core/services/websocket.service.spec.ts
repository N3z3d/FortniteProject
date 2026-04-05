import { TestBed } from '@angular/core/testing';
import { take } from 'rxjs';
import { WebSocketService, TradeNotification } from './websocket.service';
import { AuthService } from './auth.service';
import { UserContextService } from './user-context.service';
import { LoggerService } from './logger.service';
import { environment } from '../../../environments/environment';

describe('WebSocketService', () => {
  let service: WebSocketService;
  let authService: jasmine.SpyObj<AuthService>;
  let userContext: jasmine.SpyObj<UserContextService>;
  let logger: jasmine.SpyObj<LoggerService>;

  beforeEach(() => {
    authService = jasmine.createSpyObj('AuthService', ['getToken']);
    userContext = jasmine.createSpyObj('UserContextService', ['getCurrentUser', 'getLastUser']);
    logger = jasmine.createSpyObj('LoggerService', ['debug', 'info', 'warn', 'error']);
    authService.getToken.and.returnValue(null);
    userContext.getCurrentUser.and.returnValue({ id: 'u1', username: 'TestUser', email: 'test@example.com' });
    userContext.getLastUser.and.returnValue(null);

    TestBed.configureTestingModule({
      providers: [
        WebSocketService,
        { provide: AuthService, useValue: authService },
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

  it('replays the last draft event through service.draftEvents to late subscribers', () => {
    const event = { event: 'PICK_PROMPT', draftId: 'draft-1', participantUsername: 'KARIM' };
    (service as any).draftEvents$.next(event);

    let received: unknown;
    service.draftEvents.pipe(take(1)).subscribe(value => {
      received = value;
    });

    expect(received).toEqual(event);
  });

  describe('buildConnectHeaders - production mode', () => {
    let originalProduction: boolean;

    beforeEach(() => {
      originalProduction = environment.production;
      (environment as any).production = true;
    });

    afterEach(() => {
      (environment as any).production = originalProduction;
    });

    it('injects stored JWT token as Authorization header in production', () => {
      authService.getToken.and.returnValue('stored-jwt-123');

      const headers = (service as any).buildConnectHeaders();

      expect(headers['Authorization']).toBe('Bearer stored-jwt-123');
    });

    it('returns empty headers in production when no token is stored', () => {
      authService.getToken.and.returnValue(null);

      const headers = (service as any).buildConnectHeaders();

      expect(headers['Authorization']).toBeUndefined();
      expect(headers['X-Test-User']).toBeUndefined();
    });

    it('explicit token overrides stored token in production', () => {
      authService.getToken.and.returnValue('stored-jwt-123');

      const headers = (service as any).buildConnectHeaders('explicit-token');

      expect(headers['Authorization']).toBe('Bearer explicit-token');
    });
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
