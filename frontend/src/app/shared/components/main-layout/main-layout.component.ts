import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterOutlet, RouterModule, ChildrenOutletContexts } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { fadeSlideAnimation } from '../../animations/route.animations';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSidenavModule, MatSidenav } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatBadgeModule } from '@angular/material/badge';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { GameService } from '../../../features/game/services/game.service';
import { UserContextService, UserProfile } from '../../../core/services/user-context.service';
import { GameSelectionService } from '../../../core/services/game-selection.service';
import { LoggerService } from '../../../core/services/logger.service';
import { Game, GameStatus } from '../../../features/game/models/game.interface';
import { AccessibilityAnnouncerService } from '../../services/accessibility-announcer.service';
import { FocusManagementService } from '../../services/focus-management.service';
import { DataSourceIndicator } from '../data-source-indicator/data-source-indicator';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatSidenavModule,
    MatListModule,
    MatDividerModule,
    MatToolbarModule,
    MatMenuModule,
    MatTooltipModule,
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule,
    MatBadgeModule,
    DataSourceIndicator
  ],
  templateUrl: './main-layout.component.html',
  styleUrls: ['./main-layout.component.scss'],
  animations: [fadeSlideAnimation]
})
export class MainLayoutComponent implements OnInit, OnDestroy {
  private readonly contexts: ChildrenOutletContexts;
  @ViewChild('gamesSidenav') gamesSidenav!: MatSidenav;

  currentUser: UserProfile | null = null;
  userGames: Game[] = [];
  showJoinCodeInput = false;
  invitationCode = '';
  joiningGame = false;

