import { Injectable } from '@angular/core';
import { Game } from '../../game/models/game.interface';
import { TranslationService } from '../../../core/services/translation.service';

@Injectable({
  providedIn: 'root'
})
export class DashboardFormattingService {
  constructor(private t: TranslationService) {}

  formatNumber(value: number | undefined | null): string {
    if (value === undefined || value === null) {
      return '0';
    }
    return value.toLocaleString(this.getNumberLocale());
  }

  getParticipantDisplayCount(game: Game | null, fallbackTeams: number): number {
    const explicitCount = game?.participantCount || 0;
    const teamCount = game?.teams?.length ? fallbackTeams || 0;
    const participantCount = game?.participants?.length || 0;

    return Math.max(explicitCount, teamCount, participantCount);
  }

  displayTeamName(rawName: string | undefined | null): string {
    if (!rawName) {
      return '';
    }
    return rawName.replace(/^É?quipe des\s+/i, '').trim();
  }

  private getNumberLocale(): string {
    switch (this.t.currentLanguage) {
      case 'en':
        return 'en-US';
      case 'es':
        return 'es-ES';
      case 'pt':
        return 'pt-PT';
      default:
        return 'fr-FR';
    }
  }
}
