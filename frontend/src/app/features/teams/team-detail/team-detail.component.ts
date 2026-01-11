import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { HttpClient } from '@angular/common/http';
import { UserContextService } from '../../../core/services/user-context.service';
import { TeamService, TeamDto } from '../../../core/services/team.service';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { LeaderboardService } from '../../../core/services/leaderboard.service';
import { formatPoints as formatPointsUtil } from '../../../shared/constants/theme.constants';

interface Player {
  id: string;
  nickname: string;
  region: string;
  tranche: string;
  points?: number;
  username?: string;
}

interface TeamPlayer {
  player: Player;
  position: number;
  playerId?: string;
  active?: boolean;
}

interface Team {
  id: string;
  name: string;
  season: number;
  owner?: {
    id: string;
    username: string;
  };
  ownerName?: string;
  ownerId?: string;
  players: TeamPlayer[];
  totalPoints?: number;
}

interface TeamStats {
  totalPoints: number;
  averagePoints: number;
  topPlayerPoints: number;
  playersCount: number;
  regionDistribution: { [key: string]: number };
  regionPointsDistribution: { [key: string]: number };
}

interface TeamWithOwner extends Team {
  owner: {
    id: string;
    username: string;
  };
}

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
  private readonly http = inject(HttpClient);
  private readonly userContext = inject(UserContextService);
  private readonly route = inject(ActivatedRoute);
  private readonly teamService = inject(TeamService);
  private readonly leaderboardService = inject(LeaderboardService);

  team: Team | null = null;
  loading = true;
  error: string | null = null;
  stats: TeamStats | null = null;
  allTeams: any[] = []; // Pour stocker toutes les équipes pour le calcul Top 10%
  private readonly loadErrorMessage = 'Donn\u00e9es indisponibles (CSV non charg\u00e9)';

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

    // Utiliser l'API leaderboard qui contient les données complètes avec les points
    this.leaderboardService.getTeamLeaderboard().subscribe({
      next: (teams: any[]) => {
        this.allTeams = teams; // Stocker toutes les équipes
        const userTeam = teams.find(team =>
          team.ownerUsername?.toLowerCase() === currentUser?.username?.toLowerCase()
        );

        if (userTeam) {
          this.team = this.convertLeaderboardToTeam(userTeam);
          this.calculateStats();
        } else {
          this.error = 'Équipe non trouvée pour cet utilisateur';
        }
        this.loading = false;
      },
      error: (error) => {
        console.warn('Erreur lors du chargement des \u00e9quipes:', error);
        this.handleLoadError();
      }
    });
  }

  loadTeamById(teamId: string) {
    this.loading = true;
    this.error = null;

    this.leaderboardService.getTeamLeaderboard().subscribe({
      next: (teams: any[]) => {
        this.allTeams = teams; // Stocker toutes les équipes
        const team = teams.find(t => t.teamId === teamId);
        if (team) {
          this.team = this.convertLeaderboardToTeam(team);
          this.calculateStats();
        } else {
          this.error = 'Équipe non trouvée';
        }
        this.loading = false;
      },
      error: (error) => {
        console.warn('Erreur lors du chargement des \u00e9quipes:', error);
        this.handleLoadError();
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

  private handleLoadError(): void {
    this.error = this.loadErrorMessage;
    this.team = null;
    this.stats = null;
    this.allTeams = [];
    this.loading = false;
  }

  private formatTeamData(teamData: any): Team {
    return {
      id: teamData.id,
      name: teamData.name,
      season: teamData.season,
      owner: teamData.owner,
      players: teamData.players || [],
      totalPoints: this.calculateTotalPoints(teamData.players || [])
    };
  }

  private calculateTotalPoints(players: TeamPlayer[]): number {
    return players.reduce((total, teamPlayer) => {
      return total + (teamPlayer.player.points || 0);
    }, 0);
  }

  private calculateStats() {
    if (!this.team) return;

    const players = this.team.players;
    const totalPoints = this.team.totalPoints || 0;
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

    this.stats = {
      totalPoints,
      averagePoints,
      topPlayerPoints,
      playersCount,
      regionDistribution,
      regionPointsDistribution
    };
  }

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

  getRegionFlag(region: string): string {
    // Utilisation de FlagKit CDN pour des drapeaux SVG de haute qualité
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

    // Retourner l'URL du drapeau SVG
    return `https://cdn.jsdelivr.net/gh/madebybowtie/FlagKit@2.4/Assets/SVG/${code}.svg`;
  }

  getTrancheColor(tranche: number): string {
    const colors = ['#FFD700', '#C0C0C0', '#CD7F32', '#4CAF50', '#2196F3'];
    return colors[tranche - 1] || '#757575';
  }

  getTrancheNumber(tranche: string): number {
    // Gère les cas spéciaux
    if (tranche.toLowerCase() === "new") {
      return 1; // Valeur par défaut pour les nouveaux joueurs
    }

    // Convertit "T1", "T2", etc. en nombre
    const numericPart = tranche.replaceAll(/[Tt]/g, '');
    const parsed = Number.parseInt(numericPart, 10);
    return Number.isNaN(parsed) ? 1 : parsed;
  }

  getPlayerRank(player: Player): number {
    if (!this.team) return 0;
    const sortedPlayers = [...this.team.players]
      .sort((a, b) => (b.player.points || 0) - (a.player.points || 0));
    return sortedPlayers.findIndex(tp => tp.player.id === player.id) + 1;
  }

  formatPoints(points: number): string {
    return formatPointsUtil(points);
  }

  getTopPlayers(): TeamPlayer[] {
    if (!this.team) return [];
    return [...this.team.players]
      .sort((a, b) => (b.player.points || 0) - (a.player.points || 0))
      .slice(0, 3);
  }

  getSortedPlayers(): TeamPlayer[] {
    if (!this.team) return [];
    return [...this.team.players]
      .sort((a, b) => (b.player.points || 0) - (a.player.points || 0));
  }

  getProgressPercentage(playerPoints: number): number {
    if (!this.stats || this.stats.topPlayerPoints === 0) return 0;
    return (playerPoints / this.stats.topPlayerPoints) * 100;
  }

  getRegionPercentage(region: string): number {
    if (!this.stats || this.stats.totalPoints === 0) return 0;
    const regionPoints = this.stats.regionPointsDistribution[region] || 0;
    return Math.round((regionPoints / this.stats.totalPoints) * 100);
  }

  getRegionArcLength(region: string): number {
    const percentage = this.getRegionPercentage(region);
    return percentage; // SVG stroke-dasharray uses percentage directly
  }

  getRegionArcOffset(region: string): number {
    if (!this.stats) return 0;

    const sortedRegions = this.getSortedRegionsByPoints();
    const currentIndex = sortedRegions.indexOf(region);

    let offset = 25; // Start from top (25% offset)
    for (let i = 0; i < currentIndex; i++) {
      offset -= this.getRegionPercentage(sortedRegions[i]);
    }

    return offset;
  }

  getSortedRegionsByPoints(): string[] {
    if (!this.stats) return [];
    return Object.keys(this.stats.regionPointsDistribution)
      .sort((a, b) => (this.stats!.regionPointsDistribution[b] || 0) - (this.stats!.regionPointsDistribution[a] || 0));
  }

  getTop10PercentPlayersCount(): number {
    if (!this.team || !this.allTeams.length) return 0;

    // Adapter la structure pour le service
    const teamWithPlayers = {
      ...this.team,
      players: this.team.players.map(tp => ({
        ...tp.player,
        points: tp.player.points || 0
      }))
    };

    const allTeamsAdapted = this.allTeams.map(t => ({
      ...t,
      players: (t.players || []).map((p: any) => ({
        ...p,
        points: p.points || 0
      }))
    }));

    // Calcul local du top percentile
    return this.calculateTopPercentileCount(teamWithPlayers, allTeamsAdapted, 10);
  }

  private calculateTopPercentileCount(team: any, allTeams: any[], percentile: number): number {
    // Collecter tous les joueurs de toutes les équipes avec leurs points
    const allPlayers: { points: number }[] = [];
    allTeams.forEach(t => {
      if (t.players) {
        t.players.forEach((p: any) => {
          allPlayers.push({ points: p.points || 0 });
        });
      }
    });

    if (allPlayers.length === 0) return 0;

    // Trier par points décroissants
    allPlayers.sort((a, b) => b.points - a.points);

    // Calculer le seuil du top X%
    const thresholdIndex = Math.ceil(allPlayers.length * (percentile / 100)) - 1;
    const threshold = allPlayers[Math.max(0, thresholdIndex)]?.points || 0;

    // Compter les joueurs de l'équipe au-dessus du seuil
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

  private convertTeamDtoToTeam(teamDto: TeamDto): Team {
    return {
      id: teamDto.id,
      name: teamDto.name,
      season: teamDto.season,
      owner: {
        id: 'unknown',
        username: teamDto.ownerUsername || 'Propriétaire inconnu'
      },
      players: teamDto.players.map((player, index) => ({
        player: {
          id: player.playerId,
          nickname: player.nickname,
          region: player.region,
          tranche: player.tranche,
          points: 0 // Pas de points disponibles dans les données actuelles
        },
        position: index + 1
      })),
      totalPoints: teamDto.totalScore || 0
    };
  }

  private convertLeaderboardToTeam(leaderboardEntry: any): Team {
    return {
      id: leaderboardEntry.teamId,
      name: leaderboardEntry.teamName,
      season: 2025,
      owner: {
        id: leaderboardEntry.ownerId || 'unknown',
        username: leaderboardEntry.ownerUsername || 'Propriétaire inconnu'
      },
      players: leaderboardEntry.players.map((player: any, index: number) => ({
        player: {
          id: player.playerId,
          nickname: player.nickname,
          region: player.region,
          tranche: player.tranche,
          points: player.points || 0
        },
        position: index + 1
      })),
      totalPoints: leaderboardEntry.totalPoints || 0
    };
  }

  // Méthode pour calculer le ratio de performance d'une région
  getRegionRatio(region: string): number {
    if (!this.stats || !this.allTeams || this.allTeams.length === 0) return 0;

    // Calculer les points totaux de cette région pour l'équipe actuelle
    const teamRegionPoints = this.stats.regionPointsDistribution[region] || 0;

    // Calculer les points totaux de cette région pour TOUTES les équipes
    let totalRegionPoints = 0;
    this.allTeams.forEach(team => {
      if (team.players) {
        team.players.forEach((player: any) => {
          if (player.region === region) {
            totalRegionPoints += player.points || 0;
          }
        });
      }
    });

    // Si pas de points dans cette région, retourner 0
    if (totalRegionPoints === 0) return 0;

    // Calculer le pourcentage de part de marché
    const marketShare = (teamRegionPoints / totalRegionPoints) * 100;
    return Math.round(marketShare);
  }

  // Méthode pour obtenir les régions triées par ratio
  getSortedRegionsByRatio(): string[] {
    if (!this.stats) return [];

    return Object.keys(this.stats.regionPointsDistribution)
      .sort((a, b) => this.getRegionRatio(b) - this.getRegionRatio(a));
  }

  // Méthode pour calculer le ratio moyen
  getAverageRatio(): number {
    if (!this.stats) return 0;

    const regions = Object.keys(this.stats.regionPointsDistribution);
    if (regions.length === 0) return 0;

    const totalRatio = regions.reduce((sum, region) => sum + this.getRegionRatio(region), 0);
    return Math.round(totalRatio / regions.length);
  }

  // Adapter les méthodes d'arc pour utiliser les ratios au lieu des points
  getRegionArcLengthByRatio(region: string): number {
    const regions = this.getSortedRegionsByRatio();
    const totalRatio = regions.reduce((sum, r) => sum + this.getRegionRatio(r), 0);

    if (totalRatio === 0) return 0;

    const ratio = this.getRegionRatio(region);
    return (ratio / totalRatio) * 100;
  }

  getRegionArcOffsetByRatio(region: string): number {
    if (!this.stats) return 0;

    const sortedRegions = this.getSortedRegionsByRatio();
    const currentIndex = sortedRegions.indexOf(region);
    const totalRatio = sortedRegions.reduce((sum, r) => sum + this.getRegionRatio(r), 0);

    if (totalRatio === 0) return 25;

    let offset = 25; // Start from top (25% offset)
    for (let i = 0; i < currentIndex; i++) {
      offset -= (this.getRegionRatio(sortedRegions[i]) / totalRatio) * 100;
    }

    return offset;
  }
}
