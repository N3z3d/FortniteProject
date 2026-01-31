import { Injectable } from '@angular/core';
import { formatPoints as formatPointsUtil } from '../../../shared/constants/theme.constants';
import { Team, TeamPlayer, Player } from './team-detail-data.service';

export interface TeamStats {
  totalPoints: number;
  averagePoints: number;
  topPlayerPoints: number;
  playersCount: number;
  regionDistribution: { [key: string]: number };
  regionPointsDistribution: { [key: string]: number };
}

/**
 * Service responsable des calculs statistiques et UI helpers pour les équipes
 * (SRP: Single Responsibility - Statistics et UI helpers)
 */
@Injectable({
  providedIn: 'root'
})
export class TeamDetailStatsService {
  /**
   * Calcule les statistiques d'une équipe
   */
  calculateStats(team: Team): TeamStats {
    const players = team.players;
    const totalPoints = team.totalPoints || 0;
    const playersCount = players.length;

    const points = players.map(tp => tp.player.points || 0);
    const averagePoints = playersCount > 0 ? totalPoints / playersCount : 0;
    const topPlayerPoints = Math.max(...points, 0);

    // Distribution par région (nombre de joueurs)
    const regionDistribution: { [key: string]: number } = {};
    players.forEach(tp => {
      const region = tp.player.region;
      regionDistribution[region] = (regionDistribution[region] || 0) + 1;
    });

    // Distribution des points par région
    const regionPointsDistribution: { [key: string]: number } = {};
    players.forEach(tp => {
      const region = tp.player.region;
      const points = tp.player.points || 0;
      regionPointsDistribution[region] = (regionPointsDistribution[region] || 0) + points;
    });

    return {
      totalPoints,
      averagePoints,
      topPlayerPoints,
      playersCount,
      regionDistribution,
      regionPointsDistribution
    };
  }

  /**
   * Retourne la couleur associée à une région
   */
  getRegionColor(region: string): string {
    const colors: { [key: string]: string } = {
      'EU': '#4CAF50',     // Vert
      'NAW': '#2196F3',    // Bleu
      'NAC': '#FF9800',    // Orange
      'BR': '#FFD700',     // Jaune doré
      'ASIA': '#E91E63',   // Rose
      'OCE': '#9C27B0',    // Violet
      'ME': '#FF5722'      // Rouge-orange
    };
    return colors[region] || '#757575';
  }

  /**
   * Retourne l'URL du drapeau d'une région
   */
  getRegionFlag(region: string): string {
    const flagCodes: { [key: string]: string } = {
      'EU': 'EU',     // European Union
      'NAW': 'US',    // United States
      'NAC': 'CA',    // Canada
      'BR': 'BR',     // Brazil
      'ASIA': 'JP',   // Japan
      'OCE': 'AU',    // Australia
      'ME': 'AE'      // United Arab Emirates
    };

    const code = flagCodes[region];
    if (!code) return '';

    return `https://cdn.jsdelivr.net/gh/madebybowtie/FlagKit@2.4/Assets/SVG/${code}.svg`;
  }

  /**
   * Retourne la couleur associée à une tranche
   */
  getTrancheColor(tranche: number): string {
    const colors = ['#FFD700', '#C0C0C0', '#CD7F32', '#4CAF50', '#2196F3'];
    return colors[tranche - 1] || '#757575';
  }

  /**
   * Convertit une tranche (ex: "T1") en numéro
   */
  getTrancheNumber(tranche: string): number {
    if (tranche.toLowerCase() === "new") {
      return 1; // Valeur par défaut pour les nouveaux joueurs
    }

    const numericPart = tranche.replaceAll(/[Tt]/g, '');
    const parsed = Number.parseInt(numericPart, 10);
    return Number.isNaN(parsed) ? 1 : parsed;
  }

  /**
   * Retourne le rang d'un joueur dans l'équipe
   */
  getPlayerRank(player: Player, team: Team): number {
    const sortedPlayers = [...team.players]
      .sort((a, b) => (b.player.points || 0) - (a.player.points || 0));
    return sortedPlayers.findIndex(tp => tp.player.id === player.id) + 1;
  }

  /**
   * Formate les points pour l'affichage
   */
  formatPoints(points: number): string {
    return formatPointsUtil(points);
  }

  /**
   * Retourne les 3 meilleurs joueurs
   */
  getTopPlayers(team: Team): TeamPlayer[] {
    return [...team.players]
      .sort((a, b) => (b.player.points || 0) - (a.player.points || 0))
      .slice(0, 3);
  }

  /**
   * Retourne les joueurs triés par points
   */
  getSortedPlayers(team: Team): TeamPlayer[] {
    return [...team.players]
      .sort((a, b) => (b.player.points || 0) - (a.player.points || 0));
  }

  /**
   * Calcule le pourcentage de progression d'un joueur
   */
  getProgressPercentage(playerPoints: number, stats: TeamStats | null): number {
    if (!stats || stats.topPlayerPoints === 0) return 0;
    return (playerPoints / stats.topPlayerPoints) * 100;
  }

  /**
   * Calcule le pourcentage des points d'une région
   */
  getRegionPercentage(region: string, stats: TeamStats | null): number {
    if (!stats || stats.totalPoints === 0) return 0;
    const regionPoints = stats.regionPointsDistribution[region] || 0;
    return Math.round((regionPoints / stats.totalPoints) * 100);
  }

