import { Injectable, inject } from '@angular/core';
import { TranslationService } from '../../../core/services/translation.service';
import { TradeDetail, TradeDetailStatus } from '../models/trade-detail.model';

@Injectable({
  providedIn: 'root'
})
export class TradeDetailService {
  private readonly t = inject(TranslationService);

  getTradeDetail(id: string): TradeDetail | null {
    return this.mockTrades[id] ?? null;
  }

  getStatusColor(status: TradeDetailStatus | string): string {
    switch (status) {
      case 'PENDING':
        return 'accent';
      case 'COMPLETED':
        return 'primary';
      case 'CANCELLED':
        return 'warn';
      default:
        return '';
    }
  }

  getStatusLabel(status: TradeDetailStatus | string): string {
    const statusKey = status?.toLowerCase() || '';
    const key = `trades.status.${statusKey}`;
    return this.t.t(key, status);
  }

  getTimelineLabel(status: TradeDetailStatus | string): string {
    switch (status) {
      case 'PENDING':
        return this.t.t('trades.detail.timelinePending');
      case 'COMPLETED':
        return this.t.t('trades.detail.timelineCompleted');
      case 'CANCELLED':
        return this.t.t('trades.detail.timelineCancelled');
      default:
        return status;
    }
  }

  private readonly mockTrades: Record<string, TradeDetail> = {
    '1': {
      id: '1',
      playerOut: {
        id: '1',
        username: 'Ninja',
        region: 'NA-EAST',
        stats: { kills: 15420, wins: 892, kd: 2.8 }
      },
      playerIn: {
        id: '2',
        username: 'Tfue',
        region: 'NA-WEST',
        stats: { kills: 18340, wins: 1124, kd: 3.2 }
      },
      team: {
        id: '1',
        name: 'Team Alpha',
        owner: 'Thibaut'
      },
      createdAt: new Date('2024-01-15T10:30:00'),
      status: 'PENDING'
    }
  };
}
