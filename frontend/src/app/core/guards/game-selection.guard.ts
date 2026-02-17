import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';

import { GameSelectionService } from '../services/game-selection.service';
import { LoggerService } from '../services/logger.service';
import { UiErrorFeedbackService } from '../services/ui-error-feedback.service';

/**
 * BE1-4: Guard that requires a game to be selected before accessing certain routes.
 * Routes protected: /dashboard, /leaderboard, /teams.
 */
@Injectable({
  providedIn: 'root'
})
export class GameSelectionGuard implements CanActivate {
  constructor(
    private readonly gameSelectionService: GameSelectionService,
    private readonly router: Router,
    private readonly logger: LoggerService,
    private readonly uiFeedback: UiErrorFeedbackService
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean | UrlTree | Observable<boolean | UrlTree> {
    const hasSelectedGame = this.gameSelectionService.hasSelectedGame();

    if (!hasSelectedGame) {
      this.logger.warn('GameSelectionGuard: No game selected, redirecting to games list', {
        attemptedUrl: state.url
      });

      this.uiFeedback.showError(null, 'games.validation.selectGameRequired', { duration: 4000 });
      return this.router.createUrlTree(['/games']);
    }

    this.logger.debug('GameSelectionGuard: Access granted', {
      gameId: this.gameSelectionService.getSelectedGame()?.id,
      url: state.url
    });

    return true;
  }
}
