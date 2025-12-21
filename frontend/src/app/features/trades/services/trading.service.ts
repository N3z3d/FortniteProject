import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, timer, throwError } from 'rxjs';
import { map, catchError, tap, switchMap, shareReplay, retry } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { LoggerService } from '../../../core/services/logger.service';

export interface Player {
  id: string;
  name: string;
  region: string;
  team?: string;
  position?: string;
  averageScore: number;
  totalScore: number;
  gamesPlayed: number;
  marketValue: number;
  imageUrl?: string;
  stats?: {
    eliminations?: number;
    placement?: number;
    survivals?: number;
  };
}

export interface Team {
  id: string;
  name: string;
  ownerId: string;
  ownerName: string;
  players: Player[];
  totalValue: number;
  currentScore: number;
  gameId: string;
}

export interface TradeOffer {
  id: string;
  fromTeamId: string;
  fromTeamName: string;
  fromUserId: string;
  fromUserName: string;
  toTeamId: string;
  toTeamName: string;
  toUserId: string;
  toUserName: string;
  offeredPlayers: Player[];
  requestedPlayers: Player[];
  status: 'pending' | 'accepted' | 'rejected' | 'withdrawn' | 'expired';
  createdAt: Date;
  updatedAt: Date;
  expiresAt: Date;
  message?: string;
  counterOffer?: boolean;
  originalOfferId?: string;
  valueBalance: number; // Positive if offering more value, negative if requesting more
}

export interface TradeHistory {
  id: string;
  fromTeamName: string;
  toTeamName: string;
  tradedPlayers: { offered: Player[], received: Player[] };
  completedAt: Date;
  success: boolean;
}

export interface TradeStats {
  totalTrades: number;
  successfulTrades: number;
  pendingOffers: number;
  receivedOffers: number;
  averageTradeValue: number;
  lastTradeDate?: Date;
}

@Injectable({
  providedIn: 'root'
})
export class TradingService {
  private readonly apiUrl = `${environment.apiBaseUrl}/api/trades`;
  private readonly cacheTimeout = 30000; // 30 seconds cache timeout

  // Real-time state management
  private tradesSubject = new BehaviorSubject<TradeOffer[]>([]);
  private teamsSubject = new BehaviorSubject<Team[]>([]);
  private tradingStatsSubject = new BehaviorSubject<TradeStats | null>(null);
  private loadingSubject = new BehaviorSubject<boolean>(false);
  private errorSubject = new BehaviorSubject<string | null>(null);

  // Observable streams
  public trades$ = this.tradesSubject.asObservable();
  public teams$ = this.teamsSubject.asObservable();
  public tradingStats$ = this.tradingStatsSubject.asObservable();
  public loading$ = this.loadingSubject.asObservable();
  public error$ = this.errorSubject.asObservable();

  // Cache
  private cache = new Map<string, { data: any, timestamp: number }>();
  private refreshInterval = 30000; // Refresh every 30 seconds

  constructor(
    private http: HttpClient,
    private logger: LoggerService
  ) {
    this.initializeRealTimeUpdates();
  }

  /**
   * Initialize real-time updates using polling
   * In a production app, this could use WebSockets or Server-Sent Events
   */
  private initializeRealTimeUpdates(): void {
    // Poll for trades updates every 30 seconds
    timer(0, this.refreshInterval).pipe(
      switchMap(() => this.loadTrades()),
      catchError(error => {
        this.logger.error('TradingService real-time updates failed', error);
        return throwError(() => error);
      })
    ).subscribe();
  }

  /**
   * Get all trades for the current user
   */
  getTrades(gameId?: string): Observable<TradeOffer[]> {
    const cacheKey = `trades_${gameId || 'all'}`;
    const cached = this.getFromCache(cacheKey);
    
    if (cached) {
      return new Observable(observer => {
        observer.next(cached);
        observer.complete();
      });
    }

    const url = gameId ? `${this.apiUrl}?gameId=${gameId}` : this.apiUrl;
    
    return this.http.get<TradeOffer[]>(url).pipe(
      map(trades => trades.map(trade => ({
        ...trade,
        createdAt: new Date(trade.createdAt),
        updatedAt: new Date(trade.updatedAt),
        expiresAt: new Date(trade.expiresAt)
      }))),
      tap(trades => {
        this.setCache(cacheKey, trades);
        this.tradesSubject.next(trades);
        this.errorSubject.next(null);
      }),
      retry(2),
      catchError(error => {
        this.errorSubject.next('Failed to load trades');
        return throwError(error);
      }),
      shareReplay(1)
    );
  }

