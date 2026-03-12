import { ElementRef, Renderer2, RendererFactory2 } from '@angular/core';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ButtonEffectsService } from './button-effects.service';

describe('ButtonEffectsService', () => {
  let service: ButtonEffectsService;
  let renderer: Renderer2;
  let rendererFactory: RendererFactory2;
  let listeners: Map<string, (event: any) => void>;

  const createRenderer = () => {
    listeners = new Map<string, (event: any) => void>();

    const stub = {
      setStyle: jasmine.createSpy('setStyle').and.callFake((el: HTMLElement, prop: string, value: string) => {
        el.style.setProperty(prop, value);
      }),
      removeStyle: jasmine.createSpy('removeStyle').and.callFake((el: HTMLElement, prop: string) => {
        el.style.removeProperty(prop);
      }),
      addClass: jasmine.createSpy('addClass').and.callFake((el: HTMLElement, className: string) => {
        el.classList.add(className);
      }),
      removeClass: jasmine.createSpy('removeClass').and.callFake((el: HTMLElement, className: string) => {
        el.classList.remove(className);
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

  const setup = () => {
    renderer = createRenderer();
    rendererFactory = {
      createRenderer: () => renderer
    } as RendererFactory2;

    TestBed.configureTestingModule({
      providers: [
        ButtonEffectsService,
        { provide: RendererFactory2, useValue: rendererFactory }
      ]
    });

    service = TestBed.inject(ButtonEffectsService);
  };

  const createElementRef = () => {
    const button = document.createElement('button');
    button.getBoundingClientRect = () => ({
      left: 0,
      top: 0,
      width: 100,
      height: 50,
      right: 100,
      bottom: 50,
      x: 0,
      y: 0,
      toJSON: () => ({})
    } as DOMRect);

    return { elementRef: { nativeElement: button } as ElementRef, button };
  };

  beforeEach(() => {
    setup();
  });

  it('initMagneticButton applies hover styles and cleanup', () => {
    const { elementRef, button } = createElementRef();

    service.initMagneticButton(elementRef, 0.5);

    expect(listeners.has('mouseenter')).toBeTrue();
    expect(listeners.has('mousemove')).toBeTrue();
    expect(listeners.has('mouseleave')).toBeTrue();

    listeners.get('mouseenter')?.(new MouseEvent('mouseenter'));
    expect(button.classList.contains('magnetic-active')).toBeTrue();

    listeners.get('mousemove')?.(new MouseEvent('mousemove', { clientX: 60, clientY: 10 }));
    expect(button.classList.contains('magnetic-hover')).toBeTrue();
    expect((renderer.setStyle as jasmine.Spy).calls.any()).toBeTrue();

    listeners.get('mouseleave')?.(new MouseEvent('mouseleave'));
    expect(button.classList.contains('magnetic-active')).toBeFalse();
    expect(button.classList.contains('magnetic-hover')).toBeFalse();
    expect((renderer.removeStyle as jasmine.Spy).calls.any()).toBeTrue();
  });

  it('createRipple creates and removes the ripple element', fakeAsync(() => {
    const { elementRef, button } = createElementRef();

    service.createRipple(elementRef, new MouseEvent('click', { clientX: 10, clientY: 10 }));

    const ripple = button.querySelector('span.ripple') as HTMLElement | null;
    expect(ripple).toBeTruthy();
    expect(ripple?.style.left).toBe('10px');
    expect(ripple?.style.top).toBe('10px');

    tick(600);

    expect(button.querySelector('span.ripple')).toBeNull();
  }));

  it('initSpringButton updates styles on press and release', fakeAsync(() => {
    const { elementRef, button } = createElementRef();

    service.initSpringButton(elementRef);

    listeners.get('mousedown')?.(new MouseEvent('mousedown'));
    expect(button.style.transform).toContain('translateZ');

    listeners.get('mouseup')?.(new MouseEvent('mouseup'));
    expect((renderer.removeStyle as jasmine.Spy).calls.any()).toBeTrue();

    tick(300);
    expect(button.style.transform).toContain('scale(1)');
  }));

  it('initGlowEffect toggles glow styles', () => {
    const { elementRef, button } = createElementRef();

    service.initGlowEffect(elementRef, '#ff0000');

    listeners.get('mouseenter')?.(new MouseEvent('mouseenter'));
    expect(button.style.boxShadow).toContain('#ff000040');

    listeners.get('mouseleave')?.(new MouseEvent('mouseleave'));
    expect(button.style.boxShadow).toBe('');
  });
});
