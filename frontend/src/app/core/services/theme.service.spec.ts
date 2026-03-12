import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme.service';
import { LoggerService } from './logger.service';

describe('ThemeService', () => {
  const storageKey = 'user-theme-preference';
  let loggerSpy: jasmine.SpyObj<LoggerService>;

  const resetDom = () => {
    document.body.classList.remove('dark-theme', 'light-theme');
    document.body.removeAttribute('data-theme');
  };

  const createService = (platformId: Object) => {
    loggerSpy = jasmine.createSpyObj<LoggerService>('LoggerService', ['debug', 'info', 'warn', 'error']);
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        ThemeService,
        { provide: PLATFORM_ID, useValue: platformId },
        { provide: LoggerService, useValue: loggerSpy }
      ]
    });

    return TestBed.inject(ThemeService);
  };

  beforeEach(() => {
    localStorage.clear();
    resetDom();
  });

  it('initializes from localStorage and applies theme in browser', () => {
    localStorage.setItem(storageKey, 'light');

    const service = createService('browser');

    expect(service.getCurrentTheme()).toBe('light');
    expect(document.body.classList.contains('light-theme')).toBeTrue();
    expect(document.body.getAttribute('data-theme')).toBe('light');
  });

  it('setTheme falls back to default on invalid value', () => {
    const service = createService('browser');

    service.setTheme('invalid' as any);

    expect(loggerSpy.warn).toHaveBeenCalledWith('ThemeService: invalid theme, using fallback', jasmine.objectContaining({
      attemptedTheme: 'invalid',
      fallbackTheme: 'dark'
    }));
    expect(service.getCurrentTheme()).toBe('dark');
    expect(document.body.classList.contains('dark-theme')).toBeTrue();
  });

  it('toggleTheme switches between dark and light', () => {
    const service = createService('browser');

    service.toggleTheme();

    expect(service.getCurrentTheme()).toBe('light');
    expect(document.body.classList.contains('light-theme')).toBeTrue();
  });

  it('handles localStorage errors gracefully', () => {
    const getItemSpy = vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('fail');
    });

    const service = createService('browser');

    expect(loggerSpy.warn).toHaveBeenCalledWith('ThemeService: failed to load theme from localStorage', jasmine.objectContaining({
      error: jasmine.any(Error)
    }));
    expect(service.getCurrentTheme()).toBe('dark');
    getItemSpy.mockRestore();
  });

  it('skips DOM and storage access on server platform', () => {
    const getSpy = spyOn(localStorage, 'getItem').and.callThrough();
    const setSpy = spyOn(localStorage, 'setItem').and.callThrough();

    const service = createService('server');

    expect(service.getCurrentTheme()).toBe('dark');
    expect(getSpy).not.toHaveBeenCalled();
    expect(setSpy).not.toHaveBeenCalled();
    expect(document.body.getAttribute('data-theme')).toBeNull();
  });
});
