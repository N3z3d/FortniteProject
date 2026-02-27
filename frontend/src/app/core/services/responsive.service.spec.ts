import { TestBed } from '@angular/core/testing';
import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { Subject } from 'rxjs';
import { ResponsiveService } from './responsive.service';

describe('ResponsiveService', () => {
  let service: ResponsiveService;
  let breakpointSubjects: Map<string, Subject<BreakpointState>>;

  function makeBreakpointState(matches: boolean): BreakpointState {
    return { matches, breakpoints: {} };
  }

  beforeEach(() => {
    breakpointSubjects = new Map();

    const mockObserver = {
      observe: (query: string) => {
        if (!breakpointSubjects.has(query)) {
          breakpointSubjects.set(query, new Subject<BreakpointState>());
        }
        return breakpointSubjects.get(query)!.asObservable();
      },
    };

    TestBed.configureTestingModule({
      providers: [
        ResponsiveService,
        { provide: BreakpointObserver, useValue: mockObserver },
      ],
    });

    service = TestBed.inject(ResponsiveService);
  });

  it('should be created as a singleton', () => {
    const service2 = TestBed.inject(ResponsiveService);
    expect(service).toBe(service2);
  });

  it('should default isMobile to false before any breakpoint event', () => {
    expect(service.isMobile()).toBe(false);
  });

  it('should set isMobile to true when viewport is below 768px', () => {
    TestBed.runInInjectionContext(() => {
      breakpointSubjects.get('(max-width: 767px)')?.next(makeBreakpointState(true));
    });
    expect(service.isMobile()).toBe(true);
  });

  it('should set hideSparkline to true when viewport is below 480px', () => {
    TestBed.runInInjectionContext(() => {
      breakpointSubjects.get('(max-width: 479px)')?.next(makeBreakpointState(true));
    });
    expect(service.hideSparkline()).toBe(true);
  });

  it('should set prefersReducedMotion to true when media query matches', () => {
    TestBed.runInInjectionContext(() => {
      breakpointSubjects
        .get('(prefers-reduced-motion: reduce)')
        ?.next(makeBreakpointState(true));
    });
    expect(service.prefersReducedMotion()).toBe(true);
  });

  it('should revert isMobile to false when viewport grows above 768px', () => {
    TestBed.runInInjectionContext(() => {
      const subject = breakpointSubjects.get('(max-width: 767px)')!;
      subject.next(makeBreakpointState(true));
      expect(service.isMobile()).toBe(true);
      subject.next(makeBreakpointState(false));
    });
    expect(service.isMobile()).toBe(false);
  });
});
