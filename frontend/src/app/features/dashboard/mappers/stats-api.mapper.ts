/**
 * Mapper pour transformer les réponses API de statistiques en objets métier
 * Utilisé par le DashboardDataService pour normaliser les données statistiques
 */
export class StatsApiMapper {

  /**
   * Mappe une réponse API de statistiques vers le format attendu par le dashboard
   * @param apiResponse - Réponse brute de l'API de statistiques
   * @returns Statistiques formatées pour le dashboard
   */
  static mapApiStatsToDisplayStats(apiResponse: any): {
    totalTeams: number;
    totalPlayers: number;
    totalPoints: number;
    averagePointsPerTeam: number;
    mostActiveTeam: string;
    seasonProgress: number;
    additionalStats?: {
      averageKD: number;
      totalKills: number;
      totalWins: number;
      topPerformer: string;
      winRate: number;
    };
  } {
    if (!apiResponse) {
      console.warn('StatsApiMapper: API response is null or undefined');
      return this.getEmptyStats();
    }

    // Handle different API response formats
    const stats = apiResponse.stats || apiResponse.statistics || apiResponse;
    
    // Extract basic stats
    const totalTeams = this.parseNumber(stats.totalTeams || stats.teams || 0, 0);
    const totalPlayers = this.parseNumber(stats.totalPlayers || stats.players || 0, 0);
    const totalPoints = this.parseNumber(stats.totalPoints || stats.points || 0, 0);
    
    // Calculate derived stats
    const averagePointsPerTeam = totalTeams > 0 
      ? Math.round(totalPoints / totalTeams) 
      : 0;

    const mostActiveTeam = stats.mostActiveTeam || 
                          stats.topTeam?.name || 
                          stats.leadingTeam || 
                          'Aucune équipe';

    const seasonProgress = this.parseNumber(stats.seasonProgress, this.calculateSeasonProgress());

    // Map additional stats if available
    const additionalStats = this.mapAdditionalStats(stats);

    return {
      totalTeams,
      totalPlayers,
      totalPoints,
      averagePointsPerTeam,
      mostActiveTeam,
      seasonProgress,
      additionalStats
    };
  }

  /**
   * Mappe les statistiques additionnelles si disponibles
   * @param stats - Données de statistiques de l'API
   * @returns Statistiques additionnelles formatées
   */
  private static mapAdditionalStats(stats: any): {
    averageKD: number;
    totalKills: number;
    totalWins: number;
    topPerformer: string;
    winRate: number;
  } {
    return {
      averageKD: this.parseNumber(stats.averageKD || stats.avgKD || stats.globalKD, 0),
      totalKills: this.parseNumber(stats.totalKills || stats.kills || 0, 0),
      totalWins: this.parseNumber(stats.totalWins || stats.wins || 0, 0),
      topPerformer: stats.topPerformer || 
                   stats.bestPlayer || 
                   stats.mvp || 
                   'Aucun joueur',
      winRate: this.parseNumber(stats.winRate || stats.globalWinRate || 0, 0)
    };
  }

  /**
   * Valide les statistiques mappées pour s'assurer qu'elles sont cohérentes
   * @param mappedStats - Statistiques mappées à valider
   * @returns True si les statistiques sont valides
   */
  static validateMappedStats(mappedStats: any): boolean {
    if (!mappedStats) {
      console.warn('StatsApiMapper: Mapped stats is null or undefined');
      return false;
    }

    // Vérifications de base
    const requiredFields = ['totalTeams', 'totalPlayers', 'totalPoints', 'averagePointsPerTeam'];
    for (const field of requiredFields) {
      if (mappedStats[field] === undefined || mappedStats[field] === null) {
        console.warn(`StatsApiMapper: Required field '${field}' is missing`);
        return false;
      }
    }

    // Vérifications de cohérence
    if (mappedStats.totalTeams < 0 || mappedStats.totalPlayers < 0 || mappedStats.totalPoints < 0) {
      console.warn('StatsApiMapper: Negative values detected in stats');
      return false;
    }

    if (mappedStats.totalPlayers > 0 && mappedStats.totalTeams === 0) {
      console.warn('StatsApiMapper: Players exist but no teams - inconsistent data');
      return false;
    }

    if (mappedStats.seasonProgress < 0 || mappedStats.seasonProgress > 100) {
      console.warn('StatsApiMapper: Season progress out of valid range (0-100)');
      return false;
    }

    return true;
  }

  /**
   * Transforme les statistiques de leaderboard en statistiques d'équipe
   * @param leaderboardEntries - Entrées du leaderboard
   * @returns Statistiques d'équipe calculées
   */
  static mapLeaderboardToTeamStats(leaderboardEntries: any[]): {
    totalTeams: number;
    averageTeamSize: number;
    topTeams: Array<{
      name: string;
      points: number;
      playerCount: number;
      rank: number;
    }>;
  } {
    if (!Array.isArray(leaderboardEntries) || leaderboardEntries.length === 0) {
      return {
        totalTeams: 0,
        averageTeamSize: 0,
        topTeams: []
      };
    }

    // Group by team
    const teamMap = new Map();
    
    leaderboardEntries.forEach(entry => {
      const teamName = entry.teamName || entry.team?.name || 'Équipe inconnue';
      const points = this.parseNumber(entry.totalPoints || entry.points, 0);
      const playersCount = entry.players?.length || 1;

      if (teamMap.has(teamName)) {
        const existingTeam = teamMap.get(teamName);
        existingTeam.points += points;
        existingTeam.playerCount += playersCount;
      } else {
        teamMap.set(teamName, {
          name: teamName,
          points: points,
          playerCount: playersCount,
          rank: entry.rank || 0
        });
      }
    });

    const teams = Array.from(teamMap.values());
    const totalTeams = teams.length;
    const totalPlayers = teams.reduce((sum, team) => sum + team.playerCount, 0);
    const averageTeamSize = totalTeams > 0 ? Math.round(totalPlayers / totalTeams) : 0;

    // Sort teams by points and take top 10
    const topTeams = teams
      .sort((a, b) => b.points - a.points)
      .slice(0, 10)
      .map((team, index) => ({
        ...team,
        rank: index + 1
      }));

    return {
      totalTeams,
      averageTeamSize,
      topTeams
    };
  }

