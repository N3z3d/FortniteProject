import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, forkJoin, throwError } from 'rxjs';
import { map, catchError, timeout, shareReplay } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
// import { LeaderboardApiMapper } from '../mappers/leaderboard-api.mapper';
import { StatsApiMapper } from '../mappers/stats-api.mapper';
import { MockDataService } from '../../../core/services/mock-data.service';

/**
 * Enhanced Dashboard Data Service with Premium Fallback
 * Provides real data from backend with premium mock fallback when unavailable
 * Maintains premium UI experience during backend outages
 */
@Injectable({
  providedIn: 'root'
})
export class DashboardDataService {
  private readonly apiUrl = `${environment.apiUrl}/api`;
  private readonly REQUEST_TIMEOUT = 5000; // 5 seconds timeout for faster feedback
  private backendAvailable = true;

  constructor(
    private http: HttpClient,
    private mockDataService: MockDataService
  ) { 
    console.log('🎮 Enhanced DashboardDataService with Premium Fallback initialized');
  }

  /**
   * Récupère les statistiques globales depuis la BDD
   * Utilise l'endpoint général du leaderboard car les stats par game n'existent pas encore
   * @param gameId - ID de la game sélectionnée (pour compatibilité future)
   * @returns Observable avec les statistiques
   */
  getGameStatistics(gameId: string): Observable<any> {
    console.log('🔍 DashboardDataService.getGameStatistics called with gameId:', gameId);
    
    // Pour l'instant, récupère les stats globales depuis l'endpoint leaderboard
    return this.http.get<any>(`${this.apiUrl}/leaderboard/stats?season=2025`)
      .pipe(
        map(response => {
          console.log('📊 Raw statistics response from API:', response);
          
          // Utiliser le mapper pour transformer les données API
          const mappedStats = StatsApiMapper.mapApiStatsToDisplayStats(response);
          console.log('✅ Mapped statistics for dashboard:', mappedStats);
          
          // Valider les statistiques mappées
          if (!StatsApiMapper.validateMappedStats(mappedStats)) {
            console.warn('⚠️ Mapped statistics validation failed, using fallback');
            return this.getEmptyStatistics();
          }
          
          return mappedStats;
        }),
        catchError(error => {
          console.error('❌ Error loading leaderboard statistics:', error);
          console.log('🔄 Returning empty statistics as fallback');
          return of(this.getEmptyStatistics());
        })
      );
  }

  /**
   * Récupère le leaderboard depuis la BDD
   * Utilise l'endpoint général du leaderboard car les endpoints par game n'existent pas encore
   * @param gameId - ID de la game (pour compatibilité future)
   * @returns Observable avec le leaderboard
   */
  getGameLeaderboard(gameId: string): Observable<any[]> {
    console.log('🔍 DashboardDataService.getGameLeaderboard called with gameId:', gameId);
    
    // Pour l'instant, récupère le leaderboard global depuis l'endpoint existant
    return this.http.get<any[]>(`${this.apiUrl}/leaderboard?season=2025`)
      .pipe(
        map(apiResponse => {
          console.log('📊 Raw leaderboard response from API:', apiResponse);
          
          // Utiliser le mapper pour convertir les données API vers le format attendu
          // const mappedEntries = LeaderboardApiMapper.mapApiResponseToLeaderboardEntries(apiResponse);
          const mappedEntries = (apiResponse as any)?.data || apiResponse || [];
          console.log('✅ Mapped leaderboard entries count:', mappedEntries.length);
          console.log('📋 Sample mapped entry:', mappedEntries[0]);
          return mappedEntries;
        }),
        catchError(error => {
          console.error('❌ Error loading leaderboard:', error);
          return of([]);
        })
      );
  }

  /**
   * Récupère la distribution des joueurs par région depuis la BDD
   * Utilise l'endpoint général car les endpoints par game n'existent pas encore
   * @param gameId - ID de la game (pour compatibilité future)
   * @returns Observable avec la distribution par région
   */
  getRegionDistribution(gameId: string): Observable<{ [key: string]: number }> {
    // Pour l'instant, récupère la distribution globale depuis l'endpoint existant
    return this.http.get<{ [key: string]: number }>(`${this.apiUrl}/leaderboard/distribution/regions`)
      .pipe(
        map(distribution => this.normalizeRegionDistribution(distribution)),
        catchError(error => {
          console.error('Error loading region distribution:', error);
          return of(this.getEmptyRegionDistribution());
        })
      );
  }

