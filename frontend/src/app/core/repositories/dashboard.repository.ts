import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of, forkJoin } from 'rxjs';
import { catchError, map, timeout } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { LoggerService } from '../services/logger.service';

/**
 * Dashboard data interfaces
 */
export interface DashboardStats {
  totalTeams: number;
  totalPlayers: number;
  totalPoints: number;
  averagePointsPerTeam: number;
  mostActiveTeam: string;
  seasonProgress: number;
}

export interface DashboardData {
  statistics: DashboardStats;
  leaderboard: any[];
  regionDistribution: { [key: string]: number };
  teams: any[];
}

/**
 * Abstraction for dashboard data access (DIP - Dependency Inversion Principle)
 */
export abstract class DashboardRepository {
  abstract getDashboardData(gameId: string): Observable<DashboardData>;
  abstract getStatistics(gameId: string): Observable<DashboardStats>;
  abstract getLeaderboard(gameId: string): Observable<any[]>;
  abstract getRegionDistribution(gameId: string): Observable<{ [key: string]: number }>;
}

/**
 * HTTP-based implementation - connects to real backend API
 */
@Injectable({
  providedIn: 'root'
})
export class HttpDashboardRepository extends DashboardRepository {
  private readonly apiUrl = `${environment.apiUrl}/api`;
  private readonly REQUEST_TIMEOUT = 5000;

  constructor(
    private http: HttpClient,
    private logger: LoggerService
  ) {
    super();
    this.logger.debug('📡 HttpDashboardRepository initialized');
  }

  getDashboardData(gameId: string): Observable<DashboardData> {
    this.logger.debug('🔍 HttpDashboardRepository.getDashboardData called with gameId:', gameId);

    return forkJoin({
      statistics: this.getStatistics(gameId),
      leaderboard: this.getLeaderboard(gameId),
      regionDistribution: this.getRegionDistribution(gameId)
    }).pipe(
      timeout(this.REQUEST_TIMEOUT),
      map(data => ({
        statistics: data.statistics,
        leaderboard: data.leaderboard,
        regionDistribution: data.regionDistribution,
        teams: data.leaderboard.map(entry => ({
          id: entry.teamId || entry.id,
          name: entry.teamName,
          totalPoints: entry.totalPoints || 0,
          ownerName: entry.ownerName,
          players: entry.players || []
        }))
      })),
      catchError(error => {
        this.logger.error('HttpDashboardRepository: getDashboardData failed', error);
        throw error;
      })
    );
  }

  getStatistics(gameId: string): Observable<DashboardStats> {
    // BE-P0-01: Pass gameId to filter stats by selected game
    const url = gameId
      ? `${this.apiUrl}/leaderboard/stats?season=2025&gameId=${gameId}`
      : `${this.apiUrl}/leaderboard/stats?season=2025`;

    return this.http.get<any>(url).pipe(
      timeout(this.REQUEST_TIMEOUT),
      map(response => this.mapApiStatsToStats(response)),
      catchError(error => {
        this.logger.error('HttpDashboardRepository: getStatistics failed', error);
        throw error;
      })
    );
  }

  getLeaderboard(gameId: string): Observable<any[]> {
    // BE-P0-01: Pass gameId to filter leaderboard by selected game
    const url = gameId
      ? `${this.apiUrl}/leaderboard?season=2025&gameId=${gameId}`
      : `${this.apiUrl}/leaderboard?season=2025`;

    return this.http.get<any[]>(url).pipe(
      timeout(this.REQUEST_TIMEOUT),
      map(apiResponse => (apiResponse as any)?.data || apiResponse || []),
      catchError(error => {
        this.logger.error('HttpDashboardRepository: getLeaderboard failed', error);
        throw error;
      })
    );
  }

  getRegionDistribution(gameId: string): Observable<{ [key: string]: number }> {
    // BE-P0-01: Pass gameId to filter region distribution by selected game
    const url = gameId
      ? `${this.apiUrl}/leaderboard/distribution/regions?gameId=${gameId}`
      : `${this.apiUrl}/leaderboard/distribution/regions`;

    return this.http.get<{ [key: string]: number }>(url).pipe(
      timeout(this.REQUEST_TIMEOUT),
      map(distribution => this.normalizeRegionDistribution(distribution)),
      catchError(error => {
        this.logger.error('HttpDashboardRepository: getRegionDistribution failed', error);
        throw error;
      })
    );
  }

