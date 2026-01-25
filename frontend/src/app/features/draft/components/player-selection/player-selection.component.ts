import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, TrackByFunction, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { CommonModule } from '@angular/common';
import { Subject, debounceTime, takeUntil } from 'rxjs';

import { Player, PlayerRegion } from '../../models/draft.interface';
import { REGION_LABELS, FILTER_OPTIONS, PERFORMANCE_CONFIG } from '../../constants/draft.constants';
import { TranslationService } from '../../../../core/services/translation.service';

/**
 * COMPONENT OPTIMISÉ POUR 149 JOUEURS
 * Features de performance:
 * - Virtual Scrolling (CDK) pour gérer de grandes listes
 * - Filtrage optimisé avec debouncing
 * - TrackBy function pour optimiser le rendu
 * - Lazy loading des données
 */
@Component({
  selector: 'app-player-selection',
  templateUrl: './player-selection.component.html',
  styleUrls: ['./player-selection.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    ScrollingModule // CRITICAL: Pour le virtual scrolling
  ]
})
export class PlayerSelectionComponent implements OnInit, OnDestroy {
  public readonly t = inject(TranslationService);

  // Inputs
  @Input() players: Player[] = [];
  @Input() canSelect = false;
  @Input() isLoading = false;
  @Input() error: string | null = null;
  @Input() showPaginationInfo = true;

  // Outputs
  @Output() playerSelected = new EventEmitter<Player>();

  // Filtres avec valeurs par défaut optimisées
  selectedRegion: any = FILTER_OPTIONS.ALL_REGIONS;
  selectedTranche: any = FILTER_OPTIONS.ALL_TRANCHES;
  searchTerm: string = FILTER_OPTIONS.DEFAULT_SEARCH_TERM;

  // Cache des joueurs filtrés pour éviter les recalculs
  private filteredPlayersCache: Player[] = [];
  private lastFilterHash = '';

  // Gestion du cycle de vie et optimisations
  private destroy$ = new Subject<void>();
  private searchSubject = new Subject<string>();

  // PERFORMANCE: TrackBy function pour virtual scrolling
  trackByPlayerId: TrackByFunction<Player> = (index: number, player: Player) => {
    return player.id || index; // Utilise l'ID unique du joueur
  };

  // Couleurs de région pour l'UI
  private readonly regionColors: Record<any, string> = {
    'EU': '#4CAF50',    
    'NAW': '#2196F3',   
    'NAC': '#FF9800',   
    'NAE': '#FF9800',   // Alias pour NAC
    'BR': '#9C27B0',    
    'ASIA': '#F44336',  
    'OCE': '#00BCD4',   
    'ME': '#795548'     
  };

  ngOnInit(): void {
    this.setupSearchDebouncing();
    this.updateFilteredPlayers(); // Cache initial
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.searchSubject.complete();
  }

  // ===== MÉTHODES PUBLIQUES PRINCIPALES =====

  getResultsSummary(): string {
    return this.formatTemplate(this.t.t('draft.selection.countSummary'), {
      filtered: this.getFilteredCount(),
      total: this.getTotalCount()
    });
  }

  getListAriaLabel(): string {
    return this.formatTemplate(this.t.t('draft.selection.listAria'), {
      count: this.getFilteredCount()
    });
  }

  getPlayerAriaLabel(player: Player): string {
    return this.formatTemplate(this.t.t('draft.selection.playerAria'), {
      nickname: player.nickname,
      region: this.getRegionLabel(player.region),
      tranche: this.getTrancheDisplayValue(player.tranche)
    });
  }

  getPaginationInfoLabel(): string {
    return this.formatTemplate(this.t.t('draft.selection.performanceOptimized'), {
      count: this.getTotalCount()
    });
  }

  getFilteredPlayers(): Player[] {
    const currentFilterHash = this.getCurrentFilterHash();
    
    // OPTIMISATION: Retourne le cache si les filtres n'ont pas changé
    if (currentFilterHash === this.lastFilterHash && this.filteredPlayersCache.length > 0) {
      return this.filteredPlayersCache;
    }

    // Recalcule et met en cache
    this.updateFilteredPlayers();
    this.lastFilterHash = currentFilterHash;
    
    return this.filteredPlayersCache;
  }

