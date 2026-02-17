import { TestBed } from '@angular/core/testing';
import { TradeCalculationService } from './trade-calculation.service';
import { Player, TradingService } from './trading.service';

describe('TradeCalculationService', () => {
  let service: TradeCalculationService;
  let tradingService: jasmine.SpyObj<TradingService>;

  const createPlayer = (overrides: Partial<Player> = {}): Player => ({
    id: 'p1',
    name: 'Player',
    region: 'EU',
    averageScore: 10,
    totalScore: 100,
    gamesPlayed: 10,
    marketValue: 100,
    ...overrides
  });

  beforeEach(() => {
    tradingService = jasmine.createSpyObj('TradingService', [
      'calculateTradeBalance',
      'isTradeBalanced'
    ]);

    TestBed.configureTestingModule({
      providers: [
        TradeCalculationService,
        { provide: TradingService, useValue: tradingService }
      ]
    });

    service = TestBed.inject(TradeCalculationService);
  });

  it('calculateBalancePercentage returns 0 for empty or zero-value lists', () => {
    expect(service.calculateBalancePercentage([], [])).toBe(0);

    const zeroed = [createPlayer({ marketValue: 0 })];
    expect(service.calculateBalancePercentage(zeroed, zeroed)).toBe(0);
  });

  it('calculateBalancePercentage returns the absolute percentage difference', () => {
    const offered = [createPlayer({ marketValue: 100 })];
    const requested = [createPlayer({ marketValue: 50 })];

    const result = service.calculateBalancePercentage(offered, requested);

    expect(result).toBeCloseTo(33.333, 2);
  });

  it('calculateTotalValue sums player market values', () => {
    const players = [
      createPlayer({ marketValue: 120 }),
      createPlayer({ marketValue: 80 }),
      createPlayer({ marketValue: undefined as unknown as number })
    ];

    expect(service.calculateTotalValue(players)).toBe(200);
  });

  it('calculateTradeBalance delegates to TradingService', () => {
    const offered = [createPlayer({ marketValue: 120 })];
    const requested = [createPlayer({ marketValue: 90 })];

    tradingService.calculateTradeBalance.and.returnValue(30);

    expect(service.calculateTradeBalance(offered, requested)).toBe(30);
    expect(tradingService.calculateTradeBalance).toHaveBeenCalledWith(offered, requested);
  });

  it('isTradeBalanced delegates to TradingService', () => {
    const offered = [createPlayer({ marketValue: 120 })];
    const requested = [createPlayer({ marketValue: 90 })];

    tradingService.isTradeBalanced.and.returnValue(true);

    expect(service.isTradeBalanced(offered, requested)).toBeTrue();
    expect(tradingService.isTradeBalanced).toHaveBeenCalledWith(offered, requested);
  });

  it('calculateFairnessRating matches thresholds', () => {
    expect(service.calculateFairnessRating(5)).toBe('excellent');
    expect(service.calculateFairnessRating(15)).toBe('good');
    expect(service.calculateFairnessRating(25)).toBe('fair');
    expect(service.calculateFairnessRating(26)).toBe('poor');
  });

  it('calculateTradeStats aggregates totals and ratings', () => {
    const offered = [createPlayer({ marketValue: 100 })];
    const requested = [createPlayer({ marketValue: 80 }), createPlayer({ marketValue: 20 })];

    const stats = service.calculateTradeStats(offered, requested);

    expect(stats.totalPlayers).toBe(3);
    expect(stats.offeredValue).toBe(100);
    expect(stats.requestedValue).toBe(100);
    expect(stats.totalValue).toBe(200);
    expect(stats.avgPlayerValue).toBeCloseTo(66.666, 2);
    expect(stats.balancePercentage).toBe(0);
    expect(stats.fairnessRating).toBe('excellent');
  });
});