  // Méthode pour vérifier si une route est active
  isRouteActive(routes: string[]): boolean {
    return routes.some(route => this.router.url.includes(route));
  }
  loading = false;
  error: string | null = null;
  selectedGame: Game | null = null;
  isMobile = false;
  sidebarCollapsed = false;
  sidebarOpen = true; // Sidebar ouverte par défaut

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly gameService: GameService,
    private readonly userContextService: UserContextService,
    private readonly logger: LoggerService,
    private readonly gameSelectionService: GameSelectionService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly breakpointObserver: BreakpointObserver,
    private readonly accessibilityService: AccessibilityAnnouncerService,
    private readonly focusManagementService: FocusManagementService,
    private readonly snackBar: MatSnackBar,
    contexts: ChildrenOutletContexts
  ) {
    this.contexts = contexts;
    // Restaurer l'état de la sidebar depuis localStorage
    const savedState = localStorage.getItem('sidebarOpen');
    if (savedState !== null) {
      this.sidebarOpen = savedState === 'true';
    }
  }

  // Méthode pour les animations de route
  prepareRoute(outlet: RouterOutlet): string {
    return outlet?.activatedRouteData?.['animation'] ?? '*';
  }

  ngOnInit(): void {
    this.loadCurrentUser();
    this.setupResponsive();
    this.subscribeToGameSelection();
    this.subscribeToRouteChanges();

    // Charger les games après un petit délai pour s'assurer que l'utilisateur est connecté
    this.loadUserGamesFromResolver();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // Navigation methods
  createGame(): void {
    this.router.navigate(['/games/create']);
  }

  quickCreateGame(): void {
    // Création rapide avec configuration par défaut
    this.router.navigate(['/games/create'], { queryParams: { quick: true } });
  }

  openJoinDialog(): void {
    this.showJoinCodeInput = true;
  }

  joinWithCode(): void {
    if (!this.invitationCode.trim()) {
      this.snackBar.open('Please enter an invitation code', 'Close', { duration: 3000 });
      return;
    }

    this.joiningGame = true;
    this.gameService.joinGameWithCode(this.invitationCode.trim()).subscribe({
      next: (game) => {
        this.snackBar.open(`Successfully joined ${game.name}!`, 'View', {
          duration: 5000
        }).onAction().subscribe(() => {
          this.router.navigate(['/games', game.id]);
        });
        this.invitationCode = '';
        this.showJoinCodeInput = false;
        this.joiningGame = false;
        this.reloadUserGames(); // Refresh the games list
      },
      error: (error) => {
        this.snackBar.open(
          error.error?.message || 'Invalid invitation code',
          'Close',
          { duration: 5000 }
        );
        this.joiningGame = false;
      }
    });
  }

  cancelJoin(): void {
    this.showJoinCodeInput = false;
    this.invitationCode = '';
  }

  goToHome(): void {
    this.accessibilityService.announceNavigation('Home dashboard');
    // Désélectionner la partie pour revenir à la vue d'ensemble
    this.gameSelectionService.setSelectedGame(null);
    this.selectedGame = null;
    this.router.navigate(['/games']);
  }

  // Accessibility-enhanced navigation methods
  onNavigationKeyDown(event: KeyboardEvent, route: string, label: string): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.navigateWithAnnouncement(route, label);
    }
  }

  private navigateWithAnnouncement(route: string, label: string): void {
    this.accessibilityService.announceNavigation(label);
    this.router.navigate([route]);
  }

  // Game selection
  selectGame(game: Game): void {
    this.selectedGame = game;
    this.gameSelectionService.setSelectedGame(game);
    
    // Fermer la sidebar sur mobile après sélection
    if (this.isMobile && this.gamesSidenav) {
      this.gamesSidenav.close();
    }
    
    // Navigation contextuelle
    if (game.status === 'DRAFTING') {
      this.router.navigate(['/games', game.id, 'draft']);
    } else {
      this.router.navigate(['/games', game.id]);
    }
  }

  // Responsive & UI controls
  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
    // Sauvegarder l'état dans localStorage
    localStorage.setItem('sidebarOpen', this.sidebarOpen.toString());

    if (this.gamesSidenav) {
      this.gamesSidenav.toggle();
    }
  }

  // User actions
  switchProfile(): void {
    // Direct logout and redirect to login for user switching
    this.userContextService.logout();
    this.router.navigate(['/login'], {
      queryParams: { switchUser: 'true' }
    });
  }

  logout(): void {
    try {
      this.userContextService.logout();
    } catch (error) {
      this.logger.error('MainLayout: Logout failed', error);
    }

    this.clearLocalSession();
    this.router.navigate(['/login']);
  }

  private clearLocalSession(): void {
    this.currentUser = null;
    this.userGames = [];
    this.selectedGame = null;
    this.loading = false;
    this.error = null;
    this.gameSelectionService.setSelectedGame(null);
  }

  // Permissions
  canManageDraft(): boolean {
    // Le draft n'est visible que pendant la phase de draft
    return this.selectedGame?.status === 'DRAFTING';
  }

  // Status helpers
  getStatusColor(status: GameStatus): string {
    switch (status) {
      case 'CREATING': return 'primary';
      case 'DRAFTING': return 'accent';
      case 'ACTIVE': return 'primary';
      case 'FINISHED':
      case 'CANCELLED':
      case 'COMPLETED':
        return 'warn';
      default: return 'primary';
    }
  }

  getStatusLabel(status: GameStatus): string {
    switch (status) {
      case 'CREATING': return 'En création';
      case 'DRAFTING': return 'Draft en cours';
      case 'ACTIVE': return 'Active';
      case 'FINISHED':
      case 'COMPLETED':
        return 'Terminée';
      case 'CANCELLED':
        return 'Annulée';
      default:
        return status;
    }
  }

  getStatusIcon(status: GameStatus): string {
    switch (status) {
      case 'CREATING': return 'build';
      case 'DRAFTING': return 'how_to_vote';
      case 'ACTIVE': return 'play_arrow';
      case 'COMPLETED': return 'emoji_events';
      default: return 'sports_esports';
    }
  }

  // === MÉTHODES POUR L'INTERFACE SIMPLIFIÉE ===
  
  getCurrentUserInitials(): string {
    if (!this.currentUser?.username) return 'U';
    return this.currentUser.username.substring(0, 2).toUpperCase();
  }

  showAllGames(): void {
    this.router.navigate(['/games']);
  }

  // Méthodes hasGames et reloadUserGames ajoutées pour les tests
  hasGames(): boolean {
    return this.userGames && this.userGames.length > 0;
  }

  reloadUserGames(): void {
    this.reloadUserGamesFromApi();
  }

  getGameCount(): number {
    return this.userGames.length;
  }

  // Utility methods
  trackByGameId(index: number, game: Game): string {
    return game.id;
  }

  // Private methods
  private setupResponsive(): void {
    this.breakpointObserver
      .observe([Breakpoints.Handset])
      .pipe(takeUntil(this.destroy$))
      .subscribe(result => {
        this.isMobile = result.matches;
      });
  }

  private subscribeToGameSelection(): void {
    this.gameSelectionService.selectedGame$
      .pipe(takeUntil(this.destroy$))
      .subscribe(game => {
        this.selectedGame = game;
      });
  }

  private subscribeToRouteChanges(): void {
    this.router.events
      .pipe(takeUntil(this.destroy$))
      .subscribe(event => {
        if (event.constructor.name === 'NavigationEnd') {
          // Deselectionner la game si on navigue vers /games (liste)
          if (this.router.url === '/games' || this.router.url === '/') {
            this.selectedGame = null;
            this.gameSelectionService.setSelectedGame(null);
          } else {
            // Restaurer selectedGame depuis l'URL si on est sur une page de game
            this.restoreSelectedGameFromUrl();
          }
        }
      });
  }

  /**
   * Restaure la game selectionnee depuis l'URL
   * Permet d'afficher la navigation meme apres un refresh de page
   */
  private restoreSelectedGameFromUrl(): void {
    const urlMatch = this.router.url.match(/\/games\/([a-zA-Z0-9-]+)/);
    if (urlMatch && urlMatch[1] && urlMatch[1] !== 'create') {
      const gameId = urlMatch[1];

      // Si on a deja la bonne game selectionnee, ne rien faire
      if (this.selectedGame?.id === gameId) {
        return;
      }

      // Chercher la game dans la liste chargee
      const foundGame = this.userGames.find(g => g.id === gameId);
      if (foundGame) {
        this.selectedGame = foundGame;
        this.gameSelectionService.setSelectedGame(foundGame);
        this.logger.debug('MainLayout: Restored selected game from URL', { gameId, gameName: foundGame.name });
      } else if (this.userGames.length === 0 && !this.loading) {
        // Si les games ne sont pas encore chargees, attendre
        this.logger.debug('MainLayout: Games not loaded yet, will restore after load', { gameId });
      }
    }
  }

  private loadCurrentUser(): void {
    this.currentUser = this.userContextService.getCurrentUser();
  }

  private loadUserGamesFromResolver(): void {
    const resolvedGames = (this.route.snapshot.data['userGames'] as Game[] | undefined) ?? [];
    this.applyUserGames(resolvedGames);
    this.logger.info('MainLayout: Games loaded from resolver', { count: resolvedGames.length });

    this.route.data
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => {
        const games = (data['userGames'] as Game[] | undefined) ?? [];
        this.applyUserGames(games);
        this.logger.info('MainLayout: Games loaded from resolver', { count: games.length });
      });
  }

  private reloadUserGamesFromApi(): void {
    this.loading = true;
    this.error = null;

    this.gameService.getUserGames()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (games) => {
          this.applyUserGames(games);
          this.logger.info('MainLayout: Games reloaded from API', { count: games.length });
        },
        error: (error) => {
          this.logger.error('MainLayout: Failed to reload games', error);
          this.error = 'Erreur lors du chargement de vos games';
          this.loading = false;
        }
      });
  }

  private applyUserGames(games: Game[]): void {
    this.userGames = games;
    this.loading = false;
    this.error = null;
    this.restoreSelectedGameFromUrl();
  }

  // Compatibilité tests : action rapide pour rejoindre une game
  public joinGame(): void {
    this.router.navigate(['/games/join']);
  }

  // Toggle sidebar collapse
  toggleSidebarCollapse(): void {
    this.sidebarCollapsed = !this.sidebarCollapsed;
  }
}
