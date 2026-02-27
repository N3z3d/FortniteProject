import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';

import { environment } from '../../../../environments/environment';
import { LoggerService } from '../../../core/services/logger.service';
import { TranslationService } from '../../../core/services/translation.service';
import { MOCK_GAMES } from '../../../core/data/mock-game-data';
import { secureRandomId } from '../../../shared/utils/secure-random.util';
import {
  DraftHistoryEntry,
  DraftState,
  DraftStatistics,
  Game,
  GameParticipant
} from '../models/game.interface';

/**
 * CQRS Query Service - read operations only.
 */
@Injectable({
  providedIn: 'root'
})
export class GameQueryService {
  private readonly apiUrl = `${environment.apiUrl}/api`;

  constructor(
    private readonly http: HttpClient,
    private readonly logger: LoggerService,
    private readonly t: TranslationService
  ) { }

  getAllGames(): Observable<Game[]> {
    return this.http.get<Game[]>(`${this.apiUrl}/games`).pipe(
      catchError(this.handleError.bind(this))
    );
  }

  getUserGames(): Observable<Game[]> {
    return this.http.get<Game[]>(`${this.apiUrl}/games/my-games`).pipe(
      catchError(error => this.handleErrorWithFallback(error, this.getMockUserGames()))
    );
  }

  getAvailableGames(): Observable<Game[]> {
    return this.getAllGames();
  }

  getGameById(id: string): Observable<Game> {
    const requestId = this.generateRequestId();
    this.logger.info('GameQueryService: fetching game by ID', { gameId: id, requestId });

    return this.http.get<Game>(`${this.apiUrl}/games/${id}`).pipe(
      tap(game => this.logger.debug('GameQueryService: game fetched successfully', {
        gameId: id,
        requestId,
        gameName: game.name
      })),
      catchError(error => this.handleErrorWithContext(error, 'getGameById', { gameId: id, requestId }))
    );
  }

  getGameDetails(id: string): Observable<Game> {
    return this.getGameById(id);
  }

  validateInvitationCode(code: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.apiUrl}/games/validate-code/${code}`).pipe(
      catchError(this.handleError.bind(this))
    );
  }

  getDraftState(gameId: string): Observable<DraftState> {
    return this.http.get<DraftState>(`${this.apiUrl}/games/${gameId}/draft/state`).pipe(
      catchError(this.handleError.bind(this))
    );
  }

  getDraftHistory(gameId: string): Observable<DraftHistoryEntry[]> {
    return this.http.get<DraftHistoryEntry[]>(`${this.apiUrl}/games/${gameId}/draft/history`).pipe(
      catchError(this.handleError.bind(this))
    );
  }

  getDraftStatistics(gameId: string): Observable<DraftStatistics> {
    return this.http.get<DraftStatistics>(`${this.apiUrl}/games/${gameId}/draft/statistics`).pipe(
      catchError(this.handleError.bind(this))
    );
  }

  getGameParticipants(gameId: string): Observable<GameParticipant[]> {
    const requestId = this.generateRequestId();
    this.logger.info('GameQueryService: fetching game participants', { gameId, requestId });

    return this.http.get<GameParticipant[]>(`${this.apiUrl}/games/${gameId}/participants`).pipe(
      tap(participants => this.logger.debug('GameQueryService: participants fetched', {
        gameId,
        requestId,
        count: participants.length
      })),
      catchError(error => this.handleErrorWithContext(error, 'getGameParticipants', { gameId, requestId }))
    );
  }

  canJoinGame(gameId: string): Observable<boolean> {
    return this.http.get<{ canJoin: boolean }>(`${this.apiUrl}/games/${gameId}/can-join`).pipe(
      map(response => response.canJoin),
      catchError(this.handleError.bind(this))
    );
  }

  isGameHost(game: Game, userIdentifier: string): boolean {
    if (!game || !userIdentifier) {
      return false;
    }

    const normalizedIdentifier = userIdentifier.trim().toLowerCase();
    const normalizedCreatorId = (game.creatorId || '').trim().toLowerCase();
    const normalizedCreatorName = (game.creatorName || '').trim().toLowerCase();

    if (normalizedCreatorId === normalizedIdentifier || normalizedCreatorName === normalizedIdentifier) {
      return true;
    }

    if (game.participants) {
      return game.participants.some(
        participant => participant.isCreator && (
          (participant.username || '').trim().toLowerCase() === normalizedIdentifier ||
          (participant.id || '').trim().toLowerCase() === normalizedIdentifier
        )
      );
    }

    return false;
  }

  getArchivedGameIds(): string[] {
    const archived = localStorage.getItem('archived_games');
    return archived ? JSON.parse(archived) : [];
  }

  filterArchivedGames(games: Game[]): Game[] {
    const archivedIds = this.getArchivedGameIds();
    return games.filter(game => !archivedIds.includes(game.id));
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    const backendMessage = this.extractBackendMessage(error);
    const errorMessage = this.resolveHttpErrorMessage(error, backendMessage, false);

    this.logger.error('GameQueryService error', error);
    return throwError(() => new Error(errorMessage));
  }

  private handleErrorWithContext(
    error: HttpErrorResponse,
    method: string,
    context: Record<string, unknown>
  ): Observable<never> {
    const backendMessage = this.extractBackendMessage(error);
    const errorMessage = this.resolveHttpErrorMessage(error, backendMessage, true);

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

  private resolveHttpErrorMessage(
    error: HttpErrorResponse,
    backendMessage: string | null,
    withNetworkMessage: boolean
  ): string {
    if (error.error instanceof ErrorEvent) {
      return `${this.t.t('common.error')}: ${error.error.message}`;
    }

    if (withNetworkMessage && error.status === 0) {
      return this.t.t('errors.network');
    }

    switch (error.status) {
      case 400:
      case 409:
      case 422:
        return backendMessage || this.t.t('errors.validation');
      case 401:
        return this.t.t('errors.unauthorized');
      case 403:
        return backendMessage || this.t.t('errors.handler.forbiddenMessage');
      case 404:
        return backendMessage || this.t.t('errors.notFound');
      case 500:
        return backendMessage || this.t.t('errors.handler.serverErrorMessage');
      case 502:
      case 503:
      case 504:
        return this.t.t('errors.network');
      default:
        return backendMessage || this.t.t('errors.generic');
    }
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

  private generateRequestId(): string {
    return `req_${Date.now()}_${secureRandomId(7)}`;
  }

  private handleErrorWithFallback<T>(
    error: HttpErrorResponse,
    fallbackData: T[]
  ): Observable<T[]> {
    const fallbackEnabled = environment.enableFallbackData && !environment.production;
    const isNetworkOrServerError = error.status === 0 || (error.status >= 500 && error.status < 600);

    if (fallbackEnabled && isNetworkOrServerError) {
      this.logger.warn('API call failed, using fallback data', { status: error.status });
      return of(fallbackData);
    }

    return this.handleError(error);
  }

  private getMockUserGames(): Game[] {
    return MOCK_GAMES;
  }
}
