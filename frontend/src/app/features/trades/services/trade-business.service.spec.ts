import { TestBed } from '@angular/core/testing';
import { TradeBusinessService } from './trade-business.service';
import { TradeCalculationService } from './trade-calculation.service';
import { TradeFormattingService } from './trade-formatting.service';
import { TradeValidationService } from './trade-validation.service';
import { Player, TradingService } from './trading.service';

describe('TradeBusinessService', () => {
  let service: TradeBusinessService;
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

  const listTypes = {
    OFFERED_LIST: 'offered',
    REQUESTED_LIST: 'requested',
    AVAILABLE_LIST: 'available',
    TARGET_LIST: 'target'
  };

  beforeEach(() => {
    tradingService = jasmine.createSpyObj('TradingService', [
      'calculateTradeBalance',
      'isTradeBalanced'
    ]);

    TestBed.configureTestingModule({
      providers: [
        TradeBusinessService,
        TradeCalculationService,
        TradeFormattingService,
        TradeValidationService,
        { provide: TradingService, useValue: tradingService }
      ]
    });

    service = TestBed.inject(TradeBusinessService);
  });

  it('validateTradeProposal returns true only when all conditions are met', () => {
    const offered = [createPlayer({ id: 'o1' })];
    const requested = [createPlayer({ id: 'r1' })];

    expect(service.validateTradeProposal(null, offered, requested, true)).toBeFalse();
    expect(service.validateTradeProposal({}, [], requested, true)).toBeFalse();
    expect(service.validateTradeProposal({}, offered, [], true)).toBeFalse();
    expect(service.validateTradeProposal({}, offered, requested, false)).toBeFalse();
    expect(service.validateTradeProposal({}, offered, requested, true)).toBeTrue();
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

  it('canMovePlayer prevents invalid list moves', () => {
    const player = createPlayer();

    expect(service.canMovePlayer(player, 'available', 'available', listTypes)).toBeFalse();
    expect(service.canMovePlayer(player, 'available', 'offered', listTypes)).toBeTrue();
    expect(service.canMovePlayer(player, 'target', 'requested', listTypes)).toBeTrue();
    expect(service.canMovePlayer(player, 'target', 'target', listTypes)).toBeFalse();
    expect(service.canMovePlayer(player, 'available', 'requested', listTypes)).toBeFalse();
    expect(service.canMovePlayer(player, 'requested', 'available', listTypes)).toBeFalse();
    expect(service.canMovePlayer(player, 'custom', 'offered', listTypes)).toBeTrue();
  });

  it('getBalanceDisplayClass returns class names based on sign', () => {
    expect(service.getBalanceDisplayClass(10)).toBe('positive');
    expect(service.getBalanceDisplayClass(-5)).toBe('negative');
    expect(service.getBalanceDisplayClass(0)).toBe('neutral');
  });

  it('getBalanceIcon returns icon names based on sign', () => {
    expect(service.getBalanceIcon(10)).toBe('trending_up');
    expect(service.getBalanceIcon(-5)).toBe('trending_down');
    expect(service.getBalanceIcon(0)).toBe('compare_arrows');
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

  it('formatCurrency formats USD without decimals', () => {
    const formatted = service.formatCurrency(1234);

    expect(formatted).toContain('$');
    expect(formatted).toContain('1,234');
  });
});
