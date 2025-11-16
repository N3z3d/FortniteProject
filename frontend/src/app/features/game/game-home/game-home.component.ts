import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';

import { GameService } from '../services/game.service';
import { UserContextService, UserProfile } from '../../../core/services/user-context.service';
import { Game, GameStatus } from '../models/game.interface';

@Component({
  selector: 'app-game-home',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule,
    MatTooltipModule
  ],
  templateUrl: './game-home.component.html',
  styleUrls: ['./game-home.component.scss']
})
export class GameHomeComponent implements OnInit {
  currentUser: UserProfile | null = null;
  userGames: Game[] = [];
  loading = false;
  error: string | null = null;
  selectedGame: Game | null = null;
  sidebarOpened = false;
  showJoinCodeInput = false;
  invitationCode = '';
  joiningGame = false;

  constructor(
    private gameService: GameService,
    private userContextService: UserContextService,
    public router: Router,
    private cdr: ChangeDetectorRef,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadCurrentUser();
    this.loadUserGames();
  }

  createGame(): void {
    this.router.navigate(['/games/create']);
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

  goToProfile(): void {
    this.router.navigate(['/profile']);
  }

  goToSettings(): void {
    this.router.navigate(['/settings']);
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

} 