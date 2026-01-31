import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { catchError, tap, map } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { LoggerService } from '../../../core/services/logger.service';
import { MOCK_GAMES } from '../../../core/data/mock-game-data';
import {
  Game,
  GameParticipant,
  DraftState,
  DraftStatistics,
  DraftHistoryEntry
} from '../models/game.interface';

/**
 * CQRS Query Service - Read operations only
 * Handles all game-related data retrieval operations
 */
@Injectable({
  providedIn: 'root'
})
export class GameQueryService {
  private readonly apiUrl = `${environment.apiUrl}/api`;

  constructor(
    private readonly http: HttpClient,
    private readonly logger: LoggerService
  ) { }

  /**
   * Récupère toutes les games
   */
  getAllGames(): Observable<Game[]> {
    return this.http.get<Game[]>(`${this.apiUrl}/games`)
      .pipe(
        catchError(this.handleError.bind(this))
      );
  }

  /**
   * Récupère les games de l'utilisateur connecté
   */
  getUserGames(): Observable<Game[]> {
    return this.http.get<Game[]>(`${this.apiUrl}/games/my-games`)
      .pipe(
        catchError(error => this.handleErrorWithFallback(error, this.getMockUserGames()))
      );
  }

  /**
   * Récupère les games disponibles (alias pour getAllGames)
   */
  getAvailableGames(): Observable<Game[]> {
    return this.getAllGames();
  }

  /**
   * Récupère une game par son ID
   */
  getGameById(id: string): Observable<Game> {
    const requestId = this.generateRequestId();
    this.logger.info('GameQueryService: fetching game by ID', { gameId: id, requestId });

    return this.http.get<Game>(`${this.apiUrl}/games/${id}`)
      .pipe(
        tap(game => this.logger.debug('GameQueryService: game fetched successfully', { gameId: id, requestId, gameName: game.name })),
        catchError(error => this.handleErrorWithContext(error, 'getGameById', { gameId: id, requestId }))
      );
  }

  /**
   * Récupère les détails d'une game (alias pour getGameById)
   */
  getGameDetails(id: string): Observable<Game> {
    return this.getGameById(id);
  }

