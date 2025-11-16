import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { Clipboard } from '@angular/cdk/clipboard';

import { GameService } from '../services/game.service';
import { Game, GameStatus } from '../models/game.interface';
import { AccessibilityAnnouncerService } from '../../../shared/services/accessibility-announcer.service';
import { FocusManagementService } from '../../../shared/services/focus-management.service';

@Component({
  selector: 'app-game-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule,
    MatTooltipModule,
    MatMenuModule,
    MatDividerModule
  ],
  templateUrl: './game-list.component.html',
  styleUrls: ['./game-list.component.scss']
})
export class GameListComponent implements OnInit, OnDestroy {
  games: Game[] = [];
  loading = false;
  error: string | null = null;
  currentUserName = 'Current User'; // TODO: Get from user service

  constructor(
    private gameService: GameService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private router: Router,
    private accessibilityAnnouncer: AccessibilityAnnouncerService,
    private focusManagementService: FocusManagementService,
    private clipboard: Clipboard
  ) { }

  ngOnInit(): void {
    this.accessibilityAnnouncer.announceNavigation('liste des games');
    this.loadGames();
  }

  ngOnDestroy(): void {
    // Nettoyer les ressources si nécessaire
  }

  loadGames(): void {
    this.loading = true;
    this.error = null;
    this.accessibilityAnnouncer.announceLoading(true, 'games');

    // TDD: Utiliser getAllGames pour voir toutes les games de test
    this.gameService.getAllGames().subscribe({
      next: (games) => {
        this.games = games;
        this.loading = false;
        this.accessibilityAnnouncer.announceLoading(false, `${games.length} jeux`);
        console.log('Games loaded:', games);
      },
      error: (error) => {
        this.error = 'Erreur lors du chargement des games';
        this.loading = false;
        this.accessibilityAnnouncer.announceError('Impossible de charger les games');
        console.error('Error loading games:', error);
      }
    });
  }


  joinGame(gameId: string): void {
    const game = this.games.find(g => g.id === gameId);
    const gameName = game?.name || 'cette game';
    
    this.gameService.joinGame(gameId).subscribe({
      next: (success) => {
        if (success) {
          this.snackBar.open('Game rejoint avec succès!', 'Fermer', { duration: 3000 });
          this.accessibilityAnnouncer.announceSuccess(`Vous avez rejoint la game ${gameName}`);
          this.loadGames(); // Recharger la liste
        } else {
          this.snackBar.open('Impossible de rejoindre la game', 'Fermer', { duration: 3000 });
          this.accessibilityAnnouncer.announceError(`Impossible de rejoindre la game ${gameName}`);
        }
      },
      error: (error) => {
        this.snackBar.open('Erreur lors de la tentative de rejoindre la game', 'Fermer', { duration: 3000 });
        this.accessibilityAnnouncer.announceError(`Erreur lors de la tentative de rejoindre la game ${gameName}`);
        console.error('Error joining game:', error);
      }
    });
  }

  deleteGame(gameId: string): void {
    const game = this.games.find(g => g.id === gameId);
    const gameName = game?.name || 'cette game';
    
    if (confirm(`Êtes-vous sûr de vouloir supprimer la game "${gameName}" ?`)) {
      this.gameService.deleteGame(gameId).subscribe({
        next: (success) => {
          if (success) {
            this.snackBar.open('Game supprimée avec succès!', 'Fermer', { duration: 3000 });
            this.accessibilityAnnouncer.announceSuccess(`La game ${gameName} a été supprimée`);
            this.loadGames(); // Recharger la liste
          } else {
            this.snackBar.open('Impossible de supprimer la game', 'Fermer', { duration: 3000 });
            this.accessibilityAnnouncer.announceError(`Impossible de supprimer la game ${gameName}`);
          }
        },
        error: (error) => {
          this.snackBar.open('Erreur lors de la suppression de la game', 'Fermer', { duration: 3000 });
          this.accessibilityAnnouncer.announceError(`Erreur lors de la suppression de la game ${gameName}`);
          console.error('Error deleting game:', error);
        }
      });
    }
  }

