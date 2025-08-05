import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { GameService } from '../services/game.service';
import { Game, GameStatus } from '../models/game.interface';

@Component({
  selector: 'app-join-game',
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
    MatSnackBarModule,
    MatTooltipModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressBarModule
  ],
  templateUrl: './join-game.component.html',
  styleUrls: ['./join-game.component.css']
})
export class JoinGameComponent implements OnInit {
  games: Game[] = [];
  filteredGames: Game[] = [];
  loading = false;
  error: string | null = null;
  searchTerm = '';
  selectedStatus = '';
  showAllResults = false;
  isJoining = false;

  constructor(
    private gameService: GameService,
    private router: Router,
    private snackBar: MatSnackBar
  ) { }

  ngOnInit(): void {
    this.loadGames();
  }

  loadGames(): void {
    this.loading = true;
    this.error = null;

    this.gameService.getAvailableGames().subscribe({
      next: (games) => {
        this.games = games;
        this.filteredGames = games;
        this.loading = false;
      },
      error: (error) => {
        this.error = 'Erreur lors du chargement des games';
        this.loading = false;
        console.error('Error loading games:', error);
      }
    });
  }

  joinGame(gameId: string): void {
    this.gameService.joinGame(gameId).subscribe({
      next: (success) => {
        if (success) {
          this.snackBar.open('Game rejoint avec succès!', 'Fermer', { duration: 3000 });
          this.loadGames(); // Recharger la liste
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

  filterGames(): void {
    let filtered = this.games;

    // Filtrer par terme de recherche
    if (this.searchTerm.trim()) {
      const searchLower = this.searchTerm.toLowerCase();
      filtered = filtered.filter(game => 
        game.name.toLowerCase().includes(searchLower) ||
        game.creatorName.toLowerCase().includes(searchLower)
      );
    }

    // Filtrer par statut
    if (this.selectedStatus) {
      filtered = filtered.filter(game => game.status === this.selectedStatus);
    }

    this.filteredGames = filtered;
  }

  clearFilters(): void {
    this.searchTerm = '';
    this.selectedStatus = '';
    this.filteredGames = this.games;
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

  getStatusLabel(status: string): string {
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

  canJoinGame(game: Game): boolean {
    return game.canJoin && game.participantCount < game.maxParticipants;
  }

  getAvailableStatuses(): string[] {
    const statuses = new Set(this.games.map(game => game.status));
    return Array.from(statuses);
  }

  onBack(): void {
    this.router.navigate(['/games']);
  }

  getParticipantPercentage(game: Game): number {
    if (game.maxParticipants === 0) return 0;
    return Math.round((game.participantCount / game.maxParticipants) * 100);
  }

  getParticipantColor(game: Game): string {
    const percentage = this.getParticipantPercentage(game);
    if (percentage >= 90) return 'warn';
    if (percentage >= 70) return 'accent';
    return 'primary';
  }

  // === NOUVELLES MÉTHODES POUR L'INTERFACE OPTIMISÉE ===
  
  onQuickSearch(): void {
    this.filterGames();
    this.showAllResults = false;
  }

  instantJoin(game: Game): void {
    if (!this.canJoinGame(game) || this.isJoining) return;
    
    this.isJoining = true;
    this.joinGameById(game.id);
  }

  getPopularGames(): Game[] {
    // Retourne les games avec le plus de participants (simulé)
    return this.games
      .filter(game => this.canJoinGame(game))
      .sort((a, b) => b.participantCount - a.participantCount)
      .slice(0, 4);
  }

  isPopular(game: Game): boolean {
    // Une game est populaire si elle a plus de 50% de participants
    return game.participantCount / game.maxParticipants > 0.5;
  }

  joinGameById(gameId: string): void {
    // Simulate join game API call
    setTimeout(() => {
      this.isJoining = false;
      this.snackBar.open('🎉 Game rejointe avec succès !', '', { 
        duration: 2000,
        panelClass: 'success-snackbar'
      });
      // Navigate directly to the game
      this.router.navigate(['/games', gameId]);
    }, 1000);
  }

  getTimeAgo(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffInMinutes = Math.floor((now.getTime() - date.getTime()) / (1000 * 60));

    if (diffInMinutes < 1) return 'À l\'instant';
    if (diffInMinutes < 60) return `Il y a ${diffInMinutes} min`;
    
    const diffInHours = Math.floor(diffInMinutes / 60);
    if (diffInHours < 24) return `Il y a ${diffInHours}h`;
    
    const diffInDays = Math.floor(diffInHours / 24);
    if (diffInDays < 7) return `Il y a ${diffInDays}j`;
    
    return date.toLocaleDateString('fr-FR');
  }
} 