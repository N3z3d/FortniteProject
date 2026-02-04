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

  describe('formatNumber edge cases', () => {
    it('returns "0" when value is null', () => {
      expect(service.formatNumber(null)).toBe('0');
    });

    it('returns "0" when value is undefined', () => {
      expect(service.formatNumber(undefined)).toBe('0');
    });

    it('formats zero correctly', () => {
      const formatted = service.formatNumber(0);
      expect(formatted).toBe('0');
    });

    it('formats negative numbers correctly', () => {
      translationService.setLanguage('en');
      const formatted = service.formatNumber(-1234);
      const expected = new Intl.NumberFormat('en-US').format(-1234);
      expect(formatted).toBe(expected);
    });

    it('formats large numbers correctly', () => {
      translationService.setLanguage('fr');
      const formatted = service.formatNumber(1234567);
      const expected = new Intl.NumberFormat('fr-FR').format(1234567);
      expect(formatted).toBe(expected);
    });

    it('uses French locale by default', () => {
      const formatted = service.formatNumber(1000);
      const expected = new Intl.NumberFormat('fr-FR').format(1000);
      expect(formatted).toBe(expected);
    });

    it('uses English locale when language is en', () => {
      translationService.setLanguage('en');
      const formatted = service.formatNumber(1000);
      const expected = new Intl.NumberFormat('en-US').format(1000);
      expect(formatted).toBe(expected);
    });

    it('uses Portuguese locale when language is pt', () => {
      translationService.setLanguage('pt');
      const formatted = service.formatNumber(1000);
      const expected = new Intl.NumberFormat('pt-PT').format(1000);
      expect(formatted).toBe(expected);
    });
  });

  describe('getParticipantDisplayCount edge cases', () => {
    it('returns 0 when game is null', () => {
      expect(service.getParticipantDisplayCount(null, 0)).toBe(0);
    });

    it('uses fallbackTeams when game has no data', () => {
      const game = {} as any;
      expect(service.getParticipantDisplayCount(game, 5)).toBe(5);
    });

    it('returns participantCount when it is the largest', () => {
      const game = {
        participantCount: 10,
        teams: [{ id: 't1' }],
        participants: [{ id: 'p1' }]
      } as any;
      expect(service.getParticipantDisplayCount(game, 2)).toBe(10);
    });

    it('returns participants length when it is the largest', () => {
      const game = {
        participantCount: 1,
        teams: [{ id: 't1' }],
        participants: [{ id: 'p1' }, { id: 'p2' }, { id: 'p3' }, { id: 'p4' }]
      } as any;
      expect(service.getParticipantDisplayCount(game, 2)).toBe(4);
    });

    it('handles undefined teams array', () => {
      const game = {
        participantCount: 2,
        teams: undefined,
        participants: undefined
      } as any;
      expect(service.getParticipantDisplayCount(game, 5)).toBe(5);
    });

    it('handles empty arrays', () => {
      const game = {
        participantCount: 0,
        teams: [],
        participants: []
      } as any;
      expect(service.getParticipantDisplayCount(game, 3)).toBe(3);
    });

    it('ignores negative fallback', () => {
      const game = {
        participantCount: 2
      } as any;
      expect(service.getParticipantDisplayCount(game, -5)).toBe(2);
    });
  });

  describe('displayTeamName edge cases', () => {
    it('returns empty string when rawName is null', () => {
      expect(service.displayTeamName(null)).toBe('');
    });

    it('returns empty string when rawName is undefined', () => {
      expect(service.displayTeamName(undefined)).toBe('');
    });

    it('returns empty string when rawName is empty', () => {
      expect(service.displayTeamName('')).toBe('');
    });

    it('removes prefix with accent É', () => {
      expect(service.displayTeamName('Équipe des Sarah')).toBe('Sarah');
    });

    it('removes prefix case-insensitively', () => {
      expect(service.displayTeamName('EQUIPE DES TEDDY')).toBe('TEDDY');
    });

    it('handles team name without prefix', () => {
      expect(service.displayTeamName('Team Alpha')).toBe('Team Alpha');
    });

    it('trims whitespace after removing prefix', () => {
      expect(service.displayTeamName('Equipe des   Marcel  ')).toBe('Marcel');
    });

    it('handles prefix with extra spaces', () => {
      expect(service.displayTeamName('Equipe des      Team')).toBe('Team');
    });

    it('keeps content after prefix intact', () => {
      expect(service.displayTeamName('Equipe des Winners 2024')).toBe('Winners 2024');
    });
  });
});
