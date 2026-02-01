import { ElementRef, Renderer2, RendererFactory2 } from '@angular/core';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { UiEffectsService } from './ui-effects.service';

describe('UiEffectsService', () => {
  let service: UiEffectsService;
  let renderer: Renderer2;
  let listeners: Map<string, (event: any) => void>;

  const createRenderer = () => {
    listeners = new Map<string, (event: any) => void>();

    const stub = {
      setStyle: jasmine.createSpy('setStyle').and.callFake((el: HTMLElement, prop: string, value: string) => {
        el.style.setProperty(prop, value);
      }),
      addClass: jasmine.createSpy('addClass').and.callFake((el: HTMLElement, className: string) => {
        el.classList.add(className);
      }),
      setProperty: jasmine.createSpy('setProperty').and.callFake((el: HTMLElement, prop: string, value: string) => {
        (el as any)[prop] = value;
      }),
      createElement: jasmine.createSpy('createElement').and.callFake((tag: string) => document.createElement(tag)),
      appendChild: jasmine.createSpy('appendChild').and.callFake((parent: HTMLElement, child: HTMLElement) => {
        parent.appendChild(child);
      }),
      removeChild: jasmine.createSpy('removeChild').and.callFake((parent: HTMLElement, child: HTMLElement) => {
        if (parent.contains(child)) {
          parent.removeChild(child);
        }
      }),
      listen: jasmine.createSpy('listen').and.callFake((_target: HTMLElement, eventName: string, callback: (event: any) => void) => {
        listeners.set(eventName, callback);
        return () => {};
      })
    } as unknown as Renderer2;

    return stub;
  };

  const createElementRef = () => {
    const element = document.createElement('div');
    element.getBoundingClientRect = () => ({
      left: 0,
      top: 0,
      width: 100,
      height: 100,
      right: 100,
      bottom: 100,
      x: 0,
      y: 0,
      toJSON: () => ({})
    } as DOMRect);

    return { elementRef: { nativeElement: element } as ElementRef, element };
  };

  beforeEach(() => {
    renderer = createRenderer();
    const rendererFactory = {
      createRenderer: () => renderer
    } as RendererFactory2;

    TestBed.configureTestingModule({
      providers: [
        UiEffectsService,
        { provide: RendererFactory2, useValue: rendererFactory }
      ]
    });

    service = TestBed.inject(UiEffectsService);
  });

  afterEach(() => {
    document.querySelectorAll('.gaming-notification').forEach((node) => node.remove());
  });

  it('initParallaxCard reacts to mouse movement and reset', () => {
    const { elementRef, element } = createElementRef();

    service.initParallaxCard(elementRef);

    listeners.get('mousemove')?.(new MouseEvent('mousemove', { clientX: 80, clientY: 20 }));
    expect(element.style.transform).toContain('rotateX');

    listeners.get('mouseleave')?.(new MouseEvent('mouseleave'));
    expect(element.style.transform).toContain('rotateX(0deg)');
  });

  it('showGamingNotification creates and removes a notification', fakeAsync(() => {
    service.showGamingNotification('Hello', 'success');

    const notification = document.querySelector('.gaming-notification') as HTMLElement | null;
    expect(notification).toBeTruthy();
    expect(notification?.classList.contains('notification-success')).toBeTrue();
    expect(notification?.textContent).toBe('Hello');

    tick(100);
    expect(notification?.style.transform).toBe('translateX(0)');

    tick(3000);
    tick(400);
    expect(document.querySelector('.gaming-notification')).toBeNull();
  }));

  it('addShimmerEffect and removeShimmerEffect toggle the shimmer overlay', () => {
    const { elementRef, element } = createElementRef();

    service.addShimmerEffect(elementRef);

    expect(element.querySelector('.shimmer-overlay')).not.toBeNull();

    service.removeShimmerEffect(elementRef);

    expect(element.querySelector('.shimmer-overlay')).toBeNull();
  });

  it('addScreenShake applies a transform and clears it', () => {
    const nowSpy = spyOn(Date, 'now');
    let now = 0;
    nowSpy.and.callFake(() => now);

    spyOn(window, 'requestAnimationFrame').and.callFake((callback: FrameRequestCallback) => {
      now += 20;
      callback(0);
      return 0;
    });

    service.addScreenShake(10);

    const calls = (renderer.setStyle as jasmine.Spy).calls.allArgs();
    expect(calls.some((args) => args[1] === 'transform' && args[2] === 'none')).toBeTrue();
  });
});
