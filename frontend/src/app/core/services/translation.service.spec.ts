import { TestBed } from '@angular/core/testing';
import { TranslationService } from './translation.service';

describe('TranslationService', () => {
  let service: TranslationService;

  beforeEach(() => {
    localStorage.removeItem('app_language');
    TestBed.configureTestingModule({
      providers: [TranslationService]
    });
    service = TestBed.inject(TranslationService);
  });

  it('falls back to English when translation is missing in the current language', () => {
    service.setLanguage('es');

    expect(service.translate('dashboard.loading')).toBe('Loading dashboard...');
  });

  it('returns the translated value when available', () => {
    service.setLanguage('es');

    expect(service.translate('navigation.home')).toBe('Inicio');
  });

  it('returns the fallback when missing in all languages', () => {
    service.setLanguage('es');

    expect(service.translate('missing.key', 'fallback')).toBe('fallback');
  });
});