  /**
   * Retourne les régions triées par points
   */
  getSortedRegionsByPoints(stats: TeamStats | null): string[] {
    if (!stats) return [];
    return Object.keys(stats.regionPointsDistribution)
      .sort((a, b) => (stats!.regionPointsDistribution[b] || 0) - (stats!.regionPointsDistribution[a] || 0));
  }

  /**
   * Calcule l'arc length pour une région (visualisation)
   */
  getRegionArcLength(region: string, stats: TeamStats | null): number {
    return this.getRegionPercentage(region, stats);
  }

  /**
   * Calcule l'arc offset pour une région (visualisation)
   */
  getRegionArcOffset(region: string, stats: TeamStats | null): number {
    if (!stats) return 0;

    const sortedRegions = this.getSortedRegionsByPoints(stats);
    const currentIndex = sortedRegions.indexOf(region);

    let offset = 25; // Start from top (25% offset)
    for (let i = 0; i < currentIndex; i++) {
      offset -= this.getRegionPercentage(sortedRegions[i], stats);
    }

    return offset;
  }

  /**
   * Compte les joueurs dans le top 10% global
   */
  getTop10PercentPlayersCount(team: Team, allTeams: any[]): number {
    const teamWithPlayers = {
      ...team,
      players: team.players.map(tp => ({
        ...tp.player,
        points: tp.player.points || 0
      }))
    };

    const allTeamsAdapted = allTeams.map(t => ({
      ...t,
      players: (t.players || []).map((p: any) => ({
        ...p,
        points: p.points || 0
      }))
    }));

    return this.calculateTopPercentileCount(teamWithPlayers, allTeamsAdapted, 10);
  }

  /**
   * Calcule le nombre de joueurs dans le top percentile
   */
  private calculateTopPercentileCount(team: any, allTeams: any[], percentile: number): number {
    const allPlayers: { points: number }[] = [];
    allTeams.forEach(t => {
      if (t.players) {
        t.players.forEach((p: any) => {
          allPlayers.push({ points: p.points || 0 });
        });
      }
    });

    if (allPlayers.length === 0) return 0;

    allPlayers.sort((a, b) => b.points - a.points);

    const thresholdIndex = Math.ceil(allPlayers.length * (percentile / 100)) - 1;
    const threshold = allPlayers[Math.max(0, thresholdIndex)]?.points || 0;

    let count = 0;
    if (team.players) {
      team.players.forEach((p: any) => {
        if ((p.points || 0) >= threshold) {
          count++;
        }
      });
    }

    return count;
  }

  /**
   * Calcule le ratio de performance d'une région
   */
  getRegionRatio(region: string, stats: TeamStats | null, allTeams: any[]): number {
    if (!stats || !allTeams || allTeams.length === 0) return 0;

    const teamRegionPoints = stats.regionPointsDistribution[region] || 0;

    let totalRegionPoints = 0;
    allTeams.forEach(team => {
      if (team.players) {
        team.players.forEach((player: any) => {
          if (player.region === region) {
            totalRegionPoints += player.points || 0;
          }
        });
      }
    });

    if (totalRegionPoints === 0) return 0;

    const marketShare = (teamRegionPoints / totalRegionPoints) * 100;
    return Math.round(marketShare);
  }

  /**
   * Retourne les régions triées par ratio
   */
  getSortedRegionsByRatio(stats: TeamStats | null, allTeams: any[]): string[] {
    if (!stats) return [];

    return Object.keys(stats.regionPointsDistribution)
      .sort((a, b) => this.getRegionRatio(b, stats, allTeams) - this.getRegionRatio(a, stats, allTeams));
  }

  /**
   * Calcule le ratio moyen
   */
  getAverageRatio(stats: TeamStats | null, allTeams: any[]): number {
    if (!stats) return 0;

    const regions = Object.keys(stats.regionPointsDistribution);
    if (regions.length === 0) return 0;

    const totalRatio = regions.reduce((sum, region) => sum + this.getRegionRatio(region, stats, allTeams), 0);
    return Math.round(totalRatio / regions.length);
  }

  /**
   * Arc length basé sur le ratio au lieu des points
   */
  getRegionArcLengthByRatio(region: string, stats: TeamStats | null, allTeams: any[]): number {
    const regions = this.getSortedRegionsByRatio(stats, allTeams);
    const totalRatio = regions.reduce((sum, r) => sum + this.getRegionRatio(r, stats, allTeams), 0);

    if (totalRatio === 0) return 0;

    const ratio = this.getRegionRatio(region, stats, allTeams);
    return (ratio / totalRatio) * 100;
  }

  /**
   * Arc offset basé sur le ratio au lieu des points
   */
  getRegionArcOffsetByRatio(region: string, stats: TeamStats | null, allTeams: any[]): number {
    if (!stats) return 0;

    const sortedRegions = this.getSortedRegionsByRatio(stats, allTeams);
    const currentIndex = sortedRegions.indexOf(region);
    const totalRatio = sortedRegions.reduce((sum, r) => sum + this.getRegionRatio(r, stats, allTeams), 0);

    if (totalRatio === 0) return 25;

    let offset = 25;
    for (let i = 0; i < currentIndex; i++) {
      offset -= (this.getRegionRatio(sortedRegions[i], stats, allTeams) / totalRatio) * 100;
    }

    return offset;
  }
}
