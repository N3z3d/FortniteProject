import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LiveAnnouncer } from '@angular/cdk/a11y';
import { environment } from '../../../../environments/environment';
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
import { CsvDataService } from './csv-data.service';

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
    private http: HttpClient,
    private csvDataService: CsvDataService,
    private snackBar: MatSnackBar,
    private announcer: LiveAnnouncer
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
    return this.http.get<Game>(`${this.apiUrl}/games/${id}`)
      .pipe(
        catchError(this.handleError)
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
   * Supprime une game
   */
  deleteGame(gameId: string): Observable<boolean> {
    return this.http.delete<GameResponse>(`${this.apiUrl}/games/${gameId}`)
      .pipe(
        map(response => response.success),
        catchError(this.handleError)
      );
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
    return this.http.get<GameParticipant[]>(`${this.apiUrl}/games/${gameId}/participants`)
      .pipe(
        catchError(this.handleError)
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
    
    console.error('GameService error:', error);
    return throwError(() => new Error(errorMessage));
  }

  /**
   * Gestion des erreurs avec données de fallback pour le développement
   */
  private handleErrorWithFallback<T>(error: HttpErrorResponse, fallbackData: T[]): Observable<T[]> {
    const fallbackEnabled = environment.enableFallbackData && !environment.production;
    const isNetworkOrServerError = error.status === 0 || (error.status >= 500 && error.status < 600);

    if (fallbackEnabled && isNetworkOrServerError) {
      console.warn('API call failed, using fallback data:', error.status);
      return of(fallbackData);
    }
    return this.handleError(error);
  }

  /**
   * Données mock pour les games utilisateur
   */
  private getMockUserGames(): Game[] {
    return [
      {
        id: 'mock-game-1',
        name: 'Game Demo de Thibaut',
        description: 'Partie de démonstration en mode développement',
        status: 'ACTIVE',
        maxParticipants: 8,
        participantCount: 3,
        createdAt: new Date('2025-08-01'),
        creatorName: 'Thibaut',
        canJoin: true
      },
      {
        id: 'mock-game-2',
        name: 'Tournoi Test',
        description: 'Tournoi de test avec données de fallback',
        status: 'DRAFT',
        maxParticipants: 12,
        participantCount: 8,
        createdAt: new Date('2025-08-05'),
        creatorName: 'System',
        canJoin: true
      }
    ];
  }

  /**
   * Données mock pour les games disponibles - DEPRECATED
   */
  private getMockAvailableGames(): Game[] {
    return [
      {
        id: 'mock-available-1',
        name: 'Compétition Ouverte',
        description: 'Rejoignez cette compétition en cours',
        status: 'RECRUITING',
        maxParticipants: 16,
        participantCount: 12,
        createdAt: new Date('2025-08-06'),
        creatorName: 'Admin',
        canJoin: true
      },
      {
        id: 'mock-available-2',
        name: 'Liga Noob Friendly',
        description: 'Parfait pour débuter dans les fantasy leagues',
        status: 'RECRUITING',
        maxParticipants: 8,
        participantCount: 4,
        createdAt: new Date('2025-08-07'),
        creatorName: 'Helper',
        canJoin: true
      }
    ];
  }
} 
