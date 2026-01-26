import { Injectable, InjectionToken } from '@angular/core';
import { Observable, BehaviorSubject, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { LoggerService } from '../services/logger.service';

/**
 * Data source types for tracking active source
 * Used by FF-201 to display DB/Fallback indicator
 */
export enum DataSourceType {
  DATABASE = 'DATABASE',
  MOCK = 'MOCK',
  INITIALIZING = 'INITIALIZING',
  ERROR = 'ERROR',
  UNKNOWN = 'UNKNOWN'
}

/**
 * Data source status for UI indicators
 */
export interface DataSourceStatus {
  type: DataSourceType;
  isAvailable: boolean;
  lastChecked: Date;
  message: string;
}

export interface DataSourceFallbackOptions {
  allowFallback?: boolean;
}

/**
 * Strategy Pattern for automatic data source selection
 * Implements: OCP (Open/Closed), SRP (Single Responsibility)
 *
 * Automatically switches between real DB and mock data:
 * 1. Try primary repository (HTTP)
 * 2. On failure, fallback to secondary repository (Mock)
 * 3. Notify observers of current source status (for FF-201 indicator)
 */
@Injectable({
  providedIn: 'root'
})
export class DataSourceStrategy {
  private currentSourceSubject = new BehaviorSubject<DataSourceStatus>({
    type: DataSourceType.INITIALIZING,
    isAvailable: false,
    lastChecked: new Date(),
    message: 'Initialisation...'
  });

  /**
   * Observable for UI components to track data source status
   * Used by FF-201 indicator component
   */
  public currentSource$ = this.currentSourceSubject.asObservable();

  constructor(private logger: LoggerService) {
    this.logger.debug('🎯 DataSourceStrategy initialized');
  }

  /**
   * Execute request with automatic fallback strategy
   * @param primarySource - Observable from primary repository (HTTP)
   * @param fallbackSource - Observable from fallback repository (Mock)
   * @param operationName - Name for logging purposes
   * @param options - Control fallback usage
   * @returns Observable with data from successful source
   */
  executeWithFallback<T>(
    primarySource: Observable<T>,
    fallbackSource: Observable<T>,
    operationName: string,
    options: DataSourceFallbackOptions = {}
  ): Observable<T> {
    const allowFallback = options.allowFallback || false;

    return primarySource.pipe(
      tap(() => {
        this.updateStatus(DataSourceType.DATABASE, true, `✅ ${operationName} - Base de données connectée`);
      }),
      catchError(error => {
        if (!allowFallback) {
          this.logger.error(`Primary source failed for ${operationName}`, error);
          this.updateStatus(DataSourceType.ERROR, false, `${operationName} - Erreur de chargement`);
          return throwError(() => error);
        }

        this.logger.warn(`⚠️ Primary source failed for ${operationName}, switching to fallback`, error);
        this.updateStatus(DataSourceType.MOCK, false, `⚠️ ${operationName} - Mode hors ligne (données de démonstration)`);

        return fallbackSource.pipe(
          catchError(fallbackError => {
            this.logger.error(`❌ Both sources failed for ${operationName}`, fallbackError);
            this.updateStatus(DataSourceType.ERROR, false, `❌ ${operationName} - Erreur de chargement`);
            return throwError(() => new Error(`All data sources failed for ${operationName}`));
          })
        );
      })
    );
  }

  /**
   * Get current data source status (synchronous)
   * @returns Current DataSourceStatus
   */
  getCurrentStatus(): DataSourceStatus {
    return this.currentSourceSubject.value;
  }

  /**
   * Check if database is currently available
   * @returns boolean
   */
  isDatabaseAvailable(): boolean {
    return this.currentSourceSubject.value.type === DataSourceType.DATABASE
      && this.currentSourceSubject.value.isAvailable;
  }

  /**
   * Update data source status and notify observers
   * @param type - Type of data source
   * @param isAvailable - Availability status
   * @param message - User-friendly message
   */
  private updateStatus(type: DataSourceType, isAvailable: boolean, message: string): void {
    const status: DataSourceStatus = {
      type,
      isAvailable,
      lastChecked: new Date(),
      message
    };

    this.currentSourceSubject.next(status);
    this.logger.debug('📊 Data source status updated:', status);
  }

  /**
   * Manually set data source status (for testing or manual override)
   * @param status - DataSourceStatus to set
   */
  setStatus(status: DataSourceStatus): void {
    this.currentSourceSubject.next(status);
  }
}

/**
 * Injection token for repositories
 * Allows easy swapping of implementations (DIP compliance)
 */
export const LEADERBOARD_REPOSITORY = new InjectionToken<any>('LeaderboardRepository');
export const DASHBOARD_REPOSITORY = new InjectionToken<any>('DashboardRepository');
