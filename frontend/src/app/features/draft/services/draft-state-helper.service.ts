import { Injectable } from '@angular/core';
import { DraftStatus, PlayerRegion } from '../models/draft.interface';
import { REGION_LABELS, STATUS_LABELS } from '../constants/draft.constants';

/**
 * Service providing helper methods for formatting draft UI labels and templates.
 * Handles status colors, region labels, and text templates.
 */
@Injectable({
  providedIn: 'root'
})
export class DraftStateHelperService {

  /**
   * Gets Material color class for draft status
   */
  getStatusColor(status: DraftStatus | string): string {
    switch (status) {
      case 'ACTIVE':
      case 'IN_PROGRESS':
        return 'accent';
      case 'PAUSED':
      case 'CANCELLED':
      case 'ERROR':
        return 'warn';
      default:
        return 'primary';
    }
  }

  /**
   * Gets i18n key for status label
   */
  getStatusLabelKey(status: DraftStatus | string): string {
    return STATUS_LABELS[status] || status;
  }

  /**
   * Gets i18n key for region label
   */
  getRegionLabelKey(region: string): string {
    return REGION_LABELS[region as PlayerRegion] || region;
  }

  /**
   * Gets formatted tranche label (e.g., "T1" -> "Tranche 1")
   */
  getTrancheLabel(tranche: string, t: (key: string) => string): string {
    const match = /^T(\d+)$/i.exec(tranche);
    if (match) {
      return this.formatTemplate(t('draft.selection.trancheValue'), { value: match[1] });
    }
    return tranche;
  }

  /**
   * Formats search results title with count
   */
  getSearchResultsTitle(count: number, t: (key: string) => string): string {
    return this.formatTemplate(t('draft.ui.searchResultsTitle'), { count });
  }

  /**
   * Formats "show all results" label with count
   */
  getShowAllResultsLabel(count: number, t: (key: string) => string): string {
    return this.formatTemplate(t('draft.ui.showAllResults'), { count });
  }

  /**
   * Formats suggestion rank label
   */
  getSuggestionRankLabel(rank: number, t: (key: string) => string): string {
    return this.formatTemplate(t('draft.ui.suggestionRank'), { rank });
  }

  /**
   * Formats remaining slots label (singular/plural)
   */
  getSlotsRemainingLabel(count: number, t: (key: string) => string): string {
    const key = count === 1 ? 'draft.ui.slotsRemainingSingle' : 'draft.ui.slotsRemainingMultiple';
    return this.formatTemplate(t(key), { count });
  }

  /**
   * Formats waiting message with current player's username
   */
  getWaitingMessage(username: string | null, t: (key: string) => string): string {
    if (!username) return t('draft.ui.waitingNextTurn');
    return this.formatTemplate(t('draft.ui.waitingTurn'), { username });
  }

  /**
   * Replaces template placeholders with values
   * Example: formatTemplate("Hello {name}", {name: "Alice"}) -> "Hello Alice"
   */
  formatTemplate(template: string, params: Record<string, string | number>): string {
    return Object.entries(params).reduce((result, [key, value]) => {
      return result.replace(`{${key}}`, String(value));
    }, template);
  }
}
