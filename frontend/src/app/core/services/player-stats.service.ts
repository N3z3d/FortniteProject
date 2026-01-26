import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { LoggerService } from './logger.service';

export interface PlayerStats {
  totalPlayers: number;
  playersByRegion?: Record<string, number>;
  playersByTranche?: Record<string, number>;
  dataSource?: string;
}

@Injectable({
  providedIn: 'root'
})
export class PlayerStatsService {
  // BE1-5: Use /players/stats instead of /api/players/stats to avoid route conflict with /{id}
  private readonly statsUrl = `${environment.apiUrl}/players/stats`;

  constructor(private http: HttpClient, private logger: LoggerService) {}

  getPlayerStats(gameId?: string): Observable<PlayerStats> {
    const url = gameId ? `${this.statsUrl}?gameId=${gameId}` : this.statsUrl;
    return this.http.get<PlayerStats>(url).pipe(
      map(stats => {
        const playersByRegion = stats?.playersByRegion || {};
        const playersByTranche = stats?.playersByTranche || {};
        const resolvedTotal = this.resolveTotalPlayers(stats?.totalPlayers, playersByRegion);
        return {
          totalPlayers: resolvedTotal,
          playersByRegion,
          playersByTranche,
          dataSource: stats?.dataSource
        };
      }),
      tap(stats =>
        this.logger.debug('PlayerStatsService: stats loaded', {
          totalPlayers: stats.totalPlayers,
          dataSource: stats.dataSource
        })
      ),
      catchError(error => {
        this.logger.error('PlayerStatsService: failed to load stats', error);
        return throwError(() => error);
      })
    );
  }

  private resolveTotalPlayers(
    totalPlayers: unknown,
    playersByRegion: Record<string, unknown>
  ): number {
    const parsedTotal = Number(totalPlayers);
    if (Number.isFinite(parsedTotal) && parsedTotal > 0) {
      return parsedTotal;
    }

    const sumFromRegions = Object.values(playersByRegion ? {}).reduce<number>(
      (sum, value) => sum + (Number(value) || 0),
      0
    );

    if (sumFromRegions > 0) {
      this.logger.warn('PlayerStatsService: totalPlayers missing, using region sum', {
        totalPlayers,
        sumFromRegions
      });
    }

    return sumFromRegions;
  }
}
