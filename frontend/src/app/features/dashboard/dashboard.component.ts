import { Component, OnInit, AfterViewInit, ViewChild, ElementRef, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { PremiumInteractionsDirective, TooltipDirective, RevealOnScrollDirective } from '../../shared/directives/premium-interactions.directive';
import { PremiumInteractionsService } from '../../shared/services/premium-interactions.service';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatBadgeModule } from '@angular/material/badge';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
// PERFORMANCE: Import only needed Chart.js components instead of everything
import {
  Chart,
  DoughnutController,
  BarController,
  CategoryScale,
  LinearScale,
  ArcElement,
  BarElement,
  Title,
  Tooltip,
  Legend
} from 'chart.js';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { interval } from 'rxjs';
import { LeaderboardEntryDTO } from '../../core/models/leaderboard.model';
import { GameSelectionService } from '../../core/services/game-selection.service';
import { GameService } from '../game/services/game.service';
import { DashboardFacade } from '../../core/facades/dashboard.facade';
import { DashboardChartService } from './services/dashboard-chart.service';
import { Game } from '../game/models/game.interface';
import { AccessibilityAnnouncerService } from '../../shared/services/accessibility-announcer.service';
import { FocusManagementService } from '../../shared/services/focus-management.service';
import { LoggerService } from '../../core/services/logger.service';
import { TranslationService } from '../../core/services/translation.service';

// PERFORMANCE: Register only necessary components (reduces bundle size by ~100KB)
Chart.register(
  DoughnutController,
  BarController,
  CategoryScale,
  LinearScale,
  ArcElement,
  BarElement,
  Title,
  Tooltip,
  Legend
);

// Interface unifiée pour les statistiques
interface DashboardStats {
  totalTeams: number;
  totalPlayers: number;
  totalPoints: number;
  averagePointsPerTeam: number;
  mostActiveTeam: string;
  seasonProgress: number;
  proPlayersCount?: number;
  teamComposition?: {
    regions: { [key: string]: number };
    tranches: { [key: string]: number };
  };
  lastUpdate?: string;
}

interface TeamComposition {
  regions: { [key: string]: number };
  tranches: { [key: string]: number };
}

interface CompetitionStats {
  totalTeams: number;
  totalPlayers: number;
  totalPoints: number;
  averagePointsPerTeam: number;
  mostActiveTeam: string;
  seasonProgress: number;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatBadgeModule,
    MatSnackBarModule,
    PremiumInteractionsDirective,
    TooltipDirective,
    RevealOnScrollDirective
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('regionChart') regionChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('pointsChart') pointsChartRef!: ElementRef<HTMLCanvasElement>;

  // État unifié
  stats: DashboardStats = {
    totalTeams: 0,
    totalPlayers: 0,
    totalPoints: 0,
    averagePointsPerTeam: 0,
    mostActiveTeam: '',
    seasonProgress: 0,
    proPlayersCount: 0 // Initialize new field
  };

  competitionStats: CompetitionStats = {
    totalTeams: 0,
    totalPlayers: 0,
    totalPoints: 0,
    averagePointsPerTeam: 0,
    mostActiveTeam: '',
    seasonProgress: this.calculateSeasonProgress()
  };

  leaderboardEntries: LeaderboardEntryDTO[] = [];
  selectedGame: Game | null = null;
  games: Game[] = [];
  selectedGameId: string | null = null;

  // Liste complète des régions (pour assurer un graphique complet)
  static readonly ALL_REGIONS: string[] = ['EU', 'NAC', 'NAW', 'BR', 'ASIA', 'OCE', 'ME'];

  // Graphiques
  private regionChart: Chart | null = null;
  private pointsChart: Chart | null = null;

  // États
  isLoading = true;
  isLoadingProgress = 0;
  error: string | null = null;
  lastUpdate: Date | null = null;
  isUsingMockData = false;
  backendStatus: 'online' | 'offline' | 'checking' = 'checking';

  private subscriptions: Subscription[] = [];
  private readonly LOAD_TIMEOUT = 10000; // 10 secondes
  private readonly REFRESH_INTERVAL = 300000; // 5 minutes
  private readonly loadErrorMessage = 'Donn\u00e9es indisponibles (CSV non charg\u00e9)';
  currentSeason = 2025;

