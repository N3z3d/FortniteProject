import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { LiveAnnouncer } from '@angular/cdk/a11y';

import { environment } from '../../../../environments/environment';
import { LoggerService } from '../../../core/services/logger.service';
import { TranslationService } from '../../../core/services/translation.service';
import { UiErrorFeedbackService } from '../../../core/services/ui-error-feedback.service';
import {
  extractBackendErrorDetails,
  toSafeUserMessage
} from '../../../core/utils/user-facing-error-message.util';
import {
  CreateGameRequest,
  DraftActionRequest,
  DraftSelectionRequest,
  DraftState,
  Game,
  GameResponse,
  InvitationCode,
  JoinGameRequest
} from '../models/game.interface';

@Injectable({
  providedIn: 'root'
})
export class GameCommandService {
  private readonly apiUrl = `${environment.apiUrl}/api`;

  constructor(
    private readonly http: HttpClient,
    private readonly logger: LoggerService,
    private readonly uiFeedback: UiErrorFeedbackService,
    private readonly announcer: LiveAnnouncer,
    private readonly t: TranslationService
  ) { }

  createGame(request: CreateGameRequest): Observable<Game> {
    return this.http.post<Game>(`${this.apiUrl}/games`, request).pipe(
      catchError(this.handleError.bind(this))
    );
  }

  joinGame(gameId: string): Observable<boolean> {
    const joinRequest: JoinGameRequest = {
      gameId,
      userId: 'current-user-id'
    };

    return this.http.post<GameResponse>(`${this.apiUrl}/games/join`, joinRequest).pipe(
      tap(response => {
        if (!response.success) {
          return;
        }

        const successMessage = this.t.t('games.detail.actions.joinSuccess');
        this.uiFeedback.showSuccessMessage(successMessage, 3000);
        this.announcer.announce(successMessage, 'polite');
      }),
      map(response => response.success),
      catchError((error: HttpErrorResponse) => {
        this.uiFeedback.showError(error, 'games.detail.actions.joinError', { duration: 5000 });
        return throwError(() => error);
      })
    );
  }

  joinGameWithCode(invitationCode: string): Observable<Game> {
    const normalizedCode = invitationCode.trim().toUpperCase();
    return this.http.post<Game>(`${this.apiUrl}/games/join-with-code`, { code: normalizedCode }).pipe(
      catchError(this.handleError.bind(this))
    );
  }

  generateInvitationCode(gameId: string): Observable<InvitationCode> {
    return this.http.post<InvitationCode>(`${this.apiUrl}/games/${gameId}/invitation-code`, {}).pipe(
      catchError(this.handleError.bind(this))
    );
  }

  regenerateInvitationCode(
    gameId: string,
    duration: '24h' | '48h' | '7d' | 'permanent' = 'permanent'
  ): Observable<Game> {
    return this.http.post<Game>(
      `${this.apiUrl}/games/${gameId}/regenerate-code`,
      {},
      { params: { duration } }
    ).pipe(
      tap(() => {
        this.announcer.announce(
          this.t.t('games.detail.actions.invitationCodeRegeneratedAnnounce'),
          'polite'
        );
      }),
      catchError(error => {
        this.logger.error('GameCommandService: failed to regenerate invitation code', error);
        return throwError(() => error);
      })
    );
  }

  renameGame(gameId: string, newName: string): Observable<Game> {
    return this.http.post<Game>(`${this.apiUrl}/games/${gameId}/rename`, { name: newName }).pipe(
      tap(() => {
        const announceMessage = this.t
          .t('games.detail.actions.renameAnnounce')
          .replace('{name}', newName);
        this.announcer.announce(announceMessage, 'polite');
      }),
      catchError(error => {
        this.logger.error('GameCommandService: failed to rename game', error);
        return throwError(() => error);
      })
    );
  }

  deleteGame(gameId: string): Observable<boolean> {
    return this.http.delete<GameResponse>(`${this.apiUrl}/games/${gameId}`).pipe(
      map(response => response.success),
      tap(() => {
        this.announcer.announce(this.t.t('games.detail.actions.deleteSuccessAnnounce'), 'polite');
      }),
      catchError(error => {
        this.logger.error('GameCommandService: failed to delete game', error);
        this.uiFeedback.showError(error, 'games.detail.actions.deleteError', { duration: 5000 });
        return throwError(() => error);
      })
    );
  }

