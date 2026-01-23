import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';
import { MatBadgeModule } from '@angular/material/badge';

import { GameService } from '../services/game.service';
import { GameDataService } from '../services/game-data.service';
import { Game, GameStatus, GameParticipant } from '../models/game.interface';
import { GameApiMapper } from '../mappers/game-api.mapper';
import { UserContextService } from '../../../core/services/user-context.service';
import { UserGamesStore } from '../../../core/services/user-games.store';

@Component({
  selector: 'app-game-detail',
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
    MatTooltipModule,
    MatProgressBarModule,
    MatListModule,
    MatDividerModule,
    MatBadgeModule
  ],
  templateUrl: './game-detail.component.html',
  styleUrls: ['./game-detail.component.css']
})
export class GameDetailComponent implements OnInit {
  game: Game | null = null;
  participants: GameParticipant[] = [];
  loading = false;
  error: string | null = null;
  participantsError: string | null = null;
  gameId: string = '';

  constructor(
    private readonly gameService: GameService,
    private readonly gameDataService: GameDataService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly snackBar: MatSnackBar,
    private readonly userContextService: UserContextService,
    private readonly userGamesStore: UserGamesStore
  ) { }

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.gameId = params['id'];
      if (this.gameId) {
        this.loadGameDetails();
      }
    });
  }

  loadGameDetails(): void {
    this.loading = true;
    this.error = null;
    this.participantsError = null;

    this.gameDataService.getGameById(this.gameId).subscribe({
      next: (game: Game) => {
        this.game = game;
        this.loading = false;
        
        // Validation des données reçues
        const validation = this.gameDataService.validateGameData(game);
        if (!validation.isValid) {
          console.warn('Game data validation failed:', validation.errors);
        }

        this.loadParticipants();
      },
      error: (error: Error) => {
        this.error = error.message;
        this.loading = false;
        console.error('Error loading game details:', error);
        this.snackBar.open(
          error.message || 'Erreur lors du chargement de la game',
          'Fermer',
          { duration: 4000 }
        );
      }
    });
  }

  loadParticipants(): void {
    this.participantsError = null;
    this.gameDataService.getGameParticipants(this.gameId).subscribe({
      next: (participants: GameParticipant[]) => {
        this.participants = participants;
        this.participantsError = null;
      },
      error: (error: Error) => {
        const message = error.message || 'Erreur lors du chargement des participants';
        this.participantsError = message;
        console.error('Error loading participants:', error);
      }
    });
  }

  retryLoad(): void {
    this.error = null;
    this.participantsError = null;
    this.loadGameDetails();
  }

  startDraft(): void {
    this.gameService.startDraft(this.gameId).subscribe({
      next: (success) => {
        if (success) {
          this.snackBar.open('Draft démarré avec succès!', 'Fermer', { duration: 3000 });
          this.loadGameDetails(); // Recharger les détails
        } else {
          this.snackBar.open('Impossible de démarrer le draft', 'Fermer', { duration: 3000 });
        }
      },
      error: (error) => {
        this.snackBar.open('Erreur lors du démarrage du draft', 'Fermer', { duration: 3000 });
        console.error('Error starting draft:', error);
      }
    });
  }

  /**
   * Archive la game (soft delete pour l'host)
   */
  archiveGame(): void {
    if (!this.game) return;

    this.gameService.archiveGame(this.gameId).subscribe({
      next: (success) => {
        if (success) {
          this.snackBar.open('Game archivée avec succès!', 'Fermer', { duration: 3000 });
          this.router.navigate(['/games']);
        } else {
          this.snackBar.open('Impossible d\'archiver la game', 'Fermer', { duration: 3000 });
        }
      },
      error: (error) => {
        this.snackBar.open('Erreur lors de l\'archivage de la game', 'Fermer', { duration: 3000 });
        console.error('Error archiving game:', error);
      }
    });
  }

  /**
   * Quitter la game (pour les participants non-host)
   */
  leaveGame(): void {
    if (!this.game) return;

    this.gameService.leaveGame(this.gameId).subscribe({
      next: (success) => {
        if (success) {
          this.snackBar.open('Vous avez quitté la game', 'Fermer', { duration: 3000 });
          this.router.navigate(['/games']);
        } else {
          this.snackBar.open('Impossible de quitter la game', 'Fermer', { duration: 3000 });
        }
      },
      error: (error) => {
        this.snackBar.open('Erreur lors de la sortie de la game', 'Fermer', { duration: 3000 });
        console.error('Error leaving game:', error);
      }
    });
  }

  /**
   * Confirme l'archivage de la game
   */
  confirmArchive(): void {
    if (!this.game) return;

    const confirmed = confirm(
      'Êtes-vous sûr de vouloir archiver cette game ? Elle ne sera plus visible dans votre liste.'
    );

    if (confirmed) {
      this.archiveGame();
    }
  }

  /**
   * Confirme la sortie de la game
   */
  confirmLeave(): void {
    if (!this.game) return;

    const confirmed = confirm(
      'Êtes-vous sûr de vouloir quitter cette game ?'
    );

    if (confirmed) {
      this.leaveGame();
    }
  }

  /**
   * Supprime définitivement une partie (uniquement en status CREATING)
   */
  permanentlyDeleteGame(): void {
    if (!this.game) return;

    this.gameService.deleteGame(this.gameId).subscribe({
      next: (success) => {
        if (success) {
          // Rafraîchir la sidebar
          this.userGamesStore.removeGame(this.gameId);
          // Rediriger vers la page d'accueil
          this.router.navigate(['/']);
        }
      },
      error: (error) => {
        console.error('Error deleting game:', error);
      }
    });
  }

  /**
   * @deprecated Utiliser permanentlyDeleteGame() pour suppression définitive
   */
  deleteGame(): void {
    this.permanentlyDeleteGame();
  }

  joinGame(): void {
    this.gameService.joinGame(this.gameId).subscribe({
      next: (success) => {
        if (success) {
          this.snackBar.open('Game rejoint avec succès!', 'Fermer', { duration: 3000 });
          this.loadGameDetails(); // Recharger les détails
          this.loadParticipants(); // Recharger les participants
        } else {
          this.snackBar.open('Impossible de rejoindre la game', 'Fermer', { duration: 3000 });
        }
      },
      error: (error) => {
        this.snackBar.open('Erreur lors de la tentative de rejoindre la game', 'Fermer', { duration: 3000 });
        console.error('Error joining game:', error);
      }
    });
  }

  canStartDraft(): boolean {
    if (!this.game) return false;
    return this.game.status === 'CREATING' && this.game.participantCount >= 2;
  }

  /**
   * Vérifie si l'utilisateur peut archiver la game (host uniquement)
   */
  canArchiveGame(): boolean {
    if (!this.game) return false;
    const currentUser = this.userContextService.getCurrentUser();
    if (!currentUser) return false;

    return this.gameService.isGameHost(this.game, currentUser.username);
  }

  /**
   * Vérifie si l'utilisateur peut quitter la game (participant non-host)
   */
  canLeaveGame(): boolean {
    if (!this.game) return false;
    const currentUser = this.userContextService.getCurrentUser();
    if (!currentUser) return false;

    // Peut quitter si participant mais pas host
    const isHost = this.gameService.isGameHost(this.game, currentUser.username);
    const isParticipant = this.game.participants?.some(p => p.username === currentUser.username) || false;

    return isParticipant && !isHost;
  }

  /**
   * Vérifie si l'utilisateur peut supprimer définitivement la partie
   * Conditions: host + status CREATING uniquement
   */
  canDeleteGame(): boolean {
    if (!this.game) return false;
    const currentUser = this.userContextService.getCurrentUser();
    if (!currentUser) return false;

    // Peut supprimer uniquement si host ET status CREATING
    const isHost = this.gameService.isGameHost(this.game, currentUser.username);
    const isCreatingStatus = this.game.status === 'CREATING';

    return isHost && isCreatingStatus;
  }

  canJoinGame(): boolean {
    if (!this.game) return false;
    return this.game.canJoin && this.game.participantCount < this.game.maxParticipants;
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

  getParticipantPercentage(): number {
    if (!this.game) return 0;
    const stats = this.gameDataService.calculateGameStatistics(this.game);
    return stats.fillPercentage;
  }

  getGameStatistics() {
    if (!this.game) return null;
    return this.gameDataService.calculateGameStatistics(this.game);
  }

  getParticipantColor(): string {
    const percentage = this.getParticipantPercentage();
    if (percentage >= 90) return 'warn';
    if (percentage >= 70) return 'accent';
    return 'primary';
  }

  getTimeAgo(date: string | Date | null | undefined): string {
    if (!date) {
      return 'Date invalide';
    }

    const dateString = typeof date === 'string' ? date : date.toISOString();
    return GameApiMapper.formatRelativeTime(dateString);
  }

  onBack(): void {
    this.router.navigate(['/games']);
  }

  confirmDelete(): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette game ? Cette action est irréversible.')) {
      this.deleteGame();
    }
  }

  confirmStartDraft(): void {
    if (confirm('Êtes-vous sûr de vouloir démarrer le draft ? Cette action ne peut pas être annulée.')) {
      this.startDraft();
    }
  }

  copyInvitationCode(): void {
    if (!this.game?.invitationCode) return;

    navigator.clipboard.writeText(this.game.invitationCode).then(() => {
      this.snackBar.open('Code copié dans le presse-papier !', 'OK', {
        duration: 2000
      });
    }).catch(() => {
      this.snackBar.open('Impossible de copier le code', 'Fermer', {
        duration: 3000
      });
    });
  }

  /**
   * Régénère le code d'invitation de la game avec durée configurable
   */
  regenerateInvitationCode(duration: '24h' | '48h' | '7d' | 'permanent' = 'permanent'): void {
    if (!this.game) return;

    this.gameService.regenerateInvitationCode(this.gameId, duration).subscribe({
      next: (updatedGame) => {
        if (this.game) {
          this.game.invitationCode = updatedGame.invitationCode;
          this.game.invitationCodeExpiresAt = updatedGame.invitationCodeExpiresAt;
          this.game.isInvitationCodeExpired = updatedGame.isInvitationCodeExpired;
        }
      },
      error: (error) => {
        console.error('Error regenerating invitation code:', error);
      }
    });
  }

  /**
   * Affiche le prompt pour choisir la durée et régénère le code
   */
  promptRegenerateCode(): void {
    const choice = prompt(
      'Choisissez la durée du code d\'invitation:\n1 = 24 heures\n2 = 48 heures\n3 = 7 jours\n4 = Permanent',
      '4'
    );

    if (!choice) return;

    const durationMap: { [key: string]: '24h' | '48h' | '7d' | 'permanent' } = {
      '1': '24h',
      '2': '48h',
      '3': '7d',
      '4': 'permanent'
    };

    const duration = durationMap[choice] || 'permanent';
    this.regenerateInvitationCode(duration);
  }

  /**
   * Formate la date d'expiration du code d'invitation
   */
  getInvitationCodeExpiry(): string {
    if (!this.game?.invitationCodeExpiresAt) {
      return 'Permanent';
    }
    if (this.game.isInvitationCodeExpired) {
      return 'Expiré';
    }
    return this.getTimeAgo(this.game.invitationCodeExpiresAt);
  }

  /**
   * Vérifie si l'utilisateur peut régénérer le code (host uniquement)
   */
  canRegenerateCode(): boolean {
    if (!this.game) return false;
    const currentUser = this.userContextService.getCurrentUser();
    if (!currentUser) return false;

    return this.gameService.isGameHost(this.game, currentUser.username) &&
           this.game.status === 'CREATING';
  }

  /**
   * Vérifie si l'utilisateur peut renommer la partie (host uniquement)
   */
  canRenameGame(): boolean {
    if (!this.game) return false;
    const currentUser = this.userContextService.getCurrentUser();
    if (!currentUser) return false;

    return this.gameService.isGameHost(this.game, currentUser.username) &&
           this.game.status === 'CREATING';
  }

  /**
   * Renomme la partie
   */
  renameGame(): void {
    if (!this.game) return;

    const newName = prompt('Nouveau nom de la partie:', this.game.name);
    if (!newName || newName.trim() === '' || newName === this.game.name) {
      return;
    }

    this.gameService.renameGame(this.gameId, newName.trim()).subscribe({
      next: (updatedGame) => {
        if (this.game) {
          this.game.name = updatedGame.name;
        }
      },
      error: (error) => {
        console.error('Error renaming game:', error);
      }
    });
  }

  getCreator(): GameParticipant | null {
    return this.participants.find(p => p.isCreator) || null;
  }

  getNonCreatorParticipants(): GameParticipant[] {
    return this.participants.filter(p => !p.isCreator);
  }

  getParticipantStatusIcon(participant: GameParticipant): string {
    if (participant.isCreator) return 'star';
    return 'person';
  }

  getParticipantStatusColor(participant: GameParticipant): string {
    if (participant.isCreator) return 'accent';
    return 'primary';
  }

  getParticipantStatusLabel(participant: GameParticipant): string {
    if (participant.isCreator) return 'Créateur';
    return 'Participant';
  }
} 