  // Premium stats properties (calculated from real data)
  totalScore = 0;
  ranking = 0;
  activeGames = 0;
  weeklyBest = 0;

  // UI-2: New Leaderboard Mock Data
  dashboardLeaderboard: any[] = [];

  constructor(
    private http: HttpClient,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar,
    private gameSelectionService: GameSelectionService,
    private dashboardFacade: DashboardFacade,
    private chartService: DashboardChartService,
    private logger: LoggerService,
    private cdr: ChangeDetectorRef,
    private interactionsService: PremiumInteractionsService,
    private gameService: GameService,
    private accessibilityService: AccessibilityAnnouncerService,
    private focusManagementService: FocusManagementService,
    public t: TranslationService
  ) { }

  ngOnInit() {
    // Charger la liste des games
    this.loadGames();

    this.subscriptions.push(
      this.t.language$.subscribe(() => {
        this.cdr.markForCheck();
      })
    );

    // Get gameId from route params if available
    this.subscriptions.push(
      this.route.params.subscribe(params => {
        const gameId = params['id'];
        if (gameId) {
          // Load specific game data
          this.subscriptions.push(
            this.gameService.getGameById(gameId).subscribe({
              next: (game) => {
                this.selectedGame = game;
                this.loadDashboardData();
              },
              error: (error) => {
                this.logger.error('Dashboard: failed to load game', error);
              }
            })
          );
        }
      })
    );

    // S'abonner aux changements de game sélectionnée
    this.subscriptions.push(
      this.gameSelectionService.selectedGame$.subscribe(game => {
        this.selectedGame = game;
        if (game) {
          this.loadDashboardData();
        }
      })
    );

    // Charger les données si une game est déjà sélectionnée
    if (this.gameSelectionService.hasSelectedGame()) {
      this.loadDashboardData();
    }

    // Rafraîchissement automatique seulement si une game est sélectionnée
    this.subscriptions.push(
      interval(this.REFRESH_INTERVAL).subscribe(() => {
        if (this.selectedGame) {
          this.loadDashboardData(false);
        }
      })
    );
  }

  ngAfterViewInit() {
    // Initialize charts only when data is available and view is ready
    if (this.stats.totalTeams > 0 && this.leaderboardEntries.length > 0) {
      setTimeout(() => this.initializeCharts(), 50);
    }
  }

  ngOnDestroy() {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    if (this.regionChart) this.regionChart.destroy();
    if (this.pointsChart) this.pointsChart.destroy();
  }

  private loadGames() {
    this.gameService.getUserGames().subscribe({
      next: (games: Game[]) => {
        this.games = games;
      },
      error: (error: any) => {
        this.logger.error('Dashboard: failed to load games', error);
        this.games = [];
      }
    });
  }

