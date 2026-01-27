import { Injectable, inject } from '@angular/core';
import { TradingService, Player } from './trading.service';

/**
 * TradeBusinessService
 *
 * Extracted business logic from trade components to follow SRP.
 * Handles trade validation, balance calculation, and trade state management.
 */
@Injectable({
  providedIn: 'root'
})
export class TradeBusinessService {
  private readonly tradingService = inject(TradingService);

  /**
   * Validates if a trade proposal meets all requirements
   */
  validateTradeProposal(
    selectedTeam: any | null,
    offeredPlayers: Player[],
    requestedPlayers: Player[],
    isFormValid: boolean
  ): boolean {
    return (
      selectedTeam !== null &&
      offeredPlayers.length > 0 &&
      requestedPlayers.length > 0 &&
      isFormValid
    );
  }

  /**
   * Calculates trade balance percentage
   */
  calculateBalancePercentage(offered: Player[], requested: Player[]): number {
    if (offered.length === 0 && requested.length === 0) return 0;

    const offeredValue = this.calculateTotalValue(offered);
    const requestedValue = this.calculateTotalValue(requested);
    const totalValue = offeredValue + requestedValue;

    if (totalValue === 0) return 0;

    return Math.abs(offeredValue - requestedValue) / totalValue * 100;
  }

  /**
   * Calculates total market value of players
   */
  calculateTotalValue(players: Player[]): number {
    return players.reduce((sum, p) => sum + (p.marketValue || 0), 0);
  }

  /**
   * Calculates trade balance using TradingService
   */
  calculateTradeBalance(offeredPlayers: Player[], requestedPlayers: Player[]): number {
    return this.tradingService.calculateTradeBalance(offeredPlayers, requestedPlayers);
  }

  /**
   * Checks if trade is balanced according to trading service rules
   */
  isTradeBalanced(offeredPlayers: Player[], requestedPlayers: Player[]): boolean {
    return this.tradingService.isTradeBalanced(offeredPlayers, requestedPlayers);
  }

  /**
   * Validates if a player can be moved between lists
   */
  canMovePlayer(
    player: Player,
    fromList: string,
    toList: string,
    listTypes: {
      OFFERED_LIST: string;
      REQUESTED_LIST: string;
      AVAILABLE_LIST: string;
      TARGET_LIST: string;
    }
  ): boolean {
    // Can't move to the same type of list
    if (
      (fromList === listTypes.AVAILABLE_LIST || fromList === listTypes.OFFERED_LIST) &&
      (toList === listTypes.AVAILABLE_LIST || toList === listTypes.OFFERED_LIST)
    ) {
      return fromList !== toList;
    }

    if (
      (fromList === listTypes.TARGET_LIST || fromList === listTypes.REQUESTED_LIST) &&
      (toList === listTypes.TARGET_LIST || toList === listTypes.REQUESTED_LIST)
    ) {
      return fromList !== toList;
    }

    // Can't move between different team pools
    if (
      (fromList === listTypes.AVAILABLE_LIST || fromList === listTypes.OFFERED_LIST) &&
      (toList === listTypes.TARGET_LIST || toList === listTypes.REQUESTED_LIST)
    ) {
      return false;
    }

    if (
      (fromList === listTypes.TARGET_LIST || fromList === listTypes.REQUESTED_LIST) &&
      (toList === listTypes.AVAILABLE_LIST || toList === listTypes.OFFERED_LIST)
    ) {
      return false;
    }

    return true;
  }

  /**
   * Determines balance display class for UI
   */
  getBalanceDisplayClass(balance: number): string {
    if (balance > 0) return 'positive';
    if (balance < 0) return 'negative';
    return 'neutral';
  }

  /**
   * Determines balance icon for UI
   */
  getBalanceIcon(balance: number): string {
    if (balance > 0) return 'trending_up';
    if (balance < 0) return 'trending_down';
    return 'compare_arrows';
  }

  /**
   * Calculates trade fairness rating based on balance percentage
   */
  calculateFairnessRating(
    balancePercentage: number
  ): 'excellent' | 'good' | 'fair' | 'poor' {
    if (balancePercentage <= 5) return 'excellent';
    if (balancePercentage <= 15) return 'good';
    if (balancePercentage <= 25) return 'fair';
    return 'poor';
  }

  /**
   * Calculates comprehensive trade statistics
   */
  calculateTradeStats(offeredPlayers: Player[], requestedPlayers: Player[]) {
    const totalPlayers = offeredPlayers.length + requestedPlayers.length;
    const offeredValue = this.calculateTotalValue(offeredPlayers);
    const requestedValue = this.calculateTotalValue(requestedPlayers);
    const totalValue = offeredValue + requestedValue;
    const avgPlayerValue = totalPlayers > 0 ? totalValue / totalPlayers : 0;
    const balancePercentage =
      totalValue > 0 ? (Math.abs(offeredValue - requestedValue) / totalValue) * 100 : 0;
    const fairnessRating = this.calculateFairnessRating(balancePercentage);

    return {
      totalPlayers,
      totalValue,
      offeredValue,
      requestedValue,
      avgPlayerValue,
      balancePercentage,
      fairnessRating
    };
  }

  /**
   * Formats currency for display
   */
  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(value);
  }
}
