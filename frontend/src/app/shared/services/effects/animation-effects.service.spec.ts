import { ElementRef, Renderer2, RendererFactory2 } from '@angular/core';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { AnimationEffectsService } from './animation-effects.service';

describe('AnimationEffectsService', () => {
  let service: AnimationEffectsService;
  let renderer: Renderer2;

  const createRenderer = () => {
    const stub = {
      setProperty: jasmine.createSpy('setProperty').and.callFake((el: HTMLElement, prop: string, value: string) => {
        (el as any)[prop] = value;
      })
    } as unknown as Renderer2;

    return stub;
  };

  beforeEach(() => {
    renderer = createRenderer();
    const rendererFactory = {
      createRenderer: () => renderer
    } as RendererFactory2;

    TestBed.configureTestingModule({
      providers: [
        AnimationEffectsService,
        { provide: RendererFactory2, useValue: rendererFactory }
      ]
    });

    service = TestBed.inject(AnimationEffectsService);
  });

  it('smoothScrollTo returns early when target is not found', () => {
    spyOn(document, 'querySelector').and.returnValue(null);
    const scrollSpy = spyOn(window, 'scrollTo');
    const rafSpy = spyOn(window, 'requestAnimationFrame');

    service.smoothScrollTo('#missing');

    expect(scrollSpy).not.toHaveBeenCalled();
    expect(rafSpy).not.toHaveBeenCalled();
  });

  it('smoothScrollTo scrolls toward the target', () => {
    const target = document.createElement('div');
    Object.defineProperty(target, 'offsetTop', { value: 500 });
    spyOn(document, 'querySelector').and.returnValue(target);
    Object.defineProperty(window, 'pageYOffset', { value: 0, configurable: true });

    const scrollSpy = spyOn(window, 'scrollTo');
    let time = 0;
    spyOn(window, 'requestAnimationFrame').and.callFake((callback: FrameRequestCallback) => {
      time += 800;
      callback(time);
      return 0;
    });

    service.smoothScrollTo('#target', 800);

    expect(scrollSpy).toHaveBeenCalled();
  });

  it('typewriterEffect types the full string', fakeAsync(() => {
    const element = document.createElement('span');
    const elementRef = { nativeElement: element } as ElementRef;
    let resolved = false;

    service.typewriterEffect(elementRef, 'Hi', 10).then(() => {
      resolved = true;
    });

    tick(20);

    expect(element.textContent).toBe('Hi');
    expect(resolved).toBeTrue();
    expect((renderer.setProperty as jasmine.Spy).calls.any()).toBeTrue();
  }));
});
