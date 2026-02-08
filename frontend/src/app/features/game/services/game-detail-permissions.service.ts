import { Injectable } from '@angular/core';

import { GameService } from './game.service';
import { UserContextService } from '../../../core/services/user-context.service';
import { Game } from '../models/game.interface';

/**
 * Service responsable des vérifications de permissions sur les games
 * (SRP: Single Responsibility - Permissions uniquement)
 */
@Injectable({
  providedIn: 'root'
})
export class GameDetailPermissionsService {
  constructor(
    private readonly gameService: GameService,
    private readonly userContextService: UserContextService
  ) {}

  /**
   * Vérifie si le draft peut être démarré
   */
  canStartDraft(game: Game | null): boolean {
    if (!game) return false;
    const currentUser = this.userContextService.getCurrentUser();
    if (!currentUser) return false;

    const isHost = this.isHost(game, currentUser.id, currentUser.username);
    return isHost && game.status === 'CREATING' && game.participantCount >= 2;
  }

  /**
   * Vérifie si l'utilisateur peut archiver la game (host uniquement)
   */
  canArchiveGame(game: Game | null): boolean {
    if (!game) return false;
    const currentUser = this.userContextService.getCurrentUser();
    if (!currentUser) return false;

    return this.isHost(game, currentUser.id, currentUser.username);
  }

  /**
   * Vérifie si l'utilisateur peut quitter la game (participant non-host)
   */
  canLeaveGame(game: Game | null): boolean {
    if (!game) return false;
    const currentUser = this.userContextService.getCurrentUser();
    if (!currentUser) return false;

    // Peut quitter si participant mais pas host
    const isHost = this.isHost(game, currentUser.id, currentUser.username);
    return this.isParticipant(game, currentUser.id, currentUser.username) && !isHost;
  }

  /**
   * Vérifie si l'utilisateur peut supprimer définitivement la partie
   * Conditions: host + status CREATING uniquement
   */
  canDeleteGame(game: Game | null): boolean {
    if (!game) return false;
    const currentUser = this.userContextService.getCurrentUser();
    if (!currentUser) return false;

    // Peut supprimer uniquement si host ET status CREATING
    const isHost = this.isHost(game, currentUser.id, currentUser.username);
    const isCreatingStatus = game.status === 'CREATING';

    return isHost && isCreatingStatus;
  }

  /**
   * VÃ©rifie si l'action de suppression doit Ãªtre visible.
   * On affiche l'action uniquement pour le host.
   */
  canSeeDeleteGameAction(game: Game | null): boolean {
    if (!game) return false;
    const currentUser = this.userContextService.getCurrentUser();
    if (!currentUser) return false;

    return this.isHost(game, currentUser.id, currentUser.username);
  }

  /**
   * Retourne la clÃ© i18n expliquant pourquoi la suppression est indisponible.
   * null signifie qu'aucune restriction n'est Ã  afficher.
   */
  getDeleteRestrictionReasonKey(game: Game | null): string | null {
    if (!game) return null;
    if (!this.canSeeDeleteGameAction(game)) {
      return 'games.detail.deleteDisabledNotHost';
    }
    if (game.status !== 'CREATING') {
      return 'games.detail.deleteDisabledStatus';
    }
    return null;
  }

  /**
   * Vérifie si l'utilisateur peut rejoindre la game
   */
  canJoinGame(game: Game | null): boolean {
    if (!game) return false;
    if (!game.canJoin || game.participantCount >= game.maxParticipants) return false;

    const currentUser = this.userContextService.getCurrentUser();
    if (!currentUser) return false;

    if (this.isHost(game, currentUser.id, currentUser.username)) return false;

    return !this.isParticipant(game, currentUser.id, currentUser.username);
  }

  /**
   * Vérifie si l'utilisateur peut régénérer le code (host uniquement)
   */
  canRegenerateCode(game: Game | null): boolean {
    if (!game) return false;
    const currentUser = this.userContextService.getCurrentUser();
    if (!currentUser) return false;

    return this.isHost(game, currentUser.id, currentUser.username);
  }

  /**
   * Vérifie si l'utilisateur peut renommer la partie (host uniquement)
   */
  canRenameGame(game: Game | null): boolean {
    if (!game) return false;
    const currentUser = this.userContextService.getCurrentUser();
    if (!currentUser) return false;

    return this.isHost(game, currentUser.id, currentUser.username);
  }

  private isHost(game: Game, userId: string | undefined, username: string): boolean {
    if (userId && this.gameService.isGameHost(game, userId)) {
      return true;
    }

    if (username && this.gameService.isGameHost(game, username)) {
      return true;
    }

    return false;
  }

  private isParticipant(game: Game, userId: string | undefined, username: string): boolean {
    if (!game.participants) return false;
    const normalizedUsername = (username || '').trim().toLowerCase();
    return game.participants.some(
      p =>
        (userId && p.id === userId) ||
        (p.username || '').trim().toLowerCase() === normalizedUsername
    );
  }
}
