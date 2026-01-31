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
import { DashboardChartCoordinatorService } from './services/dashboard-chart-coordinator.service';
import { DashboardDisplayStats, DashboardStatsDisplayService } from './services/dashboard-stats-display.service';
import { DashboardStatsCalculatorService } from './services/dashboard-stats-calculator.service';
import { DashboardFormattingService } from './services/dashboard-formatting.service';
import { CompetitionStats, DashboardStats } from './models/dashboard.types';
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

  private regionChart: Chart | null = null;
  private pointsChart: Chart | null = null;

  isLoading = false;
  error: string | null = null;

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
    seasonProgress: 0
  };
  displayStats: DashboardDisplayStats = {};

  leaderboardEntries: LeaderboardEntryDTO[] = [];
  selectedGame: Game | null = null;
  games: Game[] = [];
  selectedGameId: string | null = null;

  private subscriptions: Subscription[] = [];
  private readonly REFRESH_INTERVAL = 300000; // 5 minutes
  currentSeason = 2025;

  totalScore = 0;
  ranking = 0;
  activeGames = 0;
  weeklyBest = 0;

  dashboardLeaderboard: any[] = [];

  constructor(
    private http: HttpClient,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar,
    private gameSelectionService: GameSelectionService,
    private dashboardFacade: DashboardFacade,
    private chartCoordinator: DashboardChartCoordinatorService,
    private statsCalculator: DashboardStatsCalculatorService,
    private statsDisplay: DashboardStatsDisplayService,
    public formatter: DashboardFormattingService,
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
        this.refreshDisplayStats();
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
      setTimeout(() => this.refreshCharts(), 50);
    }
  }

  ngOnDestroy() {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.chartCoordinator.destroyCharts(this.regionChart, this.pointsChart);
    this.regionChart = null;
    this.pointsChart = null;
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
      this.accessibilityService.announceLoading(true, this.t.t('navigation.dashboard'));
    }
    this.error = null;

    try {
      // NEW: Using DashboardFacade with automatic DB/Mock fallback
      this.dashboardFacade.getDashboardData(this.selectedGame.id).subscribe({
        next: (data: any) => {
          this.logger.debug('📊 Dashboard data received:', data);

          // Update leaderboard
          if (data.leaderboard && Array.isArray(data.leaderboard)) {
            this.logger.debug('📈 Leaderboard data', { entriesCount: data.leaderboard.length });
            this.leaderboardEntries = data.leaderboard;
            // Populate dashboardLeaderboard for the preview section (top 5)
            this.dashboardLeaderboard = data.leaderboard.slice(0, 5).map((entry: LeaderboardEntryDTO) => ({
              rank: entry.rank,
              name: entry.teamName || entry.ownerName || `Equipe ${entry.rank}`,
              points: entry.totalPoints
            }));
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

          // Update proPlayersCount - only show drafted players for active games
          // For games in CREATING/DRAFTING status, no players are drafted yet → show 0
          const isGameActive = this.selectedGame?.status === 'ACTIVE' || this.selectedGame?.status === 'FINISHED';
          this.stats.proPlayersCount = isGameActive ? (this.stats.totalPlayers || 0) : 0;

          if (showLoading) this.isLoading = false;

          this.logger.debug('📊 Final stats after update:', this.stats);

          // Announce successful data load
          this.accessibilityService.announceLoading(false, this.t.t('navigation.dashboard'));

          const formattedTeams = this.formatter.formatNumber(this.stats.totalTeams);
          const formattedPlayers = this.formatter.formatNumber(this.stats.totalPlayers);
          this.updateLiveRegion(
            `${this.t.t('dashboard.live.updatedPrefix')} ${formattedTeams} ${this.t.t('dashboard.live.teams')} ${this.t.t('common.and')} ${formattedPlayers} ${this.t.t('dashboard.live.players')}`,
            'polite'
          );

          // Trigger change detection
          this.cdr.markForCheck();

          // Update charts
          setTimeout(() => this.refreshCharts(), 100);
        },
        error: (error) => {
          this.logger.warn('Dashboard: backend offline, switching to mock data', error);

          // Fallback to mock data on error so UI is visible
          this.setupMockData();

          this.error = null; // Clear error to show content

          if (showLoading) {
            this.isLoading = false;
          }

          // Trigger change detection and charts
          this.cdr.markForCheck();
          setTimeout(() => this.refreshCharts(), 100);

          this.snackBar.open(
            this.t.t('dashboard.messages.demoMode'),
            this.t.t('common.close'),
            { duration: 3000 }
          );
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
    this.stats.seasonProgress = this.statsCalculator.calculateSeasonProgress();
    this.stats.proPlayersCount = 0;

    // Empty region data when no backend
    this.stats.teamComposition = {
      regions: {},
      tranches: {}
    };

    // Empty leaderboard when backend offline
    this.dashboardLeaderboard = [];
    this.leaderboardEntries = [];
    this.refreshDisplayStats();
  }

  private updateStats(newStats: Partial<DashboardStats>) {
    const result = this.statsCalculator.applyStatsUpdate(this.stats, newStats);
    this.stats = result.stats;
    this.competitionStats = result.competitionStats;
    this.refreshDisplayStats();
  }

  private updateStatsFromLeaderboard(entries: LeaderboardEntryDTO[]) {
    const result = this.statsCalculator.buildStatsFromLeaderboard(entries, this.games.length);
    if (!result) {
      this.logger.debug('No leaderboard entries to process');
      return;
    }

    this.updateStats(result.stats);
    this.totalScore = result.premium.totalScore;
    this.ranking = result.premium.ranking;
    this.activeGames = result.premium.activeGames;
    this.weeklyBest = result.premium.weeklyBest;
  }

  private updateCompetitionStats(apiStats: any) {
    const result = this.statsCalculator.applyCompetitionStats(this.stats, apiStats);
    this.stats = result.stats;
    this.competitionStats = result.competitionStats;
    this.refreshDisplayStats();
  }

  private refreshDisplayStats(): void {
    this.displayStats = this.statsDisplay.formatStatsForDisplay({
      totalTeams: this.stats.totalTeams,
      totalPlayers: this.stats.totalPlayers,
      totalPoints: this.stats.totalPoints,
      averagePointsPerTeam: this.stats.averagePointsPerTeam,
      seasonProgress: this.stats.seasonProgress
    });
  }

  // Méthodes utilitaires

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

    // Split route into segments for proper Angular navigation
    // e.g., '/games/123/trades' -> ['/', 'games', '123', 'trades']
    const segments = route.split('/').filter(segment => segment.length > 0);
    this.router.navigate(['/', ...segments]);
  }

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

  private refreshCharts(): void {
    const regionCanvas = this.regionChartRef?.nativeElement || null;
    const pointsCanvas = this.pointsChartRef?.nativeElement || null;
    const result = this.chartCoordinator.updateCharts(
      this.regionChart,
      this.pointsChart,
      regionCanvas,
      pointsCanvas,
      this.stats.teamComposition?.regions,
      this.leaderboardEntries,
      entry => this.getLeaderboardLabel(entry)
    );

    this.regionChart = result.regionChart;
    this.pointsChart = result.pointsChart;
  }

  private getLeaderboardLabel(entry: LeaderboardEntryDTO): string {
    return this.formatter.displayTeamName(entry.teamName) ||
      entry.ownerName ||
      `${this.t.t('dashboard.labels.team')} ${entry.rank}`;
  }

  /**
   * Retourne le statut d'une game
   */
  getGameStatus(game: Game): string {
    switch (game.status) {
      case 'CREATING':
        return this.t.t('games.status.creating');
      case 'DRAFTING':
        return this.t.t('games.status.drafting');
      case 'ACTIVE':
        return this.t.t('games.status.active');
      case 'FINISHED':
        return this.t.t('games.status.finished');
      case 'CANCELLED':
        return this.t.t('games.status.cancelled');
      case 'COMPLETED':
        return this.t.t('games.status.completed');
      case 'DRAFT':
        return this.t.t('games.status.draft');
      case 'RECRUITING':
        return this.t.t('games.status.recruiting');
      default:
        return this.t.t('games.status.unknown');
    }
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
} 
