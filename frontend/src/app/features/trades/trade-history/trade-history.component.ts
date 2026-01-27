import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslationService } from '../../../core/services/translation.service';

export interface TradeHistoryItem {
  id: string;
  playerOut: {
    id: string;
    username: string;
    region: string;
  };
  playerIn: {
    id: string;
    username: string;
    region: string;
  };
  team: {
    id: string;
    name: string;
    owner: string;
  };
  createdAt: Date;
  completedAt?: Date;
  status: 'PENDING' | 'COMPLETED' | 'CANCELLED';
  completedBy?: string;
}

@Component({
  selector: 'app-trade-history',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatSelectModule,
    MatDatepickerModule,
    MatInputModule,
    MatTooltipModule
  ],
  templateUrl: './trade-history.component.html',
  styleUrls: ['./trade-history.component.scss']
})
export class TradeHistoryComponent implements OnInit {
  allTrades: TradeHistoryItem[] = [];
  filteredTrades: TradeHistoryItem[] = [];
  isLoading = true;
  filtersForm: FormGroup;
  displayedColumns = ['status', 'details', 'team', 'createdAt', 'completedAt', 'actions'];
  public readonly t = inject(TranslationService);

  availableTeams = [
    { id: '1', name: 'Team Alpha' },
    { id: '2', name: 'Team Beta' },
    { id: '3', name: 'Team Gamma' }
  ];

  constructor(
    private fb: FormBuilder,
    private router: Router
  ) {
    this.filtersForm = this.fb.group({
      status: [''],
      team: [''],
      player: ['']
    });
  }

  ngOnInit(): void {
    this.loadTradeHistory();
  }

  private loadTradeHistory(): void {
    // Simulate API call
    setTimeout(() => {
      this.allTrades = this.getMockTradeHistory();
      this.filteredTrades = [...this.allTrades];
      this.isLoading = false;
    }, 1000);
  }

  private getMockTradeHistory(): TradeHistoryItem[] {
    return [
      {
        id: '1',
        playerOut: { id: '1', username: 'Ninja', region: 'NA-EAST' },
        playerIn: { id: '2', username: 'Tfue', region: 'NA-WEST' },
        team: { id: '1', name: 'Team Alpha', owner: 'Thibaut' },
        createdAt: new Date('2024-01-15T10:30:00'),
        completedAt: new Date('2024-01-15T14:20:00'),
        status: 'COMPLETED',
        completedBy: 'Admin'
      },
      {
        id: '2',
        playerOut: { id: '3', username: 'Bugha', region: 'NA-EAST' },
        playerIn: { id: '4', username: 'Aqua', region: 'EU' },
        team: { id: '2', name: 'Team Beta', owner: 'Marcel' },
        createdAt: new Date('2024-01-14T14:20:00'),
        status: 'PENDING'
      },
      {
        id: '3',
        playerOut: { id: '5', username: 'Mongraal', region: 'EU' },
        playerIn: { id: '6', username: 'Benjyfishy', region: 'EU' },
        team: { id: '3', name: 'Team Gamma', owner: 'Sarah' },
        createdAt: new Date('2024-01-13T09:15:00'),
        completedAt: new Date('2024-01-13T09:30:00'),
        status: 'CANCELLED',
        completedBy: 'User'
      }
    ];
  }

  applyFilters(): void {
    const filters = this.filtersForm.value;
    
    this.filteredTrades = this.allTrades.filter(trade => {
      const statusMatch = !filters.status || trade.status === filters.status;
      const teamMatch = !filters.team || trade.team.id === filters.team;
      const playerMatch = !filters.player || 
        trade.playerOut.username.toLowerCase().includes(filters.player.toLowerCase()) ||
        trade.playerIn.username.toLowerCase().includes(filters.player.toLowerCase());
      
      return statusMatch && teamMatch && playerMatch;
    });
  }

  clearFilters(): void {
    this.filtersForm.reset();
    this.filteredTrades = [...this.allTrades];
  }

  hasActiveFilters(): boolean {
    const values = this.filtersForm.value;
    return values.status || values.team || values.player;
  }

  viewTrade(tradeId: string): void {
    this.router.navigate(['/trades', tradeId]);
  }

  createNewTrade(): void {
    this.router.navigate(['/trades/new']);
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

  getTotalTrades(): number {
    return this.allTrades.length;
  }

  getCompletedTrades(): number {
    return this.allTrades.filter(trade => trade.status === 'COMPLETED').length;
  }

  getPendingTrades(): number {
    return this.allTrades.filter(trade => trade.status === 'PENDING').length;
  }

  getCancelledTrades(): number {
    return this.allTrades.filter(trade => trade.status === 'CANCELLED').length;
  }
}


