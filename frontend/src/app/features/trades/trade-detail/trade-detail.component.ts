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
  template: `
    <div class="trade-detail-container">
      <div class="loading-container" *ngIf="isLoading">
        <mat-spinner diameter="50"></mat-spinner>
        <p>{{ t.t('trades.detail.loading') }}</p>
      </div>

      <div class="trade-content" *ngIf="!isLoading && trade">
        <!-- Header Card -->
        <mat-card class="header-card">
          <mat-card-header>
            <mat-card-title>
              <mat-icon>swap_horiz</mat-icon>
              {{ t.t('trades.detail.titlePrefix') }}{{ trade.id.substring(0, 8) }}
            </mat-card-title>
            <mat-card-subtitle>
              <mat-chip [color]="getStatusColor(trade.status)">
                {{ getStatusLabel(trade.status) }}
              </mat-chip>
            </mat-card-subtitle>
          </mat-card-header>

          <mat-card-actions>
            <button mat-button (click)="goBack()">
              <mat-icon>arrow_back</mat-icon>
              {{ t.t('trades.detail.back') }}
            </button>
            
            <button 
              mat-raised-button 
              color="primary"
              *ngIf="trade.status === 'PENDING'"
              (click)="completeTrade()">
              <mat-icon>check</mat-icon>
              {{ t.t('trades.detail.validateTrade') }}
            </button>
            
            <button 
              mat-raised-button 
              color="warn"
              *ngIf="trade.status === 'PENDING'"
              (click)="cancelTrade()">
              <mat-icon>cancel</mat-icon>
              {{ t.t('trades.detail.cancelTrade') }}
            </button>
          </mat-card-actions>
        </mat-card>

        <!-- Trade Details Card -->
        <mat-card class="trade-details-card">
          <mat-card-header>
            <mat-card-title>{{ t.t('trades.detail.detailsTitle') }}</mat-card-title>
          </mat-card-header>

          <mat-card-content>
            <div class="trade-flow">
              <!-- Player Out -->
              <div class="player-card player-out">
                <div class="player-header">
                  <mat-icon color="warn">person_remove</mat-icon>
                  <h3>{{ t.t('trades.detail.playerOutTitle') }}</h3>
                </div>
                <div class="player-info">
                  <h4>{{ trade.playerOut.username }}</h4>
                  <p class="region">{{ trade.playerOut.region }}</p>
                  
                  <div class="player-stats" *ngIf="trade.playerOut.stats">
                    <div class="stat">
                      <span class="stat-label">{{ t.t('trades.detail.stats.kills') }}:</span>
                      <span class="stat-value">{{ trade.playerOut.stats.kills }}</span>
                    </div>
                    <div class="stat">
                      <span class="stat-label">{{ t.t('trades.detail.stats.wins') }}:</span>
                      <span class="stat-value">{{ trade.playerOut.stats.wins }}</span>
                    </div>
                    <div class="stat">
                      <span class="stat-label">{{ t.t('trades.detail.stats.kd') }}:</span>
                      <span class="stat-value">{{ trade.playerOut.stats.kd }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Trade Arrow -->
              <div class="trade-arrow-container">
                <mat-icon class="trade-arrow">swap_horiz</mat-icon>
              </div>

              <!-- Player In -->
              <div class="player-card player-in">
                <div class="player-header">
                  <mat-icon color="primary">person_add</mat-icon>
                  <h3>{{ t.t('trades.detail.playerInTitle') }}</h3>
                </div>
                <div class="player-info">
                  <h4>{{ trade.playerIn.username }}</h4>
                  <p class="region">{{ trade.playerIn.region }}</p>
                  
                  <div class="player-stats" *ngIf="trade.playerIn.stats">
                    <div class="stat">
                      <span class="stat-label">{{ t.t('trades.detail.stats.kills') }}:</span>
                      <span class="stat-value">{{ trade.playerIn.stats.kills }}</span>
                    </div>
                    <div class="stat">
                      <span class="stat-label">{{ t.t('trades.detail.stats.wins') }}:</span>
                      <span class="stat-value">{{ trade.playerIn.stats.wins }}</span>
                    </div>
                    <div class="stat">
                      <span class="stat-label">{{ t.t('trades.detail.stats.kd') }}:</span>
                      <span class="stat-value">{{ trade.playerIn.stats.kd }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <mat-divider></mat-divider>

            <!-- Team Information -->
            <div class="team-section">
              <h3>
                <mat-icon>group</mat-icon>
                {{ t.t('trades.detail.teamSectionTitle') }}
              </h3>
              <div class="team-info">
                <h4>{{ trade.team.name }}</h4>
                <p>{{ t.t('trades.detail.teamOwnerLabel') }}: <strong>{{ trade.team.owner }}</strong></p>
              </div>
            </div>

            <mat-divider></mat-divider>

            <!-- Timeline -->
            <div class="timeline-section">
              <h3>
                <mat-icon>timeline</mat-icon>
                {{ t.t('trades.detail.timelineTitle') }}
              </h3>
              <div class="timeline">
                <div class="timeline-item completed">
                  <mat-icon>add</mat-icon>
                  <div class="timeline-content">
                    <strong>{{ t.t('trades.detail.timelineCreated') }}</strong>
                    <p>{{ trade.createdAt | date:'dd/MM/yyyy à HH:mm' }}</p>
                  </div>
                </div>
                
                <div 
                  class="timeline-item"
                  [ngClass]="{ 'completed': trade.status === 'COMPLETED' || trade.status === 'CANCELLED' }">
                  <mat-icon>
                    {{ trade.status === 'COMPLETED' ? 'check' : trade.status === 'CANCELLED' ? 'cancel' : 'schedule' }}
                  </mat-icon>
                  <div class="timeline-content">
                    <strong>{{ getTimelineLabel(trade.status) }}</strong>
                    <p *ngIf="trade.completedAt">{{ trade.completedAt | date:'dd/MM/yyyy à HH:mm' }}</p>
                    <p *ngIf="!trade.completedAt && trade.status === 'PENDING'">{{ t.t('trades.status.pending') }}</p>
                  </div>
                </div>
              </div>
            </div>

            <!-- Reason (if cancelled) -->
            <div class="reason-section" *ngIf="trade.status === 'CANCELLED' && trade.reason">
              <mat-divider></mat-divider>
              <h3>
                <mat-icon>info</mat-icon>
                {{ t.t('trades.detail.cancelReasonTitle') }}
              </h3>
              <p class="reason-text">{{ trade.reason }}</p>
            </div>
          </mat-card-content>
        </mat-card>
      </div>

      <!-- Error State -->
      <mat-card class="error-card" *ngIf="!isLoading && !trade">
        <mat-card-content>
          <div class="error-content">
            <mat-icon color="warn">error</mat-icon>
            <h3>{{ t.t('trades.detail.notFoundTitle') }}</h3>
            <p>{{ t.t('trades.detail.notFoundDesc') }}</p>
            <button mat-raised-button color="primary" (click)="goBack()">
              {{ t.t('trades.detail.backToList') }}
            </button>
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .trade-detail-container {
      padding: 2rem;
      max-width: 1000px;
      margin: 0 auto;
    }

    .loading-container {
      text-align: center;
      padding: 3rem 0;
    }

    .loading-container p {
      margin-top: 1rem;
      color: rgba(0, 0, 0, 0.6);
    }

    .header-card {
      margin-bottom: 2rem;
    }

    .trade-details-card {
      width: 100%;
    }

    .trade-flow {
      display: flex;
      align-items: center;
      gap: 2rem;
      margin: 2rem 0;
    }

    .player-card {
      flex: 1;
      padding: 1.5rem;
      border: 2px solid #e0e0e0;
      border-radius: 12px;
      background-color: #fafafa;
    }

    .player-out {
      border-color: #f44336;
      background-color: #ffebee;
    }

    .player-in {
      border-color: #2196f3;
      background-color: #e3f2fd;
    }

    .player-header {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-bottom: 1rem;
    }

    .player-header h3 {
      margin: 0;
      color: rgba(0, 0, 0, 0.8);
    }

    .player-info h4 {
      margin: 0 0 0.5rem 0;
      font-size: 1.3rem;
    }

    .region {
      color: rgba(0, 0, 0, 0.6);
      font-style: italic;
      margin: 0 0 1rem 0;
    }

    .player-stats {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .stat {
      display: flex;
      justify-content: space-between;
    }

    .stat-label {
      color: rgba(0, 0, 0, 0.6);
    }

    .stat-value {
      font-weight: 500;
    }

    .trade-arrow-container {
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .trade-arrow {
      font-size: 3rem;
      height: 3rem;
      width: 3rem;
      color: #2196f3;
    }

    .team-section, .timeline-section, .reason-section {
      margin: 2rem 0;
    }

    .team-section h3, .timeline-section h3, .reason-section h3 {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-bottom: 1rem;
    }

    .team-info h4 {
      margin: 0.5rem 0;
    }

    .timeline {
      padding-left: 1rem;
    }

    .timeline-item {
      display: flex;
      align-items: flex-start;
      gap: 1rem;
      margin: 1rem 0;
      opacity: 0.5;
    }

    .timeline-item.completed {
      opacity: 1;
    }

    .timeline-item mat-icon {
      margin-top: 0.1rem;
    }

    .timeline-content strong {
      display: block;
      margin-bottom: 0.25rem;
    }

    .reason-text {
      background-color: #ffebee;
      padding: 1rem;
      border-radius: 8px;
      border-left: 4px solid #f44336;
      margin: 1rem 0;
    }

    .error-card {
      margin-top: 2rem;
    }

    .error-content {
      text-align: center;
      padding: 2rem;
    }

    .error-content mat-icon {
      font-size: 4rem;
      height: 4rem;
      width: 4rem;
    }

    mat-divider {
      margin: 2rem 0;
    }

    mat-card-header mat-icon {
      margin-right: 0.5rem;
    }

    @media (max-width: 768px) {
      .trade-detail-container {
        padding: 1rem;
      }

      .trade-flow {
        flex-direction: column;
        gap: 1rem;
      }

      .trade-arrow {
        transform: rotate(90deg);
      }

      .player-card {
        width: 100%;
      }
    }
  `]
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


