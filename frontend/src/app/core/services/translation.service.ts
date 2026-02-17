import { Injectable, Optional } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, forkJoin, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { LoggerService } from './logger.service';

export type SupportedLanguage = 'fr' | 'en' | 'es' | 'pt';

export interface Translations {
  [key: string]: string | Translations;
}

@Injectable({
  providedIn: 'root'
})
export class TranslationService {
  private static readonly LANG_KEY_PREFIX = 'app_language_';
  private static readonly LANG_KEY_GLOBAL = 'app_language';
  private static readonly SUPPORTED: SupportedLanguage[] = ['fr', 'en', 'es', 'pt'];

  private currentLang$ = new BehaviorSubject<SupportedLanguage>('fr');
  private translations: Record<SupportedLanguage, Translations> = {
    fr: {},
    en: {},
    es: {},
    pt: {}
  };
  private translationsLoaded$ = new BehaviorSubject<boolean>(false);
  private currentUserId: string | null = null;

  constructor(@Optional() private readonly http?: HttpClient, @Optional() private readonly logger?: LoggerService) {
    this.loadTranslations();
    this.restoreLanguagePreference();
  }

  get language$(): Observable<SupportedLanguage> {
    return this.currentLang$.asObservable();
  }

  get currentLanguage(): SupportedLanguage {
    return this.currentLang$.value;
  }

  get translationsLoaded(): Observable<boolean> {
    return this.translationsLoaded$.asObservable();
  }

  setLanguage(lang: SupportedLanguage): void {
    this.currentLang$.next(lang);
    if (this.currentUserId) {
      localStorage.setItem(this.getUserLangKey(this.currentUserId), lang);
    }
    localStorage.setItem(TranslationService.LANG_KEY_GLOBAL, lang);
  }

  setCurrentUserId(userId: string | null): void {
    this.currentUserId = userId;
    if (userId) {
      const userLang = localStorage.getItem(this.getUserLangKey(userId)) as SupportedLanguage;
      if (userLang && TranslationService.SUPPORTED.includes(userLang)) {
        this.currentLang$.next(userLang);
      }
    }
  }

  translate(key: string, fallback?: string): string {
    const direct = this.resolveTranslation(this.currentLang$.value, key);
    if (direct !== undefined) {
      return direct;
    }

    const englishFallback = this.resolveTranslation('en', key);
    if (englishFallback !== undefined) {
      return englishFallback;
    }

    return fallback || key;
  }

  t(key: string, fallback?: string): string {
    return this.translate(key, fallback);
  }

  private resolveTranslation(lang: SupportedLanguage, key: string): string | undefined {
    const keys = key.split('.');
    let result: string | Translations = this.translations[lang];

    for (const k of keys) {
      if (result && typeof result === 'object' && k in result) {
        result = result[k];
      } else {
        return undefined;
      }
    }

    return typeof result === 'string' ? result : undefined;
  }

  private restoreLanguagePreference(): void {
    const savedLang = localStorage.getItem(TranslationService.LANG_KEY_GLOBAL) as SupportedLanguage;
    if (savedLang && TranslationService.SUPPORTED.includes(savedLang)) {
      this.currentLang$.next(savedLang);
    }
  }

  private getUserLangKey(userId: string): string {
    return TranslationService.LANG_KEY_PREFIX + userId;
  }

  private loadTranslations(): void {
    const http = this.http;
    if (!http) {
      this.translationsLoaded$.next(false);
      return;
    }
    const languages = TranslationService.SUPPORTED;
    const requests = languages.map(lang =>
      http.get<Translations>(`assets/i18n/${lang}.json`).pipe(
        catchError(error => {
          this.logger?.error('TranslationService: failed to load language file', { lang, error });
          return of({});
        })
      )
    );

    forkJoin(requests)
      .pipe(
        tap(results => {
          let hasTranslations = false;
          languages.forEach((lang, index) => {
            const loaded = results[index] || {};
            this.translations[lang] = loaded;
            if (Object.keys(loaded).length > 0) {
              hasTranslations = true;
            }
          });
          this.translationsLoaded$.next(hasTranslations);
        })
      )
      .subscribe();
  }
}
