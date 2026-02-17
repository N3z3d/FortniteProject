import { Injectable, inject } from '@angular/core';

import { GameDataService } from './game-data.service';
import { GameApiMapper } from '../mappers/game-api.mapper';
import { TranslationService } from '../../../core/services/translation.service';
import { Game, GameStatus, GameParticipant } from '../models/game.interface';

/**
 * Service responsable des helpers UI pour les détails d'une game
 * (SRP: Single Responsibility - UI helpers uniquement)
 */
@Injectable({
  providedIn: 'root'
})
export class GameDetailUIService {
  private readonly t = inject(TranslationService);

  constructor(
    private readonly gameDataService: GameDataService
  ) {}

  /**
   * Retourne la couleur du statut pour Material chips
   */
  getStatusColor(status: GameStatus): string {
    switch (status) {
      case 'CREATING':
        return 'primary';
      case 'DRAFTING':
        return 'accent';
      case 'ACTIVE':
        return 'warn';
      case 'FINISHED':
        return 'default';
      case 'CANCELLED':
        return 'default';
      default:
        return 'default';
    }
  }

  /**
   * Retourne le label traduit du statut
   */
  getStatusLabel(status: GameStatus): string {
    switch (status) {
      case 'CREATING':
        return this.t.t('games.home.statusCreating');
      case 'DRAFTING':
        return this.t.t('games.home.statusDrafting');
      case 'ACTIVE':
        return this.t.t('games.home.statusActive');
      case 'FINISHED':
        return this.t.t('games.home.statusFinished');
      case 'CANCELLED':
        return this.t.t('games.home.statusCancelled');
      default:
        return status;
    }
  }

  /**
   * Calcule le pourcentage de remplissage des participants
   */
  getParticipantPercentage(game: Game | null): number {
    if (!game) return 0;
    const stats = this.gameDataService.calculateGameStatistics(game);
    return stats.fillPercentage;
  }

  /**
   * Calcule les statistiques de la game
   */
  getGameStatistics(game: Game | null) {
    if (!game) return null;
    return this.gameDataService.calculateGameStatistics(game);
  }

  /**
   * Retourne la couleur de la progress bar des participants
   */
  getParticipantColor(game: Game | null): string {
    const percentage = this.getParticipantPercentage(game);
    if (percentage >= 90) return 'warn';
    if (percentage >= 70) return 'accent';
    return 'primary';
  }

  /**
   * Formate une date en temps relatif ("il y a X minutes")
   */
  getTimeAgo(date: string | Date | null | undefined): string {
    if (!date) {
      return this.t.t('common.invalidDate');
    }

    const dateString = typeof date === 'string' ? date : date.toISOString();
    return GameApiMapper.formatRelativeTime(dateString);
  }

  /**
   * Formate la date d'expiration du code d'invitation
   */
  getInvitationCodeExpiry(game: Game | null): string {
    if (!game?.invitationCodeExpiresAt) {
      return this.t.t('games.detail.permanent');
    }
    if (game.isInvitationCodeExpired) {
      return this.t.t('games.detail.expired');
    }
    return this.getTimeAgo(game.invitationCodeExpiresAt);
  }

  /**
   * Récupère le créateur parmi les participants
   */
  getCreator(participants: GameParticipant[]): GameParticipant | null {
    return participants.find(p => p.isCreator) || null;
  }

  /**
   * Récupère les participants non-créateurs
   */
  getNonCreatorParticipants(participants: GameParticipant[]): GameParticipant[] {
    return participants.filter(p => !p.isCreator);
  }

  /**
   * Retourne l'icône du statut du participant
   */
  getParticipantStatusIcon(participant: GameParticipant): string {
    if (participant.isCreator) return 'star';
    return 'person';
  }

  /**
   * Retourne la couleur du statut du participant
   */
  getParticipantStatusColor(participant: GameParticipant): string {
    if (participant.isCreator) return 'accent';
    return 'primary';
  }

  /**
   * Retourne le label du statut du participant
   */
  getParticipantStatusLabel(participant: GameParticipant): string {
    if (participant.isCreator) {
      return this.t.t('games.detail.creator');
    }
    return this.t.t('games.detail.participant');
  }
}
