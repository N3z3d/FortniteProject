import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { TranslationService, Translations } from './translation.service';
import { LoggerService } from './logger.service';

declare const require: (path: string) => unknown;

describe('TranslationService', () => {
  let service: TranslationService;
  let httpMock: HttpTestingController;
  let loggerSpy: jasmine.SpyObj<LoggerService>;

  const mockTranslations: Record<string, Translations> = {
    fr: {
      dashboard: { loading: 'Chargement du tableau de bord...' },
      common: { yes: 'Oui', no: 'Non' }
    },
    en: {
      dashboard: { loading: 'Loading dashboard...' },
      common: { yes: 'Yes', no: 'No' }
    },
    es: {
      common: { yes: 'Sí', no: 'No' }
    },
    pt: {
      common: { yes: 'Sim', no: 'Não' }
    }
  };

  const realI18nTranslations: Record<string, Translations> = {
    fr: require('../../../assets/i18n/fr.json') as Translations,
    en: require('../../../assets/i18n/en.json') as Translations,
    es: require('../../../assets/i18n/es.json') as Translations,
    pt: require('../../../assets/i18n/pt.json') as Translations
  };

  const flattenTranslations = (node: unknown, prefix = '', out: Record<string, string> = {}): Record<string, string> => {
    if (!node || typeof node !== 'object') {
      return out;
    }

    for (const [key, value] of Object.entries(node as Record<string, unknown>)) {
      const fullKey = prefix ? `${prefix}.${key}` : key;
      if (value && typeof value === 'object' && !Array.isArray(value)) {
        flattenTranslations(value, fullKey, out);
        continue;
      }
      out[fullKey] = String(value);
    }

    return out;
  };

  const setupService = (responder?: (lang: string, req: TestRequest) => void): void => {
    localStorage.removeItem('app_language');
    loggerSpy = jasmine.createSpyObj<LoggerService>('LoggerService', ['debug', 'info', 'warn', 'error']);
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        TranslationService,
        { provide: LoggerService, useValue: loggerSpy }
      ]
    });
    service = TestBed.inject(TranslationService);
    httpMock = TestBed.inject(HttpTestingController);

    // Mock HTTP requests for all languages
    const requests = httpMock.match(req => req.url.includes('assets/i18n/'));
    requests.forEach(req => {
      const lang = req.request.url.split('/').pop()?.replace('.json', '') || 'en';
      if (responder) {
        responder(lang, req);
        return;
      }
      req.flush(mockTranslations[lang] || {});
    });
  };

  beforeEach(() => {
    setupService();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('falls back to English when translation is missing in the current language', () => {
    service.setLanguage('es');

    expect(service.translate('dashboard.loading')).toBe('Loading dashboard...');
  });

  it('returns the fallback when missing in all languages', () => {
    service.setLanguage('es');

    expect(service.translate('missing.key', 'fallback')).toBe('fallback');
  });

  it('keeps available translations when one language fails to load', () => {
    TestBed.resetTestingModule();
    setupService((lang, req) => {
      if (lang === 'pt') {
        req.flush('Not found', { status: 404, statusText: 'Not Found' });
        return;
      }
      req.flush(mockTranslations[lang] || {});
    });

    service.setLanguage('en');

    expect(service.translate('common.yes')).toBe('Yes');
    expect(loggerSpy.error).toHaveBeenCalledWith('TranslationService: failed to load language file', jasmine.objectContaining({
      lang: 'pt'
    }));
  });

  it('does not expose mojibake sequences in i18n JSON files', () => {
    const translations = realI18nTranslations as Record<string, unknown>;
    const mojibakeMarkers = [
      '\u00c3\u00a0', // sample
      '\u00c3\u00a1', // sample
      '\u00c3\u00a2', // sample
      '\u00c3\u00a3', // sample
      '\u00c3\u00a8', // sample
      '\u00c3\u00a9', // sample
      '\u00c3\u00aa', // sample
      '\u00c3\u00a7', // sample
      '\u00c3\u00b4', // sample
      '\u00c3\u00b5', // sample
      '\u00c3\u00bb', // sample
      '\u00c3\u00b9', // sample
      '\u00c3\u00ae', // sample
      '\u00c3\u0080', // sample
      '\u00c3\u0089', // sample
      '\u00c3\u008a', // sample
      '\u00c3\u009a', // sample
      '\u00c3\u00b3', // sample
      '\u00c3\u00ad', // sample
      '\u00c3\u00ba', // sample
      '\u00c3\u00bc', // sample
      '\u00c3\u00b1', // sample
      '\u00c2\u00a9', // sample
      '\u00c2\u00bf', // sample
      '\u00c2\u00a1', // sample
      '\u00c2\u00a3', // sample
      '\u00c2\u00b1', // sample
      '\u00c2\u00a7', // sample
      '\u00c2\u00b5', // sample
      '\u00c2\u00aa', // sample
      '\u00c2\u00ba', // sample
      '\u00c2\u00b3', // sample
      '\u00c2\u00ad', // sample
      '\u00e2\u0080\u0099', // sample
      '\u00e2\u0080\u009c', // sample
      '\u00e2\u0080\u009d', // sample
      '\u00e2\u0080\u0093', // sample
      '\u00e2\u0080\u0094', // sample
      '\u00e2\u0080\u00a6', // sample
      '\u00e2\u0080\u00a2', // sample
      '\u00e2\u0080\u00b0', // sample
      '\u00e2\u0086\u0092', // sample
      '\u00e2\u0086\u0094', // sample
      '\u00e2\u009a\u00a1', // sample
      '\u00e2\u009a\u00a0', // sample
      '\u00e2\u009c\u0093', // sample
      '\u00e2\u009c\u0085', // sample
      '\u00e2\u008f\u00b1', // sample
      '\u00e2\u008f\u00b3', // sample
      '\u00e2\u009d\u008c', // sample
      '\u00e2\u009d\u0093', // sample
      '\u00e2\u0096\u00b2', // sample
      '\u00e2\u0096\u00bc', // sample
      '\u00e2\u0096\u00b6', // sample
      '\u00e2\u00ad\u0090', // sample
      '\u00e2\u0099\u0082', // sample
      '\u00e2\u009e\u00a1', // sample
      '\u00f0\u009f', // sample
      '\u00d2', // sample
      '\u008f', // sample
      '\ufffd', // sample
      '\u001a', // sample
      '\u0481', // sample
      '\u04a0', // sample
      '\u04a1', // sample
      '\u04a3', // sample
      '\u04a7', // sample
      '\u04a9', // sample
      '\u2b1d', // sample
      '\u2b20', // sample
      '\u2b21', // sample
      '\u2b30' // ? artifact observed in corrupted files
    ];

    const regex = new RegExp(mojibakeMarkers.join('|'));
    const hits: string[] = [];

    const visit = (node: unknown, path: string): void => {
      if (typeof node === 'string') {
        if (regex.test(node)) {
          hits.push(`${path}: ${node}`);
        }
        return;
      }

      if (!node || typeof node !== 'object') {
        return;
      }

      for (const [key, value] of Object.entries(node as Record<string, unknown>)) {
        visit(value, path ? `${path}.${key}` : key);
      }
    };

    visit(translations, '');

    expect(hits).withContext(`Mojibake detected:\\n${hits.slice(0, 10).join('\\n')}`).toEqual([]);
  });

  it('keeps the same translation key set across all supported languages', () => {
    const flattened = {
      en: flattenTranslations(realI18nTranslations['en']),
      fr: flattenTranslations(realI18nTranslations['fr']),
      es: flattenTranslations(realI18nTranslations['es']),
      pt: flattenTranslations(realI18nTranslations['pt'])
    };
    const baseKeys = Object.keys(flattened.en).sort();

    expect(Object.keys(flattened.fr).sort()).toEqual(baseKeys);
    expect(Object.keys(flattened.es).sort()).toEqual(baseKeys);
    expect(Object.keys(flattened.pt).sort()).toEqual(baseKeys);
  });

  it('contains the critical invitation and join dialog keys in all languages', () => {
    const requiredKeys = [
      'games.detail.regenerateDialog.title',
      'games.detail.regenerateDialog.subtitle',
      'games.detail.regenerateDialog.confirm',
      'games.detail.regenerateDialog.generateTitle',
      'games.detail.regenerateDialog.generateSubtitle',
      'games.detail.regenerateDialog.generateConfirm',
      'games.joinDialog.invalidCode',
      'games.joinDialog.invalidCodeFormat',
      'games.joinDialog.invalidOrUnavailableCode',
      'games.joinDialog.tooManyAttempts',
      'games.joinDialog.alreadyInGame'
    ];

    const flattened = {
      en: flattenTranslations(realI18nTranslations['en']),
      fr: flattenTranslations(realI18nTranslations['fr']),
      es: flattenTranslations(realI18nTranslations['es']),
      pt: flattenTranslations(realI18nTranslations['pt'])
    };

    for (const key of requiredKeys) {
      expect(flattened.en[key]).withContext(`Missing key in en: ${key}`).toBeDefined();
      expect(flattened.fr[key]).withContext(`Missing key in fr: ${key}`).toBeDefined();
      expect(flattened.es[key]).withContext(`Missing key in es: ${key}`).toBeDefined();
      expect(flattened.pt[key]).withContext(`Missing key in pt: ${key}`).toBeDefined();
    }
  });

  describe('per-user language isolation', () => {
    beforeEach(() => {
      localStorage.clear();
    });

    it('stores language per user when userId is set', () => {
      service.setCurrentUserId('user-1');
      service.setLanguage('en');

      expect(localStorage.getItem('app_language_user-1')).toBe('en');
      expect(localStorage.getItem('app_language')).toBe('en');
    });

    it('restores user-specific language when setting userId', () => {
      localStorage.setItem('app_language_user-2', 'es');
      service.setCurrentUserId('user-2');

      expect(service.currentLanguage).toBe('es');
    });

    it('does not change language when user has no saved preference', () => {
      service.setLanguage('en');
      service.setCurrentUserId('user-3');

      expect(service.currentLanguage).toBe('en');
    });

    it('isolates language between users', () => {
      service.setCurrentUserId('user-1');
      service.setLanguage('en');

      service.setCurrentUserId('user-2');
      service.setLanguage('pt');

      service.setCurrentUserId('user-1');
      expect(service.currentLanguage).toBe('en');

      service.setCurrentUserId('user-2');
      expect(service.currentLanguage).toBe('pt');
    });

    it('falls back to global language when userId is null', () => {
      service.setLanguage('es');
      service.setCurrentUserId(null);

      expect(service.currentLanguage).toBe('es');
      expect(localStorage.getItem('app_language')).toBe('es');
    });

    it('only stores to global key when no userId is set', () => {
      service.setCurrentUserId(null);
      service.setLanguage('pt');

      expect(localStorage.getItem('app_language')).toBe('pt');
      expect(localStorage.getItem('app_language_null')).toBeNull();
    });
  });
});
