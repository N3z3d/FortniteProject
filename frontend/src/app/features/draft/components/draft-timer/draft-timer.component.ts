import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  inject,
  ChangeDetectorRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { interval, Subscription } from 'rxjs';
import { take } from 'rxjs/operators';

import { TranslationService } from '../../../../core/services/translation.service';
import { GameParticipant } from '../../models/draft.interface';

export type TimerState = 'warmup' | 'active' | 'warning' | 'urgent' | 'expired';

const WARNING_THRESHOLD = 15;
const URGENT_THRESHOLD = 5;
const CANCEL_WINDOW = 5;
const TICK_INTERVAL_MS = 1_000;
const SECONDS_PER_MINUTE = 60;
const PAD_LENGTH = 2;

@Component({
  selector: 'app-draft-timer',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule],
  templateUrl: './draft-timer.component.html',
  styleUrls: ['./draft-timer.component.scss'],
})
export class DraftTimerComponent implements OnChanges, OnDestroy {
  readonly t = inject(TranslationService);
  private readonly cdr = inject(ChangeDetectorRef);

  // Legacy inputs — kept for backwards compatibility with draft.component.html
  @Input() currentPlayer: GameParticipant | null = null;
  @Input() formattedTime: string | null = null;

  // UX-002a inputs
  @Input() durationSeconds = 0;
  @Input() warmup = false;
  @Input() autoPickPlayer: string | null = null;
  /** Optional ISO-8601 server timestamp. When provided, remaining time is derived from server clock. */
  @Input() serverExpiresAt: string | null = null;

  @Output() readonly expired = new EventEmitter<void>();
  @Output() readonly cancelled = new EventEmitter<void>();

  secondsLeft = 0;
  showAutopickToast = false;
  cancelWindowSecondsLeft = 0;
  criticalAnnouncement = '';

  private timerSub: Subscription | null = null;
  private cancelSub: Subscription | null = null;

  get timerState(): TimerState {
    if (this.warmup) return 'warmup';
    if (this.durationSeconds > 0 && this.secondsLeft <= 0) return 'expired';
    if (this.durationSeconds > 0 && this.secondsLeft <= URGENT_THRESHOLD) return 'urgent';
    if (this.durationSeconds > 0 && this.secondsLeft <= WARNING_THRESHOLD) return 'warning';
    return 'active';
  }

  get displayTime(): string {
    if (this.durationSeconds > 0) {
      return this.formatSeconds(this.secondsLeft);
    }
    return this.formattedTime ?? '--:--';
  }

  get ariaLabel(): string {
    if (this.warmup) return 'Phase de warmup';
    if (this.durationSeconds > 0 && this.secondsLeft <= 0) return 'Temps écoulé';
    if (this.durationSeconds > 0) return `${this.secondsLeft} secondes restantes`;
    return this.formattedTime ?? '';
  }

  get hasTimer(): boolean {
    return this.durationSeconds > 0 || Boolean(this.currentPlayer?.timeRemaining);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['serverExpiresAt'] && this.serverExpiresAt) {
      const remaining = Math.round(
        Math.max(0, (new Date(this.serverExpiresAt).getTime() - Date.now()) / 1000)
      );
      if (remaining > 0) {
        this.durationSeconds = remaining;
        this.startCountdown();
        return;
      }
    }
    if (changes['durationSeconds'] && this.durationSeconds > 0) {
      this.startCountdown();
    }
  }

  ngOnDestroy(): void {
    this.timerSub?.unsubscribe();
    this.cancelSub?.unsubscribe();
  }

  cancelAutopick(): void {
    this.cancelSub?.unsubscribe();
    this.showAutopickToast = false;
    this.cancelled.emit();
  }

  private startCountdown(): void {
    this.timerSub?.unsubscribe();
    this.secondsLeft = this.durationSeconds;
    this.showAutopickToast = false;
    this.criticalAnnouncement = '';

    this.timerSub = interval(TICK_INTERVAL_MS)
      .pipe(take(this.durationSeconds))
      .subscribe({
        next: () => {
          this.secondsLeft = Math.max(0, this.secondsLeft - 1);
          this.announceCriticalThresholds();
          if (this.secondsLeft === 0) {
            this.onExpired();
          }
          this.cdr.markForCheck();
        },
      });
  }

  private announceCriticalThresholds(): void {
    if (this.secondsLeft === WARNING_THRESHOLD) {
      this.criticalAnnouncement = `Attention : ${WARNING_THRESHOLD} secondes restantes`;
    } else if (this.secondsLeft === URGENT_THRESHOLD) {
      this.criticalAnnouncement = `Urgent : ${URGENT_THRESHOLD} secondes restantes`;
    }
  }

  private onExpired(): void {
    this.timerSub?.unsubscribe();
    this.expired.emit();
    if (this.autoPickPlayer) {
      this.startCancelWindow();
    }
    this.cdr.markForCheck();
  }

  private startCancelWindow(): void {
    this.showAutopickToast = true;
    this.cancelWindowSecondsLeft = CANCEL_WINDOW;

    this.cancelSub = interval(TICK_INTERVAL_MS)
      .pipe(take(CANCEL_WINDOW))
      .subscribe({
        next: () => {
          this.cancelWindowSecondsLeft = Math.max(0, this.cancelWindowSecondsLeft - 1);
          this.cdr.markForCheck();
        },
        complete: () => {
          this.showAutopickToast = false;
          this.cdr.markForCheck();
        },
      });
  }

  private formatSeconds(total: number): string {
    const minutes = Math.floor(total / SECONDS_PER_MINUTE).toString().padStart(PAD_LENGTH, '0');
    const seconds = (total % SECONDS_PER_MINUTE).toString().padStart(PAD_LENGTH, '0');
    return `${minutes}:${seconds}`;
  }
}
