import { fakeAsync, tick } from '@angular/core/testing';
import { AccessibilityAnnouncerService } from './accessibility-announcer.service';

describe('AccessibilityAnnouncerService', () => {
  let service: AccessibilityAnnouncerService;

  const getAnnouncer = () =>
    document.querySelector('.accessibility-announcer') as HTMLElement | null;

  beforeEach(() => {
    service = new AccessibilityAnnouncerService();
  });

  afterEach(() => {
    service.destroy();
  });

  it('creates the announcer element with required attributes', () => {
    const announcer = getAnnouncer();

    expect(announcer).toBeTruthy();
    expect(announcer?.getAttribute('aria-live')).toBe('polite');
    expect(announcer?.getAttribute('aria-atomic')).toBe('true');
    expect(announcer?.getAttribute('aria-hidden')).toBe('false');
  });

  it('announce sets polite message after delay', fakeAsync(() => {
    const announcer = getAnnouncer() as HTMLElement;

    service.announce('Hello');

    expect(announcer.textContent).toBe('');
    tick(100);

    expect(announcer.textContent).toBe('Hello');
    expect(announcer.getAttribute('aria-live')).toBe('polite');
  }));

  it('announceUrgent uses assertive priority', fakeAsync(() => {
    const announcer = getAnnouncer() as HTMLElement;

    service.announceUrgent('Warning');
    tick(100);

    expect(announcer.textContent).toBe('Warning');
    expect(announcer.getAttribute('aria-live')).toBe('assertive');
  }));

  it('announceLoading builds messages for loading and loaded states', () => {
    const announceSpy = spyOn(service, 'announce');

    service.announceLoading(true, 'data');
    service.announceLoading(false, 'data');

    const calls = announceSpy.calls.allArgs().map(args => args[0] as string);

    expect(calls[0]).toContain('Chargement');
    expect(calls[0]).toContain('data');
    expect(calls[1]).toContain('data');
    expect(calls[1].toLowerCase()).toContain('charg');
  });

  it('announceFormErrors uses singular/plural messages', () => {
    const urgentSpy = spyOn(service, 'announceUrgent');

    service.announceFormErrors([]);
    expect(urgentSpy).not.toHaveBeenCalled();

    service.announceFormErrors(['A']);
    expect(urgentSpy).toHaveBeenCalledWith('Erreur de validation: A');

    urgentSpy.calls.reset();

    service.announceFormErrors(['A', 'B']);
    expect(urgentSpy).toHaveBeenCalledWith('2 erreurs de validation: A, B');
  });

  it('destroy removes the announcer element', () => {
    service.destroy();
    expect(getAnnouncer()).toBeNull();
  });
});
