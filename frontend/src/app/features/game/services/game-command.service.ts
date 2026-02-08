import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LiveAnnouncer } from '@angular/cdk/a11y';
import { environment } from '../../../../environments/environment';
import { LoggerService } from '../../../core/services/logger.service';
import {
  Game,
  CreateGameRequest,
  JoinGameRequest,
  GameResponse,
  DraftState,
  DraftSelectionRequest,
  DraftActionRequest,
  InvitationCode
} from '../models/game.interface';

@Injectable({
  providedIn: 'root'
})
export class GameCommandService {
  private readonly apiUrl = `${environment.apiUrl}/api`;

  constructor(
    private readonly http: HttpClient,
    private readonly logger: LoggerService,
    private readonly snackBar: MatSnackBar,
    private readonly announcer: LiveAnnouncer
  ) { }

  createGame(request: CreateGameRequest): Observable<Game> {
    return this.http.post<Game>(`${this.apiUrl}/games`, request)
      .pipe(
        catchError(this.handleError.bind(this))
      );
  }

  joinGame(gameId: string): Observable<boolean> {
    const joinRequest: JoinGameRequest = {
      gameId,
      userId: 'current-user-id'
    };

    return this.http.post<GameResponse>(`${this.apiUrl}/games/${gameId}/join`, joinRequest)
      .pipe(
        tap(response => {
          if (response.success) {
            this.snackBar.open('Vous avez rejoint la partie avec succès', 'OK', {
              duration: 3000,
              horizontalPosition: 'center',
              verticalPosition: 'top'
            });

            this.announcer.announce('Vous avez rejoint la partie avec succès', 'polite');
          }
        }),
        map(response => response.success),
        catchError(error => {
          this.snackBar.open('Erreur lors de la tentative de rejoindre la partie', 'Fermer', {
            duration: 5000,
            panelClass: ['error-snackbar']
          });
          return throwError(() => error);
        })
      );
  }

  joinGameWithCode(invitationCode: string): Observable<Game> {
    const normalizedCode = invitationCode.trim().toUpperCase();
    return this.http.post<Game>(
      `${this.apiUrl}/games/join-with-code`,
      { code: normalizedCode }
    ).pipe(
      catchError(this.handleError.bind(this))
    );
  }

  generateInvitationCode(gameId: string): Observable<InvitationCode> {
    return this.http.post<InvitationCode>(`${this.apiUrl}/games/${gameId}/invitation-code`, {})
      .pipe(
        catchError(this.handleError.bind(this))
      );
  }

  regenerateInvitationCode(gameId: string, duration: '24h' | '48h' | '7d' | 'permanent' = 'permanent'): Observable<Game> {
    return this.http.post<Game>(
      `${this.apiUrl}/games/${gameId}/regenerate-code`,
      {},
      { params: { duration } }
    ).pipe(
      tap(game => {
        this.announcer.announce('Code d\'invitation régénéré avec succès', 'polite');
      }),
      catchError(error => {
        this.logger.error('GameCommandService: failed to regenerate invitation code', error);
        return throwError(() => error);
      })
    );
  }

  renameGame(gameId: string, newName: string): Observable<Game> {
    return this.http.post<Game>(`${this.apiUrl}/games/${gameId}/rename`, { name: newName })
      .pipe(
        tap(game => {
          this.announcer.announce(`Partie renommée en ${newName}`, 'polite');
        }),
        catchError(error => {
          this.logger.error('GameCommandService: failed to rename game', error);
          return throwError(() => error);
        })
      );
  }

  deleteGame(gameId: string): Observable<boolean> {
    return this.http.delete<GameResponse>(`${this.apiUrl}/games/${gameId}`)
      .pipe(
        map(response => response.success),
        tap(() => {
          this.announcer.announce('Partie supprimée avec succès', 'polite');
        }),
        catchError(error => {
          this.logger.error('GameCommandService: failed to delete game', error);
          this.snackBar.open('Erreur lors de la suppression de la partie', 'Fermer', {
            duration: 5000,
            panelClass: ['error-snackbar']
          });
          return throwError(() => error);
        })
      );
  }

  startDraft(gameId: string): Observable<boolean> {
    return this.http.post<GameResponse>(`${this.apiUrl}/games/${gameId}/start-draft`, {})
      .pipe(
        map(response => response.success),
        catchError(this.handleError.bind(this))
      );
  }

  finishDraft(gameId: string): Observable<Game> {
    return this.http.post<Game>(`${this.apiUrl}/games/${gameId}/draft/finish`, {}).pipe(
      tap(game => {
        this.announcer.announce('Draft terminé avec succès', 'polite');
      }),
      catchError(error => {
        this.logger.error('GameCommandService: failed to finish draft', error);
        this.snackBar.open('Erreur lors de la finalisation du draft', 'Fermer', {
          duration: 5000,
          panelClass: ['error-snackbar']
        });
        return throwError(() => error);
      })
    );
  }