  async loadDashboardData(showLoading = true) {
    if (!this.selectedGame?.id) {
      this.error = this.t.t('dashboard.live.noGameSelected');
      this.updateLiveRegion(this.t.t('dashboard.live.noGameSelected'), 'assertive');
      return;
    }

    if (showLoading) {
      this.isLoading = true;
      this.isLoadingProgress = 0;
      this.accessibilityService.announceLoading(true, this.t.t('navigation.dashboard'));
    }
    this.error = null;

    try {
      // NEW: Using DashboardFacade with automatic DB/Mock fallback
      this.dashboardFacade.getDashboardData(this.selectedGame.id).subscribe({
        next: (data: any) => {
          this.logger.debug('📊 Dashboard data received:', data);

          this.isUsingMockData = false;
          this.backendStatus = 'online';

          // Update leaderboard
          if (data.leaderboard && Array.isArray(data.leaderboard)) {
            this.logger.debug('📈 Leaderboard data', { entriesCount: data.leaderboard.length });
            this.leaderboardEntries = data.leaderboard;
            this.updateStatsFromLeaderboard(data.leaderboard);
          }

          // Update statistics
          if (data.statistics) {
            this.logger.debug('📊 Statistics data:', data.statistics);
            this.updateCompetitionStats(data.statistics);
          }

          // Update region distribution
          if (data.regionDistribution) {
            this.logger.debug('🌍 Region distribution:', data.regionDistribution);
            this.stats.teamComposition = this.stats.teamComposition || { regions: {}, tranches: {} };
            this.stats.teamComposition.regions = data.regionDistribution;
          }

          this.lastUpdate = new Date();
          this.isLoadingProgress = 100;
          if (showLoading) this.isLoading = false;


          // UI-2: Override with specific mock data for redesign request
          if (this.isUsingMockData) {
            this.setupMockData();
          }

          this.logger.debug('📊 Final stats after update:', this.stats);

          // Announce successful data load
          this.accessibilityService.announceLoading(false, this.t.t('navigation.dashboard'));

          const formattedTeams = this.formatNumber(this.stats.totalTeams);
          const formattedPlayers = this.formatNumber(this.stats.totalPlayers);
          this.updateLiveRegion(
            `${this.t.t('dashboard.live.updatedPrefix')} ${formattedTeams} ${this.t.t('dashboard.live.teams')} ${this.t.t('common.and')} ${formattedPlayers} ${this.t.t('dashboard.live.players')}`,
            'polite'
          );

          // Trigger change detection
          this.cdr.markForCheck();

          // Update charts
          setTimeout(() => this.updateCharts(), 100);
        },
        error: (error) => {
          this.logger.warn('Dashboard: backend offline, switching to mock data', error);

          // Fallback to mock data on error so UI is visible
          this.isUsingMockData = true;
          this.backendStatus = 'offline';
          this.setupMockData();

          this.error = null; // Clear error to show content
          this.isLoadingProgress = 100;

          if (showLoading) {
            this.isLoading = false;
          }

          // Trigger change detection and charts
          this.cdr.markForCheck();
          setTimeout(() => {
            this.initializeCharts();
            this.updateCharts();
          }, 100);

          this.snackBar.open('Mode Démonstration (Backend hors ligne)', 'OK', { duration: 3000 });
        }
      });
    } catch (error) {
      this.logger.error('Dashboard: unexpected error', error);
      this.isLoading = false;
    }
  }

  private setupMockData() {
    // BE-P1-02: Use zero/placeholder values instead of fake hardcoded data
    // These values indicate no real data is available (backend offline)
    this.stats.totalTeams = 0;
    this.stats.totalPlayers = 0;
    this.stats.totalPoints = 0;
    this.stats.seasonProgress = this.calculateSeasonProgress();
    this.stats.proPlayersCount = 0;

    // Empty region data when no backend
    this.stats.teamComposition = {
      regions: {},
      tranches: {}
    };

    // Empty leaderboard when backend offline
    this.dashboardLeaderboard = [];
    this.leaderboardEntries = [];
  }



  private updateStats(newStats: Partial<DashboardStats>) {
    // Mettre à jour les stats principales
    this.stats = {
      ...this.stats,
      ...newStats,
      lastUpdate: new Date().toISOString()
    };

    // Synchroniser competitionStats avec les nouvelles stats
    this.competitionStats = {
      totalTeams: this.stats.totalTeams,
      totalPlayers: this.stats.totalPlayers,
      totalPoints: this.stats.totalPoints,
      averagePointsPerTeam: this.stats.averagePointsPerTeam,
      mostActiveTeam: this.stats.mostActiveTeam,
      seasonProgress: this.calculateSeasonProgress()
    };

    this.logger.debug('📊 Stats mises à jour:', {
      stats: this.stats,
      competitionStats: this.competitionStats
    });
  }

