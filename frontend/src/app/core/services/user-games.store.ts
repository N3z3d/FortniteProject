import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { catchError, tap, finalize } from 'rxjs/operators';
import { Game } from '../../features/game/models/game.interface';
import { GameService } from '../../features/game/services/game.service';
import { LoggerService } from './logger.service';

/**
 * BE1-1: Centralized store for user games
 * Single source of truth for games list across all components
 */
export interface UserGamesState {
  games: Game[];
  loading: boolean;
  error: string | null;
  lastLoaded: Date | null;
}

@Injectable({
  providedIn: 'root'
})
export class UserGamesStore {
  private readonly stateSubject = new BehaviorSubject<UserGamesState>({
    games: [],
    loading: false,
    error: null,
    lastLoaded: null
  });

  public readonly state$ = this.stateSubject.asObservable();
  public readonly games$ = new BehaviorSubject<Game[]>([]);
  public readonly loading$ = new BehaviorSubject<boolean>(false);
  public readonly error$ = new BehaviorSubject<string | null>(null);

  private readonly CACHE_DURATION_MS = 30000; // 30 seconds cache

  constructor(
    private readonly gameService: GameService,
    private readonly logger: LoggerService
  ) {}

  /**
   * Get current games synchronously
   */
  getGames(): Game[] {
    return this.stateSubject.value.games;
  }

  /**
   * Get current game count
   */
  getGameCount(): number {
    return this.stateSubject.value.games.length;
  }

  /**
   * Check if games are loaded
   */
  hasGames(): boolean {
    return this.stateSubject.value.games.length > 0;
  }

  /**
   * Check if currently loading
   */
  isLoading(): boolean {
    return this.stateSubject.value.loading;
  }

  /**
   * Load games from API (with cache check)
   */
  loadGames(forceRefresh = false): Observable<Game[]> {
    const state = this.stateSubject.value;

    // Return cached data if valid and not forcing refresh
    if (!forceRefresh && this.isCacheValid(state)) {
      this.logger.debug('UserGamesStore: returning cached games', { count: state.games.length });
      return of(state.games);
    }

    // Prevent duplicate requests
    if (state.loading) {
      this.logger.debug('UserGamesStore: load already in progress');
      return this.games$.asObservable();
    }

    return this.fetchGames();
  }

  /**
   * Force refresh games from API
   */
  refreshGames(): Observable<Game[]> {
    return this.loadGames(true);
  }

  /**
   * Update games after external change (e.g., game created/deleted)
   */
  updateGames(games: Game[]): void {
    this.updateState({
      games,
      loading: false,
      error: null,
      lastLoaded: new Date()
    });
    this.logger.debug('UserGamesStore: games updated externally', { count: games.length });
  }

  /**
   * Add a game to the store (after creation)
   */
  addGame(game: Game): void {
    const currentGames = this.stateSubject.value.games;
    this.updateGames([...currentGames, game]);
  }

  /**
   * Remove a game from the store (after deletion)
   */
  removeGame(gameId: string): void {
    const currentGames = this.stateSubject.value.games;
    this.updateGames(currentGames.filter(g => g.id !== gameId));
  }

  /**
   * Find a game by ID
   */
  findGameById(gameId: string): Game | undefined {
    return this.stateSubject.value.games.find(g => g.id === gameId);
  }

  /**
   * Clear the store (on logout)
   */
  clear(): void {
    this.updateState({
      games: [],
      loading: false,
      error: null,
      lastLoaded: null
    });
    this.logger.debug('UserGamesStore: cleared');
  }

  private fetchGames(): Observable<Game[]> {
    this.updateState({ ...this.stateSubject.value, loading: true, error: null });

    return this.gameService.getUserGames().pipe(
      tap(games => {
        this.updateState({
          games,
          loading: false,
          error: null,
          lastLoaded: new Date()
        });
        this.logger.info('UserGamesStore: games loaded from API', { count: games.length });
      }),
      catchError(error => {
        const errorMessage = error?.message || 'Erreur lors du chargement des games';
        this.updateState({
          ...this.stateSubject.value,
          loading: false,
          error: errorMessage
        });
        this.logger.error('UserGamesStore: failed to load games', { error });
        return throwError(() => error);
      }),
      finalize(() => {
        this.loading$.next(false);
      })
    );
  }

  private updateState(newState: UserGamesState): void {
    this.stateSubject.next(newState);
    this.games$.next(newState.games);
    this.loading$.next(newState.loading);
    this.error$.next(newState.error);
  }

  private isCacheValid(state: UserGamesState): boolean {
    if (!state.lastLoaded) return false;
    const elapsed = Date.now() - state.lastLoaded.getTime();
    return elapsed < this.CACHE_DURATION_MS;
  }
}
