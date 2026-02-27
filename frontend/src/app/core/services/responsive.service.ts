import { Injectable, inject } from '@angular/core';
import { BreakpointObserver } from '@angular/cdk/layout';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class ResponsiveService {
  private readonly bo = inject(BreakpointObserver);

  /** true when viewport width < 768px */
  readonly isMobile = toSignal(
    this.bo.observe('(max-width: 767px)').pipe(map(s => s.matches)),
    { initialValue: false }
  );

  /** true when viewport width < 480px — hide sparkline charts */
  readonly hideSparkline = toSignal(
    this.bo.observe('(max-width: 479px)').pipe(map(s => s.matches)),
    { initialValue: false }
  );

  /** true when the user prefers reduced motion */
  readonly prefersReducedMotion = toSignal(
    this.bo.observe('(prefers-reduced-motion: reduce)').pipe(map(s => s.matches)),
    { initialValue: false }
  );
}
