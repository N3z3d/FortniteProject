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
    return game.status === 'CREATING' && game.participantCount >= 2;
  }

  /**
   * Vérifie si l'utilisateur peut archiver la game (host uniquement)
   */
  canArchiveGame(game: Game | null): boolean {
    if (!game) return false;
    const currentUser = this.userContextService.getCurrentUser();
    if (!currentUser) return false;

    return this.gameService.isGameHost(game, currentUser.username);
  }

  /**
   * Vérifie si l'utilisateur peut quitter la game (participant non-host)
   */
  canLeaveGame(game: Game | null): boolean {
    if (!game) return false;
    const currentUser = this.userContextService.getCurrentUser();
    if (!currentUser) return false;

    // Peut quitter si participant mais pas host
    const isHost = this.gameService.isGameHost(game, currentUser.username);
    const isParticipant = game.participants?.some(p => p.username === currentUser.username) || false;

    return isParticipant && !isHost;
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
    const isHost = this.gameService.isGameHost(game, currentUser.username);
    const isCreatingStatus = game.status === 'CREATING';

    return isHost && isCreatingStatus;
  }

  /**
   * Vérifie si l'utilisateur peut rejoindre la game
   */
  canJoinGame(game: Game | null): boolean {
    if (!game) return false;
    return game.canJoin && game.participantCount < game.maxParticipants;
  }

  /**
   * Vérifie si l'utilisateur peut régénérer le code (host uniquement)
   */
  canRegenerateCode(game: Game | null): boolean {
    if (!game) return false;
    const currentUser = this.userContextService.getCurrentUser();
    if (!currentUser) return false;

    return this.gameService.isGameHost(game, currentUser.username) &&
           game.status === 'CREATING';
  }

  /**
   * Vérifie si l'utilisateur peut renommer la partie (host uniquement)
   */
  canRenameGame(game: Game | null): boolean {
    if (!game) return false;
    const currentUser = this.userContextService.getCurrentUser();
    if (!currentUser) return false;

    return this.gameService.isGameHost(game, currentUser.username) &&
           game.status === 'CREATING';
  }
}
