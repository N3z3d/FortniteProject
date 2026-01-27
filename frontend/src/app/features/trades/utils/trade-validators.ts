import { Player } from '../services/trading.service';

/**
 * TradeValidators
 *
 * Utility class for trade validation logic.
 * Extracted from components to follow SRP and DRY principles.
 */
export class TradeValidators {
  /**
   * Validates player search query
   */
  static matchesSearchQuery(player: Player, query: string): boolean {
    if (!query || !query.trim()) return true;

    const lowerQuery = query.toLowerCase();
    return (
      player.name.toLowerCase().includes(lowerQuery) ||
      player.region.toLowerCase().includes(lowerQuery) ||
      player.position?.toLowerCase().includes(lowerQuery) ||
      false
    );
  }

  /**
   * Filters players based on search query
   */
  static filterPlayersBySearch(players: Player[], query: string): Player[] {
    if (!query || !query.trim()) return players;

    return players.filter(player => this.matchesSearchQuery(player, query));
  }

  /**
   * Validates if a player is eligible for trading
   */
  static isPlayerEligibleForTrade(
    player: Player,
    gameRules?: {
      minGamesPlayed?: number;
      minMarketValue?: number;
      allowedRegions?: string[];
    }
  ): { valid: boolean; reason?: string } {
    if (!player) {
      return { valid: false, reason: 'Player is null or undefined' };
    }

    // Check minimum games played
    if (gameRules?.minGamesPlayed && player.gamesPlayed < gameRules.minGamesPlayed) {
      return {
        valid: false,
        reason: `Player must have played at least ${gameRules.minGamesPlayed} games`
      };
    }

    // Check minimum market value
    if (gameRules?.minMarketValue && player.marketValue < gameRules.minMarketValue) {
      return {
        valid: false,
        reason: `Player must have minimum market value of ${gameRules.minMarketValue}`
      };
    }

    // Check allowed regions
    if (
      gameRules?.allowedRegions &&
      gameRules.allowedRegions.length > 0 &&
      !gameRules.allowedRegions.includes(player.region)
    ) {
      return {
        valid: false,
        reason: `Player region ${player.region} is not allowed in this game`
      };
    }

    return { valid: true };
  }

  /**
   * Validates region rules for a trade
   */
  static validateRegionRules(
    offeredPlayers: Player[],
    requestedPlayers: Player[],
    regionRules?: {
      maxPlayersPerRegion?: number;
      allowCrossRegionTrades?: boolean;
    }
  ): { valid: boolean; reason?: string } {
    if (!regionRules) return { valid: true };

    // Check if cross-region trades are allowed
    if (regionRules.allowCrossRegionTrades === false) {
      const offeredRegions = new Set(offeredPlayers.map(p => p.region));
      const requestedRegions = new Set(requestedPlayers.map(p => p.region));

      const crossRegion = [...offeredRegions].some(region => !requestedRegions.has(region));

      if (crossRegion) {
        return {
          valid: false,
          reason: 'Cross-region trades are not allowed in this game'
        };
      }
    }

    return { valid: true };
  }

  /**
   * Validates trade balance rules
   */
  static validateTradeBalance(
    offeredValue: number,
    requestedValue: number,
    balanceRules?: {
      maxImbalancePercent?: number;
      requireExactBalance?: boolean;
    }
  ): { valid: boolean; reason?: string } {
    if (!balanceRules) return { valid: true };

    const totalValue = offeredValue + requestedValue;
    if (totalValue === 0) {
      return { valid: false, reason: 'Trade cannot have zero total value' };
    }

    if (balanceRules.requireExactBalance && offeredValue !== requestedValue) {
      return {
        valid: false,
        reason: 'Trade must be exactly balanced (equal player values)'
      };
    }

    if (balanceRules.maxImbalancePercent !== undefined) {
      const imbalancePercent = (Math.abs(offeredValue - requestedValue) / totalValue) * 100;

      if (imbalancePercent > balanceRules.maxImbalancePercent) {
        return {
          valid: false,
          reason: `Trade imbalance (${imbalancePercent.toFixed(1)}%) exceeds maximum allowed (${balanceRules.maxImbalancePercent}%)`
        };
      }
    }

    return { valid: true };
  }

  /**
   * Validates minimum players requirement
   */
  static validateMinimumPlayers(
    offeredPlayers: Player[],
    requestedPlayers: Player[],
    minPlayers = 1
  ): { valid: boolean; reason?: string } {
    if (offeredPlayers.length < minPlayers) {
      return {
        valid: false,
        reason: `You must offer at least ${minPlayers} player(s)`
      };
    }

    if (requestedPlayers.length < minPlayers) {
      return {
        valid: false,
        reason: `You must request at least ${minPlayers} player(s)`
      };
    }

    return { valid: true };
  }

  /**
   * Comprehensive trade validation
   */
  static validateTrade(
    offeredPlayers: Player[],
    requestedPlayers: Player[],
    rules?: {
      minPlayers?: number;
      maxImbalancePercent?: number;
      requireExactBalance?: boolean;
      maxPlayersPerRegion?: number;
      allowCrossRegionTrades?: boolean;
    }
  ): { valid: boolean; errors: string[] } {
    const errors: string[] = [];

    // Validate minimum players
    const minPlayersResult = this.validateMinimumPlayers(
      offeredPlayers,
      requestedPlayers,
      rules?.minPlayers
    );
    if (!minPlayersResult.valid && minPlayersResult.reason) {
      errors.push(minPlayersResult.reason);
    }

    // Validate balance
    const offeredValue = offeredPlayers.reduce((sum, p) => sum + (p.marketValue || 0), 0);
    const requestedValue = requestedPlayers.reduce((sum, p) => sum + (p.marketValue || 0), 0);

    const balanceResult = this.validateTradeBalance(offeredValue, requestedValue, {
      maxImbalancePercent: rules?.maxImbalancePercent,
      requireExactBalance: rules?.requireExactBalance
    });
    if (!balanceResult.valid && balanceResult.reason) {
      errors.push(balanceResult.reason);
    }

    // Validate region rules
    const regionResult = this.validateRegionRules(offeredPlayers, requestedPlayers, {
      maxPlayersPerRegion: rules?.maxPlayersPerRegion,
      allowCrossRegionTrades: rules?.allowCrossRegionTrades
    });
    if (!regionResult.valid && regionResult.reason) {
      errors.push(regionResult.reason);
    }

    return {
      valid: errors.length === 0,
      errors
    };
  }
}
