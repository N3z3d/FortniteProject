import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map, timeout } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { LoggerService } from '../services/logger.service';
import {
  LeaderboardEntry,
  LeaderboardFilters,
  PronostiqueurLeaderboardEntry,
  PlayerLeaderboardEntry
} from '../services/leaderboard.service';

/**
 * Abstraction for leaderboard data access (DIP - Dependency Inversion Principle)
 * Allows multiple implementations: HTTP (real backend), Mock (fallback), etc.
 */
export abstract class LeaderboardRepository {
  abstract getLeaderboard(filters: LeaderboardFilters): Observable<LeaderboardEntry[]>;
  abstract getPronostiqueurLeaderboard(season: number): Observable<PronostiqueurLeaderboardEntry[]>;
  abstract getPlayerLeaderboard(season: number, region?: string): Observable<PlayerLeaderboardEntry[]>;
  abstract getTeamLeaderboard(): Observable<any[]>;
  abstract getStats(season: number): Observable<any>;
  abstract getRegionDistribution(): Observable<{ [key: string]: number }>;
}

/**
 * HTTP-based implementation - connects to real backend API
 * Handles errors gracefully and returns empty data on failure
 */
@Injectable({
  providedIn: 'root'
})
export class HttpLeaderboardRepository extends LeaderboardRepository {
  private readonly apiUrl = `${environment.apiUrl}/api/leaderboard`;
  private readonly REQUEST_TIMEOUT = 5000;

  constructor(
    private http: HttpClient,
    private logger: LoggerService
  ) {
    super();
    this.logger.debug('📡 HttpLeaderboardRepository initialized');
  }

  getLeaderboard(filters: LeaderboardFilters): Observable<LeaderboardEntry[]> {
    let params = new HttpParams();
    if (filters.season) params = params.set('season', filters.season.toString());
    if (filters.region) params = params.set('region', filters.region);

    return this.http.get<LeaderboardEntry[]>(this.apiUrl, { params }).pipe(
      timeout(this.REQUEST_TIMEOUT),
      catchError(error => {
        this.logger.error('HttpLeaderboardRepository: getLeaderboard failed', error);
        throw error; // Let strategy handle fallback
      })
    );
  }

  getPronostiqueurLeaderboard(season: number): Observable<PronostiqueurLeaderboardEntry[]> {
    const params = new HttpParams().set('season', season.toString());

    return this.http.get<PronostiqueurLeaderboardEntry[]>(`${this.apiUrl}/pronostiqueurs`, { params }).pipe(
      timeout(this.REQUEST_TIMEOUT),
      catchError(error => {
        this.logger.error('HttpLeaderboardRepository: getPronostiqueurLeaderboard failed', error);
        throw error;
      })
    );
  }

  getPlayerLeaderboard(season: number, region?: string): Observable<PlayerLeaderboardEntry[]> {
    let params = new HttpParams().set('season', season.toString());
    if (region) params = params.set('region', region);

    return this.http.get<PlayerLeaderboardEntry[]>(`${this.apiUrl}/joueurs`, { params }).pipe(
      timeout(this.REQUEST_TIMEOUT),
      catchError(error => {
        this.logger.error('HttpLeaderboardRepository: getPlayerLeaderboard failed', error);
        throw error;
      })
    );
  }

  getTeamLeaderboard(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl).pipe(
      timeout(this.REQUEST_TIMEOUT),
      catchError(error => {
        this.logger.error('HttpLeaderboardRepository: getTeamLeaderboard failed', error);
        throw error;
      })
    );
  }

  getStats(season: number): Observable<any> {
    const params = new HttpParams().set('season', season.toString());

    return this.http.get<any>(`${this.apiUrl}/stats`, { params }).pipe(
      timeout(this.REQUEST_TIMEOUT),
      catchError(error => {
        this.logger.error('HttpLeaderboardRepository: getStats failed', error);
        throw error;
      })
    );
  }

  getRegionDistribution(): Observable<{ [key: string]: number }> {
    return this.http.get<{ [key: string]: number }>(`${this.apiUrl}/distribution/regions`).pipe(
      timeout(this.REQUEST_TIMEOUT),
      catchError(error => {
        this.logger.error('HttpLeaderboardRepository: getRegionDistribution failed', error);
        throw error;
      })
    );
  }
}

/**
 * Mock-based implementation - provides fallback data when backend is unavailable
 * Used automatically by DataSourceStrategy when HTTP requests fail
 */
@Injectable({
  providedIn: 'root'
})
export class MockLeaderboardRepository extends LeaderboardRepository {
  constructor(private logger: LoggerService) {
    super();
    this.logger.debug('🎮 MockLeaderboardRepository initialized');
  }

  getLeaderboard(filters: LeaderboardFilters): Observable<LeaderboardEntry[]> {
    this.logger.warn('⚠️ Using mock leaderboard data (backend unavailable)');
    return of([]);
  }

  getPronostiqueurLeaderboard(season: number): Observable<PronostiqueurLeaderboardEntry[]> {
    this.logger.warn('⚠️ Using mock pronostiqueur leaderboard (backend unavailable)');
    return of([]);
  }

  getPlayerLeaderboard(season: number, region?: string): Observable<PlayerLeaderboardEntry[]> {
    this.logger.warn('⚠️ Using mock player leaderboard (backend unavailable)');
    return of([]);
  }

  getTeamLeaderboard(): Observable<any[]> {
    this.logger.warn('⚠️ Using mock team leaderboard (backend unavailable)');
    return of([]);
  }

  getStats(season: number): Observable<any> {
    this.logger.warn('⚠️ Using mock stats (backend unavailable)');
    return of({
      totalTeams: 0,
      totalPlayers: 0,
      totalPoints: 0,
      averagePointsPerTeam: 0
    });
  }

  getRegionDistribution(): Observable<{ [key: string]: number }> {
    this.logger.warn('⚠️ Using mock region distribution (backend unavailable)');
    return of({
      'EU': 0,
      'NAC': 0,
      'NAW': 0,
      'BR': 0,
      'ASIA': 0,
      'OCE': 0,
      'ME': 0
    });
  }
}
