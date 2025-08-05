import { LeaderboardEntryDTO, Player } from '../../../core/models/leaderboard.model';

/**
 * Mapper pour transformer les réponses API de leaderboard en objets métier
 * Utilisé pour normaliser les données du leaderboard provenant de l'API backend
 */
export class LeaderboardApiMapper {

  /**
   * Mappe une réponse API de leaderboard vers le format attendu par l'interface
   * @param apiResponse - Réponse brute de l'API de leaderboard
   * @returns Entrées de leaderboard formatées
   */
  static mapApiResponseToLeaderboardEntries(apiResponse: any): LeaderboardEntryDTO[] {
    if (!apiResponse) {
      console.warn('LeaderboardApiMapper: API response is null or undefined');
      return [];
    }

    // Handle different API response formats
    const entries = apiResponse.entries || apiResponse.leaderboard || apiResponse.data || apiResponse;

    if (!Array.isArray(entries)) {
      console.warn('LeaderboardApiMapper: API response is not an array');
      return [];
    }

    return entries.map((entry, index) => this.mapSingleEntry(entry, index + 1));
  }

  /**
   * Mappe une entrée individuelle du leaderboard
   * @param entry - Entrée brute de l'API
   * @param fallbackRank - Rang de fallback si non spécifié
   * @returns Entrée de leaderboard formatée
   */
  static mapSingleEntry(entry: any, fallbackRank: number): LeaderboardEntryDTO {
    if (!entry) {
      return this.createEmptyEntry(fallbackRank);
    }

    return {
      teamId: entry.teamId || entry.id || `team_${fallbackRank}`,
      teamName: entry.teamName || entry.team?.name || entry.name || `Équipe ${fallbackRank}`,
      ownerName: entry.ownerName || entry.pronostiqueurName || entry.user?.name || entry.username || 'Joueur anonyme',
      totalPoints: this.parseNumber(entry.totalPoints || entry.points || entry.score, 0),
      rank: this.parseNumber(entry.rank || entry.position, fallbackRank),
      players: this.mapPlayers(entry.players || entry.teamPlayers || []),
      lastUpdate: this.parseDate(entry.lastUpdated || entry.updatedAt || entry.timestamp).toISOString()
    };
  }

  /**
   * Mappe les joueurs d'une équipe
   * @param players - Liste des joueurs de l'API
   * @returns Liste des joueurs formatée
   */
  static mapPlayers(players: any[]): Player[] {
    if (!Array.isArray(players)) {
      return [];
    }

    return players.map((player, index) => ({
      id: player.id || `player_${index}`,
      name: player.name || player.playerName || 'Joueur inconnu',
      nickname: player.nickname || player.displayName,
      avatar: player.avatar || player.profilePicture,
      region: player.region || player.zone || 'UNKNOWN',
      tranche: player.tranche || player.tier || 'Bronze',
      points: this.parseNumber(player.points || player.score, 0),
      teamId: player.teamId
    }));
  }

  /**
   * Calcule les statistiques agrégées d'une équipe à partir des joueurs
   * @param players - Liste des joueurs de l'équipe
   * @returns Statistiques calculées
   */
  static calculateTeamStats(players: Player[]): {
    totalPlayers: number;
    averagePoints: number;
    topPlayer: Player | null;
    regionDistribution: { [region: string]: number };
  } {
    if (!players || players.length === 0) {
      return {
        totalPlayers: 0,
        averagePoints: 0,
        topPlayer: null,
        regionDistribution: {}
      };
    }

    const totalPoints = players.reduce((sum, player) => sum + player.points, 0);
    const averagePoints = Math.round(totalPoints / players.length);
    const topPlayer = players.reduce((top, player) => 
      player.points > (top?.points || 0) ? player : top, players[0]
    );

    const regionDistribution = players.reduce((dist, player) => {
      dist[player.region] = (dist[player.region] || 0) + 1;
      return dist;
    }, {} as { [region: string]: number });

    return {
      totalPlayers: players.length,
      averagePoints,
      topPlayer,
      regionDistribution
    };
  }

  /**
   * Filtre et trie les entrées de leaderboard
   * @param entries - Entrées à filtrer
   * @param options - Options de filtrage et tri
   * @returns Entrées filtrées et triées
   */
  static filterAndSortEntries(
    entries: LeaderboardEntryDTO[], 
    options: {
      limit?: number;
      sortBy?: 'rank' | 'points' | 'teamName' | 'ownerName';
      sortOrder?: 'asc' | 'desc';
      minPoints?: number;
      searchTerm?: string;
    } = {}
  ): LeaderboardEntryDTO[] {
    let filteredEntries = [...entries];

    // Filtrage par nombre minimum de points
    if (options.minPoints && options.minPoints > 0) {
      filteredEntries = filteredEntries.filter(entry => entry.totalPoints >= options.minPoints!);
    }

    // Filtrage par terme de recherche
    if (options.searchTerm && options.searchTerm.trim()) {
      const searchTerm = options.searchTerm.toLowerCase().trim();
      filteredEntries = filteredEntries.filter(entry => 
        entry.teamName.toLowerCase().includes(searchTerm) ||
        entry.ownerName.toLowerCase().includes(searchTerm)
      );
    }

    // Tri
    if (options.sortBy) {
      filteredEntries.sort((a, b) => {
        let comparison = 0;
        
        switch (options.sortBy) {
          case 'rank':
            comparison = a.rank - b.rank;
            break;
          case 'points':
            comparison = b.totalPoints - a.totalPoints; // Ordre décroissant par défaut pour les points
            break;
          case 'teamName':
            comparison = a.teamName.localeCompare(b.teamName);
            break;
          case 'ownerName':
            comparison = a.ownerName.localeCompare(b.ownerName);
            break;
        }

        return options.sortOrder === 'asc' ? comparison : -comparison;
      });
    }

    // Limitation du nombre d'entrées
    if (options.limit && options.limit > 0) {
      filteredEntries = filteredEntries.slice(0, options.limit);
    }

    // Recalculer les rangs après filtrage
    return filteredEntries.map((entry, index) => ({
      ...entry,
      rank: index + 1
    }));
  }

