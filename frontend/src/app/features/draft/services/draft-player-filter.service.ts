import { Injectable } from '@angular/core';
import { Player, DraftBoardState } from './draft.service';
import { PlayerRegion } from '../models/draft.interface';

/**
 * Service responsible for filtering and searching draft players.
 * Handles region, tranche, and text-based filtering logic.
 */
@Injectable({
  providedIn: 'root'
})
export class DraftPlayerFilterService {

  /**
   * Filters players based on current filter criteria
   */
  filterPlayers(
    players: Player[],
    options: {
      selectedRegion: PlayerRegion | 'ALL';
      selectedTranche: string | 'ALL';
      searchTerm: string;
    }
  ): Player[] {
    return players
      .filter(p => this.filterByRegion(p, options.selectedRegion))
      .filter(p => this.filterByTranche(p, options.selectedTranche))
      .filter(p => this.filterBySearch(p, options.searchTerm));
  }

  /**
   * Extracts unique regions from available players
   */
  extractUniqueRegions(players: Player[]): PlayerRegion[] {
    const regions = [...new Set(players.map(p => p.region))];
    return regions.filter((region): region is PlayerRegion => typeof region === 'string');
  }

  /**
   * Extracts unique tranches from available players
   */
  extractUniqueTranches(players: Player[]): string[] {
    return [...new Set(players.map(p => p.tranche))];
  }

  /**
   * Gets smart player suggestions (top ranked available players)
   */
  getSmartSuggestions(players: Player[], limit: number = 3): Array<{
    player: Player;
    rank: number;
    score: number;
  }> {
    return players.slice(0, limit).map((player, index) => ({
      player,
      rank: index + 1,
      score: Math.floor(Math.random() * 1000) + 500 // Simulated score
    }));
  }

  /**
   * Filters player by region
   */
  private filterByRegion(player: Player, selectedRegion: PlayerRegion | 'ALL'): boolean {
    return selectedRegion === 'ALL' || player.region === selectedRegion;
  }

  /**
   * Filters player by tranche
   */
  private filterByTranche(player: Player, selectedTranche: string | 'ALL'): boolean {
    return selectedTranche === 'ALL' || player.tranche === selectedTranche;
  }

  /**
   * Filters player by search term (nickname or username)
   */
  private filterBySearch(player: Player, searchTerm: string): boolean {
    if (!searchTerm.trim()) return true;

    const searchLower = searchTerm.toLowerCase();
    return player.nickname.toLowerCase().includes(searchLower) ||
           player.username.toLowerCase().includes(searchLower);
  }
}
