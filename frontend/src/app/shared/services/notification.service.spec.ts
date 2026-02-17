import { Subject } from 'rxjs';
import { NotificationService } from './notification.service';
import { UiErrorFeedbackService } from '../../core/services/ui-error-feedback.service';

describe('NotificationService', () => {
  let logger: { info: jasmine.Spy; debug: jasmine.Spy };
  let translationService: { t: jasmine.Spy };

  const createSnackBar = () => {
    const afterDismissed$ = new Subject<void>();
    const onAction$ = new Subject<void>();
    const snackBarRef = {
      afterDismissed: () => afterDismissed$,
      onAction: () => onAction$,
      dismiss: jasmine.createSpy('dismiss')
    };
    const snackBar = {
      open: jasmine.createSpy('open').and.returnValue(snackBarRef)
    };

    return { snackBar, snackBarRef, afterDismissed$, onAction$ };
  };

  const createUiFeedback = () =>
    jasmine.createSpyObj<UiErrorFeedbackService>('UiErrorFeedbackService', [
      'showSuccessMessage',
      'showErrorMessage',
      'showWarningMessage',
      'showInfoMessage'
    ]);

  const createService = (snackBar?: unknown, uiFeedback?: unknown) =>
    new NotificationService(
      logger as never,
      translationService as never,
      uiFeedback as never,
      snackBar as never
    );

  beforeEach(() => {
    logger = {
      info: jasmine.createSpy('info'),
      debug: jasmine.createSpy('debug')
    };
    translationService = {
      t: jasmine.createSpy('t').and.callFake((_key: string, fallback?: string) => fallback || _key)
    };
  });

  it('showSuccess adds notification and uses snackBar with close label', () => {
    const { snackBar } = createSnackBar();
    const service = createService(snackBar);

    const id = service.showSuccess('Hello');
    const notification = service.getNotifications()[0];
    const [message, action, config] = snackBar.open.calls.mostRecent().args as [
      string,
      string,
      { duration: number; panelClass: string[]; horizontalPosition: string; verticalPosition: string }
    ];

    expect(id).toContain('notification_');
    expect(notification.type).toBe('success');
    expect(notification.message).toBe('Hello');
    expect(message).toBe('Hello');
    expect(action).toBe('Close');
    expect(config.duration).toBe(4000);
    expect(config.panelClass).toEqual(jasmine.arrayContaining(['notification-success', 'custom-snackbar']));
  });

  it('showSuccess uses UiErrorFeedbackService bridge when no action or persistent', () => {
    const { snackBar } = createSnackBar();
    const uiFeedback = createUiFeedback();
    const service = createService(snackBar, uiFeedback);

    service.showSuccess('Bridge me');

    expect(uiFeedback.showSuccessMessage).toHaveBeenCalledWith('Bridge me', 4000);
    expect(snackBar.open).not.toHaveBeenCalled();
  });

  it('showSuccess keeps legacy snackbar path when action is provided', () => {
    const { snackBar } = createSnackBar();
    const uiFeedback = createUiFeedback();
    const service = createService(snackBar, uiFeedback);

    service.showSuccess('Actionable', 'View');

    expect(snackBar.open).toHaveBeenCalled();
    expect(uiFeedback.showSuccessMessage).not.toHaveBeenCalled();
  });

  it('showPersistent keeps legacy snackbar path even with UiErrorFeedbackService', () => {
    const { snackBar } = createSnackBar();
    const uiFeedback = createUiFeedback();
    const service = createService(snackBar, uiFeedback);

    service.showPersistent('Hold', 'warning', 'Acknowledge');

    expect(snackBar.open).toHaveBeenCalled();
    expect(uiFeedback.showWarningMessage).not.toHaveBeenCalled();
  });

  it('showPersistent logs when snackBar is not available', () => {
    const service = createService();

    service.showPersistent('Hold', 'warning');

    const notification = service.getNotifications()[0];
    expect(notification.persistent).toBeTrue();
    expect(notification.duration).toBe(0);
    expect(logger.info).toHaveBeenCalled();
  });

  it('dismiss removes notification and dismisses snackbar', () => {
    const { snackBar, snackBarRef } = createSnackBar();
    const service = createService(snackBar);

    const id = service.showInfo('Info');
    service.dismiss(id);

    expect(snackBarRef.dismiss).toHaveBeenCalled();
    expect(service.getNotifications().length).toBe(0);
  });

  it('handles action clicks by logging and removing notification', () => {
    const { snackBar, onAction$ } = createSnackBar();
    const service = createService(snackBar);

    service.showInfo('Actionable', 'Do it');
    expect(service.getNotifications().length).toBe(1);

    onAction$.next();

    expect(logger.debug).toHaveBeenCalled();
    expect(service.getNotifications().length).toBe(0);
  });

  it('clears notifications older than 24 hours', () => {
    const service = createService();

    service.showInfo('Old');
    service.showInfo('New');

    const notifications = service.getNotifications();
    notifications[0].timestamp = new Date(Date.now() - 25 * 60 * 60 * 1000);
    notifications[1].timestamp = new Date();

    service.clearOldNotifications();

    expect(service.getNotifications().length).toBe(1);
    expect(service.getNotifications()[0].message).toBe('New');
  });

  it('api.error builds message with details and shows error notification', () => {
    const service = createService();

    service.api.error('Sync', 'boom');

    const notification = service.getNotifications()[0];
    expect(notification.type).toBe('error');
    expect(notification.message).toContain('Sync');
    expect(notification.message).toContain('boom');
  });

  describe('notification type methods', () => {
    it('showError creates error notification with correct duration', () => {
      const service = createService();
      const id = service.showError('Error message');

      const notification = service.getNotifications()[0];
      expect(notification.type).toBe('error');
      expect(notification.message).toBe('Error message');
      expect(notification.duration).toBe(6000);
    });

    it('showWarning creates warning notification', () => {
      const service = createService();
      const id = service.showWarning('Warning');

      const notification = service.getNotifications()[0];
      expect(notification.type).toBe('warning');
      expect(notification.duration).toBe(5000);
    });

    it('showInfo creates info notification', () => {
      const service = createService();
      const id = service.showInfo('Information');

      const notification = service.getNotifications()[0];
      expect(notification.type).toBe('info');
      expect(notification.duration).toBe(4000);
    });
  });

  describe('loading notifications', () => {
    it('showLoading creates persistent info notification', () => {
      const service = createService();
      const id = service.showLoading('Loading data...');

      const notification = service.getNotifications()[0];
      expect(notification.type).toBe('info');
      expect(notification.message).toBe('Loading data...');
      expect(notification.duration).toBe(0);
    });

    it('showLoading uses default message if none provided', () => {
      const service = createService();
      service.showLoading();

      const notification = service.getNotifications()[0];
      expect(notification.message).toBe('Loading...');
    });

    it('updateLoadingToSuccess dismisses loading and shows success', () => {
      const { snackBar, snackBarRef } = createSnackBar();
      const service = createService(snackBar);

      const loadingId = service.showLoading('Loading...');
      const successId = service.updateLoadingToSuccess(loadingId, 'Done!');

      expect(snackBarRef.dismiss).toHaveBeenCalled();
      const notifications = service.getNotifications();
      expect(notifications.length).toBe(1);
      expect(notifications[0].type).toBe('success');
      expect(notifications[0].message).toBe('Done!');
    });

    it('updateLoadingToError dismisses loading and shows error', () => {
      const { snackBar, snackBarRef } = createSnackBar();
      const service = createService(snackBar);

      const loadingId = service.showLoading('Processing...');
      const errorId = service.updateLoadingToError(loadingId, 'Failed!');

      expect(snackBarRef.dismiss).toHaveBeenCalled();
      const notifications = service.getNotifications();
      expect(notifications.length).toBe(1);
      expect(notifications[0].type).toBe('error');
      expect(notifications[0].message).toBe('Failed!');
    });
  });

  describe('dismissAll', () => {
    it('dismisses all active snackbars and clears notifications', () => {
      const { snackBar, snackBarRef } = createSnackBar();
      const service = createService(snackBar);

      service.showSuccess('First');
      service.showInfo('Second');
      service.showWarning('Third');

      expect(service.getNotifications().length).toBe(3);

      service.dismissAll();

      expect(snackBarRef.dismiss).toHaveBeenCalled();
      expect(service.getNotifications().length).toBe(0);
    });
  });

  describe('getNotificationsByType', () => {
    it('filters notifications by type', () => {
      const service = createService();

      service.showSuccess('Success 1');
      service.showError('Error 1');
      service.showSuccess('Success 2');
      service.showInfo('Info 1');

      const successNotifications = service.getNotificationsByType('success');
      const errorNotifications = service.getNotificationsByType('error');

      expect(successNotifications.length).toBe(2);
      expect(errorNotifications.length).toBe(1);
      expect(successNotifications[0].message).toBe('Success 1');
      expect(errorNotifications[0].message).toBe('Error 1');
    });
  });

  describe('api helpers', () => {
    it('api.loading shows loading notification', () => {
      const service = createService();
      const id = service.api.loading('Saving data');

      const notification = service.getNotifications()[0];
      expect(notification.message).toContain('Saving data');
      expect(notification.duration).toBe(0);
    });

    it('api.success shows success notification without loading ID', () => {
      const service = createService();
      service.api.success('Data saved');

      const notification = service.getNotifications()[0];
      expect(notification.type).toBe('success');
      expect(notification.message).toContain('Data saved');
    });

    it('api.success updates loading notification when ID provided', () => {
      const { snackBar, snackBarRef } = createSnackBar();
      const service = createService(snackBar);

      const loadingId = service.api.loading('Saving');
      service.api.success('Save', loadingId);

      expect(snackBarRef.dismiss).toHaveBeenCalled();
      const notifications = service.getNotifications();
      expect(notifications[notifications.length - 1].type).toBe('success');
    });

    it('api.error shows error without details', () => {
      const service = createService();
      service.api.error('Delete');

      const notification = service.getNotifications()[0];
      expect(notification.type).toBe('error');
      expect(notification.message).toContain('Delete');
    });

    it('api.error updates loading notification when ID provided', () => {
      const { snackBar, snackBarRef } = createSnackBar();
      const service = createService(snackBar);

      const loadingId = service.api.loading('Deleting');
      service.api.error('Delete', 'Not found', loadingId);

      expect(snackBarRef.dismiss).toHaveBeenCalled();
      const notifications = service.getNotifications();
      expect(notifications[notifications.length - 1].type).toBe('error');
    });
  });

  describe('game helpers', () => {
    it('game.created shows success with action', () => {
      const { snackBar } = createSnackBar();
      const service = createService(snackBar);

      service.game.created('Epic Battle');

      const notification = service.getNotifications()[0];
      expect(notification.type).toBe('success');
      expect(notification.message).toContain('Epic Battle');
      expect(notification.action).toBe('View');
    });

    it('game.joined shows success with action', () => {
      const { snackBar } = createSnackBar();
      const service = createService(snackBar);

      service.game.joined('Tournament');

      const notification = service.getNotifications()[0];
      expect(notification.type).toBe('success');
      expect(notification.message).toContain('Tournament');
      expect(notification.action).toBe('View team');
    });

    it('game.left shows info notification', () => {
      const service = createService();

      service.game.left('Old Game');

      const notification = service.getNotifications()[0];
      expect(notification.type).toBe('info');
      expect(notification.message).toContain('Old Game');
    });

    it('game.draftStarted shows info with action', () => {
      const { snackBar } = createSnackBar();
      const service = createService(snackBar);

      service.game.draftStarted('Draft Game');

      const notification = service.getNotifications()[0];
      expect(notification.type).toBe('info');
      expect(notification.message).toContain('Draft Game');
      expect(notification.action).toBe('Participate');
    });

    it('game.yourTurn shows warning with action', () => {
      const { snackBar } = createSnackBar();
      const service = createService(snackBar);

      service.game.yourTurn();

      const notification = service.getNotifications()[0];
      expect(notification.type).toBe('warning');
      expect(notification.action).toBe('Go to draft');
    });
  });

  describe('team helpers', () => {
    it('team.playerAdded shows success', () => {
      const service = createService();

      service.team.playerAdded('John Doe');

      const notification = service.getNotifications()[0];
      expect(notification.type).toBe('success');
      expect(notification.message).toContain('John Doe');
    });

    it('team.playerRemoved shows info', () => {
      const service = createService();

      service.team.playerRemoved('Jane Smith');

      const notification = service.getNotifications()[0];
      expect(notification.type).toBe('info');
      expect(notification.message).toContain('Jane Smith');
    });

    it('team.tradeCompleted shows success with both players', () => {
      const service = createService();

      service.team.tradeCompleted('Player A', 'Player B');

      const notification = service.getNotifications()[0];
      expect(notification.type).toBe('success');
      expect(notification.message).toContain('Player A');
      expect(notification.message).toContain('Player B');
    });
  });

  describe('auth helpers', () => {
    it('auth.loginSuccess shows success with username', () => {
      const service = createService();

      service.auth.loginSuccess('testuser');

      const notification = service.getNotifications()[0];
      expect(notification.type).toBe('success');
      expect(notification.message).toContain('testuser');
    });

    it('auth.logoutSuccess shows info', () => {
      const service = createService();

      service.auth.logoutSuccess();

      const notification = service.getNotifications()[0];
      expect(notification.type).toBe('info');
    });

    it('auth.sessionExpired shows warning with action', () => {
      const { snackBar } = createSnackBar();
      const service = createService(snackBar);

      service.auth.sessionExpired();

      const notification = service.getNotifications()[0];
      expect(notification.type).toBe('warning');
      expect(notification.action).toBe('Log in');
    });

    it('auth.profileSwitched shows info with username', () => {
      const service = createService();

      service.auth.profileSwitched('newuser');

      const notification = service.getNotifications()[0];
      expect(notification.type).toBe('info');
      expect(notification.message).toContain('newuser');
    });
  });

  it('afterDismissed removes notification from list', () => {
    const { snackBar, afterDismissed$ } = createSnackBar();
    const service = createService(snackBar);

    service.showSuccess('Test');
    expect(service.getNotifications().length).toBe(1);

    afterDismissed$.next();

    expect(service.getNotifications().length).toBe(0);
  });

  it('showPersistent creates notification with action', () => {
    const { snackBar } = createSnackBar();
    const service = createService(snackBar);

    const id = service.showPersistent('Important', 'warning', 'Acknowledge');

    const notification = service.getNotifications()[0];
    expect(notification.persistent).toBe(true);
    expect(notification.action).toBe('Acknowledge');
    expect(notification.duration).toBe(0);
  });

  it('dismiss handles non-existent snackbar gracefully', () => {
    const service = createService();

    expect(() => service.dismiss('non-existent-id')).not.toThrow();
  });
});
