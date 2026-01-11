import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';

import { LoggerService } from '../../../core/services/logger.service';
import { UserContextService } from '../../../core/services/user-context.service';
import { UserGamesStore } from '../../../core/services/user-games.store';
import { Game } from '../models/game.interface';

export const gamesResolver: ResolveFn<Game[]> = (): Observable<Game[]> => {
  const userContextService = inject(UserContextService);
  const userGamesStore = inject(UserGamesStore);
  const logger = inject(LoggerService);

  if (!userContextService.isLoggedIn()) {
    logger.warn('GamesResolver: No user logged in');
    userGamesStore.clear();
    return of([]);
  }

  logger.info('GamesResolver: Loading games');
  return userGamesStore.loadGames().pipe(
    tap(games => logger.info('GamesResolver: Games loaded', { count: games.length })),
    catchError(error => {
      logger.error('GamesResolver: Failed to load games', error);
      return of([]);
    })
  );
};
