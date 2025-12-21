import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  HttpDashboardRepository,
  MockDashboardRepository,
  DashboardData,
  DashboardStats
} from '../repositories/dashboard.repository';
import { DataSourceStrategy } from '../strategies/data-source.strategy';
import { environment } from '../../../environments/environment';

/**
 * Facade Pattern for Dashboard operations
 * Simplifies usage for components by:
 * 1. Hiding repository selection complexity
 * 2. Automatically handling DB/Mock fallback via DataSourceStrategy
 * 3. Providing single entry point for all dashboard operations
 *
 * Principles: SRP, DIP, Facade Pattern
 */
@Injectable({
  providedIn: 'root'
})
export class DashboardFacade {
  private readonly allowFallback = environment.enableFallbackData;

  constructor(
    private httpRepo: HttpDashboardRepository,
    private mockRepo: MockDashboardRepository,
    private strategy: DataSourceStrategy
  ) {}

  /**
   * Get complete dashboard data with automatic DB/Mock fallback
   * @param gameId - Game identifier
   * @returns Observable<DashboardData>
   */
  getDashboardData(gameId: string): Observable<DashboardData> {
    return this.strategy.executeWithFallback(
      this.httpRepo.getDashboardData(gameId),
      this.mockRepo.getDashboardData(gameId),
      'Dashboard Data',
      { allowFallback: this.allowFallback }
    );
  }

  /**
   * Get statistics with automatic fallback
   * @param gameId - Game identifier
   * @returns Observable<DashboardStats>
   */
  getStatistics(gameId: string): Observable<DashboardStats> {
    return this.strategy.executeWithFallback(
      this.httpRepo.getStatistics(gameId),
      this.mockRepo.getStatistics(gameId),
      'Dashboard Statistics',
      { allowFallback: this.allowFallback }
    );
  }

  /**
   * Get leaderboard with automatic fallback
   * @param gameId - Game identifier
   * @returns Observable<any[]>
   */
  getLeaderboard(gameId: string): Observable<any[]> {
    return this.strategy.executeWithFallback(
      this.httpRepo.getLeaderboard(gameId),
      this.mockRepo.getLeaderboard(gameId),
      'Dashboard Leaderboard',
      { allowFallback: this.allowFallback }
    );
  }

  /**
   * Get region distribution with automatic fallback
   * @param gameId - Game identifier
   * @returns Observable<{ [key: string]: number }>
   */
  getRegionDistribution(gameId: string): Observable<{ [key: string]: number }> {
    return this.strategy.executeWithFallback(
      this.httpRepo.getRegionDistribution(gameId),
      this.mockRepo.getRegionDistribution(gameId),
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
   * For UI indicators
   */
  getDataSourceStatus$() {
    return this.strategy.currentSource$;
  }
}
