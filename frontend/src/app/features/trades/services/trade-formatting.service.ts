import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class TradeFormattingService {
  getBalanceDisplayClass(balance: number): string {
    if (balance > 0) return 'positive';
    if (balance < 0) return 'negative';
    return 'neutral';
  }

  getBalanceIcon(balance: number): string {
    if (balance > 0) return 'trending_up';
    if (balance < 0) return 'trending_down';
    return 'compare_arrows';
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(value);
  }
}
