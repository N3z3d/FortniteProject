import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AccessibilityAnnouncerService } from '../../shared/services/accessibility-announcer.service';

interface Player {
  playerId: string;
  nickname: string;
  username: string;
  region: string;
  totalPoints: number;
  rank: number;
}

@Component({
  selector: 'app-simple-leaderboard',
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule],
  templateUrl: './simple-leaderboard.component.html',
  styleUrls: ['./simple-leaderboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SimpleLeaderboardComponent implements OnInit {
  allPlayers: Player[] = [];
  filteredPlayers: Player[] = [];
  loading = false;
  error = '';
  searchTerm = '';
  selectedRegion = '';
  currentPage = 1;
  itemsPerPage = 20;
  currentSort = 'points';
  sortDirection: 'asc' | 'desc' = 'desc';

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private accessibilityService: AccessibilityAnnouncerService
  ) {}

  ngOnInit() {
    this.loadData();
  }

  async loadData() {
    this.loading = true;
    this.error = '';

    try {
      // Charger les données des équipes depuis l'API leaderboard
      const response = await this.http.get<any[]>(`${environment.apiUrl}/api/leaderboard?user=Thibaut`).toPromise();
      
      if (response && response.length > 0) {
        console.log('📊 Données équipes reçues:', response);
        
        // Convertir les équipes en joueurs individuels pour l'affichage
        const allPlayersFromTeams: Player[] = [];
        
        response.forEach((team, teamIndex) => {
          // Ajouter le propriétaire de l'équipe comme "joueur spécial"
          allPlayersFromTeams.push({
            playerId: `team-owner-${team.ownerId}`,
            nickname: `🏆 ${team.ownerUsername} (Chef d'équipe)`,
            username: team.ownerUsername,
            region: 'TEAM',
            totalPoints: team.totalPoints,
            rank: team.rank
          });
          
          // Ajouter tous les joueurs de l'équipe
          if (team.players && team.players.length > 0) {
            team.players.forEach((player: any) => {
              allPlayersFromTeams.push({
                playerId: player.playerId,
                nickname: player.nickname,
                username: player.username,
                region: player.region,
                totalPoints: player.points,
                rank: 0 // Sera recalculé lors du tri
              });
            });
          }
        });
        
        // Trier tous les joueurs par points et assigner les rangs
        allPlayersFromTeams.sort((a, b) => b.totalPoints - a.totalPoints);
        allPlayersFromTeams.forEach((player, index) => {
          player.rank = index + 1;
        });
        
        this.allPlayers = allPlayersFromTeams;
        console.log(`✅ ${this.allPlayers.length} joueurs extraits des équipes`);
      } else {
        console.warn('⚠️ Aucune équipe reçue de l\'API');
        this.allPlayers = [];
      }
      
      this.filterPlayers();
    } catch (error) {
      console.error('❌ Erreur lors du chargement du leaderboard:', error);
      this.allPlayers = [];
      this.error = 'Impossible de charger les données. Vérifiez que le backend est démarré.';
      this.filterPlayers();
    } finally {
      this.loading = false;
      this.cdr.markForCheck();
    }
  }


  getPaginatedPlayers(): Player[] {
    const start = (this.currentPage - 1) * this.itemsPerPage;
    return this.filteredPlayers.slice(start, start + this.itemsPerPage);
  }

  getTotalPages(): number {
    return Math.ceil(this.filteredPlayers.length / this.itemsPerPage);
  }


  getTotalPoints(): number {
    return this.filteredPlayers.reduce((sum, p) => sum + p.totalPoints, 0);
  }

  getInitials(name: string): string {
    return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  }

  getAtSymbol(): string {
    return '@';
  }

  // ============== OPTIMISATIONS PERFORMANCE ANGULAR ==============
  
  /**
   * TrackBy function pour optimiser *ngFor des joueurs
   */
  trackByPlayerId(index: number, player: Player): string {
    return player.playerId || `player-${index}`;
  }

  /**
   * TrackBy function pour les pages de pagination
   */
  trackByPageNumber(index: number, page: number): number {
    return page;
  }

  /**
   * TrackBy function compatible with template trackByPlayer
   */
  trackByPlayer(index: number, player: Player): string {
    return this.trackByPlayerId(index, player);
  }

  // ============== ACCESSIBILITY ENHANCEMENTS ==============
  
  /**
   * Sort table by column with accessibility announcements
   */
  sortBy(column: 'rank' | 'region' | 'points'): void {
    if (this.currentSort === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.currentSort = column;
      this.sortDirection = column === 'points' ? 'desc' : 'asc';
    }

    // Sort the filtered players
    this.filteredPlayers.sort((a, b) => {
      let aVal: any;
      let bVal: any;

      switch (column) {
        case 'rank':
          aVal = a.rank;
          bVal = b.rank;
          break;
        case 'region':
          aVal = a.region;
          bVal = b.region;
          break;
        case 'points':
          aVal = a.totalPoints;
          bVal = b.totalPoints;
          break;
      }

      if (this.sortDirection === 'asc') {
        return aVal > bVal ? 1 : -1;
      } else {
        return aVal < bVal ? 1 : -1;
      }
    });

    // Update ranks after sorting
    this.filteredPlayers.forEach((player, index) => {
      player.rank = index + 1;
    });

    // Announce the sort change to screen readers
    const sortLabel = this.getSortLabel(column);
    const directionLabel = this.sortDirection === 'asc' ? 'ascending' : 'descending';
    this.accessibilityService.announce(`Table sorted by ${sortLabel} in ${directionLabel} order`);

    this.currentPage = 1;
    this.cdr.markForCheck();
  }

  /**
   * Get human-readable sort label
   */
  private getSortLabel(column: string): string {
    switch (column) {
      case 'rank': return 'rank';
      case 'region': return 'region';
      case 'points': return 'points';
      default: return column;
    }
  }

  /**
   * Enhanced filter with accessibility announcements
   */
  filterPlayers() {
    let filtered = [...this.allPlayers];

    if (this.selectedRegion) {
      filtered = filtered.filter(p => p.region === this.selectedRegion);
    }

    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(p => 
        p.nickname.toLowerCase().includes(term) ||
        p.username.toLowerCase().includes(term)
      );
    }

    // Apply current sort
    this.filteredPlayers = filtered;
    if (this.currentSort) {
      this.sortBy(this.currentSort as any);
    }

    // Announce filter results
    this.announceFilterResults();
  }

  /**
   * Announce filter and search results to screen readers
   */
  private announceFilterResults(): void {
    const resultCount = this.filteredPlayers.length;
    let message = `${resultCount} player${resultCount !== 1 ? 's' : ''} found`;
    
    if (this.searchTerm) {
      message += ` for search term "${this.searchTerm}"`;
    }
    
    if (this.selectedRegion) {
      message += ` in ${this.selectedRegion} region`;
    }

    this.accessibilityService.announce(message);
  }

  /**
   * Enhanced pagination with announcements
   */
  goToPage(page: number) {
    if (page >= 1 && page <= this.getTotalPages()) {
      this.currentPage = page;
      this.accessibilityService.announce(`Navigated to page ${page} of ${this.getTotalPages()}`);
      this.cdr.markForCheck();
    }
  }

  /**
   * Enhanced reset with announcements
   */
  resetFilters() {
    this.searchTerm = '';
    this.selectedRegion = '';
    this.filterPlayers();
    this.accessibilityService.announce('All filters have been reset');
  }
} 