import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TranslationService, Translations } from './translation.service';

describe('TranslationService (en)', () => {
  let service: TranslationService;
  let httpMock: HttpTestingController;

  const mockTranslationsEn: Translations = {
    common: {
      save: 'Save',
      cancel: 'Cancel',
      loading: 'Loading...'
    },
    navigation: {
      home: 'Home'
    },
    games: {
      myGames: 'My games',
      joinGame: 'Join a game',
      home: {
        myGames: 'My games',
        joinWithCode: 'Join with code',
        statusCreating: 'Creating'
      },
      status: {
        completed: 'Completed'
      }
    },
    dashboard: {
      loading: 'Loading dashboard...',
      labels: {
        currentRanking: 'Current ranking'
      }
    },
    trades: {
      title: 'Trading Hub',
      filters: {
        all: 'All Trades'
      },
      tabs: {
        pending: 'Pending'
      }
    }
  };

  const mockTranslationsOther: Translations = {
    common: {},
    navigation: {},
    games: {},
    dashboard: {},
    trades: {}
  };

  beforeEach(() => {
    localStorage.removeItem('app_language');
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TranslationService]
    });
    service = TestBed.inject(TranslationService);
    httpMock = TestBed.inject(HttpTestingController);

    // Mock HTTP requests for all languages
    const requests = httpMock.match(req => req.url.includes('assets/i18n/'));
    requests.forEach(req => {
      const lang = req.request.url.split('/').pop()?.replace('.json', '') || 'en';
      req.flush(lang === 'en' ? mockTranslationsEn : mockTranslationsOther);
    });
  });

  afterEach(() => {
    httpMock.verify();
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

  it('returns English games home translations when available', () => {
    service.setLanguage('en');

    expect(service.translate('games.home.myGames')).toBe('My games');
    expect(service.translate('games.home.joinWithCode')).toBe('Join with code');
    expect(service.translate('games.home.statusCreating')).toBe('Creating');
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
