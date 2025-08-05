import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { Observable, Subject, takeUntil } from 'rxjs';

import { TeamService, TeamDto } from '../../../core/services/team.service';

interface Team {
  id: string;
  name: string;
  ownerName: string;
  playerCount: number;
  totalPoints: number;
  gameId: string;
  gameName: string;
  lastUpdate: Date;
  isActive: boolean;
}

@Component({
  selector: 'app-teams-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule
  ],
  template: `
    <div class="teams-list-container">
      <div class="header">
        <h2>Mes Équipes</h2>
        <button mat-raised-button color="primary" routerLink="/teams/create">
          <mat-icon>add</mat-icon>
          Créer une équipe
        </button>
      </div>

      <div class="teams-grid" *ngIf="!loading; else loadingTemplate">
        <mat-card 
          *ngFor="let team of teams; trackBy: trackByTeamId" 
          class="team-card"
          [class.inactive]="!team.isActive">
          
          <mat-card-header>
            <mat-card-title>{{ team.name }}</mat-card-title>
            <mat-card-subtitle>{{ team.gameName }}</mat-card-subtitle>
          </mat-card-header>

          <mat-card-content>
            <div class="team-stats">
              <div class="stat">
                <mat-icon>person</mat-icon>
                <span>{{ team.playerCount }} joueurs</span>
              </div>
              <div class="stat">
                <mat-icon>star</mat-icon>
                <span>{{ team.totalPoints }} points</span>
              </div>
            </div>

            <div class="team-meta">
              <p class="owner">Propriétaire: {{ team.ownerName }}</p>
              <p class="last-update">
                Dernière mise à jour: {{ team.lastUpdate | date:'short' }}
              </p>
            </div>

            <div class="status-chips">
              <mat-chip 
                [color]="team.isActive ? 'primary' : 'warn'">
                {{ team.isActive ? 'Active' : 'Inactive' }}
              </mat-chip>
            </div>
          </mat-card-content>

          <mat-card-actions>
            <button 
              mat-button 
              color="primary"
              [routerLink]="['/teams', team.id]">
              <mat-icon>visibility</mat-icon>
              Voir détails
            </button>
            <button 
              mat-button 
              color="accent"
              [routerLink]="['/teams', team.id, 'edit']"
              [disabled]="!team.isActive">
              <mat-icon>edit</mat-icon>
              Modifier
            </button>
          </mat-card-actions>
        </mat-card>
      </div>

      <div class="empty-state" *ngIf="!loading && teams.length === 0">
        <mat-icon class="large-icon">groups</mat-icon>
        <h3>Aucune équipe trouvée</h3>
        <p>Vous n'avez pas encore créé d'équipe. Commencez par rejoindre un jeu ou créer votre première équipe.</p>
        <button mat-raised-button color="primary" routerLink="/games">
          <mat-icon>sports_esports</mat-icon>
          Voir les jeux disponibles
        </button>
      </div>

      <ng-template #loadingTemplate>
        <div class="loading-container">
          <mat-spinner></mat-spinner>
          <p>Chargement de vos équipes...</p>
        </div>
      </ng-template>
    </div>
  `,
  styleUrls: ['./teams-list.component.scss']
})
export class TeamsListComponent implements OnInit, OnDestroy {
  teams: Team[] = [];
  loading = true;
  error: string | null = null;
  private destroy$ = new Subject<void>();

  constructor(private teamService: TeamService) {}

  ngOnInit(): void {
    this.loadTeams();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadTeams(): void {
    this.loading = true;
    this.error = null;

    this.teamService.getUserTeams()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (teams: TeamDto[]) => {
          this.teams = this.mapTeamsData(teams);
          this.loading = false;
        },
        error: (error: Error) => {
          console.error('Erreur lors du chargement des équipes:', error);
          this.error = 'Impossible de charger les équipes';
          this.loading = false;
          // Données de fallback pour éviter un écran vide
          this.teams = this.getMockTeams();
        }
      });
  }

  private mapTeamsData(apiTeams: any[]): Team[] {
    if (!Array.isArray(apiTeams)) {
      return [];
    }

    return apiTeams.map(team => ({
      id: team.id || '',
      name: team.name || team.teamName || 'Équipe sans nom',
      ownerName: team.ownerName || team.owner || 'Propriétaire inconnu',
      playerCount: team.players?.length || team.playerCount || 0,
      totalPoints: team.totalPoints || team.points || 0,
      gameId: team.gameId || '',
      gameName: team.gameName || team.game?.name || 'Jeu inconnu',
      lastUpdate: team.lastUpdate ? new Date(team.lastUpdate) : new Date(),
      isActive: team.isActive !== false // Active par défaut
    }));
  }

  private getMockTeams(): Team[] {
    return [
      {
        id: 'team1',
        name: 'Les Legends',
        ownerName: 'Joueur Test',
        playerCount: 5,
        totalPoints: 1250,
        gameId: 'game1',
        gameName: 'Championnat Fortnite 2024',
        lastUpdate: new Date(),
        isActive: true
      },
      {
        id: 'team2',
        name: 'Victory Squad',
        ownerName: 'Joueur Test',
        playerCount: 4,
        totalPoints: 890,
        gameId: 'game2',
        gameName: 'Tournoi Hiver',
        lastUpdate: new Date(Date.now() - 86400000), // Hier
        isActive: false
      }
    ];
  }

  trackByTeamId(index: number, team: Team): string {
    return team.id;
  }

  refreshTeams(): void {
    this.loadTeams();
  }
}