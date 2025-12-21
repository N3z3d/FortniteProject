import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LiveAnnouncer } from '@angular/cdk/a11y';
import { environment } from '../../../../environments/environment';
import { LoggerService } from '../../../core/services/logger.service';
import { MOCK_GAMES } from '../../../core/data/mock-game-data';
import {
  Game,
  CreateGameRequest,
  JoinGameRequest,
  GameResponse,
  GameParticipant,
  DraftState,
  DraftSelectionRequest,
  DraftActionRequest,
  DraftStatistics,
  DraftHistoryEntry,
  InvitationCode
} from '../models/game.interface';

/**
 * PHASE 1A: CONSOLIDATED GameService with accessibility features
 * Unified service combining game operations and accessibility support
 * Replaces multiple duplicated services (SimplifiedGameService, AccessibleGameService)
 */
@Injectable({
  providedIn: 'root'
})
export class GameService {
  private readonly apiUrl = `${environment.apiUrl}/api`;

  constructor(
    private readonly http: HttpClient,
    private readonly logger: LoggerService,
    private readonly snackBar: MatSnackBar,
    private readonly announcer: LiveAnnouncer
  ) { }

  /**
   * Récupère toutes les games
   */
  getAllGames(): Observable<Game[]> {
    return this.http.get<Game[]>(`${this.apiUrl}/games`)
      .pipe(
        catchError(this.handleError)
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
   * Récupère les games disponibles (alias pour getAllGames pour compatibilité tests)
   */
  getAvailableGames(): Observable<Game[]> {
    return this.getAllGames();
  }

  /**
   * Récupère une game par son ID
   */
  getGameById(id: string): Observable<Game> {
    const requestId = this.generateRequestId();
    this.logger.info('GameService: fetching game by ID', { gameId: id, requestId });

    return this.http.get<Game>(`${this.apiUrl}/games/${id}`)
      .pipe(
        tap(game => this.logger.debug('GameService: game fetched successfully', { gameId: id, requestId, gameName: game.name })),
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
   * Crée une nouvelle game
   */
  createGame(request: CreateGameRequest): Observable<Game> {
    return this.http.post<Game>(`${this.apiUrl}/games`, request)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Rejoint une game avec accessibilité intégrée
   * PHASE 1A: Enhanced with accessibility features from consolidated services
   */
  joinGame(gameId: string): Observable<boolean> {
    const joinRequest: JoinGameRequest = { gameId };
    
    return this.http.post<GameResponse>(`${this.apiUrl}/games/join`, joinRequest)
      .pipe(
        tap(response => {
          if (response.success) {
            // Accessibility feedback for successful join
            this.snackBar.open(
              `Successfully joined game`, 
              'View Game', 
              { duration: 5000 }
            );
            
            this.announcer.announce(
              `Successfully joined game. You can now view game details or wait for the draft to begin.`
            );
          }
        }),
        map(response => response.success),
        catchError(error => {
          // Enhanced error handling with accessibility
          const message = error.error?.message || 'Failed to join game';
          this.snackBar.open(message, 'Retry', { 
            duration: 8000,
            politeness: 'assertive'
          });
          this.announcer.announce(`Error: ${message}. Please try again.`);
          return this.handleError(error);
        })
      );
  }

  /**
   * Rejoint une game avec un code d'invitation
   */
  joinGameWithCode(invitationCode: string): Observable<Game> {
    const joinRequest: JoinGameRequest = { 
      gameId: '', // Sera déterminé par le code
      invitationCode 
    };
    
    return this.http.post<Game>(`${this.apiUrl}/games/join-by-code`, joinRequest)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Génère un code d'invitation pour une game
   */
  generateInvitationCode(gameId: string): Observable<InvitationCode> {
    return this.http.get<InvitationCode>(`${this.apiUrl}/games/${gameId}/invitation-code`)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Valide un code d'invitation
   */
  validateInvitationCode(code: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.apiUrl}/games/validate-invitation/${code}`)
      .pipe(
        catchError(this.handleError)
      );
  }


  /**
   * Démarre le draft d'une game
   */
  startDraft(gameId: string): Observable<boolean> {
    return this.http.post<GameResponse>(`${this.apiUrl}/games/${gameId}/start-draft`, {})
      .pipe(
        map(response => response.success),
        catchError(this.handleError)
      );
  }

  /**
   * PHASE 1A: Finit le draft avec accessibilité (consolidé depuis SimplifiedGameService)
   */
  finishDraft(gameId: string): Observable<Game> {
    return this.http.post<Game>(`${this.apiUrl}/games/${gameId}/finish-draft`, {}).pipe(
      tap(game => {
        this.snackBar.open('Draft completed successfully', 'View Teams', { duration: 5000 });
        this.announcer.announce(
          `Draft completed for ${game.name}. All teams are ready and the game is now active.`
        );
      }),
      catchError(error => {
        const message = error.error?.message || 'Failed to finish draft';
        this.snackBar.open(message, 'Retry', { 
          duration: 8000,
          politeness: 'assertive'
        });
        return this.handleError(error);
      })
    );
  }

  /**
   * Supprime une game (hard delete)
   * @deprecated Use archiveGame for soft delete instead
   */
  deleteGame(gameId: string): Observable<boolean> {
    return this.http.delete<GameResponse>(`${this.apiUrl}/games/${gameId}`)
      .pipe(
        map(response => response.success),
        catchError(this.handleError)
      );
  }

  /**
   * Archive une game (soft delete)
   * La game reste en base mais n'apparaît plus dans les listes actives
   */
  archiveGame(gameId: string): Observable<boolean> {
    // En mode offline/dev, utiliser localStorage
    const storedGames = localStorage.getItem('archivedGames');
    const archivedGames = storedGames ? JSON.parse(storedGames) : [];

    if (!archivedGames.includes(gameId)) {
      archivedGames.push(gameId);
      localStorage.setItem('archivedGames', JSON.stringify(archivedGames));
    }

    // En mode online, appeler l'API
    return this.http.patch<GameResponse>(`${this.apiUrl}/games/${gameId}/archive`, {})
      .pipe(
        map(response => response.success),
        catchError(() => {
          // Fallback: si l'API échoue, on considère que le localStorage suffit
          return of(true);
        })
      );
  }

  /**
   * Quitter une game (pour les participants non-host)
   */
  leaveGame(gameId: string): Observable<boolean> {
    return this.http.post<GameResponse>(`${this.apiUrl}/games/${gameId}/leave`, {})
      .pipe(
        map(response => response.success),
        catchError(this.handleError)
      );
  }

  /**
   * Vérifie si l'utilisateur est le créateur/host d'une game
   */
  isGameHost(game: Game, userId: string): boolean {
    return game.creatorName === userId ||
           game.participants?.some(p => p.isCreator && p.username === userId) || false;
  }

  /**
   * Récupère la liste des games archivées
   */
  getArchivedGameIds(): string[] {
    const stored = localStorage.getItem('archivedGames');
    return stored ? JSON.parse(stored) : [];
  }

  /**
   * Filtre les games pour exclure les archivées
   */
  filterArchivedGames(games: Game[]): Game[] {
    const archivedIds = this.getArchivedGameIds();
    return games.filter(game => !archivedIds.includes(game.id));
  }

  /**
   * Initialise le draft d'une game
   */
  initializeDraft(gameId: string): Observable<DraftState> {
    return this.http.post<DraftState>(`${this.apiUrl}/drafts/${gameId}/initialize`, {})
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Récupère l'état du draft
   */
  getDraftState(gameId: string): Observable<DraftState> {
    return this.http.get<DraftState>(`${this.apiUrl}/drafts/${gameId}/state`)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Effectue une sélection de joueur
   */
  makePlayerSelection(gameId: string, playerId: string): Observable<boolean> {
    const selection: DraftSelectionRequest = { gameId, playerId };
    
    return this.http.post<GameResponse>(`${this.apiUrl}/drafts/${gameId}/select`, selection)
      .pipe(
        map(response => response.success),
        catchError(this.handleError)
      );
  }

  /**
   * Met en pause le draft avec accessibilité (PHASE 1A enhanced)
   */
  pauseDraft(gameId: string): Observable<boolean> {
    const request: DraftActionRequest = { gameId, action: 'pause' };
    
    return this.http.post<GameResponse>(`${this.apiUrl}/drafts/${gameId}/action`, request)
      .pipe(
        tap(response => {
          if (response.success) {
            this.snackBar.open('Draft paused', 'Resume', { duration: 5000 });
            this.announcer.announce('Draft paused. You can resume when ready.');
          }
        }),
        map(response => response.success),
        catchError(error => {
          const message = error.error?.message || 'Failed to pause draft';
          this.snackBar.open(message, 'Retry', { duration: 8000, politeness: 'assertive' });
          return this.handleError(error);
        })
      );
  }

  /**
   * Reprend le draft avec accessibilité (PHASE 1A enhanced)
   */
  resumeDraft(gameId: string): Observable<boolean> {
    const request: DraftActionRequest = { gameId, action: 'resume' };
    
    return this.http.post<GameResponse>(`${this.apiUrl}/drafts/${gameId}/action`, request)
      .pipe(
        tap(response => {
          if (response.success) {
            this.snackBar.open('Draft resumed', 'Monitor', { duration: 5000 });
            this.announcer.announce('Draft resumed. Players can continue making selections.');
          }
        }),
        map(response => response.success),
        catchError(error => {
          const message = error.error?.message || 'Failed to resume draft';
          this.snackBar.open(message, 'Retry', { duration: 8000, politeness: 'assertive' });
          return this.handleError(error);
        })
      );
  }

  /**
   * Annule le draft
   */
  cancelDraft(gameId: string): Observable<boolean> {
    const request: DraftActionRequest = { gameId, action: 'cancel' };
    
    return this.http.post<GameResponse>(`${this.apiUrl}/drafts/${gameId}/action`, request)
      .pipe(
        map(response => response.success),
        catchError(this.handleError)
      );
  }

  /**
   * Récupère l'historique du draft
   */
  getDraftHistory(gameId: string): Observable<DraftHistoryEntry[]> {
    return this.http.get<DraftHistoryEntry[]>(`${this.apiUrl}/drafts/${gameId}/history`)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Récupère les statistiques du draft
   */
  getDraftStatistics(gameId: string): Observable<DraftStatistics> {
    return this.http.get<DraftStatistics>(`${this.apiUrl}/drafts/${gameId}/statistics`)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Récupère les participants d'une game
   */
  getGameParticipants(gameId: string): Observable<GameParticipant[]> {
    const requestId = this.generateRequestId();
    this.logger.info('GameService: fetching game participants', { gameId, requestId });

    return this.http.get<GameParticipant[]>(`${this.apiUrl}/games/${gameId}/participants`)
      .pipe(
        tap(participants => this.logger.debug('GameService: participants fetched', { gameId, requestId, count: participants.length })),
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
        catchError(this.handleError)
      );
  }

  /**
   * Gestion centralisée des erreurs
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'Une erreur est survenue';

    if (error.error instanceof ErrorEvent) {
      // Erreur côté client
      errorMessage = `Erreur: ${error.error.message}`;
    } else {
      // Erreur côté serveur
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

    this.logger.error('GameService error', error);
    return throwError(() => new Error(errorMessage));
  }

  /**
   * Gestion des erreurs avec contexte enrichi et request-id
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

    this.logger.error(`GameService.${method}: HTTP error`, {
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
   * Génère un ID de requête unique pour le traçage
   */
  private generateRequestId(): string {
    return `req_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
  }

  /**
   * Gestion des erreurs avec données de fallback pour le développement
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
