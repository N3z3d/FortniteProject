import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router } from '@angular/router';
import { LoggerService } from '../../../core/services/logger.service';
import { TranslationService } from '../../../core/services/translation.service';

interface TradeDetail {
  id: string;
  playerOut: {
    id: string;
    username: string;
    region: string;
    stats?: {
      kills: number;
      wins: number;
      kd: number;
    };
  };
  playerIn: {
    id: string;
    username: string;
    region: string;
    stats?: {
      kills: number;
      wins: number;
      kd: number;
    };
  };
  team: {
    id: string;
    name: string;
    owner: string;
  };
  createdAt: Date;
  completedAt?: Date;
  status: 'PENDING' | 'COMPLETED' | 'CANCELLED';
  reason?: string;
}

@Component({
  selector: 'app-trade-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './trade-detail.component.html',
  styleUrls: ['./trade-detail.component.scss']
})
export class TradeDetailComponent implements OnInit {
  trade: TradeDetail | null = null;
  isLoading = true;
  tradeId: string = '';
  public readonly t = inject(TranslationService);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private logger: LoggerService
  ) {}

  ngOnInit(): void {
    this.tradeId = this.route.snapshot.paramMap.get('id') || '';
    this.loadTradeDetail();
  }

  private loadTradeDetail(): void {
    // Simulate API call
    setTimeout(() => {
      this.trade = this.getMockTradeDetail(this.tradeId);
      this.isLoading = false;
    }, 1000);
  }

  private getMockTradeDetail(id: string): TradeDetail | null {
    const mockTrades = {
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
        status: 'PENDING' as const
      }
    };

    return mockTrades[id as keyof typeof mockTrades] || null;
  }

  completeTrade(): void {
    this.logger.debug('TradeDetail: completing trade', { tradeId: this.tradeId });
    // Implement complete trade logic
  }

  cancelTrade(): void {
    this.logger.debug('TradeDetail: cancelling trade', { tradeId: this.tradeId });
    // Implement cancel trade logic
  }

  goBack(): void {
    this.router.navigate(['/trades']);
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'PENDING': return 'accent';
      case 'COMPLETED': return 'primary';
      case 'CANCELLED': return 'warn';
      default: return '';
    }
  }

  getStatusLabel(status: string): string {
    const statusKey = status.toLowerCase();
    const statusMap: Record<string, string> = {
      pending: 'trades.status.pending',
      completed: 'trades.status.completed',
      cancelled: 'trades.status.cancelled'
    };

    const key = statusMap[statusKey] || `trades.status.${statusKey}`;
    return this.t.t(key, status);
  }

  getTimelineLabel(status: string): string {
    switch (status) {
      case 'PENDING': return this.t.t('trades.detail.timelinePending');
      case 'COMPLETED': return this.t.t('trades.detail.timelineCompleted');
      case 'CANCELLED': return this.t.t('trades.detail.timelineCancelled');
      default: return status;
    }
  }
}