  /**
   * Récupère les équipes depuis la BDD
   * Pour l'instant, extrait les équipes du leaderboard car pas d'endpoint dédié
   * @param gameId - ID de la game (pour compatibilité future)
   * @returns Observable avec les équipes
   */
  getGameTeams(gameId: string): Observable<any[]> {
    // Pour l'instant, récupère les équipes via le leaderboard
    return this.getGameLeaderboard(gameId).pipe(
      map(leaderboard => {
        // Transforme les entrées du leaderboard en format équipes
        return leaderboard.map(entry => ({
          id: entry.teamId || entry.id,
          name: entry.teamName,
          totalPoints: entry.totalPoints || 0,
          ownerName: entry.ownerName,
          players: entry.players || []
        }));
      }),
      catchError(error => {
        console.error('Error loading teams from leaderboard:', error);
        return of([]);
      })
    );
  }

  /**
   * Enhanced method to retrieve all dashboard data with premium fallback
   * @param gameId - ID of the selected game
   * @returns Observable with all combined data or premium mock data
   */
  getDashboardData(gameId: string): Observable<{
    statistics: any;
    leaderboard: any[];
    regionDistribution: { [key: string]: number };
    teams: any[];
  }> {
    if (!gameId) {
      return throwError(() => new Error('Game ID is required'));
    }

    console.log('🔍 Enhanced DashboardDataService.getDashboardData called with gameId:', gameId);
    console.log('🌐 Backend availability status:', this.backendAvailable);

    // PERFORMANCE: Use shareReplay to avoid duplicate requests during concurrent calls
    const sharedRequest = forkJoin({
      statistics: this.getGameStatistics(gameId),
      leaderboard: this.getGameLeaderboard(gameId),
      regionDistribution: this.getRegionDistribution(gameId)
    }).pipe(
      timeout(this.REQUEST_TIMEOUT),
      shareReplay(1) // Cache the result for 1 emission
    );

    return sharedRequest.pipe(
      map(data => {
        console.log('✅ Real API data received:', data);
        this.backendAvailable = true;
        
        const finalData = {
          statistics: data.statistics,
          leaderboard: data.leaderboard,
          regionDistribution: data.regionDistribution,
          teams: Array.isArray(data.leaderboard)
            ? data.leaderboard.map(entry => ({
                id: entry.teamId || entry.id,
                name: entry.teamName,
                totalPoints: entry.totalPoints || 0,
                ownerName: entry.ownerName,
                players: entry.players || []
              }))
            : []
        };
        
        console.log('✅ Final real dashboard data:', finalData);
        return finalData;
      }),
      catchError(error => {
        console.warn('⚠️ Backend unavailable, switching to premium mock data:', error.message);
        this.backendAvailable = false;
        
        // Fallback to premium mock data to maintain UI experience
        return this.mockDataService.getMockDashboardData(gameId).pipe(
          map(mockData => {
            console.log('🎮 Premium mock data loaded as fallback:', mockData);
            return {
              ...mockData,
              _isPremiumMockData: true // Flag to indicate mock data
            };
          })
        );
      })
    );
  }

  /**
   * Enhanced game validation with premium fallback
   * @param gameId - ID of the game to validate
   * @returns Observable<boolean>
   */
  validateGameAccess(gameId: string): Observable<boolean> {
    if (!gameId) {
      return of(false);
    }

    return this.http.get(`${this.apiUrl}/games/${gameId}`)
      .pipe(
        timeout(this.REQUEST_TIMEOUT),
        map(() => {
          this.backendAvailable = true;
          return true;
        }),
        catchError(error => {
          console.warn('🌐 Backend validation failed, using mock validation:', error.message);
          this.backendAvailable = false;
          // For mock data, accept any non-empty gameId
          return of(gameId.startsWith('mock-') || gameId.length > 0);
        })
      );
  }

  /**
   * Check if backend is available
   * @returns boolean indicating backend availability
   */
  isBackendAvailable(): boolean {
    return this.backendAvailable;
  }

  /**
   * Get premium mock data status for UI indicators
   * @returns Observable<boolean>
   */
  getPremiumMockStatus(): Observable<boolean> {
    return of(!this.backendAvailable);
  }

