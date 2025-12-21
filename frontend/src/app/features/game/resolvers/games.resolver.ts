import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';

import { LoggerService } from '../../../core/services/logger.service';
import { UserContextService } from '../../../core/services/user-context.service';
import { Game } from '../models/game.interface';
import { GameService } from '../services/game.service';

export const gamesResolver: ResolveFn<Game[]> = (): Observable<Game[]> => {
  const gameService = inject(GameService);
  const userContextService = inject(UserContextService);
  const logger = inject(LoggerService);

  if (!userContextService.isLoggedIn()) {
    logger.warn('GamesResolver: No user logged in');
    return of([]);
  }

  logger.info('GamesResolver: Loading games');
  return gameService.getUserGames().pipe(
    tap(games => logger.info('GamesResolver: Games loaded', { count: games.length })),
    catchError(error => {
      logger.error('GamesResolver: Failed to load games', error);
      return of([]);
    })
  );
};