  /**
   * Load trades and update the subject
   */
  private loadTrades(): Observable<TradeOffer[]> {
    this.loadingSubject.next(true);
    
    return this.getTrades().pipe(
      tap(() => this.loadingSubject.next(false)),
      catchError(error => {
        this.loadingSubject.next(false);
        return throwError(error);
      })
    );
  }

  /**
   * Get teams available for trading
   */
  getTeams(gameId: string): Observable<Team[]> {
    const cacheKey = `teams_${gameId}`;
    const cached = this.getFromCache(cacheKey);
    
    if (cached) {
      return new Observable(observer => {
        observer.next(cached);
        observer.complete();
      });
    }

    return this.http.get<Team[]>(`${this.apiUrl}/teams?gameId=${gameId}`).pipe(
      tap(teams => {
        this.setCache(cacheKey, teams);
        this.teamsSubject.next(teams);
      }),
      retry(2),
      catchError(error => {
        this.errorSubject.next('Failed to load teams');
        return throwError(error);
      }),
      shareReplay(1)
    );
  }

  /**
   * Create a new trade offer
   */
  createTradeOffer(offer: Partial<TradeOffer>): Observable<TradeOffer> {
    this.loadingSubject.next(true);
    
    return this.http.post<TradeOffer>(this.apiUrl, offer).pipe(
      tap(newOffer => {
        // Update the trades list
        const currentTrades = this.tradesSubject.value;
        this.tradesSubject.next([...currentTrades, {
          ...newOffer,
          createdAt: new Date(newOffer.createdAt),
          updatedAt: new Date(newOffer.updatedAt),
          expiresAt: new Date(newOffer.expiresAt)
        }]);
        
        this.clearTradesCache();
        this.loadingSubject.next(false);
        this.errorSubject.next(null);
      }),
      retry(1),
      catchError(error => {
        this.loadingSubject.next(false);
        this.errorSubject.next('Failed to create trade offer');
        return throwError(error);
      })
    );
  }

  /**
   * Accept a trade offer
   */
  acceptTradeOffer(offerId: string): Observable<TradeOffer> {
    this.loadingSubject.next(true);
    
    return this.http.post<TradeOffer>(`${this.apiUrl}/${offerId}/accept`, {}).pipe(
      tap(updatedOffer => {
        this.updateTradeInList(updatedOffer);
        this.clearTradesCache();
        this.loadingSubject.next(false);
      }),
      catchError(error => {
        this.loadingSubject.next(false);
        this.errorSubject.next('Failed to accept trade offer');
        return throwError(error);
      })
    );
  }

  /**
   * Reject a trade offer
   */
  rejectTradeOffer(offerId: string, reason?: string): Observable<TradeOffer> {
    this.loadingSubject.next(true);
    
    return this.http.post<TradeOffer>(`${this.apiUrl}/${offerId}/reject`, { reason }).pipe(
      tap(updatedOffer => {
        this.updateTradeInList(updatedOffer);
        this.clearTradesCache();
        this.loadingSubject.next(false);
      }),
      catchError(error => {
        this.loadingSubject.next(false);
        this.errorSubject.next('Failed to reject trade offer');
        return throwError(error);
      })
    );
  }

  /**
   * Withdraw a trade offer
   */
  withdrawTradeOffer(offerId: string): Observable<TradeOffer> {
    this.loadingSubject.next(true);
    
    return this.http.post<TradeOffer>(`${this.apiUrl}/${offerId}/withdraw`, {}).pipe(
      tap(updatedOffer => {
        this.updateTradeInList(updatedOffer);
        this.clearTradesCache();
        this.loadingSubject.next(false);
      }),
      catchError(error => {
        this.loadingSubject.next(false);
        this.errorSubject.next('Failed to withdraw trade offer');
        return throwError(error);
      })
    );
  }

  /**
   * Create a counter offer
   */
  createCounterOffer(originalOfferId: string, counterOffer: Partial<TradeOffer>): Observable<TradeOffer> {
    this.loadingSubject.next(true);
    
    return this.http.post<TradeOffer>(`${this.apiUrl}/${originalOfferId}/counter`, counterOffer).pipe(
      tap(newOffer => {
        const currentTrades = this.tradesSubject.value;
        this.tradesSubject.next([...currentTrades, {
          ...newOffer,
          createdAt: new Date(newOffer.createdAt),
          updatedAt: new Date(newOffer.updatedAt),
          expiresAt: new Date(newOffer.expiresAt)
        }]);
        
        this.clearTradesCache();
        this.loadingSubject.next(false);
      }),
      catchError(error => {
        this.loadingSubject.next(false);
        this.errorSubject.next('Failed to create counter offer');
        return throwError(error);
      })
    );
  }

