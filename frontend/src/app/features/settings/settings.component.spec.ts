import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';

import { TranslationService } from '../../core/services/translation.service';
import { UserContextService } from '../../core/services/user-context.service';
import { LoggerService } from '../../core/services/logger.service';
import { ThemeService } from '../../core/services/theme.service';
import { SettingsComponent } from './settings.component';

describe('SettingsComponent', () => {
  let component: SettingsComponent;
  let userContextService: jasmine.SpyObj<UserContextService>;
  let router: jasmine.SpyObj<Router>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;
  let loggerService: jasmine.SpyObj<LoggerService>;
  let translationService: jasmine.SpyObj<TranslationService>;
  let themeService: jasmine.SpyObj<ThemeService>;

  beforeEach(() => {
    localStorage.clear();

    userContextService = jasmine.createSpyObj<UserContextService>('UserContextService', ['logout']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    snackBar = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    loggerService = jasmine.createSpyObj<LoggerService>('LoggerService', ['info', 'error', 'debug']);
    translationService = jasmine.createSpyObj<TranslationService>(
      'TranslationService',
      ['setLanguage', 't'],
      { currentLanguage: 'fr' }
    );
    themeService = jasmine.createSpyObj<ThemeService>(
      'ThemeService',
      ['setTheme', 'getCurrentTheme']
    );

    translationService.t.and.callFake((key: string) => key);
    themeService.getCurrentTheme.and.returnValue('dark');

    component = new SettingsComponent(
      userContextService,
      router,
      snackBar,
      loggerService,
      translationService,
      themeService
    );
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should initialize theme from ThemeService', () => {
      themeService.getCurrentTheme.and.returnValue('light');

      component.ngOnInit();

      expect(component.theme).toBe('light');
      expect(themeService.getCurrentTheme).toHaveBeenCalled();
    });

    it('should initialize language from TranslationService', () => {
      Object.defineProperty(translationService, 'currentLanguage', { value: 'en', writable: true });

      component.ngOnInit();

      expect(component.language).toBe('en');
    });

    it('should load settings from localStorage if available', () => {
      const savedSettings = {
        emailNotifications: false,
        tradeNotifications: false,
        theme: 'light',
        language: 'en'
      };
      localStorage.setItem('userSettings', JSON.stringify(savedSettings));

      component.ngOnInit();

      expect(component.emailNotifications).toBe(false);
      expect(component.tradeNotifications).toBe(false);
    });
  });

  describe('loadSettings', () => {
    it('should load and apply settings from localStorage', () => {
      const savedSettings = {
        emailNotifications: false,
        tradeNotifications: false,
        draftTurnNotifications: false,
        theme: 'light',
        language: 'en',
        autoJoinDraft: true,
        showOnlineStatus: false
      };
      localStorage.setItem('userSettings', JSON.stringify(savedSettings));
      themeService.getCurrentTheme.and.returnValue('light');
      Object.defineProperty(translationService, 'currentLanguage', { value: 'en', writable: true });

      component.loadSettings();

      expect(component.emailNotifications).toBe(false);
      expect(component.tradeNotifications).toBe(false);
      expect(component.draftTurnNotifications).toBe(false);
      expect(component.autoJoinDraft).toBe(true);
      expect(component.showOnlineStatus).toBe(false);
      expect(component.theme).toBe('light');
      expect(component.language).toBe('en');
    });

    it('should use default settings if no localStorage data', () => {
      component.loadSettings();

      expect(component.theme).toBe('dark');
      expect(component.language).toBe('fr');
    });
  });

  describe('onLanguageChange', () => {
    it('should call TranslationService.setLanguage with selected language', () => {
      component.language = 'en';

      component.onLanguageChange();

      expect(translationService.setLanguage).toHaveBeenCalledWith('en');
    });
  });

  describe('onThemeChange', () => {
    it('should call ThemeService.setTheme with selected theme', () => {
      component.theme = 'light';

      component.onThemeChange();

      expect(themeService.setTheme).toHaveBeenCalledWith('light');
      expect(loggerService.info).toHaveBeenCalledWith('Theme changed to: light');
    });

    it('should log theme change', () => {
      component.theme = 'dark';

      component.onThemeChange();

      expect(loggerService.info).toHaveBeenCalled();
    });
  });

  describe('saveSettings', () => {
    it('should save all settings to localStorage', () => {
      component.emailNotifications = false;
      component.tradeNotifications = true;
      component.draftTurnNotifications = false;
      component.theme = 'light';
      component.language = 'en';
      component.autoJoinDraft = true;
      component.showOnlineStatus = false;

      component.saveSettings();

      const saved = JSON.parse(localStorage.getItem('userSettings') || '{}');
      expect(saved.emailNotifications).toBe(false);
      expect(saved.tradeNotifications).toBe(true);
      expect(saved.draftTurnNotifications).toBe(false);
      expect(saved.theme).toBe('light');
      expect(saved.language).toBe('en');
      expect(saved.autoJoinDraft).toBe(true);
      expect(saved.showOnlineStatus).toBe(false);
    });

    it('should show snackbar confirmation', () => {
      component.saveSettings();

      expect(snackBar.open).toHaveBeenCalledWith(
        'settings.settingsSaved',
        'common.close',
        { duration: 3000 }
      );
    });
  });

  describe('resetSettings', () => {
    it('should reset all settings to defaults', () => {
      component.emailNotifications = false;
      component.tradeNotifications = false;
      component.draftTurnNotifications = false;
      component.theme = 'light';
      component.language = 'en';
      component.autoJoinDraft = true;
      component.showOnlineStatus = false;

      component.resetSettings();

      expect(component.emailNotifications).toBe(true);
      expect(component.tradeNotifications).toBe(true);
      expect(component.draftTurnNotifications).toBe(true);
      expect(component.theme).toBe('dark');
      expect(component.language).toBe('fr');
      expect(component.autoJoinDraft).toBe(false);
      expect(component.showOnlineStatus).toBe(true);
    });

    it('should default to French and update TranslationService', () => {
      component.resetSettings();

      expect(component.language).toBe('fr');
      expect(translationService.setLanguage).toHaveBeenCalledWith('fr');
      expect(snackBar.open).toHaveBeenCalled();
    });

    it('should show snackbar confirmation', () => {
      component.resetSettings();

      expect(snackBar.open).toHaveBeenCalledWith(
        'settings.settingsReset',
        'common.close',
        { duration: 3000 }
      );
    });
  });

  describe('deleteAccount', () => {
    it('should log deletion request when confirmed', () => {
      spyOn(window, 'confirm').and.returnValue(true);

      component.deleteAccount();

      expect(loggerService.info).toHaveBeenCalledWith('Settings: account deletion requested');
    });

    it('should not log if user cancels', () => {
      spyOn(window, 'confirm').and.returnValue(false);

      component.deleteAccount();

      expect(loggerService.info).not.toHaveBeenCalled();
    });
  });

  describe('exportData', () => {
    it('should log export data request', () => {
      component.exportData();

      expect(loggerService.info).toHaveBeenCalledWith('Settings: export data requested');
    });

    it('should show snackbar notification', () => {
      component.exportData();

      expect(snackBar.open).toHaveBeenCalledWith(
        'Data export started. You will receive an email when ready.',
        'Close',
        { duration: 5000 }
      );
    });
  });
});
