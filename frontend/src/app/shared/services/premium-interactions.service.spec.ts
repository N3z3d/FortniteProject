import { ElementRef, Renderer2, RendererFactory2 } from '@angular/core';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { PremiumInteractionsService } from './premium-interactions.service';

class MockRenderer implements Renderer2 {
  data: { [key: string]: any } = {};
  listeners: Array<{ target: any; eventName: string; callback: (event?: any) => void }> = [];
  destroyNode: ((node: any) => void) | null = null;

  destroy = jasmine.createSpy('destroy');
  createElement = jasmine.createSpy('createElement').and.callFake((name: string) => document.createElement(name));
  createComment = jasmine.createSpy('createComment').and.callFake((value: string) => document.createComment(value));
  createText = jasmine.createSpy('createText').and.callFake((value: string) => document.createTextNode(value));
  appendChild = jasmine.createSpy('appendChild').and.callFake((parent: any, child: any) => {
    if (parent && child) {
      parent.appendChild(child);
    }
  });
  insertBefore = jasmine.createSpy('insertBefore').and.callFake((parent: any, child: any, ref: any) => {
    if (parent && child) {
      parent.insertBefore(child, ref);
    }
  });
  removeChild = jasmine.createSpy('removeChild').and.callFake((parent: any, child: any) => {
    if (parent && child && parent.contains(child)) {
      parent.removeChild(child);
    }
  });
  selectRootElement = jasmine.createSpy('selectRootElement').and.callFake((selector: string) => {
    return document.querySelector(selector);
  });
  parentNode = jasmine.createSpy('parentNode').and.callFake((node: any) => node.parentNode);
  nextSibling = jasmine.createSpy('nextSibling').and.callFake((node: any) => node.nextSibling);
  setAttribute = jasmine.createSpy('setAttribute').and.callFake((el: any, name: string, value: string) => {
    el.setAttribute(name, value);
  });
  removeAttribute = jasmine.createSpy('removeAttribute').and.callFake((el: any, name: string) => {
    el.removeAttribute(name);
  });
  addClass = jasmine.createSpy('addClass').and.callFake((el: any, name: string) => {
    el.classList.add(name);
  });
  removeClass = jasmine.createSpy('removeClass').and.callFake((el: any, name: string) => {
    el.classList.remove(name);
  });
  setStyle = jasmine.createSpy('setStyle').and.callFake((el: any, style: string, value: any) => {
    el.style.setProperty(style, value);
  });
  removeStyle = jasmine.createSpy('removeStyle').and.callFake((el: any, style: string) => {
    el.style.removeProperty(style);
  });
  setProperty = jasmine.createSpy('setProperty').and.callFake((el: any, name: string, value: any) => {
    el[name] = value;
  });
  setValue = jasmine.createSpy('setValue').and.callFake((node: any, value: string) => {
    node.nodeValue = value;
  });
  listen = jasmine.createSpy('listen').and.callFake((target: any, eventName: string, callback: any) => {
    this.listeners.push({ target, eventName, callback });
    return () => undefined;
  });
}

