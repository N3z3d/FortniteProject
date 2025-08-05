import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { Router } from '@angular/router';

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
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  template: `
    <div class="trade-list-container">
      <mat-card class="header-card">
        <mat-card-header>
          <mat-card-title>
            <mat-icon>swap_horiz</mat-icon>
            Gestion des Échanges
          </mat-card-title>
          <mat-card-subtitle>
            Gérez les échanges de joueurs entre équipes
          </mat-card-subtitle>
        </mat-card-header>
        
        <mat-card-actions>
          <button mat-raised-button color="primary" (click)="createNewTrade()">
            <mat-icon>add</mat-icon>
            Nouveau Trade
          </button>
          <button mat-button (click)="viewHistory()">
            <mat-icon>history</mat-icon>
            Historique
          </button>
        </mat-card-actions>
      </mat-card>

      <mat-card class="trades-card">
        <mat-card-header>
          <mat-card-title>Trades en cours</mat-card-title>
        </mat-card-header>

        <mat-card-content>
          <div class="loading-container" *ngIf="isLoading">
            <mat-spinner diameter="50"></mat-spinner>
            <p>Chargement des trades...</p>
          </div>

          <div class="no-trades" *ngIf="!isLoading && trades.length === 0">
            <mat-icon>swap_horiz</mat-icon>
            <h3>Aucun trade en cours</h3>
            <p>Commencez par créer votre premier échange de joueurs</p>
            <button mat-raised-button color="primary" (click)="createNewTrade()">
              <mat-icon>add</mat-icon>
              Créer un Trade
            </button>
          </div>

          <div class="trades-table" *ngIf="!isLoading && trades.length > 0">
            <table mat-table [dataSource]="trades" class="full-width-table">
              <!-- Status Column -->
              <ng-container matColumnDef="status">
                <th mat-header-cell *matHeaderCellDef>Statut</th>
                <td mat-cell *matCellDef="let trade">
                  <mat-chip 
                    [color]="getStatusColor(trade.status)">
                    {{ getStatusLabel(trade.status) }}
                  </mat-chip>
                </td>
              </ng-container>

              <!-- Trade Details Column -->
              <ng-container matColumnDef="details">
                <th mat-header-cell *matHeaderCellDef>Échange</th>
                <td mat-cell *matCellDef="let trade" class="trade-details">
                  <div class="trade-info">
                    <div class="player-out">
                      <span class="player-label">Sort:</span>
                      <strong>{{ trade.playerOut.username }}</strong>
                      <span class="region" *ngIf="trade.playerOut.region">({{ trade.playerOut.region }})</span>
                    </div>
                    <mat-icon class="trade-arrow">arrow_forward</mat-icon>
                    <div class="player-in">
                      <span class="player-label">Entre:</span>
                      <strong>{{ trade.playerIn.username }}</strong>
                      <span class="region" *ngIf="trade.playerIn.region">({{ trade.playerIn.region }})</span>
                    </div>
                  </div>
                </td>
              </ng-container>

              <!-- Team Column -->
              <ng-container matColumnDef="team">
                <th mat-header-cell *matHeaderCellDef>Équipe</th>
                <td mat-cell *matCellDef="let trade">
                  <div class="team-info">
                    <strong>{{ trade.team.name }}</strong>
                    <div class="team-owner">{{ trade.team.owner }}</div>
                  </div>
                </td>
              </ng-container>

              <!-- Date Column -->
              <ng-container matColumnDef="date">
                <th mat-header-cell *matHeaderCellDef>Date</th>
                <td mat-cell *matCellDef="let trade">
                  {{ trade.createdAt | date:'dd/MM/yyyy HH:mm' }}
                </td>
              </ng-container>

              <!-- Actions Column -->
              <ng-container matColumnDef="actions">
                <th mat-header-cell *matHeaderCellDef>Actions</th>
                <td mat-cell *matCellDef="let trade">
                  <button mat-icon-button (click)="viewTrade(trade.id)" matTooltip="Voir détails">
                    <mat-icon>visibility</mat-icon>
                  </button>
                  <button 
                    mat-icon-button 
                    color="warn" 
                    (click)="cancelTrade(trade.id)"
                    *ngIf="trade.status === 'PENDING'"
                    matTooltip="Annuler">
                    <mat-icon>cancel</mat-icon>
                  </button>
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
            </table>
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .trade-list-container {
      padding: 2rem;
      max-width: 1200px;
      margin: 0 auto;
    }

    .header-card {
      margin-bottom: 2rem;
    }

    .trades-card {
      width: 100%;
    }

    .loading-container {
      text-align: center;
      padding: 3rem 0;
    }

    .loading-container p {
      margin-top: 1rem;
      color: rgba(0, 0, 0, 0.6);
    }

    .no-trades {
      text-align: center;
      padding: 3rem 0;
      color: rgba(0, 0, 0, 0.6);
    }

    .no-trades mat-icon {
      font-size: 4rem;
      height: 4rem;
      width: 4rem;
      color: rgba(0, 0, 0, 0.3);
    }

    .no-trades h3 {
      margin: 1rem 0;
    }

    .full-width-table {
      width: 100%;
    }

    .trade-details {
      min-width: 300px;
    }

    .trade-info {
      display: flex;
      align-items: center;
      gap: 1rem;
    }

    .player-out, .player-in {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .player-label {
      font-size: 0.8rem;
      color: rgba(0, 0, 0, 0.6);
    }

    .region {
      font-size: 0.8rem;
      color: rgba(0, 0, 0, 0.5);
      font-style: italic;
    }

    .trade-arrow {
      color: #2196f3;
      margin: 0 0.5rem;
    }

    .team-info {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .team-owner {
      font-size: 0.8rem;
      color: rgba(0, 0, 0, 0.6);
    }

    mat-card-header mat-icon {
      margin-right: 0.5rem;
    }

    mat-card-actions {
      display: flex;
      gap: 1rem;
    }

    @media (max-width: 768px) {
      .trade-list-container {
        padding: 1rem;
      }

      .trade-info {
        flex-direction: column;
        align-items: flex-start;
        gap: 0.5rem;
      }

      .trade-arrow {
        transform: rotate(90deg);
      }
    }
  `]
})
export class TradeListComponent implements OnInit {
  trades: Trade[] = [];
  isLoading = true;
  displayedColumns = ['status', 'details', 'team', 'date', 'actions'];

  constructor(private router: Router) {}

  ngOnInit(): void {
    this.loadTrades();
  }

  private loadTrades(): void {
    // Simulate API call with mock data
    setTimeout(() => {
      this.trades = this.getMockTrades();
      this.isLoading = false;
    }, 1000);
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
      }
    ];
  }

  createNewTrade(): void {
    this.router.navigate(['/trades/new']);
  }

  viewHistory(): void {
    this.router.navigate(['/trades/history']);
  }

  viewTrade(tradeId: string): void {
    this.router.navigate(['/trades', tradeId]);
  }

  cancelTrade(tradeId: string): void {
    console.log('Annulation du trade:', tradeId);
    // Implement cancel trade logic
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
    switch (status) {
      case 'PENDING': return 'En attente';
      case 'COMPLETED': return 'Terminé';
      case 'CANCELLED': return 'Annulé';
      default: return status;
    }
  }
}