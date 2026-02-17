import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { BehaviorSubject } from 'rxjs';
import { NotificationCenterComponent } from './notification-center.component';
import { NotificationService, NotificationMessage } from '../../services/notification.service';
import { TranslationService } from '../../../core/services/translation.service';

describe('NotificationCenterComponent', () => {
  let component: NotificationCenterComponent;
  let fixture: ComponentFixture<NotificationCenterComponent>;
  let notificationService: jasmine.SpyObj<NotificationService>;
  let translationService: jasmine.SpyObj<TranslationService>;

  let notificationsSubject: BehaviorSubject<NotificationMessage[]>;
  let unreadCountSubject: BehaviorSubject<number>;
  let newNotificationSubject: BehaviorSubject<NotificationMessage | null>;
  let connectionStatusSubject: BehaviorSubject<boolean>;

  const mockNotification: NotificationMessage = {
    id: 'notif1',
    type: 'info',
    message: 'New trade offer received',
    timestamp: new Date()
  };

  beforeEach(async () => {
    notificationsSubject = new BehaviorSubject<NotificationMessage[]>([]);
    unreadCountSubject = new BehaviorSubject<number>(0);
    newNotificationSubject = new BehaviorSubject<NotificationMessage | null>(null);
    connectionStatusSubject = new BehaviorSubject<boolean>(false);

    notificationService = jasmine.createSpyObj('NotificationService', [
      'dismiss',
      'dismissAll'
    ], {
      notifications$: notificationsSubject.asObservable(),
      unreadCount$: unreadCountSubject.asObservable(),
      newNotification$: newNotificationSubject.asObservable(),
      connectionStatus$: connectionStatusSubject.asObservable()
    });

    translationService = jasmine.createSpyObj('TranslationService', ['t'], {
      currentLanguage: 'fr'
    });
    translationService.t.and.callFake((key: string) => key);

    await TestBed.configureTestingModule({
      imports: [NotificationCenterComponent, NoopAnimationsModule],
      providers: [
        { provide: NotificationService, useValue: notificationService },
        { provide: TranslationService, useValue: translationService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(NotificationCenterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with closed state', () => {
    expect(component.isOpen).toBeFalse();
    expect(component.unreadCount).toBe(0);
    expect(component.notifications).toEqual([]);
    expect(component.connectionStatus).toBeFalse();
  });

  it('should subscribe to notifications on init', () => {
    const notifications = [mockNotification];
    notificationsSubject.next(notifications);

    expect(component.notifications).toEqual(notifications);
  });

  it('should subscribe to unread count on init', () => {
    unreadCountSubject.next(5);

    expect(component.unreadCount).toBe(5);
  });

  it('should subscribe to connection status on init', () => {
    connectionStatusSubject.next(true);

    expect(component.connectionStatus).toBeTrue();
  });

  it('should toggle notification center open state', () => {
    expect(component.isOpen).toBeFalse();

    component.toggleNotificationCenter();

    expect(component.isOpen).toBeTrue();

    component.toggleNotificationCenter();

    expect(component.isOpen).toBeFalse();
  });

  it('should expose main notification button with aria-label', () => {
    const toggleButton = fixture.nativeElement.querySelector('.notification-button') as HTMLButtonElement;

    expect(toggleButton.getAttribute('aria-label')).toBe('notificationCenter.title');
  });

  it('should dismiss all notifications after 2s when opening with unread count', fakeAsync(() => {
    component.unreadCount = 3;
    component.isOpen = false;

    component.toggleNotificationCenter();

    expect(component.isOpen).toBeTrue();
    expect(notificationService.dismissAll).not.toHaveBeenCalled();

    tick(2000);

    expect(notificationService.dismissAll).toHaveBeenCalled();
  }));

  it('should not dismiss when opening with no unread', fakeAsync(() => {
    component.unreadCount = 0;
    component.isOpen = false;

    component.toggleNotificationCenter();

    tick(2000);

    expect(notificationService.dismissAll).not.toHaveBeenCalled();
  }));

  it('should close notification center', () => {
    component.isOpen = true;

    component.closeNotificationCenter();

    expect(component.isOpen).toBeFalse();
  });

  it('should mark notification as read', () => {
    component.markAsRead(mockNotification);

    expect(notificationService.dismiss).toHaveBeenCalledWith('notif1');
  });

  it('should mark notification as read on Enter key', () => {
    const keyboardEvent = new KeyboardEvent('keydown', { key: 'Enter' });

    component.onNotificationKeydown(keyboardEvent, mockNotification);

    expect(notificationService.dismiss).toHaveBeenCalledWith('notif1');
  });

  it('should ignore unrelated key for notification keyboard handler', () => {
    const keyboardEvent = new KeyboardEvent('keydown', { key: 'Escape' });

    component.onNotificationKeydown(keyboardEvent, mockNotification);

    expect(notificationService.dismiss).not.toHaveBeenCalled();
  });

  it('should delete notification and stop event propagation', () => {
    const event = new Event('click');
    spyOn(event, 'stopPropagation');

    component.deleteNotification(mockNotification, event);

    expect(event.stopPropagation).toHaveBeenCalled();
    expect(notificationService.dismiss).toHaveBeenCalledWith('notif1');
  });

  it('should return correct icon for info notification', () => {
    expect(component.getNotificationIcon('info')).toBe('notifications');
  });

  it('should return correct icon for success notification', () => {
    expect(component.getNotificationIcon('success')).toBe('notifications');
  });

  it('should return correct icon for error notification', () => {
    expect(component.getNotificationIcon('error')).toBe('notifications');
  });

  it('should return correct icon for warning notification', () => {
    expect(component.getNotificationIcon('warning')).toBe('notifications');
  });

  it('should return default icon for unknown type', () => {
    expect(component.getNotificationIcon('unknown')).toBe('notifications');
  });

  it('should return notification class based on type', () => {
    expect(component.getNotificationClass('info')).toBe('notification-info');
    expect(component.getNotificationClass('success')).toBe('notification-success');
  });

  it('should format timestamp as just now for recent notifications', () => {
    const now = new Date();

    const result = component.formatTimestamp(now);

    expect(result).toBe('notificationCenter.time.justNow');
  });

  it('should format timestamp as minutes ago', () => {
    const fiveMinsAgo = new Date(Date.now() - 5 * 60 * 1000);
    translationService.t.and.returnValue('{n} minutes ago');

    const result = component.formatTimestamp(fiveMinsAgo);

    expect(result).toContain('5');
  });

  it('should format timestamp as hours ago', () => {
    const twoHoursAgo = new Date(Date.now() - 2 * 60 * 60 * 1000);
    translationService.t.and.returnValue('{n} hours ago');

    const result = component.formatTimestamp(twoHoursAgo);

    expect(result).toContain('2');
  });

  it('should format timestamp as days ago', () => {
    const threeDaysAgo = new Date(Date.now() - 3 * 24 * 60 * 60 * 1000);
    translationService.t.and.returnValue('{n} days ago');

    const result = component.formatTimestamp(threeDaysAgo);

    expect(result).toContain('3');
  });

  it('should format old timestamp as locale date', () => {
    const eightDaysAgo = new Date(Date.now() - 8 * 24 * 60 * 60 * 1000);

    const result = component.formatTimestamp(eightDaysAgo);

    // Should be a formatted date string
    expect(result).toBeTruthy();
    expect(result).not.toContain('notificationCenter');
  });

  it('should not show toast for null notification', () => {
    spyOn(document, 'createElement');

    component['showToastNotification'](null);

    expect(document.createElement).not.toHaveBeenCalled();
  });

  it('should create toast element for new notification', () => {
    const appendChildSpy = spyOn(document.body, 'appendChild');

    component['showToastNotification'](mockNotification);

    expect(appendChildSpy).toHaveBeenCalled();
  });

  it('should render malicious toast payload as plain text (no HTML injection)', () => {
    const appendChildSpy = spyOn(document.body, 'appendChild').and.callThrough();
    const maliciousPayload = '<img src=x onerror=\"window.__xss=1\"><script>window.__xss=2</script>';
    const maliciousNotification: NotificationMessage = {
      ...mockNotification,
      id: 'malicious',
      message: maliciousPayload
    };

    component['showToastNotification'](maliciousNotification);

    const toast = appendChildSpy.calls.mostRecent().args[0] as HTMLElement;
    const messageElement = toast.querySelector('.toast-content p') as HTMLElement;

    expect(toast.querySelector('script')).toBeNull();
    expect(messageElement.textContent).toBe(maliciousPayload);
    expect(messageElement.innerHTML).toContain('&lt;script&gt;');

    toast.remove();
  });

  it('should track notifications by id', () => {
    const result = component.trackByFn(0, mockNotification);

    expect(result).toBe('notif1');
  });

  it('should mark all as read', () => {
    component.markAllAsRead();

    expect(notificationService.dismissAll).toHaveBeenCalled();
  });

  it('should unsubscribe on destroy', () => {
    const subscription = component['subscriptions'][0];
    spyOn(subscription, 'unsubscribe');

    component.ngOnDestroy();

    expect(subscription.unsubscribe).toHaveBeenCalled();
  });

  it('should show toast when new notification arrives', () => {
    spyOn<any>(component, 'showToastNotification');

    newNotificationSubject.next(mockNotification);

    expect(component['showToastNotification']).toHaveBeenCalledWith(mockNotification);
  });

  it('should render notification items as keyboard-focusable buttons', () => {
    notificationsSubject.next([mockNotification]);
    component.isOpen = true;
    fixture.detectChanges();

    const notificationItem = fixture.nativeElement.querySelector('.notification-item') as HTMLElement;

    expect(notificationItem.getAttribute('role')).toBe('button');
    expect(notificationItem.getAttribute('tabindex')).toBe('0');
    expect(notificationItem.getAttribute('aria-label')).toContain('notificationCenter.notification');
  });
});
