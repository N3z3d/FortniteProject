import { Injectable, inject } from '@angular/core';
import { TranslationService } from '../../../core/services/translation.service';
import { TradeOffer } from './trading.service';

export interface TradeTimeline {
  date: Date;
  action: string;
  actor: string;
  icon: string;
  description: string;
  status: 'completed' | 'current' | 'pending';
}

/**
 * TradeTimelineService
 *
 * Generates timeline entries for trade offers.
 * Extracted from TradeDetailsComponent to follow SRP.
 */
@Injectable({
  providedIn: 'root'
})
export class TradeTimelineService {
  private readonly t = inject(TranslationService);

  /**
   * Generates complete timeline for a trade offer
   */
  generateTradeTimeline(trade: TradeOffer): TradeTimeline[] {
    const timeline: TradeTimeline[] = [this.buildProposedTimelineEntry(trade)];

    if (trade.status === 'pending') {
      timeline.push(...this.buildPendingTimelineEntries(trade));
    } else {
      timeline.push(this.buildStatusTimelineEntry(trade));
    }

    return timeline.sort((a, b) => a.date.getTime() - b.date.getTime());
  }

  private buildProposedTimelineEntry(trade: TradeOffer): TradeTimeline {
    return {
      date: trade.createdAt,
      action: this.t.t('trades.details.timelineProposedAction'),
      actor: trade.fromUserName,
      icon: 'send',
      description: this.t.t('trades.details.timelineProposedDesc'),
      status: 'completed'
    };
  }

  private buildPendingTimelineEntries(trade: TradeOffer): TradeTimeline[] {
    return [
      {
        date: new Date(),
        action: this.t.t('trades.details.timelineAwaitingAction'),
        actor: trade.toUserName,
        icon: 'schedule',
        description: this.t.t('trades.details.timelineAwaitingDesc'),
        status: 'current'
      },
      {
        date: trade.expiresAt,
        action: this.t.t('trades.details.timelineExpiresAction'),
        actor: this.t.t('trades.details.system'),
        icon: 'access_time',
        description: this.t.t('trades.details.timelineExpiresDesc'),
        status: 'pending'
      }
    ];
  }

  private buildStatusTimelineEntry(trade: TradeOffer): TradeTimeline {
    const status = trade.status;

    return {
      date: trade.updatedAt,
      action: this.getStatusLabel(status),
      actor: this.getStatusTimelineActor(trade, status),
      icon: this.getStatusTimelineIcon(status),
      description: this.getStatusTimelineDescription(status),
      status: 'completed'
    };
  }

  private getStatusTimelineActor(trade: TradeOffer, status: string): string {
    if (status === 'withdrawn') return trade.fromUserName;
    if (status === 'expired') return this.t.t('trades.details.system');
    return trade.toUserName;
  }

  private getStatusTimelineIcon(status: string): string {
    switch (status) {
      case 'accepted':
        return 'check_circle';
      case 'rejected':
        return 'cancel';
      case 'withdrawn':
        return 'undo';
      case 'expired':
        return 'access_time';
      default:
        return 'help_outline';
    }
  }

  private getStatusTimelineDescription(status: string): string {
    switch (status) {
      case 'accepted':
        return this.t.t('trades.details.timelineAcceptedDesc');
      case 'rejected':
        return this.t.t('trades.details.timelineRejectedDesc');
      case 'withdrawn':
        return this.t.t('trades.details.timelineWithdrawnDesc');
      case 'expired':
        return this.t.t('trades.details.timelineExpiredDesc');
      default:
        return this.t.t('trades.details.timelineStatusUpdatedDesc');
    }
  }

  private getStatusLabel(status: string): string {
    const key = status?.toLowerCase() || '';
    return this.t.t(`trades.status.${key}`, status);
  }
}