  private updateStatsFromLeaderboard(entries: LeaderboardEntryDTO[]) {
    if (!entries?.length) {
      this.logger.debug('⚠️ No leaderboard entries to process');
      return;
    }

    this.logger.debug('📊 Processing leaderboard entries:', entries.length);
    this.logger.debug('📊 First entry example:', entries[0]);

    const newStats: Partial<DashboardStats> = {
      totalTeams: entries.length,
      totalPoints: entries.reduce((sum, entry) => sum + (entry.totalPoints || 0), 0),
      seasonProgress: this.calculateSeasonProgress()
    };

    newStats.averagePointsPerTeam = Math.round(newStats.totalPoints! / newStats.totalTeams!);

    // Calculer la composition par région
    const regionCounts: { [key: string]: number } = {};
    const trancheCounts: { [key: string]: number } = {};
    let totalPlayers = 0;

    entries.forEach((entry, index) => {
      this.logger.debug(`📊 Processing team ${index}: ${entry.teamName} with ${entry.players?.length || 0} players`);

      entry.players?.forEach(player => {
        totalPlayers++;
        // Gérer le cas où region pourrait être un objet ou un string
        const region = typeof player.region === 'string' ? player.region :
          (player.region as any)?.name || player.region || 'Unknown';
        regionCounts[region] = (regionCounts[region] || 0) + 1;

        const tranche = `Tranche ${player.tranche || 'Unknown'}`;
        trancheCounts[tranche] = (trancheCounts[tranche] || 0) + 1;

        this.logger.debug(`👤 Player: ${player.nickname}, Region: ${region}, Tranche: ${player.tranche}`);
      });
    });

    this.logger.debug('📊 Region counts:', regionCounts);
    this.logger.debug('📊 Total players:', totalPlayers);

    newStats.totalPlayers = totalPlayers;

    // Vérifier que entries n'est pas vide avant reduce
    newStats.mostActiveTeam = entries.length > 0
      ? entries.reduce((prev, current) => (current.totalPoints || 0) > (prev.totalPoints || 0) ? current : prev).teamName
      : 'N/A';

    newStats.teamComposition = {
      regions: regionCounts,
      tranches: trancheCounts
    };

    this.logger.debug('📊 Final calculated stats:', newStats);
    this.updateStats(newStats as DashboardStats);

    // Update premium stats from real data
    this.updatePremiumStats(entries);
  }

  /**
   * Update premium stats widgets from real leaderboard data
   */
  private updatePremiumStats(entries: LeaderboardEntryDTO[]) {
    // Total score = total points across all teams
    this.totalScore = entries.reduce((sum, entry) => sum + (entry.totalPoints || 0), 0);

    // Active games = number of games (for now, set to games count)
    this.activeGames = this.games.length;

    // Weekly best = highest scoring team this period
    this.weeklyBest = entries.length > 0
      ? Math.max(...entries.map(e => e.totalPoints || 0))
      : 0;

    // Ranking = find current user's team rank (for now, show total teams as placeholder)
    this.ranking = entries.length;

    this.logger.debug('📊 Premium stats updated:', {
      totalScore: this.totalScore,
      ranking: this.ranking,
      activeGames: this.activeGames,
      weeklyBest: this.weeklyBest
    });
  }

  private updateCompetitionStats(apiStats: any) {
    if (apiStats) {
      const seasonProgress = this.calculateSeasonProgress();

      this.competitionStats = {
        totalTeams: apiStats.totalTeams || this.stats.totalTeams,
        totalPlayers: apiStats.totalPlayers || this.stats.totalPlayers,
        totalPoints: this.stats.totalPoints,
        averagePointsPerTeam: apiStats.averagePoints || this.stats.averagePointsPerTeam,
        mostActiveTeam: this.stats.mostActiveTeam,
        seasonProgress: seasonProgress
      };

      // Utiliser les vraies données de l'API pour les stats globales
      this.stats.totalPlayers = apiStats.totalPlayers;
      this.stats.totalPoints = apiStats.totalPoints || this.stats.totalPoints;

      this.logger.debug('📊 Stats API reçues:', apiStats);
    } else {
      // Utiliser les stats calculées
      const seasonProgress = this.calculateSeasonProgress();

      this.competitionStats = {
        totalTeams: this.stats.totalTeams,
        totalPlayers: this.stats.totalPlayers,
        totalPoints: this.stats.totalPoints,
        averagePointsPerTeam: this.stats.averagePointsPerTeam,
        mostActiveTeam: this.stats.mostActiveTeam,
        seasonProgress: seasonProgress
      };
    }
  }

  /**
   * Calculer le progrès de la saison dynamiquement
   * Base sur la période janvier-décembre
   */
  private calculateSeasonProgress(): number {
    const now = new Date();
    const startOfYear = new Date(now.getFullYear(), 0, 1); // 1er janvier
    const endOfYear = new Date(now.getFullYear(), 11, 31); // 31 décembre

    const totalDays = (endOfYear.getTime() - startOfYear.getTime()) / (1000 * 60 * 60 * 24);
    const daysElapsed = (now.getTime() - startOfYear.getTime()) / (1000 * 60 * 60 * 24);

    const progress = (daysElapsed / totalDays) * 100;
    return Math.round(progress * 10) / 10; // Arrondir à 1 décimale
  }

  private initializeCharts() {
    this.createRegionChart();
    this.createPointsChart();
  }

