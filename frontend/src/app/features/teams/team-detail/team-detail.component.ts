import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { UserContextService } from '../../../core/services/user-context.service';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { TranslationService } from '../../../core/services/translation.service';
import { TeamDetailDataService, Team, TeamPlayer, Player } from '../services/team-detail-data.service';
import { TeamDetailStatsService, TeamStats } from '../services/team-detail-stats.service';

@Component({
  selector: 'app-team-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatProgressBarModule,
    MatTabsModule
  ],
  templateUrl: './team-detail.html',
  styleUrls: ['./team-detail.scss']
})
export class TeamDetailComponent implements OnInit {
  private readonly userContext = inject(UserContextService);
  private readonly route = inject(ActivatedRoute);
  private readonly dataService = inject(TeamDetailDataService);
  private readonly statsService = inject(TeamDetailStatsService);
  public readonly t = inject(TranslationService);

  team: Team | null = null;
  loading = true;
  error: string | null = null;
  stats: TeamStats | null = null;
  allTeams: any[] = [];

  // Expose Object for template use
  Object = Object;

  ngOnInit() {
    const routeId = this.route.snapshot.paramMap.get('id');
    if (routeId) {
      this.loadTeamById(routeId);
    } else {
      this.loadMyTeam();
    }
  }

  loadMyTeam() {
    this.loading = true;
    this.error = null;

    const currentUser = this.userContext.getCurrentUser();
    if (!currentUser) {
      this.handleLoadError('User not found');
      return;
    }

    this.dataService.loadMyTeam(currentUser.username).subscribe(result => {
      this.team = result.team;
      this.allTeams = result.allTeams;
      this.error = result.error;
      this.loading = false;

      if (this.team) {
        this.calculateStats();
      }
    });
  }

  loadTeamById(teamId: string) {
    this.loading = true;
    this.error = null;

    this.dataService.loadTeamById(teamId).subscribe(result => {
      this.team = result.team;
      this.allTeams = result.allTeams;
      this.error = result.error;
      this.loading = false;

      if (this.team) {
        this.calculateStats();
      }
    });
  }

  retryLoad(): void {
    const routeId = this.route.snapshot.paramMap.get('id');
    if (routeId) {
      this.loadTeamById(routeId);
      return;
    }
    this.loadMyTeam();
  }

  private handleLoadError(errorMessage?: string): void {
    this.error = errorMessage || this.t.t('teams.detail.dataUnavailable');
    this.team = null;
    this.stats = null;
    this.allTeams = [];
    this.loading = false;
  }

  private calculateStats() {
    if (!this.team) return;
    this.stats = this.statsService.calculateStats(this.team);
  }

  getRegionColor(region: string): string {
    return this.statsService.getRegionColor(region);
  }

  getRegionFlag(region: string): string {
    return this.statsService.getRegionFlag(region);
  }

  getTrancheColor(tranche: number): string {
    return this.statsService.getTrancheColor(tranche);
  }

  getTrancheNumber(tranche: string): number {
    return this.statsService.getTrancheNumber(tranche);
  }

  getPlayerRank(player: Player): number {
    if (!this.team) return 0;
    return this.statsService.getPlayerRank(player, this.team);
  }

  formatPoints(points: number): string {
    return this.statsService.formatPoints(points);
  }

  getTopPlayers(): TeamPlayer[] {
    if (!this.team) return [];
    return this.statsService.getTopPlayers(this.team);
  }

  getSortedPlayers(): TeamPlayer[] {
    if (!this.team) return [];
    return this.statsService.getSortedPlayers(this.team);
  }

  getProgressPercentage(playerPoints: number): number {
    return this.statsService.getProgressPercentage(playerPoints, this.stats);
  }

  getRegionPercentage(region: string): number {
    return this.statsService.getRegionPercentage(region, this.stats);
  }

  getRegionArcLength(region: string): number {
    return this.statsService.getRegionArcLength(region, this.stats);
  }

  getRegionArcOffset(region: string): number {
    return this.statsService.getRegionArcOffset(region, this.stats);
  }

  getSortedRegionsByPoints(): string[] {
    return this.statsService.getSortedRegionsByPoints(this.stats);
  }

  getTop10PercentPlayersCount(): number {
    if (!this.team || !this.allTeams.length) return 0;
    return this.statsService.getTop10PercentPlayersCount(this.team, this.allTeams);
  }

  getRegionRatio(region: string): number {
    return this.statsService.getRegionRatio(region, this.stats, this.allTeams);
  }

  getSortedRegionsByRatio(): string[] {
    return this.statsService.getSortedRegionsByRatio(this.stats, this.allTeams);
  }

  getAverageRatio(): number {
    return this.statsService.getAverageRatio(this.stats, this.allTeams);
  }

  getRegionArcLengthByRatio(region: string): number {
    return this.statsService.getRegionArcLengthByRatio(region, this.stats, this.allTeams);
  }

  getRegionArcOffsetByRatio(region: string): number {
    return this.statsService.getRegionArcOffsetByRatio(region, this.stats, this.allTeams);
  }
}