  /**
   * Calcule les statistiques globales du leaderboard
   * @param entries - Entrées du leaderboard
   * @returns Statistiques globales
   */
  static calculateGlobalStats(entries: LeaderboardEntryDTO[]): {
    totalTeams: number;
    totalPlayers: number;
    averagePoints: number;
    totalPoints: number;
    topPerformer: LeaderboardEntryDTO | null;
    distribution: {
      rookie: number;      // < 100 points
      intermediate: number; // 100-500 points
      advanced: number;    // 500-1000 points
      expert: number;      // > 1000 points
    };
    regionDistribution: { [region: string]: number };
  } {
    if (!entries || entries.length === 0) {
      return {
        totalTeams: 0,
        totalPlayers: 0,
        averagePoints: 0,
        totalPoints: 0,
        topPerformer: null,
        distribution: { rookie: 0, intermediate: 0, advanced: 0, expert: 0 },
        regionDistribution: {}
      };
    }

    const totalTeams = entries.length;
    const totalPlayers = entries.reduce((sum, entry) => sum + entry.players.length, 0);
    const totalPoints = entries.reduce((sum, entry) => sum + entry.totalPoints, 0);
    const averagePoints = totalTeams > 0 ? Math.round(totalPoints / totalTeams) : 0;

    // Top performer (plus de points)
    const topPerformer = entries.reduce((top, entry) => 
      entry.totalPoints > (top?.totalPoints || 0) ? entry : top, entries[0]
    );

    // Distribution par niveau
    const distribution = entries.reduce((dist, entry) => {
      if (entry.totalPoints < 100) {
        dist.rookie++;
      } else if (entry.totalPoints < 500) {
        dist.intermediate++;
      } else if (entry.totalPoints < 1000) {
        dist.advanced++;
      } else {
        dist.expert++;
      }
      return dist;
    }, { rookie: 0, intermediate: 0, advanced: 0, expert: 0 });

    // Distribution par région
    const regionDistribution = entries.reduce((dist, entry) => {
      entry.players.forEach(player => {
        dist[player.region] = (dist[player.region] || 0) + 1;
      });
      return dist;
    }, {} as { [region: string]: number });

    return {
      totalTeams,
      totalPlayers,
      averagePoints,
      totalPoints,
      topPerformer,
      distribution,
      regionDistribution
    };
  }

  /**
   * Parse un nombre avec gestion des erreurs
   * @param value - Valeur à parser
   * @param defaultValue - Valeur par défaut
   * @returns Nombre parsé
   */
  private static parseNumber(value: any, defaultValue: number = 0): number {
    if (value === null || value === undefined) {
      return defaultValue;
    }

    const parsed = parseFloat(value);
    return isNaN(parsed) ? defaultValue : parsed;
  }

  /**
   * Parse une date avec gestion des erreurs
   * @param value - Valeur à parser
   * @returns Date parsée ou date actuelle
   */
  private static parseDate(value: any): Date {
    if (!value) {
      return new Date();
    }

    const parsed = new Date(value);
    return isNaN(parsed.getTime()) ? new Date() : parsed;
  }

  /**
   * Parse la tendance avec validation
   * @param value - Valeur de tendance
   * @returns Tendance valide
   */
  private static parseTrend(value: any): 'up' | 'down' | 'stable' {
    if (typeof value === 'string') {
      const normalized = value.toLowerCase();
      if (['up', 'rising', 'increasing', 'positive'].includes(normalized)) {
        return 'up';
      }
      if (['down', 'falling', 'decreasing', 'negative'].includes(normalized)) {
        return 'down';
      }
    }
    return 'stable';
  }

  /**
   * Crée une entrée de leaderboard vide
   * @param rank - Rang de l'entrée
   * @returns Entrée vide
   */
  private static createEmptyEntry(rank: number): LeaderboardEntryDTO {
    return {
      teamId: `team_${rank}`,
      teamName: `Équipe ${rank}`,
      ownerName: 'Joueur anonyme',
      totalPoints: 0,
      rank,
      players: [],
      lastUpdate: new Date().toISOString()
    };
  }

  /**
   * Valide une entrée de leaderboard mappée
   * @param entry - Entrée à valider
   * @returns True si l'entrée est valide
   */
  static validateEntry(entry: LeaderboardEntryDTO): boolean {
    if (!entry) {
      return false;
    }

    // Vérifications de base
    if (entry.rank < 1 || entry.totalPoints < 0) {
      return false;
    }

    if (!entry.teamId || entry.teamId.trim() === '') {
      return false;
    }

    if (!entry.teamName || entry.teamName.trim() === '') {
      return false;
    }

    if (!entry.ownerName || entry.ownerName.trim() === '') {
      return false;
    }

    if (!entry.lastUpdate) {
      return false;
    }

    // Validation des joueurs
    if (!Array.isArray(entry.players)) {
      return false;
    }

    return true;
  }
}