describe('PremiumInteractionsService', () => {
  let service: PremiumInteractionsService;
  let renderer: MockRenderer;

  const getListener = (eventName: string) => {
    const entry = renderer.listeners.find(listener => listener.eventName === eventName);
    if (!entry) {
      throw new Error(`Listener for ${eventName} not found`);
    }
    return entry.callback;
  };

  beforeEach(() => {
    renderer = new MockRenderer();

    TestBed.configureTestingModule({
      providers: [
        PremiumInteractionsService,
        {
          provide: RendererFactory2,
          useValue: {
            createRenderer: () => renderer
          }
        }
      ]
    });

    service = TestBed.inject(PremiumInteractionsService);
  });

  it('initMagneticButton registers listeners and applies hover styles', fakeAsync(() => {
    const element = document.createElement('button');
    spyOn(element, 'getBoundingClientRect').and.returnValue({
      left: 0,
      top: 0,
      width: 100,
      height: 50,
      right: 100,
      bottom: 50,
      x: 0,
      y: 0,
      toJSON: () => ({})
    });

    service.initMagneticButton(new ElementRef(element), 0.5);

    const enter = getListener('mouseenter');
    const move = getListener('mousemove');
    const leave = getListener('mouseleave');

    enter();
    move(new MouseEvent('mousemove', { clientX: 10, clientY: 20 }));
    leave();

    expect(renderer.addClass).toHaveBeenCalledWith(element, 'magnetic-active');
    expect(renderer.addClass).toHaveBeenCalledWith(element, 'magnetic-hover');
    expect(renderer.setStyle).toHaveBeenCalledWith(element, 'transform', jasmine.any(String));
    expect(renderer.removeClass).toHaveBeenCalledWith(element, 'magnetic-hover');
  }));

  it('createRipple adds and removes ripple element', fakeAsync(() => {
    const element = document.createElement('button');
    spyOn(element, 'getBoundingClientRect').and.returnValue({
      left: 5,
      top: 6,
      width: 100,
      height: 50,
      right: 105,
      bottom: 56,
      x: 0,
      y: 0,
      toJSON: () => ({})
    });

    service.createRipple(new ElementRef(element), new MouseEvent('click', { clientX: 15, clientY: 25 }));

    expect(renderer.appendChild).toHaveBeenCalled();
    tick(600);
    expect(renderer.removeChild).toHaveBeenCalled();
  }));

  it('initSpringButton reacts to press and release', fakeAsync(() => {
    const element = document.createElement('button');
    spyOn(element, 'getBoundingClientRect').and.returnValue({
      left: 0,
      top: 0,
      width: 100,
      height: 50,
      right: 100,
      bottom: 50,
      x: 0,
      y: 0,
      toJSON: () => ({})
    });

    service.initSpringButton(new ElementRef(element));

    const down = getListener('mousedown');
    const up = getListener('mouseup');

    down(new MouseEvent('mousedown', { clientX: 10, clientY: 10 }));
    expect(renderer.setStyle).toHaveBeenCalledWith(
      element,
      'transform',
      jasmine.stringContaining('translateZ(-8px)')
    );

    up();
    tick(300);
    expect(renderer.setStyle).toHaveBeenCalledWith(
      element,
      'transform',
      jasmine.stringContaining('scale(1)')
    );
  }));

  it('initParallaxCard updates transform on mouse move and resets on leave', () => {
    const element = document.createElement('div');
    spyOn(element, 'getBoundingClientRect').and.returnValue({
      left: 0,
      top: 0,
      width: 200,
      height: 100,
      right: 200,
      bottom: 100,
      x: 0,
      y: 0,
      toJSON: () => ({})
    });

    service.initParallaxCard(new ElementRef(element));

    const move = getListener('mousemove');
    const leave = getListener('mouseleave');

    move(new MouseEvent('mousemove', { clientX: 30, clientY: 20 }));
    leave();

    expect(renderer.setStyle).toHaveBeenCalledWith(element, 'transform', jasmine.any(String));
  });

  it('initGlowEffect applies and removes glow styles', () => {
    const element = document.createElement('div');

    service.initGlowEffect(new ElementRef(element), '#ff0000');

    const enter = getListener('mouseenter');
    const leave = getListener('mouseleave');

    enter();
    leave();

    expect(renderer.setStyle).toHaveBeenCalledWith(element, 'box-shadow', jasmine.stringContaining('#ff0000'));
    expect(renderer.removeStyle).toHaveBeenCalledWith(element, 'box-shadow');
  });

  it('smoothScrollTo scrolls when target exists and skips when missing', () => {
    const target = document.createElement('div');
    target.id = 'scroll-target';
    document.body.appendChild(target);

    spyOn(window, 'scrollTo');
    let time = 0;
    spyOn(window, 'requestAnimationFrame').and.callFake((callback: FrameRequestCallback) => {
      time += 800;
      callback(time);
      return 0;
    });

    service.smoothScrollTo('#scroll-target', 800);
    expect(window.scrollTo).toHaveBeenCalled();

    (window.scrollTo as jasmine.Spy).calls.reset();
    service.smoothScrollTo('#missing-target', 800);
    expect(window.scrollTo).not.toHaveBeenCalled();
  });

  it('typewriterEffect writes text and resolves', fakeAsync(() => {
    const element = document.createElement('span');
    const ref = new ElementRef(element);
    let resolved = false;

    service.typewriterEffect(ref, 'Hi', 10).then(() => {
      resolved = true;
    });

    tick(30);

    expect(element.textContent).toBe('Hi');
    expect(resolved).toBeTrue();
  }));

  it('showGamingNotification appends and removes notification', fakeAsync(() => {
    service.showGamingNotification('Victory', 'success');

    const notification = document.body.querySelector('.gaming-notification') as HTMLElement;
    expect(notification).not.toBeNull();
    expect(notification.classList.contains('notification-success')).toBeTrue();

    tick(3500);

    expect(document.body.querySelector('.gaming-notification')).toBeNull();
  }));

  it('addShimmerEffect adds overlay and removeShimmerEffect removes it', () => {
    const element = document.createElement('div');
    const ref = new ElementRef(element);

    service.addShimmerEffect(ref);
    expect(element.querySelector('.shimmer-overlay')).not.toBeNull();

    service.removeShimmerEffect(ref);
    expect(element.querySelector('.shimmer-overlay')).toBeNull();
  });

  it('addContextualFeedback wires click to particle explosion', () => {
    const element = document.createElement('button');
    const ref = new ElementRef(element);
    const explosionSpy = spyOn<any>(service, 'createParticleExplosion');

    service.addContextualFeedback(ref, 'high');

    const clickListener = getListener('click');
    clickListener();

    expect(renderer.addClass).toHaveBeenCalledWith(element, 'importance-high');
    expect(renderer.setStyle).toHaveBeenCalledWith(element, '--importance-glow', jasmine.any(String));
    expect(explosionSpy).toHaveBeenCalled();
  });

  it('initFortniteButton applies classes and triggers effects', () => {
    const element = document.createElement('button');
    const ref = new ElementRef(element);

    const magneticSpy = spyOn(service, 'initMagneticButton');
    const contextualSpy = spyOn(service, 'addContextualFeedback');
    const springSpy = spyOn(service, 'initSpringButton');
    const glowSpy = spyOn(service, 'initGlowEffect');

    service.initFortniteButton(ref, { type: 'primary', size: 'large', effects: true });

    expect(renderer.addClass).toHaveBeenCalledWith(element, 'fortnite-btn');
    expect(renderer.addClass).toHaveBeenCalledWith(element, 'fortnite-btn-primary');
    expect(renderer.addClass).toHaveBeenCalledWith(element, 'fortnite-btn-large');
    expect(magneticSpy).toHaveBeenCalled();
    expect(contextualSpy).toHaveBeenCalledWith(ref, 'high');
    expect(springSpy).toHaveBeenCalled();
    expect(glowSpy).toHaveBeenCalled();
  });
});
