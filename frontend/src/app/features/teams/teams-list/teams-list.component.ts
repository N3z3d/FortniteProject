import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Params } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { Subject, combineLatest, of, takeUntil } from 'rxjs';

import { TeamService, TeamDto } from '../../../core/services/team.service';
import { LoggerService } from '../../../core/services/logger.service';

interface Player {
  id: string;
  name: string;
  gamertag: string;
  points?: number;
}

interface Team {
  id: string;
  name: string;
  ownerName: string;
  playerCount: number;
  players: Player[];
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
        <h2>{{ gameId ? 'Équipes du jeu' : 'Mes Équipes' }}</h2>
        <button mat-raised-button color="primary" routerLink="/teams/create" *ngIf="!gameId">
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
            <mat-card-subtitle>Propriétaire: {{ team.ownerName }}</mat-card-subtitle>
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

            <!-- Liste des joueurs -->
            <div class="players-section" *ngIf="team.players && team.players.length > 0">
              <h4 class="players-title">
                <mat-icon>group</mat-icon>
                Joueurs de l'équipe
              </h4>
              <div class="players-list">
                <div
                  *ngFor="let player of team.players; trackBy: trackByPlayerId"
                  class="player-item">
                  <mat-icon class="player-icon">sports_esports</mat-icon>
                  <div class="player-info">
                    <span class="player-name">{{ player.gamertag || player.name }}</span>
                    <span class="player-points" *ngIf="player.points">
                      {{ player.points }} pts
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <div class="team-meta">
              <p class="last-update">
                <mat-icon>schedule</mat-icon>
                Mis à jour: {{ team.lastUpdate | date:'short' }}
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
              [routerLink]="['detail', team.id]">
              <mat-icon>visibility</mat-icon>
              Voir détails
            </button>
            <button
              mat-button
              color="accent"
              [routerLink]="['edit', team.id]"
              [disabled]="!team.isActive">
              <mat-icon>edit</mat-icon>
              Modifier
            </button>
          </mat-card-actions>
        </mat-card>
      </div>

      <div class="error-state" *ngIf="!loading && error">
        <mat-icon class="large-icon">error</mat-icon>
        <h3>Erreur de chargement</h3>
        <p>{{ error }}</p>
        <button mat-raised-button color="primary" (click)="refreshTeams()">
          <mat-icon>refresh</mat-icon>
          Recharger
        </button>
      </div>

      <div class="empty-state" *ngIf="!loading && !error && teams.length === 0">
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
  gameId: string | null = null;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly teamService: TeamService,
    private readonly route: ActivatedRoute,
    private readonly logger: LoggerService
  ) {}

  ngOnInit(): void {
    const parentParams$ = this.route.parent?.params ?? of({} as Params);

    combineLatest([this.route.params, parentParams$])
      .pipe(takeUntil(this.destroy$))
      .subscribe(([params, parentParams]) => {
        this.gameId = params['id'] || parentParams['id'] || null;
        this.loadTeams();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadTeams(): void {
    this.loading = true;
    this.error = null;

    // If we have a gameId from the route, load teams for that game only
    const teamsObservable = this.gameId 
      ? this.teamService.getTeamsByGame(this.gameId)
      : this.teamService.getUserTeams();

    teamsObservable
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (teams: TeamDto[]) => {
          this.teams = this.mapTeamsData(teams);
          this.loading = false;
        },
        error: (error: Error) => {
          this.logger.error('Erreur lors du chargement des equipes', error);
          this.error = 'Donn\u00e9es indisponibles (CSV non charg\u00e9)';
          this.loading = false;
          this.teams = [];
        }
      });
  }

  private mapTeamsData(apiTeams: TeamDto[]): Team[] {
    if (!Array.isArray(apiTeams)) {
      return [];
    }

    return apiTeams.map((team: any) => {
      const apiPlayers = Array.isArray(team.players) ? team.players : [];
      const lastUpdateValue = team.updatedAt || team.lastUpdate || team.createdAt;

      return {
        id: team.id || '',
        name: team.name || team.teamName || 'Équipe sans nom',
        ownerName:
          team.ownerUsername ||
          team.ownerName ||
          team.owner?.username ||
          team.owner ||
          'Propriétaire inconnu',
        playerCount: apiPlayers.length || team.playerCount || 0,
        players: this.mapPlayers(apiPlayers),
        totalPoints: team.totalScore ?? team.totalPoints ?? team.points ?? 0,
        gameId: team.gameId || this.gameId || '',
        gameName:
          team.gameName ||
          team.game?.name ||
          (this.gameId ? 'Jeu sélectionné' : 'Saison 2025'),
        lastUpdate: lastUpdateValue ? new Date(lastUpdateValue) : new Date(),
        isActive: team.isActive !== false // Active par défaut
      };
    });
  }

  private mapPlayers(apiPlayers: any[]): Player[] {
    if (!Array.isArray(apiPlayers)) {
      return [];
    }

    return apiPlayers.map((player: any) => {
      const nickname =
        player.nickname ||
        player.gamertag ||
        player.fortniteId ||
        player.name ||
        player.playerName ||
        'Joueur';

      return {
        id: player.id || player.playerId || '',
        name: nickname,
        gamertag: nickname,
        points: player.points || player.totalPoints || 0
      };
    });
  }


  trackByTeamId(index: number, team: Team): string {
    return team.id;
  }

  trackByPlayerId(index: number, player: Player): string {
    return player.id;
  }

  refreshTeams(): void {
    this.loadTeams();
  }
}
