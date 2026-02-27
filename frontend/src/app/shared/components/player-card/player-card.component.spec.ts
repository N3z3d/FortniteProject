import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { PlayerCardComponent, RankSnapshot } from './player-card.component';
import { ResponsiveService } from '../../../core/services/responsive.service';
import { AvailablePlayer } from '../../../features/draft/models/draft.interface';

const mockPlayer: AvailablePlayer = {
  id: 'p1',
  username: 'Bugha',
  nickname: 'Bugha',
  region: 'NAE',
  tranche: 'expert',
  currentSeason: 1,
  totalPoints: 1850,
  isRecommended: false,
};

const twoSnapshots: RankSnapshot[] = [
  { date: '2026-01-01', rank: 120 },
  { date: '2026-01-15', rank: 95 },
];

function makeResponsiveMock(hideSparkline = false) {
  return {
    isMobile: signal(false),
    hideSparkline: signal(hideSparkline),
    prefersReducedMotion: signal(false),
  };
}

describe('PlayerCardComponent', () => {
  let component: PlayerCardComponent;
  let fixture: ComponentFixture<PlayerCardComponent>;
  let responsiveMock: ReturnType<typeof makeResponsiveMock>;

  beforeEach(async () => {
    responsiveMock = makeResponsiveMock();

    await TestBed.configureTestingModule({
      imports: [PlayerCardComponent, NoopAnimationsModule],
      providers: [{ provide: ResponsiveService, useValue: responsiveMock }],
    }).compileComponents();

    fixture = TestBed.createComponent(PlayerCardComponent);
    component = fixture.componentInstance;
    component.player = mockPlayer;
    fixture.detectChanges();
  });

  // ===== MODES =====

  it('should apply draft mode class when mode is draft', () => {
    component.mode = 'draft';
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.player-card--draft')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.player-card--browse')).toBeNull();
  });

  it('should apply browse mode class when mode is browse', () => {
    component.mode = 'browse';
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.player-card--browse')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.player-card--draft')).toBeNull();
  });

  // ===== NAME DISPLAY =====

  it('should display player username', () => {
    expect(fixture.nativeElement.querySelector('.player-name').textContent).toContain('Bugha');
  });

  it('should apply uppercase + letter-spacing style via CSS class on player-name', () => {
    const name = fixture.nativeElement.querySelector('.player-name');
    expect(name).not.toBeNull();
    expect(name.classList.contains('player-name')).toBe(true);
  });

  // ===== STATES =====

  it('should apply selected class when selected is true', () => {
    component.selected = true;
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.player-card--selected')).not.toBeNull();
  });

  it('should set aria-selected to true when selected', () => {
    component.selected = true;
    fixture.detectChanges();

    const card = fixture.nativeElement.querySelector('[role="button"]');
    expect(card.getAttribute('aria-selected')).toBe('true');
  });

  it('should set aria-selected to false when not selected', () => {
    component.selected = false;
    fixture.detectChanges();

    const card = fixture.nativeElement.querySelector('[role="button"]');
    expect(card.getAttribute('aria-selected')).toBe('false');
  });

  it('should apply taken class and reduce opacity when taken is true', () => {
    component.taken = true;
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.player-card--taken')).not.toBeNull();
  });

  it('should show Signaler button only when taken', () => {
    component.taken = false;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.btn-report')).toBeNull();

    component.taken = true;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.btn-report')).not.toBeNull();
  });

  it('should show recommended badge when recommended is true', () => {
    component.recommended = true;
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.badge-recommended')).not.toBeNull();
  });

  it('should hide recommended badge when recommended is false', () => {
    component.recommended = false;
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.badge-recommended')).toBeNull();
  });

  // ===== INTERACTION =====

  it('should emit cardSelected on click when not taken', () => {
    const spy = jasmine.createSpy('cardSelected');
    component.cardSelected.subscribe(spy);
    component.taken = false;
    fixture.detectChanges();

    fixture.nativeElement.querySelector('[role="button"]').click();

    expect(spy).toHaveBeenCalledWith(mockPlayer);
  });

  it('should NOT emit cardSelected on click when taken', () => {
    const spy = jasmine.createSpy('cardSelected');
    component.cardSelected.subscribe(spy);
    component.taken = true;
    fixture.detectChanges();

    fixture.nativeElement.querySelector('[role="button"]').click();

    expect(spy).not.toHaveBeenCalled();
  });

  it('should emit cardSelected on Enter key', () => {
    const spy = jasmine.createSpy('cardSelected');
    component.cardSelected.subscribe(spy);
    component.taken = false;
    fixture.detectChanges();

    const card = fixture.nativeElement.querySelector('[role="button"]');
    card.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));

    expect(spy).toHaveBeenCalledWith(mockPlayer);
  });

  it('should emit cardSelected on Space key', () => {
    const spy = jasmine.createSpy('cardSelected');
    component.cardSelected.subscribe(spy);
    component.taken = false;
    fixture.detectChanges();

    const card = fixture.nativeElement.querySelector('[role="button"]');
    card.dispatchEvent(new KeyboardEvent('keydown', { key: ' ' }));

    expect(spy).toHaveBeenCalledWith(mockPlayer);
  });

  it('should emit reportPlayer when Signaler button is clicked', () => {
    const spy = jasmine.createSpy('reportPlayer');
    component.reportPlayer.subscribe(spy);
    component.taken = true;
    fixture.detectChanges();

    fixture.nativeElement.querySelector('.btn-report').click();

    expect(spy).toHaveBeenCalledWith(mockPlayer);
  });

  // ===== SPARKLINE =====

  it('should show sparkline slot in browse mode with >= 2 snapshots', () => {
    component.mode = 'browse';
    component.snapshots = twoSnapshots;
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.sparkline-slot')).not.toBeNull();
  });

  it('should hide sparkline slot in browse mode with < 2 snapshots', () => {
    component.mode = 'browse';
    component.snapshots = [{ date: '2026-01-01', rank: 100 }];
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.sparkline-slot')).toBeNull();
  });

  it('should hide sparkline slot in draft mode even with >= 2 snapshots', () => {
    component.mode = 'draft';
    component.snapshots = twoSnapshots;
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.sparkline-slot')).toBeNull();
  });

  it('should hide sparkline when ResponsiveService.hideSparkline is true', () => {
    responsiveMock.hideSparkline.set(true);
    component.mode = 'browse';
    component.snapshots = twoSnapshots;
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.sparkline-slot')).toBeNull();
  });

  // ===== ARIA =====

  it('should include username in aria-label', () => {
    const card = fixture.nativeElement.querySelector('[role="button"]');
    expect(card.getAttribute('aria-label')).toContain('Bugha');
  });
});
