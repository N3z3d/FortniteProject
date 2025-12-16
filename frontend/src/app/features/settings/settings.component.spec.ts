import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';

import { TranslationService } from '../../core/services/translation.service';
import { UserContextService } from '../../core/services/user-context.service';
import { SettingsComponent } from './settings.component';

describe('SettingsComponent', () => {
  it('resetSettings should default to French and update TranslationService', () => {
    const userContextService = jasmine.createSpyObj<UserContextService>('UserContextService', ['logout']);
    const router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    const snackBar = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    const translationService = jasmine.createSpyObj<TranslationService>(
      'TranslationService',
      ['setLanguage', 't'],
      { currentLanguage: 'en' }
    );

    translationService.t.and.callFake((key: string) => key);

    const component = new SettingsComponent(
      userContextService as unknown as UserContextService,
      router as unknown as Router,
      snackBar as unknown as MatSnackBar,
      translationService as unknown as TranslationService
    );

    component.resetSettings();

    expect(component.language).toBe('fr');
    expect(translationService.setLanguage).toHaveBeenCalledWith('fr');
    expect(snackBar.open).toHaveBeenCalled();
  });
});