  /**
   * Calcule les statistiques dérivées à partir des données BDD
   * @param rawData - Données brutes de la BDD
   * @returns Statistiques calculées
   */
  calculateDerivedStatistics(rawData: any) {
    if (!rawData) {
      return this.getEmptyStatistics();
    }

    return {
      totalTeams: rawData.teams?.length || 0,
      totalPlayers: this.calculateTotalPlayers(rawData.teams || []),
      totalPoints: this.calculateTotalPoints(rawData.teams || []),
      averagePointsPerTeam: this.calculateAveragePoints(rawData.teams || []),
      mostActiveTeam: this.findMostActiveTeam(rawData.teams || []),
      seasonProgress: this.calculateSeasonProgress()
    };
  }

  /**
   * Normalise la distribution des régions avec toutes les régions possibles
   * @param distribution - Distribution brute de l'API
   * @returns Distribution normalisée
   */
  private normalizeRegionDistribution(distribution: { [key: string]: number } = {}): { [key: string]: number } {
    const allRegions = ['EU', 'NAC', 'NAW', 'BR', 'ASIA', 'OCE', 'ME'];
    const normalized: { [key: string]: number } = {};

    allRegions.forEach(region => {
      normalized[region] = distribution[region] || 0;
    });

    return normalized;
  }

  /**
   * Calcule le nombre total de joueurs
   * @param teams - Liste des équipes
   * @returns Nombre total de joueurs
   */
  private calculateTotalPlayers(teams: any[]): number {
    console.log('🔍 Calculating total players from teams:', teams);
    console.log('📊 Teams count:', teams.length);
    
    const totalPlayers = teams.reduce((total, team, index) => {
      const playersCount = team.players?.length || 0;
      console.log(`Team ${index + 1} (${team.name || team.teamName || 'Unknown'}): ${playersCount} players`);
      return total + playersCount;
    }, 0);
    
    console.log('✅ Total players calculated:', totalPlayers);
    return totalPlayers;
  }

  /**
   * Calcule le total des points
   * @param teams - Liste des équipes
   * @returns Total des points
   */
  private calculateTotalPoints(teams: any[]): number {
    return teams.reduce((total, team) => {
      return total + (team.totalPoints || 0);
    }, 0);
  }

  /**
   * Calcule la moyenne des points par équipe
   * @param teams - Liste des équipes
   * @returns Moyenne des points
   */
  private calculateAveragePoints(teams: any[]): number {
    if (teams.length === 0) return 0;
    const totalPoints = this.calculateTotalPoints(teams);
    return Math.round(totalPoints / teams.length);
  }

  /**
   * Trouve l'équipe la plus active
   * @param teams - Liste des équipes
   * @returns Nom de l'équipe la plus active
   */
  private findMostActiveTeam(teams: any[]): string {
    if (teams.length === 0) return 'Aucune équipe';

    const mostActive = teams.reduce((prev, current) => {
      return (current.totalPoints || 0) > (prev.totalPoints || 0) ? current : prev;
    });

    return mostActive.name || mostActive.ownerName || 'Équipe inconnue';
  }

  /**
   * Calcule le progrès de la saison
   * @returns Pourcentage du progrès (0-100)
   */
  private calculateSeasonProgress(): number {
    const now = new Date();
    const startOfYear = new Date(now.getFullYear(), 0, 1);
    const endOfYear = new Date(now.getFullYear(), 11, 31);
    
    const totalDays = (endOfYear.getTime() - startOfYear.getTime()) / (1000 * 60 * 60 * 24);
    const daysElapsed = (now.getTime() - startOfYear.getTime()) / (1000 * 60 * 60 * 24);
    
    return Math.round((daysElapsed / totalDays) * 100 * 10) / 10;
  }

  /**
   * Retourne des statistiques vides par défaut
   * @returns Objet de statistiques vide
   */
  private getEmptyStatistics() {
    return {
      totalTeams: 0,
      totalPlayers: 0,
      totalPoints: 0,
      averagePointsPerTeam: 0,
      mostActiveTeam: 'Aucune équipe',
      seasonProgress: this.calculateSeasonProgress()
    };
  }

  /**
   * Retourne une distribution de régions vide
   * @returns Distribution vide avec toutes les régions à 0
   */
  private getEmptyRegionDistribution(): { [key: string]: number } {
    return {
      'EU': 0,
      'NAC': 0,
      'NAW': 0,
      'BR': 0,
      'ASIA': 0,
      'OCE': 0,
      'ME': 0
    };
  }
}
