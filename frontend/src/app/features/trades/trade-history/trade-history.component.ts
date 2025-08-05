import { Component, OnInit } from '@angular/core';
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
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';

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
    MatInputModule
  ],
  template: `
    <div class="trade-history-container">
      <mat-card class="header-card">
        <mat-card-header>
          <mat-card-title>
            <mat-icon>history</mat-icon>
            Historique des Trades
          </mat-card-title>
          <mat-card-subtitle>
            Consultez l'historique complet des échanges
          </mat-card-subtitle>
        </mat-card-header>
        
        <mat-card-actions>
          <button mat-button (click)="goBack()">
            <mat-icon>arrow_back</mat-icon>
            Retour aux trades
          </button>
          <button mat-raised-button color="primary" (click)="createNewTrade()">
            <mat-icon>add</mat-icon>
            Nouveau Trade
          </button>
        </mat-card-actions>
      </mat-card>

      <!-- Filters Card -->
      <mat-card class="filters-card">
        <mat-card-header>
          <mat-card-title>Filtres</mat-card-title>
        </mat-card-header>
        
        <mat-card-content>
          <form [formGroup]="filtersForm" class="filters-form">
            <mat-form-field appearance="outline">
              <mat-label>Statut</mat-label>
              <mat-select formControlName="status" (selectionChange)="applyFilters()">
                <mat-option value="">Tous</mat-option>
                <mat-option value="PENDING">En attente</mat-option>
                <mat-option value="COMPLETED">Terminés</mat-option>
                <mat-option value="CANCELLED">Annulés</mat-option>
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Équipe</mat-label>
              <mat-select formControlName="team" (selectionChange)="applyFilters()">
                <mat-option value="">Toutes</mat-option>
                <mat-option *ngFor="let team of availableTeams" [value]="team.id">
                  {{ team.name }}
                </mat-option>
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Joueur</mat-label>
              <mat-input 
                formControlName="player" 
                placeholder="Nom du joueur"
                (input)="applyFilters()">
              </mat-input>
            </mat-form-field>

            <button 
              mat-button 
              type="button" 
              (click)="clearFilters()"
              [disabled]="!hasActiveFilters()">
              <mat-icon>clear</mat-icon>
              Effacer
            </button>
          </form>
        </mat-card-content>
      </mat-card>

      <!-- History Table Card -->
      <mat-card class="history-table-card">
        <mat-card-header>
          <mat-card-title>
            Historique 
            <span class="results-count" *ngIf="!isLoading">
              ({{ filteredTrades.length }} résultat{{ filteredTrades.length !== 1 ? 's' : '' }})
            </span>
          </mat-card-title>
        </mat-card-header>

        <mat-card-content>
          <div class="loading-container" *ngIf="isLoading">
            <mat-spinner diameter="50"></mat-spinner>
            <p>Chargement de l'historique...</p>
          </div>

          <div class="no-results" *ngIf="!isLoading && filteredTrades.length === 0">
            <mat-icon>search_off</mat-icon>
            <h3>Aucun résultat</h3>
            <p>Aucun trade ne correspond aux critères sélectionnés</p>
          </div>

          <div class="history-table" *ngIf="!isLoading && filteredTrades.length > 0">
            <table mat-table [dataSource]="filteredTrades" class="full-width-table">
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
                    <div class="player-change">
                      <span class="player-out">{{ trade.playerOut.username }}</span>
                      <mat-icon class="trade-arrow">arrow_forward</mat-icon>
                      <span class="player-in">{{ trade.playerIn.username }}</span>
                    </div>
                    <div class="regions">
                      <span class="region">{{ trade.playerOut.region }}</span>
                      →
                      <span class="region">{{ trade.playerIn.region }}</span>
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

              <!-- Created Date Column -->
              <ng-container matColumnDef="createdAt">
                <th mat-header-cell *matHeaderCellDef>Créé</th>
                <td mat-cell *matCellDef="let trade">
                  {{ trade.createdAt | date:'dd/MM/yyyy HH:mm' }}
                </td>
              </ng-container>

              <!-- Completed Date Column -->
              <ng-container matColumnDef="completedAt">
                <th mat-header-cell *matHeaderCellDef>Finalisé</th>
                <td mat-cell *matCellDef="let trade">
                  <span *ngIf="trade.completedAt">
                    {{ trade.completedAt | date:'dd/MM/yyyy HH:mm' }}
                  </span>
                  <span *ngIf="!trade.completedAt" class="pending-text">-</span>
                </td>
              </ng-container>

              <!-- Actions Column -->
              <ng-container matColumnDef="actions">
                <th mat-header-cell *matHeaderCellDef>Actions</th>
                <td mat-cell *matCellDef="let trade">
                  <button mat-icon-button (click)="viewTrade(trade.id)" matTooltip="Voir détails">
                    <mat-icon>visibility</mat-icon>
                  </button>
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
            </table>
          </div>

          <!-- Statistics Section -->
          <div class="statistics-section" *ngIf="!isLoading && allTrades.length > 0">
            <h3>
              <mat-icon>bar_chart</mat-icon>
              Statistiques
            </h3>
            <div class="stats-grid">
              <div class="stat-card">
                <div class="stat-number">{{ getTotalTrades() }}</div>
                <div class="stat-label">Total trades</div>
              </div>
              <div class="stat-card">
                <div class="stat-number">{{ getCompletedTrades() }}</div>
                <div class="stat-label">Terminés</div>
              </div>
              <div class="stat-card">
                <div class="stat-number">{{ getPendingTrades() }}</div>
                <div class="stat-label">En attente</div>
              </div>
              <div class="stat-card">
                <div class="stat-number">{{ getCancelledTrades() }}</div>
                <div class="stat-label">Annulés</div>
              </div>
            </div>
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .trade-history-container {
      padding: 2rem;
      max-width: 1400px;
      margin: 0 auto;
    }

    .header-card {
      margin-bottom: 2rem;
    }

    .filters-card {
      margin-bottom: 2rem;
    }

    .history-table-card {
      width: 100%;
    }

    .filters-form {
      display: flex;
      gap: 1rem;
      align-items: center;
      flex-wrap: wrap;
    }

    .filters-form mat-form-field {
      min-width: 200px;
    }

    .results-count {
      font-size: 0.9rem;
      color: rgba(0, 0, 0, 0.6);
      font-weight: 400;
    }

    .loading-container {
      text-align: center;
      padding: 3rem 0;
    }

    .loading-container p {
      margin-top: 1rem;
      color: rgba(0, 0, 0, 0.6);
    }

    .no-results {
      text-align: center;
      padding: 3rem 0;
      color: rgba(0, 0, 0, 0.6);
    }

    .no-results mat-icon {
      font-size: 4rem;
      height: 4rem;
      width: 4rem;
      color: rgba(0, 0, 0, 0.3);
    }

    .no-results h3 {
      margin: 1rem 0;
    }

    .full-width-table {
      width: 100%;
    }

    .trade-details {
      min-width: 250px;
    }

    .trade-info {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .player-change {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .player-out {
      color: #f44336;
      font-weight: 500;
    }

    .player-in {
      color: #2196f3;
      font-weight: 500;
    }

    .trade-arrow {
      font-size: 1.2rem;
      height: 1.2rem;
      width: 1.2rem;
      color: #4caf50;
    }

    .regions {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.8rem;
      color: rgba(0, 0, 0, 0.6);
    }

    .region {
      font-style: italic;
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

    .pending-text {
      color: rgba(0, 0, 0, 0.4);
      font-style: italic;
    }

    .statistics-section {
      margin-top: 3rem;
      padding-top: 2rem;
      border-top: 1px solid #e0e0e0;
    }

    .statistics-section h3 {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-bottom: 1.5rem;
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
      gap: 1rem;
    }

    .stat-card {
      text-align: center;
      padding: 1.5rem;
      background-color: #f5f5f5;
      border-radius: 12px;
      border: 1px solid #e0e0e0;
    }

    .stat-number {
      font-size: 2rem;
      font-weight: 700;
      color: #2196f3;
      margin-bottom: 0.5rem;
    }

    .stat-label {
      font-size: 0.9rem;
      color: rgba(0, 0, 0, 0.6);
    }

    mat-card-header mat-icon {
      margin-right: 0.5rem;
    }

    mat-card-actions {
      display: flex;
      justify-content: space-between;
      gap: 1rem;
    }

    @media (max-width: 768px) {
      .trade-history-container {
        padding: 1rem;
      }

      .filters-form {
        flex-direction: column;
        align-items: stretch;
      }

      .filters-form mat-form-field {
        min-width: auto;
        width: 100%;
      }

      .trade-info {
        font-size: 0.9rem;
      }

      .stats-grid {
        grid-template-columns: repeat(2, 1fr);
      }

      mat-card-actions {
        flex-direction: column;
      }
    }
  `]
})
export class TradeHistoryComponent implements OnInit {
  allTrades: TradeHistoryItem[] = [];
  filteredTrades: TradeHistoryItem[] = [];
  isLoading = true;
  filtersForm: FormGroup;
  displayedColumns = ['status', 'details', 'team', 'createdAt', 'completedAt', 'actions'];

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
    switch (status) {
      case 'PENDING': return 'En attente';
      case 'COMPLETED': return 'Terminé';
      case 'CANCELLED': return 'Annulé';
      default: return status;
    }
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