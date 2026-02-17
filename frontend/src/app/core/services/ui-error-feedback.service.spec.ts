import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';

import { UiErrorFeedbackService } from './ui-error-feedback.service';
import { TranslationService } from './translation.service';

describe('UiErrorFeedbackService', () => {
  let service: UiErrorFeedbackService;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let translationSpy: jasmine.SpyObj<TranslationService>;

  beforeEach(() => {
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    translationSpy = jasmine.createSpyObj('TranslationService', ['t']);
    translationSpy.t.and.callFake((key: string) => `tx:${key}`);

    TestBed.configureTestingModule({
      providers: [
        UiErrorFeedbackService,
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: TranslationService, useValue: translationSpy }
      ]
    });

    service = TestBed.inject(UiErrorFeedbackService);
  });

  it('shows success snackbar with unified classes', () => {
    service.showSuccessFromKey('games.detail.actions.renameSuccess');

    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'tx:games.detail.actions.renameSuccess',
      'tx:common.close',
      jasmine.objectContaining({
        duration: 3000,
        panelClass: ['custom-snackbar', 'notification-success']
      })
    );
  });

  it('shows error snackbar from plain message', () => {
    service.showErrorMessage('plain error', 4500);

    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'plain error',
      'tx:common.close',
      jasmine.objectContaining({
        duration: 4500,
        panelClass: ['custom-snackbar', 'notification-error']
      })
    );
  });

  it('maps known error message with a rule', () => {
    service.showError(new Error('Createur de la partie introuvable'), 'errors.generic', {
      rules: [
        {
          pattern: /createur de la partie introuvable/,
          translationKey: 'games.detail.actions.draftCreatorMissing'
        }
      ]
    });

    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'tx:games.detail.actions.draftCreatorMissing',
      'tx:common.close',
      jasmine.objectContaining({
        panelClass: ['custom-snackbar', 'notification-error']
      })
    );
  });

  it('falls back to translation key when no safe message is available', () => {
    const message = service.resolveMessage({}, 'errors.handler.serverErrorMessage');

    expect(message).toBe('tx:errors.handler.serverErrorMessage');
  });

  it('shows success snackbar with action callback', () => {
    const snackBarRefSpy = jasmine.createSpyObj('MatSnackBarRef', ['onAction']);
    snackBarRefSpy.onAction.and.returnValue(of(undefined));
    snackBarSpy.open.and.returnValue(snackBarRefSpy);
    const onActionSpy = jasmine.createSpy('onAction');

    service.showSuccessWithAction('joined', 'games.joinDialog.view', onActionSpy);

    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'joined',
      'tx:games.joinDialog.view',
      jasmine.objectContaining({
        duration: 5000,
        panelClass: ['custom-snackbar', 'notification-success']
      })
    );
    expect(onActionSpy).toHaveBeenCalled();
  });

  it('shows info snackbar from translation key', () => {
    service.showInfoFromKey('dashboard.messages.demoMode', 4000);

    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'tx:dashboard.messages.demoMode',
      'tx:common.close',
      jasmine.objectContaining({
        duration: 4000,
        panelClass: ['custom-snackbar', 'notification-info']
      })
    );
  });

  it('shows warning snackbar from plain message', () => {
    service.showWarningMessage('warning text', 5500);

    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'warning text',
      'tx:common.close',
      jasmine.objectContaining({
        duration: 5500,
        panelClass: ['custom-snackbar', 'notification-warning']
      })
    );
  });

  it('shows info snackbar with action callback', () => {
    const snackBarRefSpy = jasmine.createSpyObj('MatSnackBarRef', ['onAction']);
    snackBarRefSpy.onAction.and.returnValue(of(undefined));
    snackBarSpy.open.and.returnValue(snackBarRefSpy);
    const onActionSpy = jasmine.createSpy('onAction');

    service.showInfoWithAction('player removed', 'common.cancel', onActionSpy, 2000);

    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'player removed',
      'tx:common.cancel',
      jasmine.objectContaining({
        duration: 2000,
        panelClass: ['custom-snackbar', 'notification-info']
      })
    );
    expect(onActionSpy).toHaveBeenCalled();
  });
});
