import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

import { GameService } from './game.service';
import { UserGamesStore } from '../../../core/services/user-games.store';
import { Game } from '../models/game.interface';

/**
 * Service responsable des actions sur les détails d'une game
 * (SRP: Single Responsibility - Actions uniquement)
 */
@Injectable({
  providedIn: 'root'
})
export class GameDetailActionsService {
  constructor(
    private readonly gameService: GameService,
    private readonly router: Router,
    private readonly snackBar: MatSnackBar,
    private readonly userGamesStore: UserGamesStore
  ) {}

  /**
   * Démarre le draft d'une game
   */
  startDraft(gameId: string, onSuccess?: () => void): void {
    this.gameService.startDraft(gameId).subscribe({
      next: (success) => {
        if (success) {
          this.snackBar.open('Draft démarré avec succès!', 'Fermer', { duration: 3000 });
          if (onSuccess) onSuccess();
        } else {
          this.snackBar.open('Impossible de démarrer le draft', 'Fermer', { duration: 3000 });
        }
      },
      error: (error) => {
        this.snackBar.open('Erreur lors du démarrage du draft', 'Fermer', { duration: 3000 });
        console.error('Error starting draft:', error);
      }
    });
  }

  /**
   * Archive la game (soft delete pour l'host)
   */
  archiveGame(gameId: string): void {
    this.gameService.archiveGame(gameId).subscribe({
      next: (success) => {
        if (success) {
          this.snackBar.open('Game archivée avec succès!', 'Fermer', { duration: 3000 });
          this.router.navigate(['/games']);
        } else {
          this.snackBar.open('Impossible d\'archiver la game', 'Fermer', { duration: 3000 });
        }
      },
      error: (error) => {
        this.snackBar.open('Erreur lors de l\'archivage de la game', 'Fermer', { duration: 3000 });
        console.error('Error archiving game:', error);
      }
    });
  }

  /**
   * Quitter la game (pour les participants non-host)
   */
  leaveGame(gameId: string): void {
    this.gameService.leaveGame(gameId).subscribe({
      next: (success) => {
        if (success) {
          this.snackBar.open('Vous avez quitté la game', 'Fermer', { duration: 3000 });
          this.router.navigate(['/games']);
        } else {
          this.snackBar.open('Impossible de quitter la game', 'Fermer', { duration: 3000 });
        }
      },
      error: (error) => {
        this.snackBar.open('Erreur lors de la sortie de la game', 'Fermer', { duration: 3000 });
        console.error('Error leaving game:', error);
      }
    });
  }

  /**
   * Supprime définitivement une partie (uniquement en status CREATING)
   */
  permanentlyDeleteGame(gameId: string): void {
    this.gameService.deleteGame(gameId).subscribe({
      next: (success) => {
        if (success) {
          // Rafraîchir la sidebar
          this.userGamesStore.removeGame(gameId);
          // Rediriger vers la page d'accueil
          this.router.navigate(['/']);
        }
      },
      error: (error) => {
        console.error('Error deleting game:', error);
      }
    });
  }

  /**
   * Rejoindre une game
   */
  joinGame(gameId: string, onSuccess?: () => void): void {
    this.gameService.joinGame(gameId).subscribe({
      next: (success) => {
        if (success) {
          this.snackBar.open('Game rejoint avec succès!', 'Fermer', { duration: 3000 });
          if (onSuccess) onSuccess();
        } else {
          this.snackBar.open('Impossible de rejoindre la game', 'Fermer', { duration: 3000 });
        }
      },
      error: (error) => {
        this.snackBar.open('Erreur lors de la tentative de rejoindre la game', 'Fermer', { duration: 3000 });
        console.error('Error joining game:', error);
      }
    });
  }

  /**
   * Copie le code d'invitation dans le presse-papier
   */
  copyInvitationCode(invitationCode: string): void {
    navigator.clipboard.writeText(invitationCode).then(() => {
      this.snackBar.open('Code copié dans le presse-papier !', 'OK', {
        duration: 2000
      });
    }).catch(() => {
      this.snackBar.open('Impossible de copier le code', 'Fermer', {
        duration: 3000
      });
    });
  }

  /**
   * Régénère le code d'invitation de la game avec durée configurable
   */
  regenerateInvitationCode(
    gameId: string,
    duration: '24h' | '48h' | '7d' | 'permanent',
    onSuccess?: (game: Game) => void
  ): void {
    this.gameService.regenerateInvitationCode(gameId, duration).subscribe({
      next: (updatedGame) => {
        if (onSuccess) onSuccess(updatedGame);
      },
      error: (error) => {
        console.error('Error regenerating invitation code:', error);
      }
    });
  }

  /**
   * Renomme la partie
   */
  renameGame(gameId: string, newName: string, onSuccess?: (game: Game) => void): void {
    this.gameService.renameGame(gameId, newName).subscribe({
      next: (updatedGame) => {
        if (onSuccess) onSuccess(updatedGame);
      },
      error: (error) => {
        console.error('Error renaming game:', error);
      }
    });
  }

  /**
   * Confirme l'archivage de la game
   */
  confirmArchive(gameId: string): void {
    const confirmed = confirm(
      'Êtes-vous sûr de vouloir archiver cette game ? Elle ne sera plus visible dans votre liste.'
    );

    if (confirmed) {
      this.archiveGame(gameId);
    }
  }

  /**
   * Confirme la sortie de la game
   */
  confirmLeave(gameId: string): void {
    const confirmed = confirm(
      'Êtes-vous sûr de vouloir quitter cette game ?'
    );

    if (confirmed) {
      this.leaveGame(gameId);
    }
  }

  /**
   * Confirme la suppression de la game
   */
  confirmDelete(gameId: string): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette game ? Cette action est irréversible.')) {
      this.permanentlyDeleteGame(gameId);
    }
  }

  /**
   * Confirme le démarrage du draft
   */
  confirmStartDraft(gameId: string, onSuccess?: () => void): void {
    if (confirm('Êtes-vous sûr de vouloir démarrer le draft ? Cette action ne peut pas être annulée.')) {
      this.startDraft(gameId, onSuccess);
    }
  }

  /**
   * Affiche le prompt pour choisir la durée et régénère le code
   */
  promptRegenerateCode(gameId: string, onSuccess?: (game: Game) => void): void {
    const choice = prompt(
      'Choisissez la durée du code d\'invitation:\n1 = 24 heures\n2 = 48 heures\n3 = 7 jours\n4 = Permanent',
      '4'
    );

    if (!choice) return;

    const durationMap: { [key: string]: '24h' | '48h' | '7d' | 'permanent' } = {
      '1': '24h',
      '2': '48h',
      '3': '7d',
      '4': 'permanent'
    };

    const duration = durationMap[choice] || 'permanent';
    this.regenerateInvitationCode(gameId, duration, onSuccess);
  }

  /**
   * Prompt pour renommer la game
   */
  promptRenameGame(gameId: string, currentName: string, onSuccess?: (game: Game) => void): void {
    const newName = prompt('Nouveau nom de la partie:', currentName);
    if (!newName || newName.trim() === '' || newName === currentName) {
      return;
    }

    this.renameGame(gameId, newName.trim(), onSuccess);
  }
}