  private mapApiStatsToStats(apiResponse: any): DashboardStats {
    return {
      totalTeams: apiResponse.totalTeams || 0,
      totalPlayers: apiResponse.totalPlayers || 0,
      totalPoints: apiResponse.totalPoints || 0,
      averagePointsPerTeam: apiResponse.averagePointsPerTeam || 0,
      mostActiveTeam: apiResponse.mostActiveTeam || 'Aucune équipe',
      seasonProgress: this.calculateSeasonProgress()
    };
  }

  private normalizeRegionDistribution(distribution: { [key: string]: number } = {}): { [key: string]: number } {
    const allRegions = ['EU', 'NAC', 'NAW', 'BR', 'ASIA', 'OCE', 'ME'];
    const normalized: { [key: string]: number } = {};

    allRegions.forEach(region => {
      normalized[region] = distribution[region] || 0;
    });

    return normalized;
  }

  private calculateSeasonProgress(): number {
    const now = new Date();
    const startOfYear = new Date(now.getFullYear(), 0, 1);
    const endOfYear = new Date(now.getFullYear(), 11, 31);

    const totalDays = (endOfYear.getTime() - startOfYear.getTime()) / (1000 * 60 * 60 * 24);
    const daysElapsed = (now.getTime() - startOfYear.getTime()) / (1000 * 60 * 60 * 24);

    return Math.round((daysElapsed / totalDays) * 100 * 10) / 10;
  }
}

/**
 * Mock-based implementation - provides fallback data when backend is unavailable
 */
@Injectable({
  providedIn: 'root'
})
export class MockDashboardRepository extends DashboardRepository {
  constructor(private logger: LoggerService) {
    super();
    this.logger.debug('🎮 MockDashboardRepository initialized');
  }

  getDashboardData(gameId: string): Observable<DashboardData> {
    this.logger.warn('⚠️ Using mock dashboard data (backend unavailable)');

    return of({
      statistics: this.getEmptyStats(),
      leaderboard: [],
      regionDistribution: this.getEmptyRegionDistribution(),
      teams: []
    });
  }

  getStatistics(gameId: string): Observable<DashboardStats> {
    this.logger.warn('⚠️ Using mock statistics (backend unavailable)');
    return of(this.getEmptyStats());
  }

  getLeaderboard(gameId: string): Observable<any[]> {
    this.logger.warn('⚠️ Using mock leaderboard (backend unavailable)');
    return of([]);
  }

  getRegionDistribution(gameId: string): Observable<{ [key: string]: number }> {
    this.logger.warn('⚠️ Using mock region distribution (backend unavailable)');
    return of(this.getEmptyRegionDistribution());
  }

  private getEmptyStats(): DashboardStats {
    return {
      totalTeams: 0,
      totalPlayers: 0,
      totalPoints: 0,
      averagePointsPerTeam: 0,
      mostActiveTeam: 'Aucune équipe',
      seasonProgress: this.calculateSeasonProgress()
    };
  }

  private getEmptyRegionDistribution(): { [key: string]: number } {
    return {
      'EU': 0,
      'NAC': 0,
      'NAW': 0,
      'BR': 0,
      'ASIA': 0,
      'OCE': 0,
      'ME': 0
    };
  }

  private calculateSeasonProgress(): number {
    const now = new Date();
    const startOfYear = new Date(now.getFullYear(), 0, 1);
    const endOfYear = new Date(now.getFullYear(), 11, 31);

    const totalDays = (endOfYear.getTime() - startOfYear.getTime()) / (1000 * 60 * 60 * 24);
    const daysElapsed = (now.getTime() - startOfYear.getTime()) / (1000 * 60 * 60 * 24);

    return Math.round((daysElapsed / totalDays) * 100 * 10) / 10;
  }
}
