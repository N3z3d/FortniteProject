import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { UserProfile } from '../../../core/services/user-context.service';
import { formatPoints } from '../../../shared/constants/theme.constants';

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
    @Inject(MAT_DIALOG_DATA) public data: { user: UserProfile }
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
    const gamesPlayed = Math.floor(Math.random() * 50) + 10;
    const gamesWon = Math.floor(gamesPlayed * (Math.random() * 0.4 + 0.1));

    return {
      gamesPlayed,
      gamesWon,
      totalPoints: Math.floor(Math.random() * 500000) + 100000,
      bestRank: Math.floor(Math.random() * 5) + 1,
      averageRank: Math.floor(Math.random() * 10) + 3,
      winRate: Math.round((gamesWon / gamesPlayed) * 100),
      teamsCreated: Math.floor(Math.random() * 20) + 5,
      tradesCompleted: Math.floor(Math.random() * 100) + 20,
      draftParticipations: Math.floor(Math.random() * 30) + 10,
      favoriteRegion: ['EU', 'NAW', 'ASIA', 'BR'][Math.floor(Math.random() * 4)],
      topPlayer: ['Bugha', 'Clix', 'Mongraal', 'Tfue', 'Ninja'][Math.floor(Math.random() * 5)],
      memberSince: new Date(2024, Math.floor(Math.random() * 12), Math.floor(Math.random() * 28) + 1),
      lastActive: new Date()
    };
  }

  onClose(): void {
    this.dialogRef.close();
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
}
