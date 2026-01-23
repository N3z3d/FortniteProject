import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { GameService } from '../services/game.service';
import { UserContextService, UserProfile } from '../../../core/services/user-context.service';
import { Game, GameStatus } from '../models/game.interface';
import { GameSelectionService } from '../../../core/services/game-selection.service';
import { UserGamesStore } from '../../../core/services/user-games.store';
import { TranslationService } from '../../../core/services/translation.service';

@Component({
  selector: 'app-game-home',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './game-home.component.html',
  styleUrls: ['./game-home.component.scss']
})
export class GameHomeComponent implements OnInit, OnDestroy {
  currentUser: UserProfile | null = null;
  userGames: Game[] = [];
  availableGames: Game[] = [];
  loading = false;
  error: string | null = null;
  selectedGame: Game | null = null;
  sidebarOpened = false;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly gameService: GameService,
    private readonly userContextService: UserContextService,
    private readonly gameSelectionService: GameSelectionService,
    private readonly userGamesStore: UserGamesStore,
    public readonly t: TranslationService,
    public readonly router: Router,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadCurrentUser();
    this.subscribeToUserGamesState();
    this.userGamesStore.loadGames().subscribe({ error: () => undefined });
    this.loadAvailableGames();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  createGame(): void {
    this.router.navigate(['/games/create']);
  }

  joinGame(): void {
    // Redirection vers la page dédiée de join game
    this.router.navigate(['/games/join']);
  }

  loadAvailableGames(): void {
    this.gameService.getAvailableGames().subscribe({
      next: (games) => {
        this.availableGames = games;
      },
      error: () => {
        // ignore optional errors for available list
      }
    });
  }

  goToProfile(): void {
    this.router.navigate(['/profile']);
  }

  goToSettings(): void {
    this.router.navigate(['/settings']);
  }

  viewGame(gameId: string): void {
    const selected = this.userGames.find(game => game.id === gameId);
    if (selected) {
      this.gameSelectionService.setSelectedGame(selected);
    }
    this.router.navigate(['/games', gameId]);
  }

  getStatusColor(status: GameStatus): string {
    switch (status) {
      case 'CREATING':
        return 'primary';
      case 'DRAFTING':
        return 'accent';
      case 'ACTIVE':
        return 'primary';
      case 'FINISHED':
        return 'warn';
      case 'CANCELLED':
        return 'warn';
      default:
        return 'primary';
    }
  }

  getStatusLabel(status: GameStatus): string {
    switch (status) {
      case 'CREATING':
        return 'En création';
      case 'DRAFTING':
        return 'Draft en cours';
      case 'ACTIVE':
        return 'Active';
      case 'FINISHED':
        return 'Terminée';
      case 'CANCELLED':
        return 'Annulée';
      default:
        return 'Inconnu';
    }
  }

  hasGames(): boolean {
    return this.userGames.length > 0;
  }

  getGameCount(): number {
    return this.userGames.length;
  }

  trackByGameId(index: number, game: Game): string {
    return game.id;
  }

  reloadUserGames(): void {
    this.userGamesStore.refreshGames().subscribe({ error: () => undefined });
  }

  toggleSidebar(): void {
    this.sidebarOpened = !this.sidebarOpened;
  }

  logout(): void {
    this.userContextService.logout();
    this.router.navigate(['/login']);
  }

  selectGame(game: Game): void {
    this.selectedGame = game;
    this.gameSelectionService.setSelectedGame(game);

    // Fermer la sidebar sur mobile pour une meilleure UX
    if (window.innerWidth <= 768) {
      this.sidebarOpened = false;
    }
  }

  getTotalFortnitePlayers(game: Game): number {
    // Use API value if available, fallback to 147 (7 regions x 21 players)
    return game.fortnitePlayerCount ?? 147;
  }

  getMaxParticipants(): number {
    // Maximum participants per game
    return 8;
  }

  getStatusIcon(status: GameStatus): string {
    switch (status) {
      case 'CREATING':
        return 'construction';
      case 'DRAFTING':
        return 'how_to_vote';
      case 'ACTIVE':
        return 'flash_on';
      case 'FINISHED':
        return 'emoji_events';
      case 'CANCELLED':
        return 'cancel';
      default:
        return 'help_outline';
    }
  }


  private loadCurrentUser(): void {
    this.currentUser = this.userContextService.getCurrentUser();
  }

  private subscribeToUserGamesState(): void {
    this.userGamesStore.state$
      .pipe(takeUntil(this.destroy$))
      .subscribe(state => {
        this.userGames = state.games;
        this.loading = state.loading;
        this.error = state.error;

        if (!state.loading) {
          if (state.games.length === 0) {
            this.selectedGame = null;
            this.gameSelectionService.setSelectedGame(null);
          } else {
            const isSelectedStillAvailable = this.selectedGame
              ? state.games.some(game => game.id === this.selectedGame?.id)
              : false;
            if (!this.selectedGame || !isSelectedStillAvailable) {
              this.selectedGame = state.games[0];
              this.gameSelectionService.setSelectedGame(state.games[0]);
            }
          }
        }

        this.cdr.detectChanges();
      });
  }

} 
