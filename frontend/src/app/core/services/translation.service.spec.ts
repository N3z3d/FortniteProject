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

  it('returns the fallback when missing in all languages', () => {
    service.setLanguage('es');

    expect(service.translate('missing.key', 'fallback')).toBe('fallback');
  });

  it('does not expose mojibake sequences in translations', () => {
    const translations = (service as unknown as { translations: Record<string, unknown> }).translations;
    const mojibakeMarkers = [
      '\u00c3\u00a0', // à
      '\u00c3\u00a1', // á
      '\u00c3\u00a2', // â
      '\u00c3\u00a3', // ã
      '\u00c3\u00a8', // è
      '\u00c3\u00a9', // é
      '\u00c3\u00aa', // ê
      '\u00c3\u00a7', // ç
      '\u00c3\u00b4', // ô
      '\u00c3\u00b5', // õ
      '\u00c3\u00bb', // û
      '\u00c3\u00b9', // ù
      '\u00c3\u00ae', // î
      '\u00c3\u0080', // Ã€
      '\u00c3\u0089', // É
      '\u00c3\u008a', // ÃŠ
      '\u00c3\u009a', // Ú
      '\u00c3\u00b3', // ó
      '\u00c3\u00ad', // í
      '\u00c3\u00ba', // ú
      '\u00c3\u00bc', // ü
      '\u00c3\u00b1', // ñ
      '\u00c2\u00a9', // ©
      '\u00c2\u00bf', // ¿
      '\u00c2\u00a1', // ¡
      '\u00c2\u00a3', // £
      '\u00c2\u00b1', // ±
      '\u00c2\u00a7', // §
      '\u00c2\u00b5', // µ
      '\u00c2\u00aa', // ª
      '\u00c2\u00ba', // º
      '\u00c2\u00b3', // ³
      '\u00c2\u00ad', // ­
      '\u00e2\u0080\u0099', // â€™
      '\u00e2\u0080\u009c', // â€œ
      '\u00e2\u0080\u009d', // â€
      '\u00e2\u0080\u0093', // â€“
      '\u00e2\u0080\u0094', // â—
      '\u00e2\u0080\u00a6', // â€¦
      '\u00e2\u0080\u00a2', // â€¢
      '\u00e2\u0080\u00b0', // â€°
      '\u00e2\u0086\u0092', // →
      '\u00e2\u0086\u0094', // ↔
      '\u00e2\u009a\u00a1', // ⚡
      '\u00e2\u009a\u00a0', // âš 
      '\u00e2\u009c\u0093', // âœ“
      '\u00e2\u009c\u0085', // âœ…
      '\u00e2\u008f\u00b1', // ⏱
      '\u00e2\u008f\u00b3', // ⏳
      '\u00e2\u009d\u008c', // âŒ
      '\u00e2\u009d\u0093', // â“
      '\u00e2\u0096\u00b2', // â–²
      '\u00e2\u0096\u00bc', // â–¼
      '\u00e2\u0096\u00b6', // â–¶
      '\u00e2\u00ad\u0090', // ⭐
      '\u00e2\u0099\u0082', // â™‚
      '\u00e2\u009e\u00a1', // âž¡
      '\u00f0\u009f' // ðŸ (emoji mojibake prefix)
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
});
