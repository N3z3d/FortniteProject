import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  HttpLeaderboardRepository,
  MockLeaderboardRepository
} from '../repositories/leaderboard.repository';
import { DataSourceStrategy } from '../strategies/data-source.strategy';
import {
  LeaderboardEntry,
  LeaderboardFilters,
  PronostiqueurLeaderboardEntry,
  PlayerLeaderboardEntry
} from '../services/leaderboard.service';
import { environment } from '../../../environments/environment';

/**
 * Facade Pattern for Leaderboard operations
 * Simplifies usage for components by:
 * 1. Hiding repository selection complexity
 * 2. Automatically handling DB/Mock fallback via DataSourceStrategy
 * 3. Providing single entry point for all leaderboard operations
 *
 * Principles: SRP, DIP, Facade Pattern
 */
@Injectable({
  providedIn: 'root'
})
export class LeaderboardFacade {
  private readonly allowFallback = environment.enableFallbackData;

  constructor(
    private httpRepo: HttpLeaderboardRepository,
    private mockRepo: MockLeaderboardRepository,
    private strategy: DataSourceStrategy
  ) {}

  /**
   * Get leaderboard with automatic DB/Mock fallback
   * @param filters - LeaderboardFilters
   * @returns Observable<LeaderboardEntry[]>
   */
  getLeaderboard(filters: LeaderboardFilters): Observable<LeaderboardEntry[]> {
    return this.strategy.executeWithFallback(
      this.httpRepo.getLeaderboard(filters),
      this.mockRepo.getLeaderboard(filters),
      'Leaderboard',
      { allowFallback: this.allowFallback }
    );
  }

  /**
   * Get pronostiqueur leaderboard with automatic fallback
   * @param season - Season number
   * @returns Observable<PronostiqueurLeaderboardEntry[]>
   */
  getPronostiqueurLeaderboard(season: number): Observable<PronostiqueurLeaderboardEntry[]> {
    return this.strategy.executeWithFallback(
      this.httpRepo.getPronostiqueurLeaderboard(season),
      this.mockRepo.getPronostiqueurLeaderboard(season),
      'Pronostiqueur Leaderboard',
      { allowFallback: this.allowFallback }
    );
  }

  /**
   * Get player leaderboard with automatic fallback
   * @param season - Season number
   * @param region - Optional region filter
   * @returns Observable<PlayerLeaderboardEntry[]>
   */
  getPlayerLeaderboard(season: number, region?: string): Observable<PlayerLeaderboardEntry[]> {
    return this.strategy.executeWithFallback(
      this.httpRepo.getPlayerLeaderboard(season, region),
      this.mockRepo.getPlayerLeaderboard(season, region),
      'Player Leaderboard',
      { allowFallback: this.allowFallback }
    );
  }

  /**
   * Get team leaderboard with automatic fallback
   * @returns Observable<any[]>
   */
  getTeamLeaderboard(): Observable<any[]> {
    return this.strategy.executeWithFallback(
      this.httpRepo.getTeamLeaderboard(),
      this.mockRepo.getTeamLeaderboard(),
      'Team Leaderboard',
      { allowFallback: this.allowFallback }
    );
  }

  /**
   * Get statistics with automatic fallback
   * @param season - Season number
   * @returns Observable<any>
   */
  getStats(season: number): Observable<any> {
    return this.strategy.executeWithFallback(
      this.httpRepo.getStats(season),
      this.mockRepo.getStats(season),
      'Statistics',
      { allowFallback: this.allowFallback }
    );
  }

  /**
   * Get region distribution with automatic fallback
   * @returns Observable<{ [key: string]: number }>
   */
  getRegionDistribution(): Observable<{ [key: string]: number }> {
    return this.strategy.executeWithFallback(
      this.httpRepo.getRegionDistribution(),
      this.mockRepo.getRegionDistribution(),
      'Region Distribution',
      { allowFallback: this.allowFallback }
    );
  }

  /**
   * Check if database is currently available
   * @returns boolean
   */
  isDatabaseAvailable(): boolean {
    return this.strategy.isDatabaseAvailable();
  }

  /**
   * Get current data source status observable
   * For FF-201 indicator component
   */
  getDataSourceStatus$() {
    return this.strategy.currentSource$;
  }
}
