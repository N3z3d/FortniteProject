import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterModule } from '@angular/router';
import { LoggerService } from '../../../core/services/logger.service';

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
export class TradeListComponent implements OnInit {
  trades: Trade[] = [];
  filteredTrades: Trade[] = [];
  isLoading = true;
  searchTerm = '';
  activeTab: 'ALL' | 'PENDING' | 'HISTORY' = 'ALL';

  constructor(
    private router: Router,
    private logger: LoggerService
  ) { }

  ngOnInit(): void {
    this.loadTrades();
  }

  private loadTrades(): void {
    // Simulate API call with mock data
    setTimeout(() => {
      this.trades = this.getMockTrades();
      this.applyFilters();
      this.isLoading = false;
    }, 1000);
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
    switch (status) {
      case 'PENDING': return 'En cours';
      case 'COMPLETED': return 'Terminé';
      case 'CANCELLED': return 'Annulé';
      default: return status;
    }
  }
}