  private createRegionChart() {
    if (!this.regionChartRef || !this.stats.teamComposition?.regions) return;

    // Destroy existing chart
    if (this.regionChart) {
      this.chartService.destroyChart(this.regionChart);
      this.regionChart = null;
    }

    // Validate data
    if (!this.chartService.isValidRegionDistribution(this.stats.teamComposition.regions)) {
      this.logger.warn('⚠️ Invalid region distribution data, skipping chart creation');
      return;
    }

    // NEW: Use DashboardChartService to create chart
    this.regionChart = this.chartService.createRegionChart(
      this.regionChartRef.nativeElement,
      this.stats.teamComposition.regions
    );
  }

  private createPointsChart() {
    if (!this.pointsChartRef || !this.leaderboardEntries.length) return;

    // Destroy existing chart
    if (this.pointsChart) {
      this.chartService.destroyChart(this.pointsChart);
      this.pointsChart = null;
    }

    // Validate data
    if (!this.chartService.isValidLeaderboardData(this.leaderboardEntries)) {
      this.logger.warn('⚠️ Invalid leaderboard data, skipping chart creation');
      return;
    }

    // NEW: Use DashboardChartService to create chart
    this.pointsChart = this.chartService.createPointsChart(
      this.pointsChartRef.nativeElement,
      this.leaderboardEntries,
      10 // Top 10 teams
    );
  }

  // Méthodes utilitaires
  private getNumberLocale(): string {
    return this.t.currentLanguage === 'en' ? 'en-US' : 'fr-FR';
  }

  getTotalFormattedPoints(): string {
    const points = this.stats.totalPoints || 0;
    return points.toLocaleString(this.getNumberLocale());
  }

  getAverageFormattedPoints(): string {
    const points = this.stats.averagePointsPerTeam || 0;
    return points.toLocaleString(this.getNumberLocale());
  }

  getTopTeams(): LeaderboardEntryDTO[] {
    return this.leaderboardEntries.slice(0, 5);
  }

  private getRouteLabel(route: string): string {
    switch (route) {
      case '/games':
        return this.t.t('navigation.games');
      case '/leaderboard':
        return this.t.t('navigation.leaderboard');
      case '/teams':
        return this.t.t('navigation.teams');
      case '/trades':
        return this.t.t('navigation.trades');
      case '/draft':
        return this.t.t('navigation.draft');
      case '/games/create':
        return this.t.t('games.createGame');
      default:
        return route;
    }
  }

  navigateTo(route: string) {
    // Announce navigation intent to screen readers
    const label = this.getRouteLabel(route);
    this.accessibilityService.announceNavigation(label);

    // Update live region with navigation announcement
    this.updateLiveRegion(`${this.t.t('dashboard.live.navigatingTo')} ${label}`, 'polite');

    this.router.navigate([route]);
  }

