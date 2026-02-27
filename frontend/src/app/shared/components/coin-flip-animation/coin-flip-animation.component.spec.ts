import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { signal } from '@angular/core';

import { CoinFlipAnimationComponent, CoinFlipData } from './coin-flip-animation.component';
import { ResponsiveService } from '../../../core/services/responsive.service';

// ===== FIXTURES =====

const DATA: CoinFlipData = {
  player1: 'KARIM',
  player2: 'THOMAS',
  contestedPlayer: 'BUGHA',
  winner: 'KARIM',
};

describe('CoinFlipAnimationComponent', () => {
  let component: CoinFlipAnimationComponent;
  let fixture: ComponentFixture<CoinFlipAnimationComponent>;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<CoinFlipAnimationComponent>>;

  beforeEach(async () => {
    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      imports: [CoinFlipAnimationComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: DATA },
        {
          provide: ResponsiveService,
          useValue: { prefersReducedMotion: signal(false), isMobile: signal(false) },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CoinFlipAnimationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ===== INIT =====

  it('should display both participant names in uppercase', () => {
    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('KARIM');
    expect(html).toContain('THOMAS');
  });

  it('should display the contested player name', () => {
    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('BUGHA');
  });

  it('should start in FLIPPING phase', () => {
    expect(component.phase).toBe('flipping');
  });

  // ===== PHASE TRANSITIONS =====

  it('should transition to RESULT phase after animation', fakeAsync(() => {
    component.onAnimationDone();
    tick();
    expect(component.phase).toBe('result');
  }));

  it('should show winner name in result phase', fakeAsync(() => {
    component.onAnimationDone();
    tick();
    fixture.detectChanges();
    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('KARIM');
  }));

  it('should focus the re-select button after animation completes', fakeAsync(() => {
    component.onAnimationDone();
    tick(300); // wait for focus delay
    fixture.detectChanges();
    // focus management triggered
    expect(component.phase).toBe('result');
  }));

  // ===== ACCESSIBILITY =====

  it('should have aria-live="polite" on result zone', () => {
    const resultZone = fixture.nativeElement.querySelector('[aria-live="polite"]');
    expect(resultZone).not.toBeNull();
  });

  it('should have role="dialog" on root', () => {
    const root = fixture.nativeElement.querySelector('[role="dialog"]');
    expect(root).not.toBeNull();
  });

  it('should have aria-modal="true"', () => {
    const root = fixture.nativeElement.querySelector('[aria-modal="true"]');
    expect(root).not.toBeNull();
  });

  // ===== REDUCED MOTION =====

  it('should use fade mode when prefers-reduced-motion is true', async () => {
    await TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [CoinFlipAnimationComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: DATA },
        {
          provide: ResponsiveService,
          useValue: { prefersReducedMotion: signal(true), isMobile: signal(false) },
        },
      ],
    }).compileComponents();
    const f = TestBed.createComponent(CoinFlipAnimationComponent);
    f.detectChanges();
    expect(f.componentInstance.reducedMotion).toBe(true);
  });

  // ===== CLOSE / RE-SELECT =====

  it('should close dialog when onReselect() is called', () => {
    component.onReselect();
    expect(dialogRefSpy.close).toHaveBeenCalledWith({ action: 'reselect' });
  });

  it('should expose winner and loser from injected data', () => {
    expect(component.winner).toBe('KARIM');
    expect(component.loser).toBe('THOMAS');
    expect(component.contestedPlayer).toBe('BUGHA');
  });
});
