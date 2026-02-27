import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { UserProfile } from '../../../core/services/user-context.service';
import { formatPoints } from '../../../shared/constants/theme.constants';
import { TranslationService } from '../../../core/services/translation.service';
import {
  secureRandomFloat,
  secureRandomIntInRange,
  secureRandomPick
} from '../../../shared/utils/secure-random.util';

interface UserStats {
  gamesPlayed: number;
  gamesWon: number;
  totalPoints: number;
  bestRank: number;
  averageRank: number;
  winRate: number;
  teamsCreated: number;
  tradesCompleted: number;
  draftParticipations: number;
  favoriteRegion: string;
  topPlayer: string;
  memberSince: Date;
  lastActive: Date;
}

@Component({
  selector: 'app-user-stats-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule
  ],
  templateUrl: './user-stats-dialog.component.html',
  styleUrls: ['./user-stats-dialog.component.scss']
})
export class UserStatsDialogComponent implements OnInit {
  stats: UserStats | null = null;
  loading = true;

  constructor(
    private readonly dialogRef: MatDialogRef<UserStatsDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { user: UserProfile },
    public readonly t: TranslationService
  ) {}

  ngOnInit(): void {
    this.loadStats();
  }

  private loadStats(): void {
    // Simuler un appel API
    setTimeout(() => {
      this.stats = this.generateMockStats();
      this.loading = false;
    }, 800);
  }

  private generateMockStats(): UserStats {
    const gamesPlayed = secureRandomIntInRange(10, 59);
    const gamesWon = Math.floor(gamesPlayed * (secureRandomFloat() * 0.4 + 0.1));
    const regions = ['EU', 'NAW', 'ASIA', 'BR'] as const;
    const players = ['Bugha', 'Clix', 'Mongraal', 'Tfue', 'Ninja'] as const;

    return {
      gamesPlayed,
      gamesWon,
      totalPoints: secureRandomIntInRange(100000, 599999),
      bestRank: secureRandomIntInRange(1, 5),
      averageRank: secureRandomIntInRange(3, 12),
      winRate: Math.round((gamesWon / gamesPlayed) * 100),
      teamsCreated: secureRandomIntInRange(5, 24),
      tradesCompleted: secureRandomIntInRange(20, 119),
      draftParticipations: secureRandomIntInRange(10, 39),
      favoriteRegion: secureRandomPick(regions),
      topPlayer: secureRandomPick(players),
      memberSince: new Date(
        2024,
        secureRandomIntInRange(0, 11),
        secureRandomIntInRange(1, 28)
      ),
      lastActive: new Date()
    };
  }

  onClose(): void {
    this.dialogRef.close();
  }

  getStatsTitle(): string {
    return this.formatTemplate('profile.statsDialog.title', {
      username: this.data.user.username
    });
  }

  getGamesPlayedLabel(count: number): string {
    return this.formatTemplate('profile.statsDialog.gamesPlayedSuffix', { count });
  }

  getFavoriteRegionLabel(region: string): string {
    return this.t.t(`leaderboard.regions.${region}`, region);
  }

  formatNumber(value: number): string {
    return formatPoints(value);
  }

  getWinRateColor(): string {
    if (!this.stats) return '';
    if (this.stats.winRate >= 30) return 'excellent';
    if (this.stats.winRate >= 20) return 'good';
    if (this.stats.winRate >= 10) return 'average';
    return 'low';
  }

  getRankBadgeClass(rank: number): string {
    if (rank === 1) return 'gold';
    if (rank === 2) return 'silver';
    if (rank === 3) return 'bronze';
    return 'default';
  }

  getRegionFlag(region: string): string {
    const flags: { [key: string]: string } = {
      'EU': '🇪🇺',
      'NAW': '🇺🇸',
      'NAC': '🇨🇦',
      'ASIA': '🇯🇵',
      'BR': '🇧🇷',
      'OCE': '🇦🇺',
      'ME': '🇦🇪'
    };
    return flags[region] || '🌍';
  }

  private formatTemplate(key: string, params: Record<string, string | number>): string {
    let value = this.t.t(key);
    for (const [param, paramValue] of Object.entries(params)) {
      const token = new RegExp(`\\{${param}\\}`, 'g');
      value = value.replace(token, String(paramValue));
    }
    return value;
  }
}
