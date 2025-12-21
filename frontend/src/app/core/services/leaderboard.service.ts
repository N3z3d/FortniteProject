import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of, map, switchMap, catchError, filter, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoggerService } from './logger.service';

export type Region = 'EU' | 'NAW' | 'BR' | 'ASIA' | 'OCE' | 'NAC' | 'ME';

export interface Player {
  id: string;
  nickname: string;
  region: Region;
  tranche: string;  // Niveau du joueur (1-7) ou "NOUVEAU" pour les changements en cours d'année
  points: number;
  isActive: boolean;
  rank: number;  // Rang actuel dans sa région
  isWorldChampion: boolean;  // Pour le critère d'égalité
  lastUpdate: string;  // Date de dernière mise à jour des points
}

export interface PronostiqueurLeaderboardEntry {
  userId: string;
  username: string;
  email: string;
  rank: number;
  totalPoints: number;
  totalTeams: number;
  avgPointsPerTeam: number;
  bestTeamPoints: number;
  bestTeamName: string;
  victories: number;
  winRate: number;
}

export interface TeamInfo {
  id: string;
  name: string;
  ownerUsername: string;
}

export interface PlayerLeaderboardEntry {
  playerId: string;
  nickname: string;
  username: string;
  region: string;
  tranche: string;
  rank: number;
  totalPoints: number;
  avgPointsPerGame: number;
  bestScore: number;
  teamsCount: number;
  teams?: TeamInfo[];
  pronostiqueurs?: string[];
}

export interface Team {
  id: string;
  name: string;
  season: number;
  tradesRemaining: number;  // Nombre de trades restants
  lastTradeDate?: string;   // Date du dernier trade (pour la restriction de janvier)
  players: Player[];
}

export interface LeaderboardEntry {
  rank: number;
  userId: string;
  username: string;
  isSpecial: boolean;
  totalPoints: number;
  pointsByRegion: {
    [key in Region]?: number;
  };
  regionsWon: number;  // Pour le critère d'égalité
  firstPlacePlayers: number;  // Nombre de joueurs 1ers de leur région
  worldChampions: number;  // Nombre de joueurs champions du monde
  team: Team;
  recentMovements: Array<{
    id: string;
    type: 'TRADE' | 'SCORE_UPDATE' | 'TEAM_UPDATE';
    description: string;
    timestamp: string;
  }>;
}

export interface LeaderboardFilters {
  region?: Region;
  season?: number;
  showInactive?: boolean;
}

export interface TradeValidation {
  isValid: boolean;
  reason?: string;
  newTeamState?: Team;
}

export interface TeamMovement {
  id: string;
  type: 'TRADE' | 'SCORE_UPDATE' | 'TEAM_UPDATE';
  description: string;
  timestamp: string;
  details?: {
    playerOut?: Player;
    playerIn?: Player;
    pointsChange?: number;
    reason?: string;
  };
}

export interface PlayerPool extends Player {
  isAvailable: boolean;
  stats: {
    totalPoints: number;
    tournamentsPlayed: number;
    averagePlacement: number;
    winRate: number;
  };
}

export interface ReplacementCriteria {
  region: Region;
  tranche: string;  // Niveau du joueur (1-7) ou "NOUVEAU"
  minRank?: number;  // Optionnel, par défaut top 10
  minPoints?: number;  // Optionnel, pour filtrer par performance
}

// Type pour la recherche de joueurs
interface PlayerSearchCriteria {
  region?: Region;
  tranche?: string;
  minRank?: number;
  minPoints?: number;
}

// Type pour la conversion PlayerPool vers Player
function convertPoolToPlayer(poolPlayer: PlayerPool): Player {
  return {
    id: poolPlayer.id,
    nickname: poolPlayer.nickname,
    region: poolPlayer.region,
    tranche: poolPlayer.tranche,
    points: poolPlayer.points,
    isActive: true,
    rank: poolPlayer.rank,
    isWorldChampion: poolPlayer.isWorldChampion,
    lastUpdate: poolPlayer.lastUpdate
  };
}

@Injectable({
  providedIn: 'root'
})
export class LeaderboardService {
  private apiUrl = `${environment.apiUrl}/api/leaderboard`;
  private currentSeason = 1;
  private readonly MAX_TRADES = 3;
  private readonly JANUARY_MONTH = 0; // 0-based month index
  private readonly DEFAULT_MIN_RANK = 10;
  private readonly MIN_POINTS_THRESHOLD = 500;  // Points minimum pour être considéré comme actif

  constructor(
    private http: HttpClient,
    private logger: LoggerService
  ) {}