  getAvailableRegions(): any[] {
    if (!this.players || this.players.length === 0) return [];
    return [...new Set(this.players.map(p => p.region))];
  }

  getAvailableTranches(): string[] {
    if (!this.players || this.players.length === 0) return [];
    return [...new Set(this.players.map(p => p.tranche))].sort();
  }

  hasActiveFilters(): boolean {
    return Boolean(this.selectedRegion !== FILTER_OPTIONS.ALL_REGIONS ||
           this.selectedTranche !== FILTER_OPTIONS.ALL_TRANCHES ||
           (this.searchTerm && this.searchTerm.trim() !== ''));
  }

  clearFilters(): void {
    this.selectedRegion = FILTER_OPTIONS.ALL_REGIONS;
    this.selectedTranche = FILTER_OPTIONS.ALL_TRANCHES;
    this.searchTerm = FILTER_OPTIONS.DEFAULT_SEARCH_TERM;
    this.updateFilteredPlayers();
  }

  onPlayerSelect(player: Player): void {
    if (this.canSelect) {
      this.playerSelected.emit(player);
    }
  }

  onSearchChange(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchSubject.next(value);
  }

  // ===== MÉTHODES UTILITAIRES =====

  getRegionLabel(region: any): string {
    const key = REGION_LABELS[region as PlayerRegion];
    return key ? this.t.t(key, region) : region;
  }

  getRegionColor(region: any): string {
    return this.regionColors[region] || '#9E9E9E';
  }

  getTrancheLabel(tranche: string): string {
    return this.formatTemplate(this.t.t('draft.selection.trancheValue'), {
      value: this.getTrancheDisplayValue(tranche)
    });
  }

  getTotalCount(): number {
    return this.players?.length || 0;
  }

  getFilteredCount(): number {
    return this.getFilteredPlayers().length;
  }

  // ===== MÉTHODES PRIVÉES D'OPTIMISATION =====

  private setupSearchDebouncing(): void {
    this.searchSubject
      .pipe(
        debounceTime(PERFORMANCE_CONFIG.SEARCH_DEBOUNCE_TIME),
        takeUntil(this.destroy$)
      )
      .subscribe(term => {
        this.searchTerm = term;
        this.updateFilteredPlayers();
      });
  }

  private updateFilteredPlayers(): void {
    if (!this.players || this.players.length === 0) {
      this.filteredPlayersCache = [];
      return;
    }

    this.filteredPlayersCache = this.players
      .filter(this.filterByRegion.bind(this))
      .filter(this.filterByTranche.bind(this))
      .filter(this.filterBySearch.bind(this))
      .sort(this.sortPlayers.bind(this)); // Tri optimisé
  }

  private getCurrentFilterHash(): string {
    return `${this.selectedRegion}_${this.selectedTranche}_${this.searchTerm}_${this.players.length}`;
  }

  private filterByRegion(player: Player): boolean {
    return this.selectedRegion === FILTER_OPTIONS.ALL_REGIONS || 
           player.region === this.selectedRegion;
  }

  private filterByTranche(player: Player): boolean {
    return this.selectedTranche === FILTER_OPTIONS.ALL_TRANCHES || 
           player.tranche === this.selectedTranche;
  }

  private filterBySearch(player: Player): boolean {
    if (!this.searchTerm || this.searchTerm.trim() === '') return true;
    
    const searchLower = this.searchTerm.toLowerCase();
    return Boolean(player.nickname.toLowerCase().includes(searchLower) ||
           (player.username && player.username.toLowerCase().includes(searchLower)));
  }

  private sortPlayers(a: Player, b: Player): number {
    // Tri par points décroissants puis par nom
    if (a.totalPoints !== undefined && b.totalPoints !== undefined) {
      if (a.totalPoints !== b.totalPoints) {
        return b.totalPoints - a.totalPoints;
      }
    }
    return a.nickname.localeCompare(b.nickname);
  }

  private getTrancheDisplayValue(tranche: string): string {
    const match = /^T(\d+)$/i.exec(tranche);
    return match ? match[1] : tranche;
  }

  private formatTemplate(template: string, params: Record<string, string | number>): string {
    return Object.entries(params).reduce((result, [key, value]) => {
      return result.replace(`{${key}}`, String(value));
    }, template);
  }
}
