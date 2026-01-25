import { Injectable } from '@angular/core';
import { TranslationService } from '../../../core/services/translation.service';

export type DashboardStatColor = 'primary' | 'accent' | 'warn';
export type DashboardStatTrend = 'up' | 'down' | 'stable';

export interface DashboardDisplayStat {
  value: string;
  label: string;
  trend?: DashboardStatTrend;
  color?: DashboardStatColor;
}

export type DashboardDisplayStats = Record<string, DashboardDisplayStat>;

export interface DashboardStatsInput {
  totalTeams: number;
  totalPlayers: number;
  totalPoints: number;
  averagePointsPerTeam: number;
  seasonProgress: number;
}

@Injectable({
  providedIn: 'root'
})
export class DashboardStatsDisplayService {
  constructor(private t: TranslationService) {}

  formatStatsForDisplay(stats: DashboardStatsInput | null | undefined): DashboardDisplayStats {
    if (!stats) {
      return {};
    }

    const seasonColor: DashboardStatColor = stats.seasonProgress > 75 ? 'warn' : 'primary';

    return {
      totalTeams: {
        value: this.formatNumber(stats.totalTeams),
        label: this.t.t('dashboard.labels.activeTeams'),
        color: 'primary'
      },
      totalPlayers: {
        value: this.formatNumber(stats.totalPlayers),
        label: this.t.t('dashboard.labels.players'),
        color: 'accent'
      },
      totalPoints: {
        value: this.formatCompactNumber(stats.totalPoints),
        label: this.t.t('dashboard.labels.totalPoints'),
        color: 'primary'
      },
      averagePointsPerTeam: {
        value: this.formatNumber(stats.averagePointsPerTeam),
        label: this.t.t('dashboard.labels.averagePointsPerTeam'),
        color: 'accent'
      },
      seasonProgress: {
        value: `${this.formatNumber(stats.seasonProgress)}%`,
        label: this.t.t('dashboard.labels.seasonProgress'),
        color: seasonColor
      }
    };
  }

  private formatNumber(value: number, options?: Intl.NumberFormatOptions): string {
    const locale = this.getNumberLocale();
    return new Intl.NumberFormat(locale, options).format(value);
  }

  private formatCompactNumber(value: number): string {
    return this.formatNumber(value, { notation: 'compact', compactDisplay: 'short' });
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
