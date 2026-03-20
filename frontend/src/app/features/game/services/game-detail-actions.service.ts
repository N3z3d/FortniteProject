import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';

import { GameService } from './game.service';
import { UserGamesStore } from '../../../core/services/user-games.store';
import { TranslationService } from '../../../core/services/translation.service';
import { UiErrorFeedbackService } from '../../../core/services/ui-error-feedback.service';
import { LoggerService } from '../../../core/services/logger.service';
import { Game } from '../models/game.interface';
import {
  InvitationCodeDialogMode,
  InvitationCodeDuration,
  InvitationCodeDurationDialogComponent
} from '../components/invitation-code-duration-dialog/invitation-code-duration-dialog.component';
import {
  ConfirmDialogComponent,
  ConfirmDialogData
} from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { RenameGameDialogComponent } from '../components/rename-game-dialog/rename-game-dialog.component';

/**
 * Service responsable des actions sur les details d'une game
 * (SRP: Single Responsibility - Actions uniquement)
 */
@Injectable({
  providedIn: 'root'
})
export class GameDetailActionsService {
  constructor(
    private readonly gameService: GameService,
    private readonly router: Router,
    private readonly dialog: MatDialog,
    private readonly userGamesStore: UserGamesStore,
    private readonly t: TranslationService,
    private readonly uiFeedback: UiErrorFeedbackService,
    private readonly logger: LoggerService
  ) {}

  /**
   * Demarre le draft d'une game
   */
  startDraft(gameId: string, onSuccess?: () => void, onSettled?: () => void): void {
    this.gameService.startDraft(gameId).subscribe({
      next: success => {
        if (!success) {
          this.uiFeedback.showError({}, 'games.detail.actions.draftStartBlocked');
          onSettled?.();
          return;
        }

        this.uiFeedback.showSuccessFromKey('games.detail.actions.draftStarted');
        onSuccess?.();
        onSettled?.();
      },
      error: error => {
        this.uiFeedback.showError(error, 'games.detail.actions.draftStartError', {
          rules: [
            {
              pattern: /createur de la partie introuvable|game creator is missing/,
              translationKey: 'games.detail.actions.draftCreatorMissing'
            }
          ]
        });
        this.logActionError('startDraft', gameId, error);
        onSettled?.();
      }
    });
  }

  /**
   * Archive la game (soft delete pour l'host)
   */
  archiveGame(gameId: string): void {
    this.gameService.archiveGame(gameId).subscribe({
      next: success => {
        if (!success) {
          this.uiFeedback.showError({}, 'games.detail.actions.archiveFailed', { duration: 3000 });
          return;
        }

        this.userGamesStore.removeGame(gameId);
        this.userGamesStore.refreshGames().subscribe({ error: () => undefined });
        this.uiFeedback.showSuccessFromKey('games.detail.actions.archiveSuccess');
        this.router.navigate(['/']);
      },
      error: error => {
        this.uiFeedback.showError(error, 'games.detail.actions.archiveError', { duration: 3000 });
        this.logActionError('archiveGame', gameId, error);
      }
    });
  }

  /**
   * Quitter la game (pour les participants non-host)
   */
  leaveGame(gameId: string): void {
    this.gameService.leaveGame(gameId).subscribe({
      next: success => {
        if (!success) {
          this.uiFeedback.showError({}, 'games.detail.actions.leaveFailed', { duration: 3000 });
          return;
        }

        this.userGamesStore.removeGame(gameId);
        this.userGamesStore.refreshGames().subscribe({ error: () => undefined });
        this.uiFeedback.showSuccessFromKey('games.detail.actions.leaveSuccess');
        this.router.navigate(['/']);
      },
      error: error => {
        this.uiFeedback.showError(error, 'games.detail.actions.leaveError', { duration: 3000 });
        this.logActionError('leaveGame', gameId, error);
      }
    });
  }

  /**
   * Supprime definitivement une partie (uniquement en status CREATING)
   */
  permanentlyDeleteGame(gameId: string): void {
    this.gameService.deleteGame(gameId).subscribe({
      next: success => {
        if (!success) {
          this.uiFeedback.showError({}, 'games.detail.actions.deleteFailed', { duration: 4000 });
          return;
        }

        this.userGamesStore.removeGame(gameId);
        this.router.navigate(['/']);
      },
      error: error => {
        this.uiFeedback.showError(error, 'games.detail.actions.deleteError', { duration: 5000 });
        this.logActionError('permanentlyDeleteGame', gameId, error);
      }
    });
  }

  /**
   * Rejoindre une game
   */
  joinGame(gameId: string, onSuccess?: () => void): void {
    this.gameService.joinGame(gameId).subscribe({
      next: success => {
        if (!success) {
          this.uiFeedback.showError({}, 'games.detail.actions.joinFailed', { duration: 3000 });
          return;
        }

        this.userGamesStore.refreshGames().subscribe({ error: () => undefined });
        this.uiFeedback.showSuccessFromKey('games.detail.actions.joinSuccess');
        if (onSuccess) {
          onSuccess();
        }
      },
      error: error => {
        this.uiFeedback.showError(error, 'games.detail.actions.joinError', { duration: 3000 });
        this.logActionError('joinGame', gameId, error);
      }
    });
  }

  /**
   * Copie le code d'invitation dans le presse-papier
   */
  copyInvitationCode(invitationCode: string): void {
    navigator.clipboard
      .writeText(invitationCode)
      .then(() => {
        this.uiFeedback.showSuccessFromKey('games.detail.actions.codeCopied', 2000);
      })
      .catch(() => {
        this.uiFeedback.showError({}, 'games.detail.actions.codeCopyFailed', { duration: 3000 });
      });
  }

