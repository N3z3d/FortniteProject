import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';

import { LeaderboardService } from '../../../core/services/leaderboard.service';
import { LoggerService } from '../../../core/services/logger.service';
import { TranslationService } from '../../../core/services/translation.service';
import { TeamDto } from '../../../core/services/team.service';

export interface Player {
  id: string;
  nickname: string;
  region: string;
  tranche: string;
  points?: number;
  username?: string;
}

export interface TeamPlayer {
  player: Player;
  position: number;
  playerId?: string;
  active?: boolean;
}

export interface Team {
  id: string;
  name: string;
  season: number;
  owner?: {
    id: string;
    username: string;
  };
  ownerName?: string;
  ownerId?: string;
  players: TeamPlayer[];
  totalPoints?: number;
}

export interface TeamLoadResult {
  team: Team | null;
  allTeams: any[];
  error: string | null;
}

/**
 * Service responsable du chargement et de la transformation des données d'équipe
 * (SRP: Single Responsibility - Data loading et mapping)
 */
@Injectable({
  providedIn: 'root'
})
export class TeamDetailDataService {
  private readonly leaderboardService = inject(LeaderboardService);
  private readonly logger = inject(LoggerService);
  private readonly t = inject(TranslationService);

  /**
   * Charge l'équipe de l'utilisateur courant
   */
  loadMyTeam(username: string): Observable<TeamLoadResult> {
    return this.leaderboardService.getTeamLeaderboard().pipe(
      map((teams: any[]) => {
        const userTeam = teams.find(team =>
          team.ownerUsername?.toLowerCase() === username?.toLowerCase()
        );

        if (userTeam) {
          return {
            team: this.convertLeaderboardToTeam(userTeam),
            allTeams: teams,
            error: null
          };
        } else {
          return {
            team: null,
            allTeams: teams,
            error: this.t.t('teams.detail.notFoundForUser')
          };
        }
      }),
      catchError((error) => {
        this.logger.warn('TeamDetailDataService: failed to load teams for user', { username, error });
        return of({
          team: null,
          allTeams: [],
          error: this.t.t('teams.detail.dataUnavailable')
        });
      })
    );
  }

  /**
   * Charge une équipe par son ID
   */
  loadTeamById(teamId: string): Observable<TeamLoadResult> {
    return this.leaderboardService.getTeamLeaderboard().pipe(
      map((teams: any[]) => {
        const team = teams.find(t => t.teamId === teamId);

        if (team) {
          return {
            team: this.convertLeaderboardToTeam(team),
            allTeams: teams,
            error: null
          };
        } else {
          return {
            team: null,
            allTeams: teams,
            error: this.t.t('teams.detail.notFound')
          };
        }
      }),
      catchError((error) => {
        this.logger.warn('TeamDetailDataService: failed to load team by id', { teamId, error });
        return of({
          team: null,
          allTeams: [],
          error: this.t.t('teams.detail.dataUnavailable')
        });
      })
    );
  }

  /**
   * Calcule les points totaux d'une équipe
   */
  calculateTotalPoints(players: TeamPlayer[]): number {
    return players.reduce((total, teamPlayer) => {
      return total + (teamPlayer.player.points || 0);
    }, 0);
  }

  /**
   * Convertit un TeamDto en Team
   */
  convertTeamDtoToTeam(teamDto: TeamDto): Team {
    return {
      id: teamDto.id,
      name: teamDto.name,
      season: teamDto.season,
      owner: {
        id: 'unknown',
        username: teamDto.ownerUsername || this.t.t('teams.common.unknownOwner')
      },
      players: teamDto.players.map((player, index) => ({
        player: {
          id: player.playerId,
          nickname: player.nickname,
          region: player.region,
          tranche: player.tranche,
          points: 0 // Pas de points disponibles dans les données actuelles
        },
        position: index + 1
      })),
      totalPoints: teamDto.totalScore || 0
    };
  }

  /**
   * Convertit une entrée du leaderboard en Team
   */
  private convertLeaderboardToTeam(leaderboardEntry: any): Team {
    return {
      id: leaderboardEntry.teamId,
      name: leaderboardEntry.teamName,
      season: 2025,
      owner: {
        id: leaderboardEntry.ownerId || 'unknown',
        username: leaderboardEntry.ownerUsername || this.t.t('teams.common.unknownOwner')
      },
      players: leaderboardEntry.players.map((player: any, index: number) => ({
        player: {
          id: player.playerId,
          nickname: player.nickname,
          region: player.region,
          tranche: player.tranche,
          points: player.points || 0
        },
        position: index + 1
      })),
      totalPoints: leaderboardEntry.totalPoints || 0
    };
  }
}
