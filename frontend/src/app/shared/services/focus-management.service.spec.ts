import { FocusManagementService } from './focus-management.service';

describe('FocusManagementService', () => {
  let service: FocusManagementService;
  let container: HTMLElement;

  const createButton = (label: string) => {
    const button = document.createElement('button');
    button.textContent = label;
    button.tabIndex = 0;
    Object.defineProperty(button, 'offsetParent', {
      configurable: true,
      get: () => (button.style.display === 'none' ? null : container)
    });
    return button;
  };

  beforeEach(() => {
    service = new FocusManagementService();
    container = document.createElement('div');
    document.body.appendChild(container);
  });

  afterEach(() => {
    container.remove();
  });

  it('setFocus stores previous focus and restoreFocus returns it', () => {
    const first = createButton('first');
    const second = createButton('second');
    container.append(first, second);

    first.focus();
    expect(document.activeElement).toBe(first);

    service.setFocus(second);
    expect(document.activeElement).toBe(second);

    service.restoreFocus();
    expect(document.activeElement).toBe(first);
  });

  it('pushFocus and popFocus manage the focus stack', () => {
    const first = createButton('first');
    const second = createButton('second');
    container.append(first, second);

    first.focus();

    service.pushFocus(second);
    expect(document.activeElement).toBe(second);

    service.popFocus();
    expect(document.activeElement).toBe(first);
  });

  it('setFocusById and setFocusBySelector focus matching elements', () => {
    const byId = createButton('byId');
    byId.id = 'focus-target';
    const byClass = createButton('byClass');
    byClass.className = 'focus-target';

    container.append(byId, byClass);

    service.setFocusById('focus-target');
    expect(document.activeElement).toBe(byId);

    service.setFocusBySelector('.focus-target');
    expect(document.activeElement).toBe(byClass);
  });

  it('getFocusableElements filters hidden and disabled elements', () => {
    const enabled = createButton('enabled');
    const disabled = createButton('disabled');
    disabled.setAttribute('disabled', 'true');
    const hidden = createButton('hidden');
    hidden.style.display = 'none';
    const ariaDisabled = createButton('ariaDisabled');
    ariaDisabled.setAttribute('aria-disabled', 'true');

    container.append(enabled, disabled, hidden, ariaDisabled);

    const focusable = service.getFocusableElements(container);
    expect(focusable).toEqual([enabled]);
  });

  it('trapFocus cycles focus with tab and shift+tab', () => {
    const first = createButton('first');
    const last = createButton('last');
    container.append(first, last);

    const cleanup = service.trapFocus(container);
    expect(document.activeElement).toBe(first);

    last.focus();
    const tabEvent = new KeyboardEvent('keydown', { key: 'Tab', bubbles: true, cancelable: true });
    container.dispatchEvent(tabEvent);

    expect(tabEvent.defaultPrevented).toBeTrue();
    expect(document.activeElement).toBe(first);

    first.focus();
    const shiftTabEvent = new KeyboardEvent('keydown', {
      key: 'Tab',
      shiftKey: true,
      bubbles: true,
      cancelable: true
    });
    container.dispatchEvent(shiftTabEvent);

    expect(shiftTabEvent.defaultPrevented).toBeTrue();
    expect(document.activeElement).toBe(last);

    cleanup();
  });

  it('focusFirstElement and focusLastElement focus the correct element', () => {
    const first = createButton('first');
    const last = createButton('last');
    container.append(first, last);

    service.focusFirstElement(container);
    expect(document.activeElement).toBe(first);

    service.focusLastElement(container);
    expect(document.activeElement).toBe(last);
  });

  it('announceFocus adds a temporary announcer and removes it', async () => {
    vi.useFakeTimers();
    const target = createButton('target');
    container.append(target);

    service.announceFocus(target, 'ANNOUNCE_TEST');

    const announcer = Array.from(document.body.querySelectorAll('div'))
      .find(element => element.textContent === 'ANNOUNCE_TEST') as HTMLElement;

    expect(announcer).toBeTruthy();
    expect(document.body.contains(announcer)).toBeTrue();

    vi.advanceTimersByTime(1000);
    await Promise.resolve();

    expect(document.body.contains(announcer)).toBeFalse();
    vi.useRealTimers();
  });
});
