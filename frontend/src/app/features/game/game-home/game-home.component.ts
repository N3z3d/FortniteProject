import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';

import { GameService } from '../services/game.service';
import { UserContextService, UserProfile } from '../../../core/services/user-context.service';
import { Game, GameStatus } from '../models/game.interface';
import { GameSelectionService } from '../../../core/services/game-selection.service';

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
export class GameHomeComponent implements OnInit {
  currentUser: UserProfile | null = null;
  userGames: Game[] = [];
  availableGames: Game[] = [];
  loading = false;
  error: string | null = null;
  selectedGame: Game | null = null;
  sidebarOpened = false;

  constructor(
    private readonly gameService: GameService,
    private readonly userContextService: UserContextService,
    private readonly gameSelectionService: GameSelectionService,
    public readonly router: Router,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadCurrentUser();
    this.loadUserGames();
    this.loadAvailableGames();
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
    this.loadUserGames();
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
    // Return actual Fortnite player count from the game object
    // Fallback to 0 if not available
    return (game as any).fortnitePlayerCount || 0;
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

  private loadUserGames(): void {
    this.loading = true;
    this.error = null;

    this.gameService.getUserGames().subscribe({
      next: (games) => {
        this.userGames = games;

        if (games.length === 0) {
          this.selectedGame = null;
          this.gameSelectionService.setSelectedGame(null);
        } else {
          const isSelectedStillAvailable = this.selectedGame
            ? games.some(game => game.id === this.selectedGame?.id)
            : false;

          // Sélectionner automatiquement la première game si nécessaire
          if (!this.selectedGame || !isSelectedStillAvailable) {
            this.selectedGame = games[0];
            this.gameSelectionService.setSelectedGame(games[0]);
          }
        }
        
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Erreur chargement games:', error);
        this.error = error instanceof Error ? error.message : 'Erreur lors du chargement de vos games';
        this.loading = false;
      }
    });
  }

} 
