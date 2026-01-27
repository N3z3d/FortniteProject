import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, forkJoin } from 'rxjs';
import { tap } from 'rxjs/operators';

export type SupportedLanguage = 'fr' | 'en' | 'es' | 'pt';

export interface Translations {
  [key: string]: string | Translations;
}

@Injectable({
  providedIn: 'root'
})
export class TranslationService {
  private readonly http = inject(HttpClient);
  private currentLang$ = new BehaviorSubject<SupportedLanguage>('fr');
  private translations: Record<SupportedLanguage, Translations> = {
    fr: {},
    en: {},
    es: {},
    pt: {}
  };
  private translationsLoaded$ = new BehaviorSubject<boolean>(false);

  constructor() {
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
    localStorage.setItem('app_language', lang);
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
    const savedLang = localStorage.getItem('app_language') as SupportedLanguage;
    if (savedLang && ['fr', 'en', 'es', 'pt'].includes(savedLang)) {
      this.currentLang$.next(savedLang);
    }
  }

  private loadTranslations(): void {
    const languages: SupportedLanguage[] = ['fr', 'en', 'es', 'pt'];
    const requests = languages.map(lang =>
      this.http.get<Translations>(`/assets/i18n/${lang}.json`)
    );

    forkJoin(requests).pipe(
      tap(results => {
        languages.forEach((lang, index) => {
          this.translations[lang] = results[index];
        });
        this.translationsLoaded$.next(true);
      })
    ).subscribe({
      error: (error) => {
        console.error('Failed to load translations:', error);
        this.translationsLoaded$.next(false);
      }
    });
  }
}