  getLeaderboard(filters: LeaderboardFilters): Observable<LeaderboardEntry[]> {
    this.logger.debug('LeaderboardService.getLeaderboard called', { filters });
    
    // Construire les paramètres de l'API
    let params = new HttpParams();
    if (filters.season) {
      params = params.set('season', filters.season.toString());
    }
    if (filters.region) {
      params = params.set('region', filters.region);
    }

    return this.http.get<LeaderboardEntry[]>(this.apiUrl, { params }).pipe(
      catchError(error => {
        this.logger.error('LeaderboardService: failed to load leaderboard', error);
        // En cas d'erreur, retourner des données vides plutôt que de planter
        return of([]);
      })
    );
  }

  canTrade(teamId: string, playerOutId: string, playerInId: string): Observable<TradeValidation> {
    return this.getTeamDetails(teamId).pipe(
      switchMap(team => {
        const playerOut = team.players.find(p => p.id === playerOutId);
        if (!playerOut) {
          throw new Error("Joueur sortant non trouvé dans l'équipe");
        }

        return this.getPlayerFromPool(playerInId).pipe(
          map(playerIn => {
            const validation: TradeValidation = { isValid: false };

            if (team.tradesRemaining <= 0) {
              validation.reason = "Plus de trades disponibles";
              return validation;
            }

            const currentDate = new Date();
            const isJanuary = currentDate.getMonth() === this.JANUARY_MONTH;

            if (isJanuary) {
              if (playerIn.tranche > playerOut.tranche) {
                validation.reason = "En janvier, échange uniquement avec tranche inférieure ou égale";
                return validation;
              }
            } else {
              if (playerIn.tranche !== playerOut.tranche) {
                validation.reason = "Hors janvier, échange uniquement dans la même tranche";
                return validation;
              }
            }

            if (playerIn.rank > this.DEFAULT_MIN_RANK) {
              validation.reason = "Le joueur entrant doit être top 10 de sa région";
              return validation;
            }

            validation.isValid = true;
            validation.newTeamState = this.simulateTrade(team, playerOut, convertPoolToPlayer(playerIn));
            return validation;
          })
        );
      })
    );
  }

  executeTrade(teamId: string, playerOutId: string, playerInId: string): Observable<Team> {
    return this.canTrade(teamId, playerOutId, playerInId).pipe(
      switchMap(validation => {
        if (!validation.isValid || !validation.newTeamState) {
          throw new Error(validation.reason || "Trade invalide");
        }
        return this.http.post<Team>(`${this.apiUrl}/teams/${teamId}/trade`, {
          playerOutId,
          playerInId,
          timestamp: new Date().toISOString()
        });
      })
    );
  }



  getTeamMovements(teamId: string): Observable<TeamMovement[]> {
    return this.http.get<TeamMovement[]>(`${this.apiUrl}/teams/${teamId}/movements`).pipe(
      map(movements => movements.sort((a, b) => 
        new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
      ))
    );
  }

  updatePlayerPoints(playerId: string, points: number, reason: string): Observable<TeamMovement> {
    return this.http.post<TeamMovement>(`${this.apiUrl}/players/${playerId}/points`, {
      points,
      reason,
      timestamp: new Date().toISOString()
    });
  }

  private simulateTrade(team: Team, playerOut: Player, playerIn: Player): Team {
    return {
      ...team,
      tradesRemaining: team.tradesRemaining - 1,
      lastTradeDate: new Date().toISOString(),
      players: team.players.map(p => p.id === playerOut.id ? playerIn : p)
    };
  }

  private getPlayerFromPool(playerId: string): Observable<PlayerPool> {
    return this.http.get<PlayerPool>(`${this.apiUrl}/players/${playerId}`).pipe(
      filter((player): player is PlayerPool => player !== null),
      map(player => {
        if (!player.isAvailable) {
          throw new Error("Le joueur n'est pas disponible");
        }
        return player;
      }),
      catchError(() => {
        throw new Error("Joueur non trouvé dans le pool");
      })
    );
  }

  private findReplacementPlayer(inactivePlayer: Player): Observable<Player> {
    const criteria: PlayerSearchCriteria = {
      region: inactivePlayer.region,
      tranche: inactivePlayer.tranche,
      minRank: this.DEFAULT_MIN_RANK,
      minPoints: this.MIN_POINTS_THRESHOLD
    };

    return this.searchAvailablePlayers(criteria).pipe(
      map(players => {
        if (players.length === 0) {
          throw new Error("Aucun remplaçant disponible");
        }

        const bestReplacement = players.sort((a, b) => {
          if (a.points !== b.points) return b.points - a.points;
          if (a.rank !== b.rank) return a.rank - b.rank;
          return b.stats.winRate - a.stats.winRate;
        })[0];

        return convertPoolToPlayer(bestReplacement);
      })
    );
  }

