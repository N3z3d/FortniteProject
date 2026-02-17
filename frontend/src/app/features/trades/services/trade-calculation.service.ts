import { Injectable, inject } from '@angular/core';
import { Player, TradingService } from './trading.service';

@Injectable({
  providedIn: 'root'
})
export class TradeCalculationService {
  private readonly tradingService = inject(TradingService);

  calculateBalancePercentage(offered: Player[], requested: Player[]): number {
    if (offered.length === 0 && requested.length === 0) return 0;

    const offeredValue = this.calculateTotalValue(offered);
    const requestedValue = this.calculateTotalValue(requested);
    const totalValue = offeredValue + requestedValue;

    if (totalValue === 0) return 0;

    return Math.abs(offeredValue - requestedValue) / totalValue * 100;
  }

  calculateTotalValue(players: Player[]): number {
    return players.reduce((sum, p) => sum + (p.marketValue || 0), 0);
  }

  calculateTradeBalance(offeredPlayers: Player[], requestedPlayers: Player[]): number {
    return this.tradingService.calculateTradeBalance(offeredPlayers, requestedPlayers);
  }

  isTradeBalanced(offeredPlayers: Player[], requestedPlayers: Player[]): boolean {
    return this.tradingService.isTradeBalanced(offeredPlayers, requestedPlayers);
  }

  calculateFairnessRating(
    balancePercentage: number
  ): 'excellent' | 'good' | 'fair' | 'poor' {
    if (balancePercentage <= 5) return 'excellent';
    if (balancePercentage <= 15) return 'good';
    if (balancePercentage <= 25) return 'fair';
    return 'poor';
  }

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
}
