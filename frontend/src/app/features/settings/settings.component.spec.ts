import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';

import { TranslationService } from '../../core/services/translation.service';
import { UserContextService } from '../../core/services/user-context.service';
import { LoggerService } from '../../core/services/logger.service';
import { ThemeService } from '../../core/services/theme.service';
import { SettingsComponent } from './settings.component';

describe('SettingsComponent', () => {
  it('resetSettings should default to French and update TranslationService', () => {
    const userContextService = jasmine.createSpyObj<UserContextService>('UserContextService', ['logout']);
    const router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    const snackBar = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    const loggerService = jasmine.createSpyObj<LoggerService>('LoggerService', ['info', 'error', 'debug']);
    const translationService = jasmine.createSpyObj<TranslationService>(
      'TranslationService',
      ['setLanguage', 't'],
      { currentLanguage: 'en' }
    );
    const themeService = jasmine.createSpyObj<ThemeService>(
      'ThemeService',
      ['setTheme', 'getCurrentTheme']
    );

    translationService.t.and.callFake((key: string) => key);
    themeService.getCurrentTheme.and.returnValue('dark');

    const component = new SettingsComponent(
      userContextService as unknown as UserContextService,
      router as unknown as Router,
      snackBar as unknown as MatSnackBar,
      loggerService as unknown as LoggerService,
      translationService as unknown as TranslationService,
      themeService as unknown as ThemeService
    );

    component.resetSettings();

    expect(component.language).toBe('fr');
    expect(translationService.setLanguage).toHaveBeenCalledWith('fr');
    expect(snackBar.open).toHaveBeenCalled();
  });
});
