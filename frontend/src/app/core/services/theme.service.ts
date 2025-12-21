import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { BehaviorSubject, Observable } from 'rxjs';

/**
 * Theme types - simplified to dark/light only (FF-204)
 * Removed "system" option as requested
 */
export type Theme = 'dark' | 'light';

/**
 * Theme Service (SRP - Single Responsibility Principle)
 * Responsible ONLY for theme management
 *
 * Features:
 * - Apply theme to DOM (add/remove CSS classes)
 * - Persist theme preference to localStorage
 * - Reactive updates via Observable
 * - SSR-safe (checks platform)
 *
 * FF-204: Simplified theme options (only dark/light)
 */
@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly STORAGE_KEY = 'user-theme-preference';
  private readonly DEFAULT_THEME: Theme = 'dark';

  private themeSubject: BehaviorSubject<Theme>;
  public theme$: Observable<Theme>;

  constructor(@Inject(PLATFORM_ID) private platformId: Object) {
    // Initialize with saved theme or default
    const savedTheme = this.loadThemeFromStorage();
    this.themeSubject = new BehaviorSubject<Theme>(savedTheme);
    this.theme$ = this.themeSubject.asObservable();

    // Apply theme immediately
    this.applyTheme(savedTheme);
  }

  /**
   * Get current theme (synchronous)
   * @returns Current theme
   */
  getCurrentTheme(): Theme {
    return this.themeSubject.value;
  }

  /**
   * Set theme and apply to DOM
   * @param theme - Theme to apply
   */
  setTheme(theme: Theme): void {
    if (theme !== 'dark' && theme !== 'light') {
      console.warn(`Invalid theme "${theme}", falling back to ${this.DEFAULT_THEME}`);
      theme = this.DEFAULT_THEME;
    }

    this.applyTheme(theme);
    this.saveThemeToStorage(theme);
    this.themeSubject.next(theme);
  }

  /**
   * Toggle between dark and light
   */
  toggleTheme(): void {
    const newTheme: Theme = this.getCurrentTheme() === 'dark' ? 'light' : 'dark';
    this.setTheme(newTheme);
  }

  /**
   * Apply theme to DOM by adding/removing CSS classes
   * @param theme - Theme to apply
   */
  private applyTheme(theme: Theme): void {
    if (!isPlatformBrowser(this.platformId)) {
      return; // Skip on server-side rendering
    }

    const body = document.body;

    // Remove all theme classes
    body.classList.remove('dark-theme', 'light-theme');

    // Add the selected theme class
    body.classList.add(`${theme}-theme`);

    // Optional: Also set data attribute for CSS targeting
    body.setAttribute('data-theme', theme);
  }

  /**
   * Load theme from localStorage
   * @returns Saved theme or default
   */
  private loadThemeFromStorage(): Theme {
    if (!isPlatformBrowser(this.platformId)) {
      return this.DEFAULT_THEME;
    }

    try {
      const saved = localStorage.getItem(this.STORAGE_KEY);
      if (saved === 'dark' || saved === 'light') {
        return saved;
      }
    } catch (error) {
      console.warn('Failed to load theme from localStorage:', error);
    }

    return this.DEFAULT_THEME;
  }

  /**
   * Save theme to localStorage
   * @param theme - Theme to save
   */
  private saveThemeToStorage(theme: Theme): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    try {
      localStorage.setItem(this.STORAGE_KEY, theme);
    } catch (error) {
      console.warn('Failed to save theme to localStorage:', error);
    }
  }

  /**
   * Check if dark theme is active
   * @returns boolean
   */
  isDarkTheme(): boolean {
    return this.getCurrentTheme() === 'dark';
  }

  /**
   * Check if light theme is active
   * @returns boolean
   */
  isLightTheme(): boolean {
    return this.getCurrentTheme() === 'light';
  }
}
