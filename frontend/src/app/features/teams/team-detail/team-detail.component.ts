import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { UserContextService } from '../../../core/services/user-context.service';
import { TeamService, TeamDto } from '../../../core/services/team.service';
import { ActivatedRoute } from '@angular/router';
import { map, switchMap, of } from 'rxjs';
import { LeaderboardService } from '../../../core/services/leaderboard.service';
import { StatsCalculationService } from '../../../core/services/stats-calculation.service';

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
    MatCardModule, 
    MatButtonModule, 
    MatIconModule,
    MatChipsModule,
    MatProgressBarModule,
    MatTabsModule
  ],
  templateUrl: './team-detail.html',
  styleUrls: ['./team-detail.scss']
})
export class TeamDetailComponent implements OnInit {
  private http = inject(HttpClient);
  private userContext = inject(UserContextService);
  private route = inject(ActivatedRoute);
  private teamService = inject(TeamService);
  private leaderboardService = inject(LeaderboardService);
  private statsService = inject(StatsCalculationService);
  
  team: Team | null = null;
  loading = true;
  error: string | null = null;
  stats: TeamStats | null = null;
  allTeams: any[] = []; // Pour stocker toutes les équipes pour le calcul Top 10%
  
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
        console.warn('API non disponible, utilisation des données mockées:', error);
        if (currentUser?.username) {
          this.loadMockData(currentUser.username);
        }
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
        console.warn('API non disponible, utilisation des données mockées:', error);
        this.loadMockDataById(teamId);
      }
    });
  }

  private loadMockData(username: string) {
    const mockTeams = this.getMockTeams();
    const team = mockTeams.find(t => t.owner?.username?.toLowerCase() === username.toLowerCase());
    
    if (team) {
      this.team = team;
      this.calculateStats();
    } else {
      this.error = 'Équipe non trouvée pour cet utilisateur';
    }
    this.loading = false;
  }

  private loadMockDataById(teamId: string) {
    const mockTeams = this.getMockTeams();
    const team = mockTeams.find(t => t.id === teamId);
    
    if (team) {
      this.team = team;
      this.calculateStats();
    } else {
      this.error = 'Équipe non trouvée';
    }
    this.loading = false;
  }

  private getMockTeams(): Team[] {
    return [
      {
        id: '9dd5f0f6-ff45-4bc7-9b8c-b72bb7d9e4a7',
        name: 'Équipe Marcel',
        season: 2025,
        owner: { id: 'marcel-id', username: 'marcel' },
        totalPoints: 3784379,
        players: [
          { player: { id: '1', nickname: 'NRG ronaldo', region: 'NAW', tranche: 'T1', points: 284520 }, position: 1 },
          { player: { id: '2', nickname: 'aqua', region: 'EU', tranche: 'T1', points: 198765 }, position: 2 },
          { player: { id: '3', nickname: 'ふーくん', region: 'ASIA', tranche: 'T2', points: 156890 }, position: 3 },
          { player: { id: '4', nickname: 'らぜる', region: 'ASIA', tranche: 'T2', points: 145632 }, position: 4 },
          { player: { id: '5', nickname: 'Bugha', region: 'NAW', tranche: 'T1', points: 234567 }, position: 5 }
        ]
      },
      {
        id: '88a782ba-7a8e-47bd-9865-abeb562075ba',
        name: 'Équipe Teddy',
        season: 2025,
        owner: { id: 'teddy-id', username: 'teddy' },
        totalPoints: 3952362,
        players: [
          { player: { id: '6', nickname: 'かめてぃん.魔女', region: 'ASIA', tranche: 'T1', points: 312450 }, position: 1 },
          { player: { id: '7', nickname: 'TaySon', region: 'EU', tranche: 'T1', points: 287634 }, position: 2 },
          { player: { id: '8', nickname: 'むきむきぱぱ', region: 'ASIA', tranche: 'T2', points: 178923 }, position: 3 },
          { player: { id: '9', nickname: 'Cented', region: 'NAW', tranche: 'T1', points: 245678 }, position: 4 },
          { player: { id: '10', nickname: 'E36だゾStain', region: 'ASIA', tranche: 'T3', points: 134567 }, position: 5 }
        ]
      },
      {
        id: 'e913457f-eefd-4378-8b16-0be4f8e68003',
        name: 'Équipe Thibaut',
        season: 2025,
        owner: { id: 'thibaut-id', username: 'thibaut' },
        totalPoints: 3892041,
        players: [
          { player: { id: '11', nickname: 'Mero', region: 'EU', tranche: 'T1', points: 298765 }, position: 1 },
          { player: { id: '12', nickname: 'Reet', region: 'NAW', tranche: 'T1', points: 276543 }, position: 2 },
          { player: { id: '13', nickname: 'Jahq', region: 'NAW', tranche: 'T2', points: 198456 }, position: 3 },
          { player: { id: '14', nickname: 'Kami', region: 'EU', tranche: 'T2', points: 187234 }, position: 4 },
          { player: { id: '15', nickname: 'Queasy', region: 'EU', tranche: 'T1', points: 256789 }, position: 5 }
        ]
      }
    ];
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
    const numericPart = tranche.replace(/[Tt]/g, '');
    const parsed = parseInt(numericPart, 10);
    return isNaN(parsed) ? 1 : parsed;
  }

  getPlayerRank(player: Player): number {
    if (!this.team) return 0;
    const sortedPlayers = [...this.team.players]
      .sort((a, b) => (b.player.points || 0) - (a.player.points || 0));
    return sortedPlayers.findIndex(tp => tp.player.id === player.id) + 1;
  }

  formatPoints(points: number): string {
    return this.statsService.formatPoints(points);
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
    
    // Utiliser le service centralisé pour un calcul cohérent
    return this.statsService.getTopPercentileCount(teamWithPlayers, allTeamsAdapted, 10);
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