import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { GameService } from '../services/game.service';
import { TranslationService } from '../../../core/services/translation.service';
import { UserGamesStore } from '../../../core/services/user-games.store';

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
    MatSnackBarModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './join-game.component.html',
  styleUrls: ['./join-game.component.scss']
})
export class JoinGameComponent {
  public readonly t = inject(TranslationService);
  invitationCode = '';
  joiningGame = false;

  constructor(
    private readonly gameService: GameService,
    public readonly router: Router,
    private readonly snackBar: MatSnackBar,
    private readonly userGamesStore: UserGamesStore
  ) {}

  joinWithCode(): void {
    if (!this.invitationCode.trim()) {
      this.snackBar.open(this.t.t('games.joinDialog.enterCodeError'), this.t.t('games.joinDialog.close'), {
        duration: 3000
      });
      return;
    }

    const normalizedCode = this.invitationCode.trim().toUpperCase();
    this.joiningGame = true;
    this.gameService.joinGameWithCode(normalizedCode).subscribe({
      next: (game) => {
        this.joiningGame = false;
        this.userGamesStore.refreshGames().subscribe({ error: () => undefined });
        this.snackBar.open(
          this.t.t('games.joinDialog.successJoined').replace('{name}', game.name),
          this.t.t('games.joinDialog.view'),
          { duration: 5000 }
        ).onAction().subscribe(() => {
          this.router.navigate(['/games', game.id, 'dashboard']);
        });

        // Redirect to game dashboard after 1 second
        setTimeout(() => {
          this.router.navigate(['/games', game.id, 'dashboard']);
        }, 1000);
      },
      error: (error) => {
        const message = this.extractErrorMessage(error, this.t.t('games.joinDialog.invalidCode'));
        this.snackBar.open(
          message,
          this.t.t('games.joinDialog.close'),
          { duration: 5000 }
        );
        this.joiningGame = false;
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/games']);
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
