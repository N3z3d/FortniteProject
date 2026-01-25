import { Injectable } from '@angular/core';
import { LeaderboardEntryDTO } from '../../../core/models/leaderboard.model';
import { CompetitionStats, DashboardStats, PremiumStats, TeamComposition } from '../models/dashboard.types';

export interface DashboardLeaderboardStatsResult {
  stats: DashboardStats;
  premium: PremiumStats;
}

@Injectable({
  providedIn: 'root'
})
export class DashboardStatsCalculatorService {
  buildStatsFromLeaderboard(
    entries: LeaderboardEntryDTO[],
    gamesCount: number,
    now: Date = new Date()
  ): DashboardLeaderboardStatsResult | null {
    if (!entries || entries.length === 0) {
      return null;
    }

    const totalTeams = entries.length;
    const totalPoints = entries.reduce((sum, entry) => sum + (entry.totalPoints || 0), 0);
    const averagePointsPerTeam = totalTeams > 0 ? Math.round(totalPoints / totalTeams) : 0;
    const seasonProgress = this.calculateSeasonProgress(now);

    const regionCounts: Record<string, number> = {};
    const trancheCounts: Record<string, number> = {};
    let totalPlayers = 0;

    entries.forEach(entry => {
      entry.players?.forEach(player => {
        totalPlayers += 1;
        const region = this.resolveRegion(player?.region);
        regionCounts[region] = (regionCounts[region] || 0) + 1;

        const trancheLabel = `Tranche ${player?.tranche || 'Unknown'}`;
        trancheCounts[trancheLabel] = (trancheCounts[trancheLabel] || 0) + 1;
      });
    });

    const mostActiveTeam = entries.length > 0
      ? entries.reduce((prev, current) =>
          (current.totalPoints || 0) > (prev.totalPoints || 0) ? current : prev
        ).teamName
      : 'N/A';

    const stats: DashboardStats = {
      totalTeams,
      totalPlayers,
      totalPoints,
      averagePointsPerTeam,
      mostActiveTeam: mostActiveTeam || '',
      seasonProgress,
      teamComposition: {
        regions: regionCounts,
        tranches: trancheCounts
      } as TeamComposition
    };

    const premium: PremiumStats = {
      totalScore: totalPoints,
      activeGames: gamesCount,
      weeklyBest: entries.length > 0
        ? Math.max(...entries.map(entry => entry.totalPoints || 0))
        : 0,
      ranking: entries.length
    };

    return { stats, premium };
  }

  calculateSeasonProgress(now: Date = new Date()): number {
    const startOfYear = new Date(now.getFullYear(), 0, 1);
    const endOfYear = new Date(now.getFullYear(), 11, 31);

    const totalDays = (endOfYear.getTime() - startOfYear.getTime()) / (1000 * 60 * 60 * 24);
    const daysElapsed = (now.getTime() - startOfYear.getTime()) / (1000 * 60 * 60 * 24);

    const progress = (daysElapsed / totalDays) * 100;
    return Math.round(progress * 10) / 10;
  }

  applyStatsUpdate(
    currentStats: DashboardStats,
    updates: Partial<DashboardStats>,
    now: Date = new Date()
  ): { stats: DashboardStats; competitionStats: CompetitionStats } {
    const stats: DashboardStats = {
      ...currentStats,
      ...updates,
      lastUpdate: now.toISOString()
    };

    const competitionStats: CompetitionStats = {
      totalTeams: stats.totalTeams,
      totalPlayers: stats.totalPlayers,
      totalPoints: stats.totalPoints,
      averagePointsPerTeam: stats.averagePointsPerTeam,
      mostActiveTeam: stats.mostActiveTeam,
      seasonProgress: this.calculateSeasonProgress(now)
    };

    return { stats, competitionStats };
  }

  applyCompetitionStats(
    currentStats: DashboardStats,
    apiStats: any,
    now: Date = new Date()
  ): { stats: DashboardStats; competitionStats: CompetitionStats } {
    const seasonProgress = this.calculateSeasonProgress(now);

    if (apiStats) {
      const competitionStats: CompetitionStats = {
        totalTeams: apiStats.totalTeams || currentStats.totalTeams,
        totalPlayers: apiStats.totalPlayers || currentStats.totalPlayers,
        totalPoints: currentStats.totalPoints,
        averagePointsPerTeam: apiStats.averagePoints || currentStats.averagePointsPerTeam,
        mostActiveTeam: currentStats.mostActiveTeam,
        seasonProgress
      };

      const stats: DashboardStats = {
        ...currentStats,
        totalPlayers: apiStats.totalPlayers,
        totalPoints: apiStats.totalPoints || currentStats.totalPoints
      };

      return { stats, competitionStats };
    }

    const competitionStats: CompetitionStats = {
      totalTeams: currentStats.totalTeams,
      totalPlayers: currentStats.totalPlayers,
      totalPoints: currentStats.totalPoints,
      averagePointsPerTeam: currentStats.averagePointsPerTeam,
      mostActiveTeam: currentStats.mostActiveTeam,
      seasonProgress
    };

    return { stats: currentStats, competitionStats };
  }

  private resolveRegion(region: unknown): string {
    if (typeof region === 'string' && region.length > 0) {
      return region;
    }
    if (region && typeof region === 'object' && 'name' in region) {
      const name = (region as { name?: string }).name;
      if (typeof name === 'string' && name.length > 0) {
        return name;
      }
    }
    return 'Unknown';
  }
}
