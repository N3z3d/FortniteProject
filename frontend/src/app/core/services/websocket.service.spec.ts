import { TestBed } from '@angular/core/testing';
import { WebSocketService, TradeNotification } from './websocket.service';

describe('WebSocketService', () => {
  let service: WebSocketService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [WebSocketService]
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

  it('should initially be disconnected', (done) => {
    service.isConnected$.subscribe(connected => {
      expect(connected).toBeFalse();
      done();
    });
  });

  it('should disconnect gracefully when not connected', () => {
    expect(() => service.disconnect()).not.toThrow();
  });

  it('should not throw when subscribing to game trades while disconnected', () => {
    expect(() => service.subscribeToGameTrades('test-game-id')).not.toThrow();
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
