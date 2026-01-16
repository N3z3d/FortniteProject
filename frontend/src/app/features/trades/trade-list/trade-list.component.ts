import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { LoggerService } from '../../../core/services/logger.service';
import { TranslationService } from '../../../core/services/translation.service';
import { WebSocketService, TradeNotification } from '../../../core/services/websocket.service';
import { TradingService, TradeOffer } from '../services/trading.service';

export interface Trade {
  id: string;
  playerOut: {
    id: string;
    username: string;
    region?: string;
  };
  playerIn: {
    id: string;
    username: string;
    region?: string;
  };
  team: {
    id: string;
    name: string;
    owner: string;
  };
  createdAt: Date;
  status: 'PENDING' | 'COMPLETED' | 'CANCELLED';
}

@Component({
  selector: 'app-trade-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './trade-list.component.html',
  styleUrls: ['./trade-list.component.scss']
})
export class TradeListComponent implements OnInit, OnDestroy {
  trades: Trade[] = [];
  filteredTrades: Trade[] = [];
  isLoading = true;
  searchTerm = '';
  activeTab: 'ALL' | 'PENDING' | 'HISTORY' = 'ALL';

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly router: Router,
    private readonly logger: LoggerService,
    private readonly tradingService: TradingService,
    private readonly translationService: TranslationService,
    private readonly webSocketService: WebSocketService
  ) { }

  ngOnInit(): void {
    this.loadTrades();
    this.subscribeToTradeNotifications();
  }

  private subscribeToTradeNotifications(): void {
    this.webSocketService.connect();

    this.webSocketService.tradeNotifications.pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (notification: TradeNotification) => {
        this.handleTradeNotification(notification);
      },
      error: (error) => {
        this.logger.error('TradeList: WebSocket notification error', error);
      }
    });
  }

  private handleTradeNotification(notification: TradeNotification): void {
    this.logger.debug('TradeList: received trade notification', notification);

    switch (notification.type) {
      case 'TRADE_PROPOSED':
        this.loadTrades();
        break;
      case 'TRADE_ACCEPTED':
      case 'TRADE_REJECTED':
      case 'TRADE_CANCELLED':
      case 'TRADE_COUNTERED':
        this.updateTradeStatus(notification.tradeId, notification.status);
        break;
    }
  }

  private updateTradeStatus(tradeId: string, status: string): void {
    const trade = this.trades.find(t => t.id === tradeId);
    if (trade) {
      trade.status = this.mapOfferStatus(status.toLowerCase());
      this.applyFilters();
    } else {
      this.loadTrades();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadTrades(): void {
    this.isLoading = true;

    this.tradingService.getTrades().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (tradeOffers: TradeOffer[]) => {
        this.trades = this.mapTradeOffersToTrades(tradeOffers);
        this.applyFilters();
        this.isLoading = false;
      },
      error: (error) => {
        this.logger.error('TradeList: failed to load trades', error);
        // Fallback to mock data if API fails
        this.trades = this.getMockTrades();
        this.applyFilters();
        this.isLoading = false;
      }
    });
  }

  private mapTradeOffersToTrades(offers: TradeOffer[]): Trade[] {
    return offers.map(offer => ({
      id: offer.id,
      playerOut: {
        id: offer.offeredPlayers[0]?.id || '',
        username: offer.offeredPlayers[0]?.name || '',
        region: offer.offeredPlayers[0]?.region
      },
      playerIn: {
        id: offer.requestedPlayers[0]?.id || '',
        username: offer.requestedPlayers[0]?.name || '',
        region: offer.requestedPlayers[0]?.region
      },
      team: {
        id: offer.fromTeamId,
        name: offer.fromTeamName,
        owner: offer.fromUserName
      },
      createdAt: offer.createdAt,
      status: this.mapOfferStatus(offer.status)
    }));
  }

  private mapOfferStatus(status: string): 'PENDING' | 'COMPLETED' | 'CANCELLED' {
    switch (status) {
      case 'pending': return 'PENDING';
      case 'accepted': return 'COMPLETED';
      case 'rejected':
      case 'withdrawn':
      case 'expired':
        return 'CANCELLED';
      default: return 'PENDING';
    }
  }

  applyFilters(): void {
    let filtered = this.trades;

    // Filter by Tab
    if (this.activeTab === 'PENDING') {
      filtered = filtered.filter(t => t.status === 'PENDING');
    } else if (this.activeTab === 'HISTORY') {
      filtered = filtered.filter(t => t.status !== 'PENDING');
    }

    // Filter by Search
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(t =>
        t.playerOut.username.toLowerCase().includes(term) ||
        t.playerIn.username.toLowerCase().includes(term) ||
        t.team.name.toLowerCase().includes(term)
      );
    }

    this.filteredTrades = filtered;
  }

  setTab(tab: 'ALL' | 'PENDING' | 'HISTORY'): void {
    this.activeTab = tab;
    this.applyFilters();
  }

  getActiveTradesCount(): number {
    return this.trades.filter(t => t.status === 'PENDING').length;
  }

  getPendingCount(): number {
    return this.trades.filter(t => t.status === 'PENDING').length;
  }

  trackByTradeId(index: number, trade: Trade): string {
    return trade.id;
  }

  private getMockTrades(): Trade[] {
    return [
      {
        id: '1',
        playerOut: { id: '1', username: 'Ninja', region: 'NA-EAST' },
        playerIn: { id: '2', username: 'Tfue', region: 'NA-WEST' },
        team: { id: '1', name: 'Team Alpha', owner: 'Thibaut' },
        createdAt: new Date('2024-01-15T10:30:00'),
        status: 'PENDING'
      },
      {
        id: '2',
        playerOut: { id: '3', username: 'Bugha', region: 'NA-EAST' },
        playerIn: { id: '4', username: 'Aqua', region: 'EU' },
        team: { id: '2', name: 'Team Beta', owner: 'Marcel' },
        createdAt: new Date('2024-01-14T14:20:00'),
        status: 'COMPLETED'
      },
      {
        id: '3',
        playerOut: { id: '5', username: 'Mongraal', region: 'EU' },
        playerIn: { id: '6', username: 'Benjyfishy', region: 'EU' },
        team: { id: '1', name: 'Team Alpha', owner: 'Thibaut' },
        createdAt: new Date('2024-01-13T09:15:00'),
        status: 'CANCELLED'
      }
    ];
  }

  createNewTrade(): void {
    this.router.navigate(['/trades/new']);
  }

  viewTrade(tradeId: string): void {
    this.router.navigate(['/trades', tradeId]);
  }

  cancelTrade(tradeId: string): void {
    this.logger.debug('TradeList: cancelling trade', { tradeId });
    const trade = this.trades.find(t => t.id === tradeId);
    if (trade) {
      trade.status = 'CANCELLED';
      this.applyFilters();
    }
  }

  getStatusLabel(status: string): string {
    const statusKey = status.toLowerCase();
    const translationKey = `trades.status.${statusKey}`;

    // Map legacy status values to translation keys
    const statusMap: Record<string, string> = {
      'pending': 'trades.status.pending',
      'completed': 'trades.status.accepted',
      'cancelled': 'trades.status.withdrawn'
    };

    const key = statusMap[statusKey] || translationKey;
    return this.translationService.t(key, status);
  }
}
