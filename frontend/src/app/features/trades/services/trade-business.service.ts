import { Injectable, inject } from '@angular/core';
import { Player } from './trading.service';
import { TradeCalculationService } from './trade-calculation.service';
import { TradeFormattingService } from './trade-formatting.service';
import { TradeValidationService } from './trade-validation.service';

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
  private readonly tradeValidationService = inject(TradeValidationService);
  private readonly tradeCalculationService = inject(TradeCalculationService);
  private readonly tradeFormattingService = inject(TradeFormattingService);

  /**
   * Validates if a trade proposal meets all requirements
   */
  validateTradeProposal(
    selectedTeam: any | null,
    offeredPlayers: Player[],
    requestedPlayers: Player[],
    isFormValid: boolean
  ): boolean {
    return this.tradeValidationService.validateTradeProposal(
      selectedTeam,
      offeredPlayers,
      requestedPlayers,
      isFormValid
    );
  }

  /**
   * Calculates trade balance percentage
   */
  calculateBalancePercentage(offered: Player[], requested: Player[]): number {
    return this.tradeCalculationService.calculateBalancePercentage(offered, requested);
  }

  /**
   * Calculates total market value of players
   */
  calculateTotalValue(players: Player[]): number {
    return this.tradeCalculationService.calculateTotalValue(players);
  }

  /**
   * Calculates trade balance using TradingService
   */
  calculateTradeBalance(offeredPlayers: Player[], requestedPlayers: Player[]): number {
    return this.tradeCalculationService.calculateTradeBalance(offeredPlayers, requestedPlayers);
  }

  /**
   * Checks if trade is balanced according to trading service rules
   */
  isTradeBalanced(offeredPlayers: Player[], requestedPlayers: Player[]): boolean {
    return this.tradeCalculationService.isTradeBalanced(offeredPlayers, requestedPlayers);
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
    return this.tradeValidationService.canMovePlayer(
      player,
      fromList,
      toList,
      listTypes
    );
  }

  /**
   * Determines balance display class for UI
   */
  getBalanceDisplayClass(balance: number): string {
    return this.tradeFormattingService.getBalanceDisplayClass(balance);
  }

  /**
   * Determines balance icon for UI
   */
  getBalanceIcon(balance: number): string {
    return this.tradeFormattingService.getBalanceIcon(balance);
  }

  /**
   * Calculates trade fairness rating based on balance percentage
   */
  calculateFairnessRating(
    balancePercentage: number
  ): 'excellent' | 'good' | 'fair' | 'poor' {
    return this.tradeCalculationService.calculateFairnessRating(balancePercentage);
  }

  /**
   * Calculates comprehensive trade statistics
   */
  calculateTradeStats(offeredPlayers: Player[], requestedPlayers: Player[]) {
    return this.tradeCalculationService.calculateTradeStats(offeredPlayers, requestedPlayers);
  }

  /**
   * Formats currency for display
   */
  formatCurrency(value: number): string {
    return this.tradeFormattingService.formatCurrency(value);
  }
}
