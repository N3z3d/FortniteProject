import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { GameService } from '../services/game.service';
import { TranslationService } from '../../../core/services/translation.service';
import { UserGamesStore } from '../../../core/services/user-games.store';
import { UiErrorFeedbackService } from '../../../core/services/ui-error-feedback.service';
import {
  extractJoinErrorDetails,
  isInvitationCodeFormatValid,
  resolveJoinErrorTranslationKey
} from '../services/join-error-message.resolver';

@Component({
  selector: 'app-join-game',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './join-game.component.html',
  styleUrls: ['./join-game.component.scss']
})
export class JoinGameComponent {
  public readonly t = inject(TranslationService);
  invitationCode = '';
  joiningGame = false;
  joinFeedbackMessage: string | null = null;
  joinFeedbackType: 'error' | 'success' = 'error';

  constructor(
    private readonly gameService: GameService,
    public readonly router: Router,
    private readonly userGamesStore: UserGamesStore,
    private readonly uiFeedback: UiErrorFeedbackService
  ) {}

  joinWithCode(): void {
    if (!this.invitationCode.trim()) {
      this.showJoinError(this.t.t('games.joinDialog.enterCodeError'));
      return;
    }

    const normalizedCode = this.invitationCode.trim().toUpperCase();
    if (!isInvitationCodeFormatValid(normalizedCode)) {
      this.showJoinError(this.t.t('games.joinDialog.invalidCodeFormat'));
      return;
    }

    this.clearJoinFeedback();
    this.joiningGame = true;
    this.gameService.joinGameWithCode(normalizedCode).subscribe({
      next: (game) => {
        this.joiningGame = false;
        this.clearJoinFeedback();
        this.userGamesStore.refreshGames().subscribe({ error: () => undefined });
        this.uiFeedback.showSuccessWithAction(
          this.t.t('games.joinDialog.successJoined').replace('{name}', game.name),
          'games.joinDialog.view',
          () => this.router.navigate(['/games', game.id, 'dashboard']),
          5000
        );

        // Redirect to game dashboard after 1 second
        setTimeout(() => {
          this.router.navigate(['/games', game.id, 'dashboard']);
        }, 1000);
      },
      error: (error) => {
        const message = this.extractErrorMessage(error, this.t.t('games.joinDialog.invalidCode'));
        this.showJoinError(message);
        this.joiningGame = false;
      }
    });
  }

  cancel(): void {
    this.clearJoinFeedback();
    this.router.navigate(['/games']);
  }

  private showJoinError(message: string): void {
    this.joinFeedbackType = 'error';
    this.joinFeedbackMessage = message;
  }

  private clearJoinFeedback(): void {
    this.joinFeedbackMessage = null;
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const details = extractJoinErrorDetails(error);
    const translationKey = resolveJoinErrorTranslationKey(details);
    if (translationKey) {
      return this.t.t(translationKey);
    }

    return fallback;
  }
}
