import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';

import { Player } from '../../models/draft.interface';
import { TranslationService } from '../../../../core/services/translation.service';
import { DraftStateHelperService } from '../../services/draft-state-helper.service';

@Component({
  selector: 'app-draft-player-list',
  templateUrl: './draft-player-list.component.html',
  styleUrls: ['./draft-player-list.component.scss'],
  standalone: true,
  imports: [CommonModule, MatIconModule, MatChipsModule, MatButtonModule]
})
export class DraftPlayerListComponent {
  public readonly t = inject(TranslationService);
  private readonly helperService = inject(DraftStateHelperService);

  @Input() searchTerm: string | null = '';
  @Input() filteredPlayers: Player[] | null = [];
  @Input() showAllResults = false;
  @Input() canSelect = false;
  @Input() suggestions: any[] = [];

  @Output() showAllResultsChange = new EventEmitter<boolean>();
  @Output() playerSelected = new EventEmitter<Player>();
  @Output() clearSearch = new EventEmitter<void>();

  get safePlayers(): Player[] {
    return this.filteredPlayers ?? [];
  }

  get hasSearchTerm(): boolean {
    return Boolean(this.searchTerm && this.searchTerm.trim().length > 0);
  }

  get hasResults(): boolean {
    return this.hasSearchTerm && this.safePlayers.length > 0;
  }

  get hasNoResults(): boolean {
    return this.hasSearchTerm && this.safePlayers.length === 0;
  }

  get visiblePlayers(): Player[] {
    return this.showAllResults ? this.safePlayers : this.safePlayers.slice(0, 5);
  }

  get shouldShowMoreButton(): boolean {
    return this.hasSearchTerm && this.safePlayers.length > 5;
  }

  get hasSuggestions(): boolean {
    return this.suggestions.length > 0 && !this.hasSearchTerm;
  }

  toggleShowAllResults(): void {
    this.showAllResultsChange.emit(!this.showAllResults);
  }

  selectPlayer(player: Player): void {
    if (!this.canSelect) return;
    this.playerSelected.emit(player);
  }

  requestClearSearch(): void {
    this.clearSearch.emit();
  }

  getRegionLabel(region: string | undefined): string {
    if (!region) return '';
    const key = this.helperService.getRegionLabelKey(region);
    return this.t.t(key, region);
  }

  getTrancheLabel(tranche: string): string {
    return this.helperService.getTrancheLabel(tranche, (key) => this.t.t(key));
  }

  getSearchResultsTitle(count: number): string {
    return this.helperService.getSearchResultsTitle(count, (key) => this.t.t(key));
  }

  getShowAllResultsLabel(count: number): string {
    return this.helperService.getShowAllResultsLabel(count, (key) => this.t.t(key));
  }

  getSuggestionRankLabel(rank: number): string {
    return this.helperService.getSuggestionRankLabel(rank, (key) => this.t.t(key));
  }
}
