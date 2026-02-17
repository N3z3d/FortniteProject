import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { environment } from '../../../../environments/environment';
import { TranslationService } from '../../../core/services/translation.service';
import { LoggerService } from '../../../core/services/logger.service';
import { MOCK_GAME_PARTICIPANTS, MOCK_GAMES } from '../../../core/data/mock-game-data';
import { Game, GameParticipant } from '../models/game.interface';
import { GameApiMapper } from '../mappers/game-api.mapper';

/**
 * Service dedicated to game read-model data retrieval.
 * Uses API first with optional development fallback data.
 */
@Injectable({
  providedIn: 'root'
})
export class GameDataService {
  private readonly apiUrl = `${environment.apiUrl}/api`;

  constructor(
    private readonly http: HttpClient,
    private readonly t: TranslationService,
    private readonly logger: LoggerService
  ) { }

  getGameById(gameId: string): Observable<Game> {
    if (!gameId || gameId.trim() === '') {
      return throwError(() => new Error(this.t.t('errors.validation')));
    }

    return this.http.get<unknown>(`${this.apiUrl}/games/${gameId}`).pipe(
      map(apiResponse => {
        try {
          return GameApiMapper.mapApiResponseToGame(apiResponse);
        } catch (mappingError) {
          this.logger.error('GameDataService: game payload mapping failed', {
            operation: 'getGameById',
            gameId,
            error: mappingError
          });
          throw new Error(this.t.t('errors.generic'));
        }
      }),
      catchError((error: HttpErrorResponse) => {
        if (this.shouldUseFallback(error)) {
          const fallbackGame = this.findFallbackGame(gameId);
          if (fallbackGame) {
            return of(fallbackGame);
          }
        }
        return this.handleHttpError(error);
      })
    );
  }

  getGameParticipants(gameId: string): Observable<GameParticipant[]> {
    if (!gameId || gameId.trim() === '') {
      return throwError(() => new Error(this.t.t('errors.validation')));
    }

    return this.http.get<unknown[]>(`${this.apiUrl}/games/${gameId}/participants`).pipe(
      map(apiParticipants => {
        try {
          if (!Array.isArray(apiParticipants)) {
            this.logger.warn('GameDataService: participants payload is not an array', {
              operation: 'getGameParticipants',
              gameId
            });
            return [];
          }
          return GameApiMapper.mapApiParticipants(apiParticipants);
        } catch (mappingError) {
          this.logger.error('GameDataService: participants payload mapping failed', {
            operation: 'getGameParticipants',
            gameId,
            error: mappingError
          });
          return [];
        }
      }),
      catchError((error: HttpErrorResponse) => {
        if (this.shouldUseFallback(error)) {
          return of(this.getFallbackParticipants(gameId));
        }
        return this.handleHttpError(error);
      })
    );
  }

  getUserGames(): Observable<Game[]> {
    return this.http.get<unknown[]>(`${this.apiUrl}/games/my-games`).pipe(
      map(apiGames => {
        if (!Array.isArray(apiGames)) {
          this.logger.warn('GameDataService: user games payload is not an array', {
            operation: 'getUserGames'
          });
          return [];
        }

        return apiGames
          .map(apiGame => {
            try {
              return GameApiMapper.mapApiResponseToGame(apiGame);
            } catch (mappingError) {
              const gameId = (apiGame as { id?: string })?.id || 'unknown';
              this.logger.error('GameDataService: user game payload mapping failed', {
                operation: 'getUserGames',
                gameId,
                error: mappingError
              });
              return null;
            }
          })
          .filter((game): game is Game => game !== null);
      }),
      catchError(error => this.handleHttpError(error))
    );
  }

  verifyGameExists(gameId: string): Observable<boolean> {
    if (!gameId || gameId.trim() === '') {
      return throwError(() => new Error(this.t.t('errors.validation')));
    }

    return this.getGameById(gameId).pipe(
      map(() => true),
      catchError(() => throwError(() => new Error(this.t.t('errors.notFound'))))
    );
  }

  calculateGameStatistics(game: Game): {
    fillPercentage: number;
    availableSlots: number;
    isNearlyFull: boolean;
    canAcceptMoreParticipants: boolean;
  } {
    if (!game) {
      return {
        fillPercentage: 0,
        availableSlots: 0,
        isNearlyFull: false,
        canAcceptMoreParticipants: false
      };
    }

    const fillPercentage = GameApiMapper.calculateFillPercentage(game);
    const availableSlots = game.maxParticipants - game.participantCount;
    const isNearlyFull = fillPercentage >= 80;
    const canAcceptMoreParticipants = game.canJoin && availableSlots > 0;

    return {
      fillPercentage,
      availableSlots,
      isNearlyFull,
      canAcceptMoreParticipants
    };
  }

  validateGameData(game: Game): { isValid: boolean; errors: string[] } {
    const errors: string[] = [];

    if (!game.id) {
      errors.push(this.t.t('games.validation.idMissing'));
    }
    if (!game.name || game.name.trim() === '') {
      errors.push(this.t.t('games.validation.nameRequired'));
    }
    if (!game.creatorName) {
      errors.push(this.t.t('games.validation.creatorMissing'));
    }
    if (game.maxParticipants <= 0) {
      errors.push(this.t.t('games.validation.maxParticipantsInvalid'));
    }
    if (game.participantCount < 0) {
      errors.push(this.t.t('games.validation.participantCountNegative'));
    }
    if (game.participantCount > game.maxParticipants) {
      errors.push(this.t.t('games.validation.participantCountExceeded'));
    }

    return {
      isValid: errors.length === 0,
      errors
    };
  }

  private handleHttpError(error: HttpErrorResponse): Observable<never> {
    const errorMessage = this.resolveHttpErrorMessage(error);

    this.logger.error('GameDataService: HTTP request failed', {
      status: error.status,
      message: errorMessage,
      originalError: error
    });

    const enrichedError = new Error(errorMessage) as Error & { status?: number };
    enrichedError.status = error.status;
    return throwError(() => enrichedError);
  }

  private resolveHttpErrorMessage(error: HttpErrorResponse): string {
    if (error.error?.message) {
      return error.error.message;
    }

    switch (error.status) {
      case 401:
        return this.t.t('errors.unauthorized');
      case 403:
        return this.t.t('errors.handler.forbiddenMessage', this.t.t('errors.generic'));
      case 404:
        return this.t.t('errors.notFound');
      case 500:
        return this.t.t('errors.handler.serverErrorMessage', this.t.t('errors.generic'));
      default:
        return this.t.t('errors.generic');
    }
  }

  private shouldUseFallback(error: HttpErrorResponse): boolean {
    const fallbackEnabled = environment.enableFallbackData && !environment.production;
    const isNetworkOrServerError = error.status === 0 || (error.status >= 500 && error.status < 600);
    return fallbackEnabled && isNetworkOrServerError;
  }

  private findFallbackGame(gameId: string): Game | null {
    return MOCK_GAMES.find(game => game.id === gameId) || null;
  }

  private getFallbackParticipants(gameId: string): GameParticipant[] {
    return MOCK_GAME_PARTICIPANTS[gameId] || [];
  }
}
