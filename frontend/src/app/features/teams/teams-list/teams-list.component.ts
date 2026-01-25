import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { TeamService, TeamDto } from '../../../core/services/team.service';
import { GameSelectionService } from '../../../core/services/game-selection.service';
import { LoggerService } from '../../../core/services/logger.service';
import { TranslationService } from '../../../core/services/translation.service';

interface Team {
  id: string;
  name: string;
  ownerName: string;
  playerCount: number;
  totalPoints: number;
  isActive: boolean;
  lastUpdate: Date;
  players: { id: string; name: string; gamertag?: string; points?: number }[];
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
  templateUrl: './teams-list.component.html',
  styleUrls: ['./teams-list.component.scss']
})
export class TeamsListComponent implements OnInit, OnDestroy {
  teams: Team[] = [];
  filteredTeams: Team[] = [];
  searchTerm: string = '';
  loading = true;
  error: string | null = null;
  gameId: string | null = null;
  private destroy$ = new Subject<void>();

  constructor(
    private readonly teamService: TeamService,
    private readonly gameSelectionService: GameSelectionService,
    private readonly logger: LoggerService,
    public readonly t: TranslationService
  ) { }

  ngOnInit(): void {
    // BE-P0-03: Use GameSelectionService as source of truth (guard ensures game is selected)
    this.gameSelectionService.selectedGame$
      .pipe(takeUntil(this.destroy$))
      .subscribe(game => {
        this.gameId = game?.id || null;
        this.loadTeams();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSearch(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchTerm = input.value.toLowerCase().trim();
    this.filterTeams();
  }

  private filterTeams(): void {
    if (!this.searchTerm) {
      this.filteredTeams = this.teams;
      return;
    }

    this.filteredTeams = this.teams.filter(team =>
      team.name.toLowerCase().includes(this.searchTerm) ||
      team.ownerName.toLowerCase().includes(this.searchTerm) ||
      team.players.some(p => (p.gamertag || p.name).toLowerCase().includes(this.searchTerm))
    );
  }

  getTotalPlayers(): number {
    return this.teams.reduce((acc, team) => acc + team.playerCount, 0);
  }

  private loadTeams(): void {
    this.loading = true;
    this.error = null;

    const teamsObservable = this.gameId
      ? this.teamService.getTeamsByGame(this.gameId)
      : this.teamService.getUserTeams();

    teamsObservable
      .subscribe({
        next: (teams: TeamDto[]) => {
          this.teams = this.mapTeamsData(teams);
          this.filteredTeams = this.teams;
          this.loading = false;
          this.error = null;
          this.logger.debug('TeamsListComponent: teams loaded', { count: teams.length });
        },
        error: (error: any) => {
          // BE-P0-03: Improve error message based on error type
          const status = error?.status;
          if (status === 404) {
            this.error = this.t.t('teams.list.errors.noTeamsForGame');
          } else if (status === 0 || !navigator.onLine) {
            this.error = this.t.t('teams.list.errors.serverUnavailable');
          } else {
            this.error = this.t.t('teams.list.errors.loadFailed');
          }
          this.logger.error('TeamsListComponent: failed to load teams', { error, gameId: this.gameId });
          this.loading = false;
          this.teams = [];
          this.filteredTeams = [];
        }
      });
  }

  refreshTeams(): void {
    this.loadTeams();
  }

  private mapTeamsData(teams: TeamDto[]): Team[] {
    return teams.map((team: TeamDto) => ({
      id: team.id || '',
      name: team.name || this.t.t('teams.common.unnamedTeam'),
      ownerName: team.ownerUsername || this.t.t('teams.common.unknownOwner'),
      playerCount: team.players?.length || 0,
      totalPoints: team.totalScore || 0,
      isActive: true,
      lastUpdate: new Date(),
      players: (team.players || []).map((p: any) => ({
        id: p.playerId || '',
        name: p.nickname || p.username || this.t.t('teams.common.unknownPlayer'),
        gamertag: p.nickname || p.username,
        points: p.points || 0
      }))
    }));
  }

  trackByTeamId(index: number, team: Team): string {
    return team.id;
  }

  trackByPlayerId(index: number, player: any): string {
    return player.id;
  }
}
