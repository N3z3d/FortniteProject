import { TestBed } from '@angular/core/testing';
import { TranslationService } from './translation.service';

describe('TranslationService (en)', () => {
  let service: TranslationService;

  beforeEach(() => {
    localStorage.removeItem('app_language');
    TestBed.configureTestingModule({
      providers: [TranslationService]
    });
    service = TestBed.inject(TranslationService);
  });

  it('returns English common translations when available', () => {
    service.setLanguage('en');

    expect(service.translate('common.save')).toBe('Save');
    expect(service.translate('common.cancel')).toBe('Cancel');
    expect(service.translate('common.loading')).toBe('Loading...');
  });

  it('returns English navigation and games translations when available', () => {
    service.setLanguage('en');

    expect(service.translate('navigation.home')).toBe('Home');
    expect(service.translate('games.myGames')).toBe('My games');
    expect(service.translate('games.joinGame')).toBe('Join a game');
    expect(service.translate('games.status.completed')).toBe('Completed');
  });

  it('returns English dashboard translations when available', () => {
    service.setLanguage('en');

    expect(service.translate('dashboard.loading')).toBe('Loading dashboard...');
    expect(service.translate('dashboard.labels.currentRanking')).toBe('Current ranking');
  });

  it('returns English trades translations when available', () => {
    service.setLanguage('en');

    expect(service.translate('trades.title')).toBe('Trading Hub');
    expect(service.translate('trades.filters.all')).toBe('All Trades');
    expect(service.translate('trades.tabs.pending')).toBe('Pending');
  });
});
