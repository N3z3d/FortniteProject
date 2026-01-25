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
    private readonly snackBar: MatSnackBar
  ) {}

  joinWithCode(): void {
    if (!this.invitationCode.trim()) {
      this.snackBar.open(this.t.t('games.join.enterCodeError'), this.t.t('games.join.close'), {
        duration: 3000
      });
      return;
    }

    this.joiningGame = true;
    this.gameService.joinGameWithCode(this.invitationCode.trim()).subscribe({
      next: (game) => {
        this.snackBar.open(
          this.t.t('games.join.successJoined').replace('{name}', game.name),
          this.t.t('games.join.view'),
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
        this.snackBar.open(
          error.error?.message || this.t.t('games.join.invalidCode'),
          this.t.t('games.join.close'),
          { duration: 5000 }
        );
        this.joiningGame = false;
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/games']);
  }
}
