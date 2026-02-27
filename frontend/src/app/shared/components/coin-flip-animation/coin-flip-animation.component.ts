import {
  Component,
  OnInit,
  inject,
  ViewChild,
  ElementRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { ResponsiveService } from '../../../core/services/responsive.service';

export interface CoinFlipData {
  player1: string;
  player2: string;
  contestedPlayer: string;
  winner: string;
}

export type CoinFlipPhase = 'flipping' | 'result';

const FOCUS_DELAY_MS = 300;

/**
 * Fullscreen dialog that theatricalises a simultaneous-draft conflict via a coin-flip animation.
 *
 * Phases:
 *  1. flipping â€” CSS rotateY animation (or opacity fade if prefers-reduced-motion)
 *  2. result   â€” winner/loser revealed, focus moves to "Re-sÃ©lectionner" button
 *
 * Accessibility:
 *  - role="dialog" + aria-modal="true"
 *  - aria-live="polite" on result zone
 *  - Focus management: "Re-sÃ©lectionner" button receives focus after animation
 *  - < 3 flashes/second (WCAG 2.3.1) â€” CSS animation uses smooth easing
 */
@Component({
  selector: 'app-coin-flip-animation',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  templateUrl: './coin-flip-animation.component.html',
  styleUrls: ['./coin-flip-animation.component.scss'],
})
export class CoinFlipAnimationComponent implements OnInit {
  private readonly dialogRef = inject(MatDialogRef<CoinFlipAnimationComponent>);
  private readonly data = inject<CoinFlipData>(MAT_DIALOG_DATA);
  private readonly responsive = inject(ResponsiveService);

  @ViewChild('reselectBtn') reselectBtn?: ElementRef<HTMLButtonElement>;

  phase: CoinFlipPhase = 'flipping';
  winner = '';
  loser = '';
  contestedPlayer = '';
  player1Label = '';
  player2Label = '';
  reducedMotion = false;

  ngOnInit(): void {
    this.winner = this.data.winner.toUpperCase();
    this.loser = this.resolveLosers(this.data.player1, this.data.player2, this.data.winner);
    this.contestedPlayer = this.data.contestedPlayer.toUpperCase();
    this.player1Label = this.data.player1.toUpperCase();
    this.player2Label = this.data.player2.toUpperCase();
    this.reducedMotion = this.responsive.prefersReducedMotion();

    if (this.reducedMotion) {
      setTimeout(() => this.onAnimationDone(), FOCUS_DELAY_MS);
    }
  }

  onAnimationDone(): void {
    this.phase = 'result';
    setTimeout(() => this.focusReselectButton(), FOCUS_DELAY_MS);
  }

  onReselect(): void {
    this.dialogRef.close({ action: 'reselect' });
  }

  private resolveLosers(p1: string, p2: string, winner: string): string {
    const loser = p1.toUpperCase() === winner.toUpperCase() ? p2 : p1;
    return loser.toUpperCase();
  }

  private focusReselectButton(): void {
    this.reselectBtn?.nativeElement?.focus();
  }
}

