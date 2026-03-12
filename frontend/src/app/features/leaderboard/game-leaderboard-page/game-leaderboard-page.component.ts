import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  inject,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';

import { LeaderboardService } from '../../../core/services/leaderboard.service';
import { TranslationService } from '../../../core/services/translation.service';
import { TeamDeltaLeaderboardEntry } from '../models/team-delta-leaderboard.model';

@Component({
  selector: 'app-game-leaderboard-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './game-leaderboard-page.component.html',
  styleUrls: ['./game-leaderboard-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GameLeaderboardPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly leaderboardService = inject(LeaderboardService);
  private readonly cdr = inject(ChangeDetectorRef);
  public readonly t = inject(TranslationService);

  entries: TeamDeltaLeaderboardEntry[] = [];
  loading = false;
  error = false;
  gameId = '';

  ngOnInit(): void {
    this.gameId = this.route.snapshot.params['id'] ?? '';
    this.load();
  }

  load(): void {
    if (!this.gameId) {
      return;
    }
    this.loading = true;
    this.error = false;
    this.leaderboardService.getGameDeltaLeaderboard(this.gameId).subscribe({
      next: (data) => {
        this.entries = data;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = true;
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  formatDelta(value: number): string {
    if (value > 0) {
      return `+${value} PR`;
    }
    return `${value} PR`;
  }

  formatComputedAt(iso: string): string {
    if (!iso) {
      return '';
    }
    const d = new Date(iso);
    return d.toLocaleString();
  }

  get lastUpdate(): string {
    if (this.entries.length === 0) {
      return '';
    }
    return this.formatComputedAt(this.entries[0].computedAt);
  }

  trackByParticipantId(_index: number, entry: TeamDeltaLeaderboardEntry): string {
    return entry.participantId;
  }
}