  // Enhanced navigation with keyboard support
  onNavigationKeyDown(event: KeyboardEvent, route: string): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.navigateTo(route);
    }
  }

  // Live region announcements
  private updateLiveRegion(message: string, priority: 'polite' | 'assertive' = 'polite'): void {
    const regionId = priority === 'assertive' ? 'dashboard-alerts' : 'dashboard-announcements';
    const region = document.getElementById(regionId);
    if (region) {
      region.textContent = message;
      // Clear after announcement to allow repeated messages
      setTimeout(() => {
        region.textContent = '';
      }, 1000);
    }
  }

  refreshData() {
    this.loadDashboardData();
  }

  // OPTIMIZED: Update charts without destroying/recreating for better performance
  private updateCharts() {
    // Check if we have data before attempting updates
    if (!this.stats.teamComposition?.regions || this.leaderboardEntries.length === 0) {
      return;
    }

    // Update existing charts instead of destroying/recreating
    if (this.regionChart && this.stats.teamComposition.regions) {
      this.updateRegionChartData();
    } else {
      this.createRegionChart();
    }

    if (this.pointsChart && this.leaderboardEntries.length > 0) {
      this.updatePointsChartData();
    } else {
      this.createPointsChart();
    }
  }

  // PERFORMANCE: Update existing chart data instead of recreation (using DashboardChartService)
  private updateRegionChartData() {
    if (!this.regionChart || !this.stats.teamComposition?.regions) return;

    const regions = this.stats.teamComposition.regions;
    const data = Object.values(regions);
    const labels = Object.keys(regions);

    this.chartService.updateChart(this.regionChart, data, labels);
  }

  // PERFORMANCE: Update existing chart data instead of recreation (using DashboardChartService)
  private updatePointsChartData() {
    if (!this.pointsChart || this.leaderboardEntries.length === 0) return;

    const topTeams = this.leaderboardEntries.slice(0, 10);
    const labels = topTeams.map(entry =>
      this.displayTeamName(entry.teamName) || entry.ownerName || `${this.t.t('dashboard.labels.team')} ${entry.rank}`);
    const data = topTeams.map(entry => entry.totalPoints);

    this.chartService.updateChart(this.pointsChart, data, labels);
  }

  getSeasonProgress(): number {
    return this.calculateSeasonProgress();
  }

  getRegionPercentage(region: string): number {
    if (!this.stats.teamComposition?.regions || !this.stats.totalPlayers) return 0;

    const regionCount = this.stats.teamComposition.regions[region] || 0;
    const percentage = (regionCount / this.stats.totalPlayers) * 100;

    // Arrondir à 1 décimale
    return Math.round(percentage * 10) / 10;
  }

  // Méthode utilitaire pour le formatage rapide dans le template
  formatNumber(n: number | undefined | null): string {
    if (n === undefined || n === null) return '0';
    return n.toLocaleString(this.getNumberLocale());
  }

  displayTeamName(rawName: string | undefined | null): string {
    if (!rawName) return '';
    return rawName.replace(/^É?Équipe des\s+/i, '').trim();
  }

  /**
   * Get backend status for UI indicators
   */
  getBackendStatusIcon(): string {
    switch (this.backendStatus) {
      case 'online': return 'cloud_done';
      case 'offline': return 'cloud_off';
      case 'checking': return 'cloud_sync';
      default: return 'cloud_queue';
    }
  }

  /**
   * Get backend status color for indicators
   */
  getBackendStatusColor(): string {
    switch (this.backendStatus) {
      case 'online': return 'var(--gaming-success)';
      case 'offline': return 'var(--gaming-warning)';
      case 'checking': return 'var(--gaming-primary)';
      default: return 'var(--gaming-gray)';
    }
  }

  /**
   * Retry loading data from backend
   */
  retryBackendConnection(): void {
    this.logger.debug('🔄 Retrying backend connection...');
    this.backendStatus = 'checking';
    this.isUsingMockData = false;
    this.loadDashboardData(true);
  }

  // ============== OPTIMISATIONS PERFORMANCE ANGULAR ==============

  /**
   * TrackBy function pour optimiser *ngFor des équipes dans le leaderboard
   * Évite les re-rendus inutiles quand les données changent
   */
  trackByTeamId(index: number, team: LeaderboardEntryDTO): string {
    return team.teamId?.toString() || `team-${index}`;
  }

  /**
   * TrackBy function optimisée pour les joueurs d'une équipe
   */
  trackByPlayerId(index: number, player: any): string {
    return player.playerId?.toString() || player.id?.toString() || `player-${index}`;
  }

  /**
   * TrackBy function pour les stats cards
   */
  trackByStatName(index: number, item: any): string {
    return item.name || `stat-${index}`;
  }

  // ===== PREMIUM METHODS =====

  /**
   * TrackBy function pour les games
   */
  trackByGameId(index: number, game: Game): string {
    return game.id?.toString() || `game-${index}`;
  }

  /**
   * Sélectionne une game
   */
  selectGame(game: Game): void {
    this.selectedGameId = game.id.toString();
    this.selectedGame = game;
    this.interactionsService.showGamingNotification(
      `Game "${game.name}" sélectionnée !`,
      'success'
    );
    this.cdr.detectChanges();
  }



  /**
   * Retourne le statut d'une game
   */
  getGameStatus(game: Game): string {
    // Logic to determine game status
    return 'En cours'; // Placeholder
  }

  /**
   * Calcule les jours restants pour une game
   */
  calculateDaysLeft(game: Game): string {
    // Logic to calculate days left
    return '15'; // Placeholder
  }

  /**
   * Affiche les détails d'une game
   */
  viewGameDetails(game: Game): void {
    this.router.navigate(['/games', game.id]);
  }

  /**
   * Navigate vers une route spécifique
   */
  // Méthode navigateTo supprimée - doublon résolu
} 
