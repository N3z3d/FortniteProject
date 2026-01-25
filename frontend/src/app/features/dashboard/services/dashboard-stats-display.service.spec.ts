import { DashboardStatsDisplayService } from './dashboard-stats-display.service';
import { TranslationService } from '../../../core/services/translation.service';

describe('DashboardStatsDisplayService', () => {
  let service: DashboardStatsDisplayService;
  let translationService: TranslationService;

  const sampleStats = {
    totalTeams: 1200,
    totalPlayers: 3456,
    totalPoints: 1234567,
    averagePointsPerTeam: 1024,
    seasonProgress: 82.5
  };

  beforeEach(() => {
    localStorage.removeItem('app_language');
    translationService = new TranslationService();
    service = new DashboardStatsDisplayService(translationService);
  });

  it('uses translated labels for stats', () => {
    translationService.setLanguage('en');

    const result = service.formatStatsForDisplay(sampleStats);

    expect(result['totalTeams'].label).toBe(translationService.t('dashboard.labels.activeTeams'));
    expect(result['averagePointsPerTeam'].label).toBe(translationService.t('dashboard.labels.averagePointsPerTeam'));
    expect(result['seasonProgress'].label).toBe(translationService.t('dashboard.labels.seasonProgress'));
  });

  it('formats numbers per locale for supported languages', () => {
    const cases = [
      { lang: 'fr' as const, locale: 'fr-FR' },
      { lang: 'en' as const, locale: 'en-US' },
      { lang: 'es' as const, locale: 'es-ES' },
      { lang: 'pt' as const, locale: 'pt-PT' }
    ];

    cases.forEach(({ lang, locale }) => {
      translationService.setLanguage(lang);

      const result = service.formatStatsForDisplay(sampleStats);
      const expectedTeams = new Intl.NumberFormat(locale).format(sampleStats.totalTeams);
      const expectedPoints = new Intl.NumberFormat(locale, {
        notation: 'compact',
        compactDisplay: 'short'
      }).format(sampleStats.totalPoints);

      expect(result['totalTeams'].value).toBe(expectedTeams);
      expect(result['totalPoints'].value).toBe(expectedPoints);
    });
  });

  it('adds percent and warns when season progress is high', () => {
    translationService.setLanguage('fr');

    const result = service.formatStatsForDisplay(sampleStats);
    const expected = new Intl.NumberFormat('fr-FR').format(sampleStats.seasonProgress);

    expect(result['seasonProgress'].value).toBe(`${expected}%`);
    expect(result['seasonProgress'].color).toBe('warn');
  });

  it('returns empty stats when input is missing', () => {
    expect(service.formatStatsForDisplay(null)).toEqual({});
  });
});