  /**
   * Get trade history
   */
  getTradeHistory(gameId?: string): Observable<TradeHistory[]> {
    const cacheKey = `trade_history_${gameId || 'all'}`;
    const cached = this.getFromCache(cacheKey);
    
    if (cached) {
      return new Observable(observer => {
        observer.next(cached);
        observer.complete();
      });
    }

    const url = gameId ? `${this.apiUrl}/history?gameId=${gameId}` : `${this.apiUrl}/history`;
    
    return this.http.get<TradeHistory[]>(url).pipe(
      map(history => history.map(trade => ({
        ...trade,
        completedAt: new Date(trade.completedAt)
      }))),
      tap(history => this.setCache(cacheKey, history)),
      retry(2),
      catchError(error => {
        this.errorSubject.next('Failed to load trade history');
        return throwError(error);
      }),
      shareReplay(1)
    );
  }

  /**
   * Get trading statistics
   */
  getTradingStats(gameId?: string): Observable<TradeStats> {
    const cacheKey = `trade_stats_${gameId || 'all'}`;
    const cached = this.getFromCache(cacheKey);
    
    if (cached) {
      return new Observable(observer => {
        observer.next(cached);
        observer.complete();
      });
    }

    const url = gameId ? `${this.apiUrl}/stats?gameId=${gameId}` : `${this.apiUrl}/stats`;
    
    return this.http.get<TradeStats>(url).pipe(
      map(stats => ({
        ...stats,
        lastTradeDate: stats.lastTradeDate ? new Date(stats.lastTradeDate) : undefined
      })),
      tap(stats => {
        this.setCache(cacheKey, stats);
        this.tradingStatsSubject.next(stats);
      }),
      retry(2),
      catchError(error => {
        this.errorSubject.next('Failed to load trading statistics');
        return throwError(error);
      }),
      shareReplay(1)
    );
  }

  /**
   * Calculate trade value balance
   */
  calculateTradeBalance(offeredPlayers: Player[], requestedPlayers: Player[]): number {
    const offeredValue = offeredPlayers.reduce((sum, player) => sum + player.marketValue, 0);
    const requestedValue = requestedPlayers.reduce((sum, player) => sum + player.marketValue, 0);
    return offeredValue - requestedValue;
  }

  /**
   * Validate if a trade is fair (within acceptable value range)
   */
  isTradeBalanced(offeredPlayers: Player[], requestedPlayers: Player[], maxImbalance: number = 0.15): boolean {
    const offeredValue = offeredPlayers.reduce((sum, player) => sum + player.marketValue, 0);
    const requestedValue = requestedPlayers.reduce((sum, player) => sum + player.marketValue, 0);
    
    if (offeredValue === 0 && requestedValue === 0) return true;
    
    const largerValue = Math.max(offeredValue, requestedValue);
    const imbalance = Math.abs(offeredValue - requestedValue) / largerValue;
    
    return imbalance <= maxImbalance;
  }

  /**
   * Get pending trades that require user action
   */
  getPendingTradesForUser(userId: string): Observable<TradeOffer[]> {
    return this.trades$.pipe(
      map(trades => trades.filter(trade => 
        trade.status === 'pending' && 
        (trade.toUserId === userId || trade.fromUserId === userId)
      ))
    );
  }

  /**
   * Clear all caches
   */
  clearAllCaches(): void {
    this.cache.clear();
  }

  /**
   * Refresh all data
   */
  refreshData(): Observable<any> {
    this.clearAllCaches();
    return this.loadTrades();
  }

  // Private helper methods
  private updateTradeInList(updatedTrade: TradeOffer): void {
    const currentTrades = this.tradesSubject.value;
    const index = currentTrades.findIndex(trade => trade.id === updatedTrade.id);
    
    if (index !== -1) {
      const newTrades = [...currentTrades];
      newTrades[index] = {
        ...updatedTrade,
        createdAt: new Date(updatedTrade.createdAt),
        updatedAt: new Date(updatedTrade.updatedAt),
        expiresAt: new Date(updatedTrade.expiresAt)
      };
      this.tradesSubject.next(newTrades);
    }
  }

  private clearTradesCache(): void {
    const keysToDelete = Array.from(this.cache.keys()).filter(key => key.startsWith('trades_'));
    keysToDelete.forEach(key => this.cache.delete(key));
  }

  private getFromCache(key: string): any {
    const cached = this.cache.get(key);
    if (cached && (Date.now() - cached.timestamp < this.cacheTimeout)) {
      return cached.data;
    }
    return null;
  }

  private setCache(key: string, data: any): void {
    this.cache.set(key, { data, timestamp: Date.now() });
  }
}
