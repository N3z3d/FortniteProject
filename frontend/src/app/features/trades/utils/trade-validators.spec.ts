import { TradeValidators } from './trade-validators';
import { Player } from '../services/trading.service';

describe('TradeValidators', () => {
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

  describe('matchesSearchQuery', () => {
    it('returns true for empty or whitespace query', () => {
      const player = makePlayer();

      expect(TradeValidators.matchesSearchQuery(player, '')).toBeTrue();
      expect(TradeValidators.matchesSearchQuery(player, '   ')).toBeTrue();
    });

    it('matches by name, region, or position (case-insensitive)', () => {
      const player = makePlayer({ name: 'SkyFire', region: 'NA', position: 'Sniper' });

      expect(TradeValidators.matchesSearchQuery(player, 'sky')).toBeTrue();
      expect(TradeValidators.matchesSearchQuery(player, 'na')).toBeTrue();
      expect(TradeValidators.matchesSearchQuery(player, 'sni')).toBeTrue();
    });
  });

  describe('filterPlayersBySearch', () => {
    it('returns all players when query is empty', () => {
      const players = [makePlayer({ id: 'p1' }), makePlayer({ id: 'p2', name: 'Bravo' })];

      expect(TradeValidators.filterPlayersBySearch(players, ' ')).toEqual(players);
    });

    it('filters players by search query', () => {
      const players = [
        makePlayer({ id: 'p1', name: 'Alpha' }),
        makePlayer({ id: 'p2', name: 'Bravo' })
      ];

      const result = TradeValidators.filterPlayersBySearch(players, 'br');

      expect(result.length).toBe(1);
      expect(result[0].id).toBe('p2');
    });
  });

  describe('isPlayerEligibleForTrade', () => {
    it('returns invalid when player is null', () => {
      const result = TradeValidators.isPlayerEligibleForTrade(null as unknown as Player, {
        minGamesPlayed: 1
      });

      expect(result.valid).toBeFalse();
      expect(result.reason).toContain('null or undefined');
    });

    it('validates min games, market value, and allowed regions', () => {
      const player = makePlayer({ gamesPlayed: 3, marketValue: 20, region: 'EU' });

      const minGames = TradeValidators.isPlayerEligibleForTrade(player, { minGamesPlayed: 5 });
      expect(minGames.valid).toBeFalse();

      const minValue = TradeValidators.isPlayerEligibleForTrade(player, { minMarketValue: 50 });
      expect(minValue.valid).toBeFalse();

      const region = TradeValidators.isPlayerEligibleForTrade(player, { allowedRegions: ['NA'] });
      expect(region.valid).toBeFalse();

      const ok = TradeValidators.isPlayerEligibleForTrade(player, {
        minGamesPlayed: 2,
        minMarketValue: 10,
        allowedRegions: ['EU', 'NA']
      });
      expect(ok.valid).toBeTrue();
    });
  });

  describe('validateRegionRules', () => {
    it('rejects cross-region trades when not allowed', () => {
      const offeredPlayers = [makePlayer({ region: 'EU' })];
      const requestedPlayers = [makePlayer({ region: 'NA' })];

      const result = TradeValidators.validateRegionRules(offeredPlayers, requestedPlayers, {
        allowCrossRegionTrades: false
      });

      expect(result.valid).toBeFalse();
      expect(result.reason).toContain('Cross-region trades');
    });
  });

  describe('validateTradeBalance', () => {
    it('rejects zero total value and imbalance beyond threshold', () => {
      const zeroResult = TradeValidators.validateTradeBalance(0, 0, { maxImbalancePercent: 10 });
      expect(zeroResult.valid).toBeFalse();

      const imbalance = TradeValidators.validateTradeBalance(90, 10, { maxImbalancePercent: 20 });
      expect(imbalance.valid).toBeFalse();
    });

    it('enforces exact balance when required', () => {
      const result = TradeValidators.validateTradeBalance(40, 30, { requireExactBalance: true });
      expect(result.valid).toBeFalse();
    });
  });

  describe('validateMinimumPlayers', () => {
    it('rejects when offered or requested players are below minimum', () => {
      const onePlayer = [makePlayer()];
      const resultOffered = TradeValidators.validateMinimumPlayers([], onePlayer, 1);
      const resultRequested = TradeValidators.validateMinimumPlayers(onePlayer, [], 1);

      expect(resultOffered.valid).toBeFalse();
      expect(resultRequested.valid).toBeFalse();
    });
  });

  describe('validateTrade', () => {
    it('aggregates errors across rules', () => {
      const offeredPlayers = [makePlayer({ marketValue: 10, region: 'EU' })];
      const requestedPlayers = [makePlayer({ marketValue: 90, region: 'NA' })];

      const result = TradeValidators.validateTrade(offeredPlayers, requestedPlayers, {
        minPlayers: 2,
        maxImbalancePercent: 20,
        allowCrossRegionTrades: false
      });

      expect(result.valid).toBeFalse();
      expect(result.errors.length).toBeGreaterThan(1);
    });
  });
});