  startDraft(gameId: string): Observable<boolean> {
    return this.http.post<GameResponse>(`${this.apiUrl}/games/${gameId}/start-draft`, {}).pipe(
      map(response => response.success),
      catchError(this.handleError.bind(this))
    );
  }

  finishDraft(gameId: string): Observable<Game> {
    return this.http.post<Game>(`${this.apiUrl}/games/${gameId}/draft/finish`, {}).pipe(
      tap(() => {
        this.announcer.announce(this.t.t('games.detail.actions.draftFinishedAnnounce'), 'polite');
      }),
      catchError(error => {
        this.logger.error('GameCommandService: failed to finish draft', error);
        this.uiFeedback.showError(error, 'games.detail.actions.draftFinishError', { duration: 5000 });
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

    return this.http.post<GameResponse>(`${this.apiUrl}/games/${gameId}/archive`, {}).pipe(
      map(response => response.success),
      catchError(() => throwError(() => new Error(this.t.t('games.detail.actions.archiveError'))))
    );
  }

  leaveGame(gameId: string): Observable<boolean> {
    return this.http.post<GameResponse>(`${this.apiUrl}/games/${gameId}/leave`, {}).pipe(
      map(response => response.success),
      catchError(this.handleError.bind(this))
    );
  }

  initializeDraft(gameId: string): Observable<DraftState> {
    return this.http.post<DraftState>(`${this.apiUrl}/games/${gameId}/draft/initialize`, {}).pipe(
      catchError(this.handleError.bind(this))
    );
  }

  makePlayerSelection(gameId: string, playerId: string): Observable<boolean> {
    const request: DraftSelectionRequest = {
      gameId,
      playerId
    };

    return this.http.post<GameResponse>(`${this.apiUrl}/games/${gameId}/draft/select`, request).pipe(
      map(response => response.success),
      catchError(this.handleError.bind(this))
    );
  }

  pauseDraft(gameId: string): Observable<boolean> {
    const request: DraftActionRequest = {
      gameId,
      action: 'pause'
    };

    return this.http.post<GameResponse>(`${this.apiUrl}/games/${gameId}/draft/pause`, request).pipe(
      tap(response => {
        if (response.success) {
          this.announcer.announce(this.t.t('games.detail.actions.draftPausedAnnounce'), 'polite');
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

    return this.http.post<GameResponse>(`${this.apiUrl}/games/${gameId}/draft/resume`, request).pipe(
      tap(response => {
        if (response.success) {
          this.announcer.announce(this.t.t('games.detail.actions.draftResumedAnnounce'), 'polite');
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

    return this.http.post<GameResponse>(`${this.apiUrl}/games/${gameId}/draft/cancel`, request).pipe(
      map(response => response.success),
      catchError(this.handleError.bind(this))
    );
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    const safeBackendMessage = toSafeUserMessage(extractBackendErrorDetails(error).message);
    const errorMessage = this.resolveErrorMessage(error, safeBackendMessage);

    this.logger.error('GameCommandService error', error);
    return throwError(() => new Error(errorMessage));
  }

  private resolveErrorMessage(error: HttpErrorResponse, backendMessage: string | null): string {
    if (error.error instanceof ErrorEvent) {
      return `${this.t.t('common.error')}: ${error.error.message}`;
    }

    switch (error.status) {
      case 400:
        return backendMessage || this.t.t('errors.validation');
      case 401:
        return this.t.t('errors.unauthorized');
      case 403:
        return backendMessage || this.t.t('errors.handler.forbiddenMessage');
      case 404:
        return backendMessage || this.t.t('errors.notFound');
      case 409:
        return backendMessage || this.t.t('errors.validation');
      case 422:
        return backendMessage || this.t.t('errors.validation');
      case 500:
        return this.t.t('errors.handler.serverErrorMessage');
      default:
        return backendMessage || this.t.t('errors.generic');
    }
  }

}
