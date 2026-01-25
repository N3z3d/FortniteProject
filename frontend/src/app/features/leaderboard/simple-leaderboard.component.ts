import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AccessibilityAnnouncerService } from '../../shared/services/accessibility-announcer.service';
import { GameSelectionService } from '../../core/services/game-selection.service';
import { TranslationService } from '../../core/services/translation.service';
import { Subscription } from 'rxjs';
import { LeaderboardService, PlayerLeaderboardEntry } from '../../core/services/leaderboard.service';

@Component({
  selector: 'app-simple-leaderboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './simple-leaderboard.component.html',
  styleUrls: ['./simple-leaderboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SimpleLeaderboardComponent implements OnInit, OnDestroy {
  allPlayers: PlayerLeaderboardEntry[] = [];
  filteredPlayers: PlayerLeaderboardEntry[] = [];
  loading = false;
  error = '';
  searchTerm = '';
  selectedRegion = '';
  currentPage = 1;
  itemsPerPage = 20;
  currentSort = 'points';
  sortDirection: 'asc' | 'desc' = 'desc';
  private gameSubscription?: Subscription;
  selectedGameId: string | null = null;

  constructor(
    private leaderboardService: LeaderboardService,
    private cdr: ChangeDetectorRef,
    private accessibilityService: AccessibilityAnnouncerService,
    private gameSelectionService: GameSelectionService,
    public t: TranslationService
  ) {}

  ngOnInit() {
    this.loadData();
    this.gameSubscription = this.gameSelectionService.selectedGame$.subscribe(() => {
      this.loadData();
    });
  }

  ngOnDestroy() {
    this.gameSubscription?.unsubscribe();
  }

  loadData() {
    this.loading = true;
    this.error = '';
    this.selectedGameId = this.gameSelectionService.getSelectedGame()?.id || null;

    this.leaderboardService.getPlayerLeaderboard(2025, undefined, this.selectedGameId || undefined).subscribe({
      next: (entries) => {
        this.allPlayers = Array.isArray(entries) ? entries : [];
        this.filterPlayers();
      },
      error: (err) => {
        console.error('Erreur lors du chargement du leaderboard:', err);
        this.error = this.t.t('leaderboard.errors.dataUnavailable');
        this.allPlayers = [];
        this.filteredPlayers = [];
        this.loading = false;
        this.cdr.markForCheck();
      },
      complete: () => {
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  getPaginatedPlayers(): PlayerLeaderboardEntry[] {
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

  getPageOfLabel(): string {
    return this.formatTemplate('leaderboard.pageOf', {
      current: this.currentPage,
      total: this.getTotalPages()
    });
  }

  getPlayerRowLabel(player: PlayerLeaderboardEntry): string {
    return this.formatTemplate('leaderboard.aria.playerRow', {
      rank: player.rank,
      nickname: player.nickname,
      region: player.region,
      points: this.formatNumber(player.totalPoints)
    });
  }

  getRankLabel(rank: number): string {
    return this.formatTemplate('leaderboard.aria.rank', { rank });
  }

  getAvatarLabel(nickname: string): string {
    return this.formatTemplate('leaderboard.aria.avatar', { nickname });
  }

  getRegionLabel(region: string): string {
    return this.formatTemplate('leaderboard.aria.region', { region });
  }

  getPointsLabel(points: number): string {
    return this.formatTemplate('leaderboard.aria.points', {
      points: this.formatNumber(points)
    });
  }

  trackByPlayerId = (index: number, player: PlayerLeaderboardEntry): string => {
    return player.playerId || `player-${index}`;
  }

  trackByPageNumber = (index: number, page: number): number => {
    return page;
  }

  trackByPlayer = (index: number, player: PlayerLeaderboardEntry): string => {
    return player.playerId || `player-${index}`;
  }

  sortBy(column: 'rank' | 'region' | 'points'): void {
    if (this.currentSort === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.currentSort = column;
      this.sortDirection = column === 'points' ? 'desc' : 'asc';
    }

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

    this.filteredPlayers.forEach((player, index) => {
      player.rank = index + 1;
    });

    const sortLabel = this.getSortLabel(column);
    const directionLabel = this.getSortDirectionLabel(this.sortDirection);
    this.accessibilityService.announce(
      this.formatTemplate('leaderboard.aria.sortAnnouncement', {
        column: sortLabel,
        direction: directionLabel
      })
    );

    this.currentPage = 1;
    this.cdr.markForCheck();
  }

  private getSortLabel(column: 'rank' | 'region' | 'points'): string {
    switch (column) {
      case 'rank': return this.t.t('leaderboard.sort.rank');
      case 'region': return this.t.t('leaderboard.sort.region');
      case 'points': return this.t.t('leaderboard.sort.points');
      default: return column;
    }
  }

  private getSortDirectionLabel(direction: 'asc' | 'desc'): string {
    return direction === 'asc'
      ? this.t.t('leaderboard.sort.ascending')
      : this.t.t('leaderboard.sort.descending');
  }

  private formatTemplate(key: string, params: Record<string, string | number>): string {
    let value = this.t.t(key);
    for (const [param, paramValue] of Object.entries(params)) {
      const token = new RegExp(`\\{${param}\\}`, 'g');
      value = value.replace(token, String(paramValue));
    }
    return value;
  }

  private formatNumber(value: number): string {
    return new Intl.NumberFormat(this.getLocale()).format(value);
  }

  private getLocale(): string {
    switch (this.t.currentLanguage) {
      case 'fr':
        return 'fr-FR';
      case 'es':
        return 'es-ES';
      case 'pt':
        return 'pt-BR';
      default:
        return 'en-US';
    }
  }

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

    this.filteredPlayers = filtered;
    if (this.currentSort) {
      this.sortBy(this.currentSort as any);
    }

    this.announceFilterResults();
  }

  private announceFilterResults(): void {
    const resultCount = this.filteredPlayers.length;
    let message = resultCount === 1
      ? this.t.t('leaderboard.aria.filterResultsSingle')
      : this.formatTemplate('leaderboard.aria.filterResultsMultiple', { count: resultCount });

    if (this.searchTerm) {
      message += this.formatTemplate('leaderboard.aria.filterSearchSuffix', { term: this.searchTerm });
    }

    if (this.selectedRegion) {
      message += this.formatTemplate('leaderboard.aria.filterRegionSuffix', { region: this.selectedRegion });
    }

    this.accessibilityService.announce(message);
  }

  goToPage(page: number) {
    if (page >= 1 && page <= this.getTotalPages()) {
      this.currentPage = page;
      this.accessibilityService.announce(
        this.formatTemplate('leaderboard.aria.pageNavigation', {
          page,
          total: this.getTotalPages()
        })
      );
      this.cdr.markForCheck();
    }
  }

  resetFilters() {
    this.searchTerm = '';
    this.selectedRegion = '';
    this.filterPlayers();
    this.accessibilityService.announce(this.t.t('leaderboard.aria.filtersReset'));
  }
}
