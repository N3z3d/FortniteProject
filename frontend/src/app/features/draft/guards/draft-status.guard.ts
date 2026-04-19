import { inject } from '@angular/core';
import { CanActivateFn, ActivatedRouteSnapshot, Router, UrlTree } from '@angular/router';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { UserGamesStore } from '../../../core/services/user-games.store';
import { GameService } from '../../game/services/game.service';

/**
 * Guard that restricts access to draft routes to games with status DRAFTING.
 * Redirects to /games/:id when the game exists but is not in DRAFTING status.
 * Redirects to /games on missing gameId or API error.
 */
export const draftStatusGuard: CanActivateFn = (
  route: ActivatedRouteSnapshot
): Observable<boolean | UrlTree> => {
  const router = inject(Router);
  const userGamesStore = inject(UserGamesStore);
  const gameService = inject(GameService);

  const gameId = route.paramMap.get('id');

  if (!gameId) {
    return of(router.createUrlTree(['/games']));
  }

  const cachedGame = userGamesStore.findGameById(gameId);
  if (cachedGame) {
    if (cachedGame.status === 'DRAFTING') {
      return of(true);
    }
    return of(router.createUrlTree(['/games', gameId]));
  }

  return gameService.getGameById(gameId).pipe(
    map(game => {
      if (game.status === 'DRAFTING') {
        return true;
      }
      return router.createUrlTree(['/games', gameId]);
    }),
    catchError(() => of(router.createUrlTree(['/games'])))
  );
};