  /**
   * Calcule les métriques de performance globales
   * @param rawData - Données brutes de l'API
   * @returns Métriques de performance
   */
  static calculatePerformanceMetrics(rawData: any): {
    efficiency: number;
    competition: number;
    activity: number;
    growth: number;
  } {
    if (!rawData) {
      return { efficiency: 0, competition: 0, activity: 0, growth: 0 };
    }

    const totalTeams = this.parseNumber(rawData.totalTeams, 0);
    const totalPlayers = this.parseNumber(rawData.totalPlayers, 0);
    const totalPoints = this.parseNumber(rawData.totalPoints, 0);
    const avgPoints = totalTeams > 0 ? totalPoints / totalTeams : 0;

    // Calcul des métriques (0-100)
    const efficiency = Math.min(100, Math.round((avgPoints / 1000) * 100)); // Assumant 1000 comme points max théoriques
    const competition = Math.min(100, totalTeams * 2); // Plus d'équipes = plus de compétition
    const activity = Math.min(100, (totalPlayers / 10) * 10); // Plus de joueurs = plus d'activité
    const growth = this.calculateGrowthRate(rawData.historicalData);

    return {
      efficiency: Math.max(0, efficiency),
      competition: Math.max(0, competition),
      activity: Math.max(0, activity),
      growth: Math.max(0, growth)
    };
  }

  /**
   * Parse un nombre avec gestion des erreurs et valeur par défaut
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
   * Calcule le progrès de la saison actuelle
   * @returns Pourcentage de progrès (0-100)
   */
  private static calculateSeasonProgress(): number {
    const now = new Date();
    const startOfYear = new Date(now.getFullYear(), 0, 1);
    const endOfYear = new Date(now.getFullYear(), 11, 31);
    
    const totalMs = endOfYear.getTime() - startOfYear.getTime();
    const elapsedMs = now.getTime() - startOfYear.getTime();
    
    const progress = (elapsedMs / totalMs) * 100;
    return Math.round(Math.max(0, Math.min(100, progress)) * 10) / 10;
  }

  /**
   * Calcule le taux de croissance basé sur les données historiques
   * @param historicalData - Données historiques
   * @returns Taux de croissance (0-100)
   */
  private static calculateGrowthRate(historicalData: any): number {
    if (!historicalData || !Array.isArray(historicalData) || historicalData.length < 2) {
      return 50; // Valeur neutre si pas de données historiques
    }

    const recent = historicalData[historicalData.length - 1];
    const previous = historicalData[historicalData.length - 2];

    if (!recent || !previous || previous.totalPlayers === 0) {
      return 50;
    }

    const growthRate = ((recent.totalPlayers - previous.totalPlayers) / previous.totalPlayers) * 100;
    
    // Normaliser entre 0 et 100 (50 = pas de croissance, >50 = croissance positive)
    return Math.round(Math.max(0, Math.min(100, 50 + growthRate)));
  }

  /**
   * Retourne des statistiques vides par défaut
   * @returns Objet de statistiques vide
   */
  private static getEmptyStats() {
    return {
      totalTeams: 0,
      totalPlayers: 0,
      totalPoints: 0,
      averagePointsPerTeam: 0,
      mostActiveTeam: 'Aucune équipe',
      seasonProgress: this.calculateSeasonProgress(),
      additionalStats: {
        averageKD: 0,
        totalKills: 0,
        totalWins: 0,
        topPerformer: 'Aucun joueur',
        winRate: 0
      }
    };
  }

  /**
   * Formate les statistiques pour l'affichage avec unités appropriées
   * @param stats - Statistiques à formater
   * @returns Statistiques formatées avec unités
   */
  static formatStatsForDisplay(stats: any): {
    [key: string]: {
      value: string;
      label: string;
      trend?: 'up' | 'down' | 'stable';
      color?: 'primary' | 'accent' | 'warn';
    };
  } {
    if (!stats) {
      return {};
    }

    return {
      totalTeams: {
        value: stats.totalTeams.toLocaleString(),
        label: 'Équipes actives',
        color: 'primary'
      },
      totalPlayers: {
        value: stats.totalPlayers.toLocaleString(),
        label: 'Joueurs inscrits',
        color: 'accent'
      },
      totalPoints: {
        value: this.formatLargeNumber(stats.totalPoints),
        label: 'Points totaux',
        color: 'primary'
      },
      averagePointsPerTeam: {
        value: stats.averagePointsPerTeam.toLocaleString(),
        label: 'Points moyens/équipe',
        color: 'accent'
      },
      seasonProgress: {
        value: `${stats.seasonProgress}%`,
        label: 'Progrès saison',
        color: stats.seasonProgress > 75 ? 'warn' : 'primary'
      }
    };
  }

  /**
   * Formate les grands nombres avec des unités appropriées (K, M, B)
   * @param number - Nombre à formater
   * @returns Nombre formaté avec unité
   */
  private static formatLargeNumber(number: number): string {
    if (number >= 1000000000) {
      return (number / 1000000000).toFixed(1) + 'B';
    }
    if (number >= 1000000) {
      return (number / 1000000).toFixed(1) + 'M';
    }
    if (number >= 1000) {
      return (number / 1000).toFixed(1) + 'K';
    }
    return number.toString();
  }
}