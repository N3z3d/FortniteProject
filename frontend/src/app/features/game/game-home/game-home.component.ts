import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { GameService } from '../services/game.service';
import { UserContextService, UserProfile } from '../../../core/services/user-context.service';
import { Game, GameStatus } from '../models/game.interface';

@Component({
  selector: 'app-game-home',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './game-home.component.html',
  styleUrls: ['./game-home.component.css']
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
    private gameService: GameService,
    private userContextService: UserContextService,
    public router: Router,
    private cdr: ChangeDetectorRef
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
    this.router.navigate(['/games/join']);
  }

  viewGame(gameId: string): void {
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
  }

  getTotalFortnitePlayers(): number {
    // Default value for Fortnite players across all games
    return 147;
  }

  joinSpecificGame(game: Game): void {
    // Navigate to join game with pre-selected game
    this.router.navigate(['/games/join'], { queryParams: { gameId: game.id } });
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
        
        // Sélectionner automatiquement la première game si disponible
        if (games.length > 0 && !this.selectedGame) {
          this.selectedGame = games[0];
        }
        
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Erreur chargement games:', error);
        this.error = 'Erreur lors du chargement de vos games';
        this.loading = false;
      }
    });
  }

  private loadAvailableGames(): void {
    this.gameService.getAvailableGames().subscribe({
      next: (games) => {
        this.availableGames = games;
      },
      error: (error) => {
        console.error('Erreur chargement games disponibles:', error);
      }
    });
  }
} 