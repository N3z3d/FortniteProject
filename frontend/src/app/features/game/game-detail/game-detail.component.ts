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
    private gameService: GameService,
    private gameDataService: GameDataService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar
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

  deleteGame(): void {
    this.gameService.deleteGame(this.gameId).subscribe({
      next: (success) => {
        if (success) {
          this.snackBar.open('Game supprimée avec succès!', 'Fermer', { duration: 3000 });
          this.router.navigate(['/games']);
        } else {
          this.snackBar.open('Impossible de supprimer la game', 'Fermer', { duration: 3000 });
        }
      },
      error: (error) => {
        this.snackBar.open('Erreur lors de la suppression de la game', 'Fermer', { duration: 3000 });
        console.error('Error deleting game:', error);
      }
    });
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

  canDeleteGame(): boolean {
    if (!this.game) return false;
    return this.game.status === 'CREATING';
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