  /**
   * Regenerer le code d'invitation de la game avec duree configurable
   */
  regenerateInvitationCode(
    gameId: string,
    duration: '24h' | '48h' | '7d' | 'permanent',
    mode: InvitationCodeDialogMode = 'regenerate',
    onSuccess?: (game: Game) => void
  ): void {
    this.gameService.regenerateInvitationCode(gameId, duration).subscribe({
      next: updatedGame => {
        const generatedCode = updatedGame?.invitationCode;
        const verb = this.t.t(
          mode === 'generate'
            ? 'games.detail.actions.invitationCodeGenerateVerb'
            : 'games.detail.actions.invitationCodeRegenerateVerb'
        );
        const message = generatedCode
          ? this.t.t('games.detail.actions.invitationCodeWithValue')
            .replace('{verb}', verb)
            .replace('{code}', generatedCode)
          : this.t.t('games.detail.actions.invitationCodeWithoutValue').replace('{verb}', verb);

        this.uiFeedback.showSuccessMessage(message, 3500);
        if (onSuccess) {
          onSuccess(updatedGame);
        }
      },
      error: error => {
        this.uiFeedback.showError(error, 'games.detail.actions.invitationCodeGenerationFailed');
        this.logActionError('regenerateInvitationCode', gameId, error);
      }
    });
  }

  /**
   * Renomme la partie
   */
  renameGame(gameId: string, newName: string, onSuccess?: (game: Game) => void): void {
    this.gameService.renameGame(gameId, newName).subscribe({
      next: updatedGame => {
        this.uiFeedback.showSuccessFromKey('games.detail.actions.renameSuccess');
        if (onSuccess) {
          onSuccess(updatedGame);
        }
      },
      error: error => {
        this.uiFeedback.showError(error, 'games.detail.actions.renameError');
        this.logActionError('renameGame', gameId, error);
      }
    });
  }

  /**
   * Confirme l'archivage de la game
   */
  confirmArchive(gameId: string): void {
    this.openConfirmationDialog(
      {
        title: this.t.t('games.detail.confirmDialogs.archive.title'),
        message: this.t.t('games.detail.confirmDialogs.archive.message'),
        confirmText: this.t.t('games.detail.confirmDialogs.archive.confirm'),
        cancelText: this.t.t('games.detail.confirmDialogs.archive.cancel'),
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
        title: this.t.t('games.detail.confirmDialogs.leave.title'),
        message: this.t.t('games.detail.confirmDialogs.leave.message'),
        confirmText: this.t.t('games.detail.confirmDialogs.leave.confirm'),
        cancelText: this.t.t('games.detail.confirmDialogs.leave.cancel'),
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
        title: this.t.t('games.detail.confirmDialogs.delete.title'),
        message: this.t.t('games.detail.confirmDialogs.delete.message'),
        confirmText: this.t.t('games.detail.confirmDialogs.delete.confirm'),
        cancelText: this.t.t('games.detail.confirmDialogs.delete.cancel'),
        confirmColor: 'warn'
      },
      () => this.permanentlyDeleteGame(gameId)
    );
  }

  /**
   * Confirme le demarrage du draft
   */
  confirmStartDraft(gameId: string, onSuccess?: () => void, onSettled?: () => void): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        width: '460px',
        maxWidth: '95vw',
        autoFocus: false,
        restoreFocus: true,
        data: {
          title: this.t.t('games.detail.confirmDialogs.startDraft.title'),
          message: this.t.t('games.detail.confirmDialogs.startDraft.message'),
          confirmText: this.t.t('games.detail.confirmDialogs.startDraft.confirm'),
          cancelText: this.t.t('games.detail.confirmDialogs.startDraft.cancel'),
          confirmColor: 'primary'
        } satisfies ConfirmDialogData
      })
      .afterClosed()
      .subscribe((confirmed: boolean | undefined) => {
        if (!confirmed) {
          onSettled?.();
          return;
        }

        this.startDraft(gameId, onSuccess, onSettled);
      });
  }

  /**
   * Affiche le prompt pour choisir la duree et regenerer le code
   */
  promptRegenerateCode(
    gameId: string,
    hasExistingCode: boolean,
    onSuccess?: (game: Game) => void
  ): void {
    const mode: InvitationCodeDialogMode = hasExistingCode ? 'regenerate' : 'generate';
    this.dialog
      .open(InvitationCodeDurationDialogComponent, {
        width: '520px',
        maxWidth: '95vw',
        panelClass: 'nexus-dialog-panel',
        autoFocus: false,
        restoreFocus: true,
        data: {
          defaultDuration: 'permanent' as InvitationCodeDuration,
          mode
        }
      })
      .afterClosed()
      .subscribe((selectedDuration: InvitationCodeDuration | undefined) => {
        if (!selectedDuration) {
          return;
        }

        this.regenerateInvitationCode(gameId, selectedDuration, mode, onSuccess);
      });
  }

  /**
   * Prompt pour renommer la game
   */
  promptRenameGame(gameId: string, currentName: string, onSuccess?: (game: Game) => void): void {
    this.dialog
      .open(RenameGameDialogComponent, {
        width: '500px',
        maxWidth: '95vw',
        panelClass: 'nexus-dialog-panel',
        autoFocus: false,
        restoreFocus: true,
        data: { currentName }
      })
      .afterClosed()
      .subscribe((newName: string | undefined) => {
        if (!newName) {
          return;
        }

        this.renameGame(gameId, newName, onSuccess);
      });
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

  private logActionError(action: string, gameId: string, error: unknown): void {
    this.logger.error(`GameDetailActionsService: ${action} failed`, {
      action,
      gameId,
      error
    });
  }
}