  searchAvailablePlayers(criteria: PlayerSearchCriteria): Observable<PlayerPool[]> {
    let params = new HttpParams().set('available', 'true');
    
    if (criteria.region) params = params.set('region', criteria.region);
    if (criteria.tranche) params = params.set('tranche', criteria.tranche);
    if (criteria.minRank) params = params.set('minRank', criteria.minRank.toString());
    if (criteria.minPoints) params = params.set('minPoints', criteria.minPoints.toString());

    return this.http.get<PlayerPool[]>(`${this.apiUrl}/players/search`, { params }).pipe(
      map(players => players.filter(player => 
        player.isAvailable && 
        (!criteria.minRank || player.rank <= criteria.minRank) &&
        (!criteria.minPoints || player.points >= criteria.minPoints)
      ))
    );
  }

  getRegionRankings(region: Region): Observable<PlayerPool[]> {
    return this.http.get<PlayerPool[]>(`${this.apiUrl}/rankings/${region}`).pipe(
      map(players => players.sort((a, b) => a.rank - b.rank))
    );
  }

  isPlayerEligibleForSpecial(playerId: string): Observable<boolean> {
    return this.getPlayerFromPool(playerId).pipe(
      map(player => 
        player.rank <= this.DEFAULT_MIN_RANK &&
        player.points >= this.MIN_POINTS_THRESHOLD &&
        player.stats.tournamentsPlayed >= 3 &&
        player.stats.winRate >= 0.1
      ),
      catchError(() => of(false))
    );
  }

  updatePlayerAvailability(playerId: string, isAvailable: boolean): Observable<PlayerPool> {
    return this.http.patch<PlayerPool>(`${this.apiUrl}/players/${playerId}/availability`, {
      isAvailable,
      timestamp: new Date().toISOString()
    });
  }

  getPlayerStats(playerId: string): Observable<PlayerPool['stats']> {
    return this.http.get<PlayerPool['stats']>(`${this.apiUrl}/players/${playerId}/stats`);
  }

  getTopPerformersByRegion(region: 'EU' | 'NAW' | 'BR' | 'ASIA' | 'OCE' | 'NAC' | 'ME', limit: number = 10): Observable<PlayerPool[]> {
    return this.getRegionRankings(region).pipe(
      map(players => players.slice(0, limit))
    );
  }

  updatePlayerPointsWithValidation(playerId: string, points: number, reason: string): Observable<TeamMovement> {
    return this.getPlayerFromPool(playerId).pipe(
      switchMap(player => {
        if (!player) {
          throw new Error("Joueur non trouvé dans le pool");
        }

        const newPoints = player.points + points;
        if (newPoints < 0) {
          throw new Error("Les points ne peuvent pas être négatifs");
        }

        return this.updatePlayerPoints(playerId, points, reason);
      })
    );
  }

  getTeamDetails(teamId: string): Observable<Team> {
    return this.http.get<Team>(`${this.apiUrl}/teams/${teamId}`);
  }

  // Nouvelle méthode pour le classement des pronostiqueurs
  getPronostiqueurLeaderboard(season: number): Observable<PronostiqueurLeaderboardEntry[]> {
    this.logger.debug('LeaderboardService.getPronostiqueurLeaderboard called', { season });
    
    let params = new HttpParams();
    params = params.set('season', season.toString());

    return this.http.get<PronostiqueurLeaderboardEntry[]>(`${this.apiUrl}/pronostiqueurs`, { params }).pipe(
      catchError(error => {
        this.logger.error('LeaderboardService: failed to load pronostiqueur leaderboard', error);
        return of([]);
      })
    );
  }

  // Nouvelle méthode pour le classement des joueurs
  getPlayerLeaderboard(season: number, region?: string): Observable<PlayerLeaderboardEntry[]> {
    this.logger.debug('LeaderboardService.getPlayerLeaderboard called', { season, region });
    
    let params = new HttpParams();
    params = params.set('season', season.toString());
    if (region) {
      params = params.set('region', region);
    }

    return this.http.get<PlayerLeaderboardEntry[]>(`${this.apiUrl}/joueurs`, { params }).pipe(
      catchError(error => {
        this.logger.error('LeaderboardService: failed to load player leaderboard', error);
        return throwError(() => error);
      })
    );
  }

  getTeamLeaderboard(): Observable<any[]> {
    const requestId = this.generateRequestId();
    const url = `${environment.apiUrl}/api/leaderboard`;

    this.logger.info('LeaderboardService: fetching team leaderboard', { requestId, url });

    return this.http.get<any[]>(url).pipe(
      map(data => {
        this.logger.debug('LeaderboardService: team leaderboard fetched successfully', {
          requestId,
          teamsCount: data?.length || 0,
          isEmpty: !data || data.length === 0
        });
        return data;
      }),
      catchError(error => {
        this.logger.error('LeaderboardService: failed to load team leaderboard', {
          requestId,
          status: error?.status,
          statusText: error?.statusText,
          url: error?.url,
          message: error?.message,
          timestamp: new Date().toISOString()
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Génère un ID de requête unique pour le traçage
   */
  private generateRequestId(): string {
    return `req_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
  }
} 
