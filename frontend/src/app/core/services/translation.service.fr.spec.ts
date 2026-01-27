import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TranslationService, Translations } from './translation.service';

describe('TranslationService (fr)', () => {
  let service: TranslationService;
  let httpMock: HttpTestingController;

  const mockTranslationsFr: Translations = {
    common: {
      save: 'Enregistrer',
      cancel: 'Annuler',
      loading: 'Chargement...'
    },
    navigation: {
      home: 'Accueil'
    },
    games: {
      myGames: 'Mes parties',
      joinGame: 'Rejoindre une partie',
      status: {
        completed: 'Terminée'
      }
    },
    dashboard: {
      loading: 'Chargement du tableau de bord...',
      labels: {
        currentRanking: 'Classement actuel'
      }
    },
    trades: {
      title: 'Centre d\'Échanges',
      filters: {
        all: 'Tous les échanges'
      },
      tabs: {
        pending: 'En attente'
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
    const requests = httpMock.match(req => req.url.includes('/assets/i18n/'));
    requests.forEach(req => {
      const lang = req.url.split('/').pop()?.replace('.json', '') || 'en';
      req.flush(lang === 'fr' ? mockTranslationsFr : mockTranslationsOther);
    });
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('returns French common translations when available', () => {
    service.setLanguage('fr');

    expect(service.translate('common.save')).toBe('Enregistrer');
    expect(service.translate('common.cancel')).toBe('Annuler');
    expect(service.translate('common.loading')).toBe('Chargement...');
  });

  it('returns French navigation and games translations when available', () => {
    service.setLanguage('fr');

    expect(service.translate('navigation.home')).toBe('Accueil');
    expect(service.translate('games.myGames')).toBe('Mes parties');
    expect(service.translate('games.joinGame')).toBe('Rejoindre une partie');
    expect(service.translate('games.status.completed')).toBe('Terminée');
  });

  it('returns French dashboard translations when available', () => {
    service.setLanguage('fr');

    expect(service.translate('dashboard.loading')).toBe('Chargement du tableau de bord...');
    expect(service.translate('dashboard.labels.currentRanking')).toBe('Classement actuel');
  });

  it('returns French trades translations when available', () => {
    service.setLanguage('fr');

    expect(service.translate('trades.title')).toBe('Centre d\'Échanges');
    expect(service.translate('trades.filters.all')).toBe('Tous les échanges');
    expect(service.translate('trades.tabs.pending')).toBe('En attente');
  });
});
