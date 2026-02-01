import { ElementRef, Renderer2, RendererFactory2 } from '@angular/core';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ParticleEffectsService } from './particle-effects.service';

describe('ParticleEffectsService', () => {
  let service: ParticleEffectsService;
  let renderer: Renderer2;

  const createRenderer = () => {
    const stub = {
      setStyle: jasmine.createSpy('setStyle').and.callFake((el: HTMLElement, prop: string, value: string) => {
        el.style.setProperty(prop, value);
      }),
      addClass: jasmine.createSpy('addClass').and.callFake((el: HTMLElement, className: string) => {
        el.classList.add(className);
      }),
      createElement: jasmine.createSpy('createElement').and.callFake((tag: string) => document.createElement(tag)),
      appendChild: jasmine.createSpy('appendChild').and.callFake((parent: HTMLElement, child: HTMLElement) => {
        parent.appendChild(child);
      }),
      removeChild: jasmine.createSpy('removeChild').and.callFake((parent: HTMLElement, child: HTMLElement) => {
        if (parent.contains(child)) {
          parent.removeChild(child);
        }
      })
    } as unknown as Renderer2;

    return stub;
  };

  const createElementRef = () => {
    const element = document.createElement('div');
    element.getBoundingClientRect = () => ({
      left: 0,
      top: 0,
      width: 120,
      height: 60,
      right: 120,
      bottom: 60,
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
        ParticleEffectsService,
        { provide: RendererFactory2, useValue: rendererFactory }
      ]
    });

    service = TestBed.inject(ParticleEffectsService);
  });

  it('createParticleExplosion generates and removes particles', fakeAsync(() => {
    const { elementRef, element } = createElementRef();
    spyOn(Math, 'random').and.returnValue(0);

    service.createParticleExplosion(elementRef, 3);

    tick(0);
    tick(100);
    tick(100);

    expect(element.querySelectorAll('.explosion-particle').length).toBe(3);

    tick(1000);

    expect(element.querySelectorAll('.explosion-particle').length).toBe(0);
    expect((renderer.removeChild as jasmine.Spy).calls.count()).toBe(3);
  }));

  it('createMagneticParticle appends and removes the particle', fakeAsync(() => {
    const button = document.createElement('button');

    service.createMagneticParticle(button, 10, 20);

    expect(button.querySelectorAll('div').length).toBe(1);

    tick(500);

    expect(button.querySelectorAll('div').length).toBe(0);
  }));

  it('createPressRipple appends and removes the ripple', fakeAsync(() => {
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

    service.createPressRipple(button, new MouseEvent('click', { clientX: 20, clientY: 10 }));

    expect(button.querySelectorAll('div').length).toBe(1);

    tick(400);

    expect(button.querySelectorAll('div').length).toBe(0);
  }));
});
