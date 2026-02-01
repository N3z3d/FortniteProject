import { TestBed } from '@angular/core/testing';
import { TradeTimelineService } from './trade-timeline.service';
import { TranslationService } from '../../../core/services/translation.service';
import { TradeOffer } from './trading.service';

describe('TradeTimelineService', () => {
  let service: TradeTimelineService;
  let translation: jasmine.SpyObj<TranslationService>;

  const baseTrade: TradeOffer = {
    id: 'trade-1',
    fromTeamId: 'team-a',
    fromTeamName: 'Team A',
    fromUserId: 'user-a',
    fromUserName: 'Alice',
    toTeamId: 'team-b',
    toTeamName: 'Team B',
    toUserId: 'user-b',
    toUserName: 'Bob',
    offeredPlayers: [],
    requestedPlayers: [],
    status: 'pending',
    createdAt: new Date('2025-01-01T00:00:00Z'),
    updatedAt: new Date('2025-01-02T00:00:00Z'),
    expiresAt: new Date('2025-01-03T00:00:00Z'),
    valueBalance: 0
  };

  beforeEach(() => {
    translation = jasmine.createSpyObj('TranslationService', ['t']);
    translation.t.and.callFake((key: string, fallback?: string) => fallback ?? key);

    TestBed.configureTestingModule({
      providers: [
        TradeTimelineService,
        { provide: TranslationService, useValue: translation }
      ]
    });

    service = TestBed.inject(TradeTimelineService);
  });

  it('builds a pending timeline sorted by date', () => {
    jasmine.clock().install();
    jasmine.clock().mockDate(new Date('2025-01-02T00:00:00Z'));

    const timeline = service.generateTradeTimeline({ ...baseTrade, status: 'pending' });

    expect(timeline.length).toBe(3);
    expect(timeline[0].status).toBe('completed');
    expect(timeline[1].status).toBe('current');
    expect(timeline[2].status).toBe('pending');
    expect(timeline[0].date.getTime()).toBe(baseTrade.createdAt.getTime());
    expect(timeline[2].date.getTime()).toBe(baseTrade.expiresAt.getTime());

    jasmine.clock().uninstall();
  });

  it('builds an accepted timeline with correct actor and icon', () => {
    const timeline = service.generateTradeTimeline({
      ...baseTrade,
      status: 'accepted'
    });

    const statusEntry = timeline[1];
    expect(statusEntry.actor).toBe('Bob');
    expect(statusEntry.icon).toBe('check_circle');
  });

  it('uses the proposer for withdrawn and system for expired', () => {
    const withdrawnTimeline = service.generateTradeTimeline({
      ...baseTrade,
      status: 'withdrawn'
    });

    expect(withdrawnTimeline[1].actor).toBe('Alice');

    const expiredTimeline = service.generateTradeTimeline({
      ...baseTrade,
      status: 'expired'
    });

    expect(expiredTimeline[1].actor).toBe('trades.details.system');
  });

  it('falls back to defaults for unknown status', () => {
    const timeline = service.generateTradeTimeline({
      ...baseTrade,
      status: 'unknown' as TradeOffer['status']
    });

    const statusEntry = timeline[1];
    expect(statusEntry.icon).toBe('help_outline');
    expect(statusEntry.description).toBe('trades.details.timelineStatusUpdatedDesc');
  });
});
