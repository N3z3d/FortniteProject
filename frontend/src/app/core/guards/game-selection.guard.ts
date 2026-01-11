import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { GameSelectionService } from '../services/game-selection.service';
import { LoggerService } from '../services/logger.service';
import { MatSnackBar } from '@angular/material/snack-bar';

/**
 * BE1-4: Guard that requires a game to be selected before accessing certain routes
 * Routes protected: /dashboard, /leaderboard, /teams
 * If no game is selected, redirects to /games with a notification
 */
@Injectable({
  providedIn: 'root'
})
export class GameSelectionGuard implements CanActivate {

  constructor(
    private readonly gameSelectionService: GameSelectionService,
    private readonly router: Router,
    private readonly logger: LoggerService,
    private readonly snackBar: MatSnackBar
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

      this.snackBar.open(
        'Veuillez d\'abord sélectionner une partie',
        'Fermer',
        { duration: 4000, panelClass: ['snackbar-warning'] }
      );

      return this.router.createUrlTree(['/games']);
    }

    this.logger.debug('GameSelectionGuard: Access granted', {
      gameId: this.gameSelectionService.getSelectedGame()?.id,
      url: state.url
    });

    return true;
  }
}