  archiveGame(gameId: string): Observable<boolean> {
    const archivedGames = localStorage.getItem('archived_games');
    const archived: string[] = archivedGames ? JSON.parse(archivedGames) : [];

  if (!archived.includes(gameId)) {
      archived.push(gameId);
      localStorage.setItem('archived_games', JSON.stringify(archived));
    }

    return this.http.post<GameResponse>(
      `${this.apiUrl}/games/${gameId}/archive`,
      {}
    ).pipe(
        map(response => response.success),
        catchError(() => {
          return throwError(() => new Error('Échec de l\'archivage de la partie'));
        })
      );
  }

  leaveGame(gameId: string): Observable<boolean> {
    return this.http.post<GameResponse>(`${this.apiUrl}/games/${gameId}/leave`, {})
      .pipe(
        map(response => response.success),
        catchError(this.handleError.bind(this))
      );
  }

  initializeDraft(gameId: string): Observable<DraftState> {
    return this.http.post<DraftState>(`${this.apiUrl}/games/${gameId}/draft/initialize`, {})
      .pipe(
        catchError(this.handleError.bind(this))
      );
  }

  makePlayerSelection(gameId: string, playerId: string): Observable<boolean> {
    const request: DraftSelectionRequest = {
      gameId,
      playerId
    };

    return this.http.post<GameResponse>(`${this.apiUrl}/games/${gameId}/draft/select`, request)
      .pipe(
        map(response => response.success),
        catchError(this.handleError.bind(this))
      );
  }

  pauseDraft(gameId: string): Observable<boolean> {
    const request: DraftActionRequest = {
      gameId,
      action: 'pause'
    };

    return this.http.post<GameResponse>(`${this.apiUrl}/games/${gameId}/draft/pause`, request)
      .pipe(
        tap(response => {
          if (response.success) {
            this.announcer.announce('Draft mis en pause', 'polite');
          }
        }),
        map(response => response.success),
        catchError(error => {
          this.logger.error('GameCommandService: failed to pause draft', error);
          return throwError(() => error);
        })
      );
  }

  resumeDraft(gameId: string): Observable<boolean> {
    const request: DraftActionRequest = {
      gameId,
      action: 'resume'
    };

    return this.http.post<GameResponse>(`${this.apiUrl}/games/${gameId}/draft/resume`, request)
      .pipe(
        tap(response => {
          if (response.success) {
            this.announcer.announce('Draft repris', 'polite');
          }
        }),
        map(response => response.success),
        catchError(error => {
          this.logger.error('GameCommandService: failed to resume draft', error);
          return throwError(() => error);
        })
      );
  }

  cancelDraft(gameId: string): Observable<boolean> {
    const request: DraftActionRequest = {
      gameId,
      action: 'cancel'
    };

    return this.http.post<GameResponse>(`${this.apiUrl}/games/${gameId}/draft/cancel`, request)
      .pipe(
        map(response => response.success),
        catchError(this.handleError.bind(this))
      );
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'Une erreur est survenue';
    const backendMessage = this.extractBackendMessage(error);

    if (error.error instanceof ErrorEvent) {
      errorMessage = `Erreur: ${error.error.message}`;
    } else {
      switch (error.status) {
        case 400:
          errorMessage = backendMessage || 'Requete invalide';
          break;
        case 401:
          errorMessage = 'Non autorise';
          break;
        case 403:
          errorMessage = backendMessage || 'Acces refuse';
          break;
        case 404:
          errorMessage = backendMessage || 'Ressource non trouvee';
          break;
        case 409:
          errorMessage = backendMessage || 'Conflit';
          break;
        case 422:
          errorMessage = backendMessage || 'Donnees invalides';
          break;
        case 500:
          errorMessage = backendMessage || 'Erreur serveur';
          break;
        default:
          errorMessage = backendMessage || `Erreur ${error.status}: Erreur inconnue`;
      }
    }

    this.logger.error('GameCommandService error', error);
    return throwError(() => new Error(errorMessage));
  }

  private extractBackendMessage(error: HttpErrorResponse): string | null {
    const payload = error.error;

    if (!payload) {
      return null;
    }

    if (typeof payload === 'string') {
      const trimmedPayload = payload.trim();
      if (!trimmedPayload) {
        return null;
      }

      try {
        const parsedPayload = JSON.parse(trimmedPayload) as { message?: string };
        return parsedPayload.message || trimmedPayload;
      } catch {
        return trimmedPayload;
      }
    }

    if (typeof payload === 'object') {
      const payloadObject = payload as { message?: string; error?: { message?: string } | string };
      if (payloadObject.message) {
        return payloadObject.message;
      }
      if (typeof payloadObject.error === 'string') {
        return payloadObject.error;
      }
      if (payloadObject.error?.message) {
        return payloadObject.error.message;
      }
    }

    return null;
  }
}
