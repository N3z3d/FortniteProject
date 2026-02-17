import { TestBed } from '@angular/core/testing';
import { TradeValidationService } from './trade-validation.service';
import { Player } from './trading.service';

describe('TradeValidationService', () => {
  let service: TradeValidationService;

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
    TestBed.configureTestingModule({
      providers: [TradeValidationService]
    });

    service = TestBed.inject(TradeValidationService);
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

  it('canMovePlayer prevents invalid list moves', () => {
    const player = createPlayer();

    expect(service.canMovePlayer(player, 'available', 'available', listTypes)).toBeFalse();
    expect(service.canMovePlayer(player, 'available', 'offered', listTypes)).toBeTrue();
    expect(service.canMovePlayer(player, 'target', 'requested', listTypes)).toBeTrue();
    expect(service.canMovePlayer(player, 'target', 'target', listTypes)).toBeFalse();
  });

  it('canMovePlayer blocks moves across team pools', () => {
    const player = createPlayer();

    expect(service.canMovePlayer(player, 'available', 'requested', listTypes)).toBeFalse();
    expect(service.canMovePlayer(player, 'requested', 'available', listTypes)).toBeFalse();
  });

  it('canMovePlayer allows unknown lists to be handled permissively', () => {
    const player = createPlayer();

    expect(service.canMovePlayer(player, 'custom', 'offered', listTypes)).toBeTrue();
  });
});
