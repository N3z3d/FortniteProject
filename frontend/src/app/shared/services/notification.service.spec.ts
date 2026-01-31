import { Subject } from 'rxjs';
import { NotificationService } from './notification.service';

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

  const createService = (snackBar?: unknown) =>
    new NotificationService(logger as never, translationService as never, snackBar as never);

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
});
