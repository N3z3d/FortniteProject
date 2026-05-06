import { inject } from '@angular/core';
import { CanActivateFn, ActivatedRouteSnapshot, Router, UrlTree } from '@angular/router';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { UserGamesStore } from '../../../core/services/user-games.store';
import { GameService } from '../../game/services/game.service';

/**
 * Guard that restricts access to the /simultaneous draft route.
 *
 * The simultaneous draft mode is currently disabled in the UI pending
 * full validation in production conditions. This guard ensures that
 * even direct URL access is blocked for games not using SIMULTANEOUS mode.
 */
export const simultaneousModeGuard: CanActivateFn = (
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
    return of(resolveDraftRoute(router, gameId, cachedGame.draftMode));
  }

  return gameService.getGameById(gameId).pipe(
    map(game => resolveDraftRoute(router, gameId, game.draftMode)),
    catchError(() => of(router.createUrlTree(['/games'])))
  );
};

function resolveDraftRoute(
  router: Router,
  gameId: string,
  draftMode: string | undefined
): boolean | UrlTree {
  if (draftMode === 'SIMULTANEOUS') {
    return true;
  }
  return router.createUrlTree(['/games', gameId, 'draft', 'snake']);
}