  /**
   * Valide un code d'invitation
   */
  validateInvitationCode(code: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.apiUrl}/games/validate-code/${code}`)
      .pipe(
        catchError(this.handleError.bind(this))
      );
  }

  /**
   * Récupère l'état du draft
   */
  getDraftState(gameId: string): Observable<DraftState> {
    return this.http.get<DraftState>(`${this.apiUrl}/games/${gameId}/draft/state`)
      .pipe(
        catchError(this.handleError.bind(this))
      );
  }

  /**
   * Récupère l'historique du draft
   */
  getDraftHistory(gameId: string): Observable<DraftHistoryEntry[]> {
    return this.http.get<DraftHistoryEntry[]>(`${this.apiUrl}/games/${gameId}/draft/history`)
      .pipe(
        catchError(this.handleError.bind(this))
      );
  }

  /**
   * Récupère les statistiques du draft
   */
  getDraftStatistics(gameId: string): Observable<DraftStatistics> {
    return this.http.get<DraftStatistics>(`${this.apiUrl}/games/${gameId}/draft/statistics`)
      .pipe(
        catchError(this.handleError.bind(this))
      );
  }

  /**
   * Récupère les participants d'une game
   */
  getGameParticipants(gameId: string): Observable<GameParticipant[]> {
    const requestId = this.generateRequestId();
    this.logger.info('GameQueryService: fetching game participants', { gameId, requestId });

    return this.http.get<GameParticipant[]>(`${this.apiUrl}/games/${gameId}/participants`)
      .pipe(
        tap(participants => this.logger.debug('GameQueryService: participants fetched', { gameId, requestId, count: participants.length })),
        catchError(error => this.handleErrorWithContext(error, 'getGameParticipants', { gameId, requestId }))
      );
  }

  /**
   * Vérifie si l'utilisateur peut rejoindre une game
   */
  canJoinGame(gameId: string): Observable<boolean> {
    return this.http.get<{ canJoin: boolean }>(`${this.apiUrl}/games/${gameId}/can-join`)
      .pipe(
        map(response => response.canJoin),
        catchError(this.handleError.bind(this))
      );
  }

  /**
   * Vérifie si l'utilisateur est l'hôte de la game
   */
  isGameHost(game: Game, userId: string): boolean {
    return game.creatorId === userId;
  }

  /**
   * Récupère les IDs des games archivées depuis le localStorage
   */
  getArchivedGameIds(): string[] {
    const archived = localStorage.getItem('archived_games');
    return archived ? JSON.parse(archived) : [];
  }

  /**
   * Filtre les games archivées
   */
  filterArchivedGames(games: Game[]): Game[] {
    const archivedIds = this.getArchivedGameIds();
    return games.filter(game => !archivedIds.includes(game.id));
  }

  /**
   * Gestion des erreurs standard
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'Une erreur est survenue';

    if (error.error instanceof ErrorEvent) {
      errorMessage = `Erreur: ${error.error.message}`;
    } else {
      switch (error.status) {
        case 400:
          errorMessage = error.error?.message || 'Requête invalide';
          break;
        case 401:
          errorMessage = 'Non autorisé';
          break;
        case 403:
          errorMessage = 'Accès refusé';
          break;
        case 404:
          errorMessage = 'Ressource non trouvée';
          break;
        case 409:
          errorMessage = error.error?.message || 'Conflit';
          break;
        case 422:
          errorMessage = error.error?.message || 'Données invalides';
          break;
        case 500:
          errorMessage = 'Erreur serveur';
          break;
        default:
          errorMessage = `Erreur ${error.status}: ${error.error?.message || 'Erreur inconnue'}`;
      }
    }

    this.logger.error('GameQueryService error', error);
    return throwError(() => new Error(errorMessage));
  }

  /**
   * Gestion des erreurs avec contexte enrichi
   */
  private handleErrorWithContext(error: HttpErrorResponse, method: string, context: Record<string, unknown>): Observable<never> {
    let errorMessage = 'Une erreur est survenue';

    if (error.error instanceof ErrorEvent) {
      errorMessage = `Erreur: ${error.error.message}`;
    } else {
      switch (error.status) {
        case 0:
          errorMessage = 'Erreur de communication avec le serveur (vérifiez votre connexion)';
          break;
        case 400:
          errorMessage = error.error?.message || 'Requête invalide';
          break;
        case 401:
          errorMessage = 'Non autorisé - Veuillez vous reconnecter';
          break;
        case 403:
          errorMessage = 'Accès refusé';
          break;
        case 404:
          errorMessage = 'Ressource non trouvée';
          break;
        case 409:
          errorMessage = error.error?.message || 'Conflit';
          break;
        case 422:
          errorMessage = error.error?.message || 'Données invalides';
          break;
        case 500:
          errorMessage = 'Erreur serveur interne';
          break;
        case 502:
        case 503:
        case 504:
          errorMessage = 'Service temporairement indisponible';
          break;
        default:
          errorMessage = `Erreur ${error.status}: ${error.error?.message || 'Erreur inconnue'}`;
      }
    }

    this.logger.error(`GameQueryService.${method}: HTTP error`, {
      ...context,
      status: error.status,
      statusText: error.statusText,
      errorMessage,
      url: error.url,
      timestamp: new Date().toISOString()
    });

    return throwError(() => new Error(errorMessage));
  }

  /**
   * Génère un ID de requête unique
   */
  private generateRequestId(): string {
    return `req_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
  }

  /**
   * Gestion des erreurs avec fallback data
   */
  private handleErrorWithFallback<T>(error: HttpErrorResponse, fallbackData: T[]): Observable<T[]> {
    const fallbackEnabled = environment.enableFallbackData && !environment.production;
    const isNetworkOrServerError = error.status === 0 || (error.status >= 500 && error.status < 600);

    if (fallbackEnabled && isNetworkOrServerError) {
      this.logger.warn('API call failed, using fallback data', { status: error.status });
      return of(fallbackData);
    }
    return this.handleError(error);
  }

  /**
   * Données mock pour les games utilisateur
   */
  private getMockUserGames(): Game[] {
    return MOCK_GAMES;
  }
}