  startDraft(gameId: string): void {
    const game = this.games.find(g => g.id === gameId);
    const gameName = game?.name || 'cette game';
    
    this.gameService.startDraft(gameId).subscribe({
      next: (success) => {
        if (success) {
          this.snackBar.open('Draft démarré avec succès!', 'Fermer', { duration: 3000 });
          this.accessibilityAnnouncer.announceGameStatusChange('draft démarré', `pour la game ${gameName}`);
          this.loadGames(); // Recharger la liste
        } else {
          this.snackBar.open('Impossible de démarrer le draft', 'Fermer', { duration: 3000 });
          this.accessibilityAnnouncer.announceError(`Impossible de démarrer le draft pour la game ${gameName}`);
        }
      },
      error: (error) => {
        this.snackBar.open('Erreur lors du démarrage du draft', 'Fermer', { duration: 3000 });
        this.accessibilityAnnouncer.announceError(`Erreur lors du démarrage du draft pour la game ${gameName}`);
        console.error('Error starting draft:', error);
      }
    });
  }

  getStatusColor(status: GameStatus): string {
    switch (status) {
      case 'CREATING':
        return 'primary';
      case 'DRAFTING':
        return 'accent';
      case 'ACTIVE':
        return 'warn';
      case 'FINISHED':
        return 'default';
      case 'CANCELLED':
        return 'default';
      default:
        return 'default';
    }
  }

  getStatusLabel(status: GameStatus): string {
    switch (status) {
      case 'CREATING':
        return 'En création';
      case 'DRAFTING':
        return 'En draft';
      case 'ACTIVE':
        return 'Active';
      case 'FINISHED':
        return 'Terminée';
      case 'CANCELLED':
        return 'Annulée';
      default:
        return status;
    }
  }

  canStartDraft(game: Game): boolean {
    return game.status === 'CREATING' && game.participantCount >= 2;
  }

  canJoinGame(game: Game): boolean {
    return game.canJoin && game.participantCount < game.maxParticipants;
  }

  canDeleteGame(game: Game): boolean {
    return game.status === 'CREATING' || game.status === 'CANCELLED';
  }

  /**
   * TrackBy function pour améliorer les performances des *ngFor
   */
  trackByGameId(index: number, game: Game): string {
    return game.id;
  }

  /**
   * Sélectionne une game (pour navigation au clavier)
   */
  selectGame(game: Game): void {
    this.router.navigate(['/games', game.id]);
    this.accessibilityAnnouncer.announceNavigation(`détails de la game ${game.name}`);
  }

  /**
   * Gère la navigation clavier sur les cartes de games
   */
  onGameCardKeydown(event: KeyboardEvent, game: Game): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.selectGame(game);
    }
  }

  /**
   * Retourne l'icône appropriée pour le statut de la game
   */
  getStatusIcon(status: GameStatus): string {
    switch (status) {
      case 'CREATING':
        return 'hourglass_empty';
      case 'DRAFTING':
        return 'edit';
      case 'ACTIVE':
        return 'play_circle';
      case 'FINISHED':
        return 'check_circle';
      case 'CANCELLED':
        return 'cancel';
      default:
        return 'help_outline';
    }
  }

  /**
   * Copie le code d'invitation dans le presse-papiers
   */
  copyInvitationCode(code: string): void {
    if (this.clipboard.copy(code)) {
      this.snackBar.open('Code d\'invitation copié !', 'Fermer', { duration: 2000 });
      this.accessibilityAnnouncer.announceSuccess('Code d\'invitation copié dans le presse-papiers');
    } else {
      this.snackBar.open('Impossible de copier le code', 'Fermer', { duration: 2000 });
    }
  }

  /**
   * Partage une game
   */
  shareGame(game: Game): void {
    const shareData = {
      title: `Game ${game.name}`,
      text: `Rejoignez la game ${game.name} créée par ${game.creatorName}`,
      url: `${window.location.origin}/games/${game.id}`
    };

    if (navigator.share) {
      navigator.share(shareData).catch(() => {
        this.copyGameUrl(game);
      });
    } else {
      this.copyGameUrl(game);
    }
  }

  /**
   * Copie l'URL de la game
   */
  private copyGameUrl(game: Game): void {
    const url = `${window.location.origin}/games/${game.id}`;
    if (this.clipboard.copy(url)) {
      this.snackBar.open('Lien de la game copié !', 'Fermer', { duration: 2000 });
    }
  }

  /**
   * Exporte les données de la game
   */
  exportGame(game: Game): void {
    const gameData = JSON.stringify(game, null, 2);
    const blob = new Blob([gameData], { type: 'application/json' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `game-${game.name}-${game.id}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
    
    this.snackBar.open('Game exportée !', 'Fermer', { duration: 2000 });
  }

  /**
   * Signale une game
   */
  reportGame(game: Game): void {
    // TODO: Implement game reporting
    this.snackBar.open('Fonctionnalité de signalement à venir', 'Fermer', { duration: 2000 });
  }
} 