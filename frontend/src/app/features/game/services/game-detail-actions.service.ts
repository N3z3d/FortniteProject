import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

import { GameService } from './game.service';
import { UserGamesStore } from '../../../core/services/user-games.store';
import { Game } from '../models/game.interface';
import {
  InvitationCodeDuration,
  InvitationCodeDurationDialogComponent
} from '../components/invitation-code-duration-dialog/invitation-code-duration-dialog.component';
import {
  ConfirmDialogComponent,
  ConfirmDialogData
} from '../../../shared/components/confirm-dialog/confirm-dialog.component';

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
    private readonly dialog: MatDialog,
    private readonly userGamesStore: UserGamesStore
  ) { }

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
          this.snackBar.open(
            'Impossible de demarrer le draft. Verifiez que vous etes le createur et qu il y a au moins 2 participants.',
            'Fermer',
            { duration: 5000 }
          );
        }
      },
      error: (error) => {
        this.snackBar.open(
          this.extractErrorMessage(error, 'Erreur lors du demarrage du draft'),
          'Fermer',
          { duration: 5000 }
        );
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
          this.userGamesStore.removeGame(gameId);
          this.snackBar.open('Game archivée avec succès!', 'Fermer', { duration: 3000 });
          this.router.navigate(['/']);
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
          this.userGamesStore.removeGame(gameId);
          this.snackBar.open('Vous avez quitté la game', 'Fermer', { duration: 3000 });
          this.router.navigate(['/']);
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
          this.userGamesStore.refreshGames().subscribe({ error: () => undefined });
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
        const generatedCode = updatedGame?.invitationCode;
        const message = generatedCode
          ? `Code d'invitation regenere: ${generatedCode}`
          : 'Code d\'invitation regenere avec succes';
        this.snackBar.open(message, 'Fermer', { duration: 3500 });
        if (onSuccess) onSuccess(updatedGame);
      },
      error: (error) => {
        this.snackBar.open(this.extractErrorMessage(error, 'Impossible de regenerer le code'), 'Fermer', {
          duration: 5000
        });
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
        this.snackBar.open('Partie renommée avec succès!', 'Fermer', { duration: 3000 });
        if (onSuccess) onSuccess(updatedGame);
      },
      error: (error) => {
        this.snackBar.open(
          this.extractErrorMessage(error, 'Erreur lors du renommage de la partie'),
          'Fermer',
          { duration: 5000 }
        );
        console.error('Error renaming game:', error);
      }
    });
  }

  /**
   * Confirme l'archivage de la game
   */
  confirmArchive(gameId: string): void {
    this.openConfirmationDialog(
      {
        title: 'Archiver cette game ?',
        message: 'La game sera retiree de la liste active, mais conservee en base.',
        confirmText: 'Archiver',
        cancelText: 'Annuler',
        confirmColor: 'warn'
      },
      () => this.archiveGame(gameId)
    );
  }

  /**
   * Confirme la sortie de la game
   */
  confirmLeave(gameId: string): void {
    this.openConfirmationDialog(
      {
        title: 'Quitter cette game ?',
        message: 'Vous quitterez la game et devrez utiliser un code pour revenir.',
        confirmText: 'Quitter',
        cancelText: 'Annuler',
        confirmColor: 'primary'
      },
      () => this.leaveGame(gameId)
    );
  }

  /**
   * Confirme la suppression de la game
   */
  confirmDelete(gameId: string): void {
    this.openConfirmationDialog(
      {
        title: 'Supprimer cette game ?',
        message: 'Cette action est irreversible (soft delete).',
        confirmText: 'Supprimer',
        cancelText: 'Annuler',
        confirmColor: 'warn'
      },
      () => this.permanentlyDeleteGame(gameId)
    );
  }

  /**
   * Confirme le démarrage du draft
   */
  confirmStartDraft(gameId: string, onSuccess?: () => void): void {
    this.openConfirmationDialog(
      {
        title: 'Demarrer le draft ?',
        message: 'Cette action ne peut pas etre annulee.',
        confirmText: 'Demarrer',
        cancelText: 'Annuler',
        confirmColor: 'primary'
      },
      () => this.startDraft(gameId, onSuccess)
    );
  }

  /**
   * Affiche le prompt pour choisir la durée et régénère le code
   */
  promptRegenerateCode(gameId: string, onSuccess?: (game: Game) => void): void {
    this.dialog
      .open(InvitationCodeDurationDialogComponent, {
        width: '520px',
        maxWidth: '95vw',
        panelClass: 'nexus-dialog-panel',
        autoFocus: false,
        restoreFocus: true,
        data: { defaultDuration: 'permanent' as InvitationCodeDuration }
      })
      .afterClosed()
      .subscribe((duration: InvitationCodeDuration | undefined) => {
        if (!duration) {
          return;
        }

        this.regenerateInvitationCode(gameId, duration, onSuccess);
      });
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

  private openConfirmationDialog(data: ConfirmDialogData, onConfirm: () => void): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        width: '460px',
        maxWidth: '95vw',
        autoFocus: false,
        restoreFocus: true,
        data
      })
      .afterClosed()
      .subscribe((confirmed: boolean | undefined) => {
        if (confirmed) {
          onConfirm();
        }
      });
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    if (error instanceof Error && error.message) {
      return error.message;
    }

    if (typeof error === 'object' && error !== null) {
      const errorObject = error as { error?: { message?: string } };
      if (errorObject.error?.message) {
        return errorObject.error.message;
      }
    }

    return fallback;
  }
}


