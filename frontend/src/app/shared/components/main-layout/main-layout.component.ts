import { Component, OnInit, OnDestroy, ViewChild, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterOutlet, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
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
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { GameService } from '../../../features/game/services/game.service';
import { UserContextService, UserProfile } from '../../../core/services/user-context.service';
import { GameSelectionService } from '../../../core/services/game-selection.service';
import { Game, GameStatus } from '../../../features/game/models/game.interface';
import { AccessibilityAnnouncerService } from '../../services/accessibility-announcer.service';
import { FocusManagementService } from '../../services/focus-management.service';

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
    MatSnackBarModule
  ],
  templateUrl: './main-layout.component.html',
  styleUrls: ['./main-layout.component.scss']
})
export class MainLayoutComponent implements OnInit, OnDestroy {
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
  
  private destroy$ = new Subject<void>();

  constructor(
    private gameService: GameService,
    private userContextService: UserContextService,
    private gameSelectionService: GameSelectionService,
    private router: Router,
    private breakpointObserver: BreakpointObserver,
    private accessibilityService: AccessibilityAnnouncerService,
    private focusManagementService: FocusManagementService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadCurrentUser();
    this.setupResponsive();
    this.subscribeToGameSelection();
    
    // Charger les games après un petit délai pour s'assurer que l'utilisateur est connecté
    setTimeout(() => {
      this.loadUserGames();
    }, 100);
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
        this.loadUserGames(); // Refresh the games list
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
    if (this.gamesSidenav) {
      this.gamesSidenav.toggle();
    }
  }

  // User actions
  switchProfile(): void {
    // Direct logout and redirect to login for user switching
    this.userContextService.logout();
    this.userContextService.disableAutoLogin(); // Disable auto-login when switching
    this.router.navigate(['/login'], { 
      queryParams: { switchUser: 'true' } 
    });
  }

  logout(): void {
    this.userContextService.logout();
    this.router.navigate(['/login']);
  }

  // Permissions
  canManageDraft(): boolean {
    // Le draft n'est visible que pendant la phase de draft
    return this.selectedGame?.status === 'DRAFTING';
  }

  // Status helpers
  getStatusColor(status: GameStatus): string {
    switch (status) {
      case 'CREATING': return 'warn';
      case 'DRAFTING': return 'accent';
      case 'ACTIVE': return 'primary';
      case 'COMPLETED': return 'warn';
      default: return 'primary';
    }
  }

  getStatusLabel(status: GameStatus): string {
    switch (status) {
      case 'CREATING': return 'Création';
      case 'DRAFTING': return 'Draft';
      case 'ACTIVE': return 'Active';
      case 'COMPLETED': return 'Terminée';
      default: return status;
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
    this.loadUserGames();
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

  private loadCurrentUser(): void {
    this.currentUser = this.userContextService.getCurrentUser();
  }

  private loadUserGames(): void {
    this.loading = true;
    this.error = null;

    this.gameService.getUserGames().subscribe({
      next: (games) => {
        this.userGames = games;
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des games:', error);
        this.error = 'Erreur lors du chargement de vos games';
        this.loading = false;
      }
    });
  }
} 