import { DashboardFormattingService } from './dashboard-formatting.service';
import { TranslationService } from '../../../core/services/translation.service';

describe('DashboardFormattingService', () => {
  let service: DashboardFormattingService;
  let translationService: TranslationService;

  beforeEach(() => {
    localStorage.removeItem('app_language');
    translationService = new TranslationService();
    service = new DashboardFormattingService(translationService);
  });

  it('formats numbers using the current locale', () => {
    translationService.setLanguage('es');

    const formatted = service.formatNumber(1234);
    const expected = new Intl.NumberFormat('es-ES').format(1234);

    expect(formatted).toBe(expected);
  });

  it('returns the largest participant count available', () => {
    const game = {
      participantCount: 2,
      teams: [{ id: 't1' }, { id: 't2' }, { id: 't3' }],
      participants: [{ id: 'p1' }]
    } as any;

    expect(service.getParticipantDisplayCount(game, 1)).toBe(3);
  });

  it('removes the "Equipe des" prefix', () => {
    expect(service.displayTeamName('Equipe des Marcel')).toBe('Marcel');
  });
});
