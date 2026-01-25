import { TestBed } from '@angular/core/testing';
import { TranslationService } from './translation.service';

describe('TranslationService (fr)', () => {
  let service: TranslationService;

  beforeEach(() => {
    localStorage.removeItem('app_language');
    TestBed.configureTestingModule({
      providers: [TranslationService]
    });
    service = TestBed.inject(TranslationService);
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
