import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { PlayerSearchFilterComponent } from './player-search-filter.component';

describe('PlayerSearchFilterComponent', () => {
  let component: PlayerSearchFilterComponent;
  let fixture: ComponentFixture<PlayerSearchFilterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PlayerSearchFilterComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(PlayerSearchFilterComponent);
    component = fixture.componentInstance;
    component.availableRegions = ['NAE', 'NAW', 'EU'];
    component.availableTranches = ['expert', 'intermédiaire', 'débutant'];
    fixture.detectChanges();
  });

  afterEach(() => {
    component.ngOnDestroy();
  });

  // ===== DEBOUNCE =====

  it('should debounce search input and emit after 300ms', fakeAsync(() => {
    const spy = jasmine.createSpy('filterChanged');
    component.filterChanged.subscribe(spy);

    component.searchControl.setValue('b');
    component.searchControl.setValue('bu');
    component.searchControl.setValue('bug');

    expect(spy).not.toHaveBeenCalled();

    tick(300);

    expect(spy).toHaveBeenCalledTimes(1);
    expect(spy.calls.mostRecent().args[0].searchTerm).toBe('bug');
  }));

  it('should not re-emit if value did not change', fakeAsync(() => {
    const spy = jasmine.createSpy('filterChanged');
    component.filterChanged.subscribe(spy);

    component.searchControl.setValue('abc');
    tick(300);
    component.searchControl.setValue('abc');
    tick(300);

    expect(spy).toHaveBeenCalledTimes(1);
  }));

  // ===== ACCENT NORMALIZATION =====

  it('should normalize accented characters in emitted searchTerm', fakeAsync(() => {
    const spy = jasmine.createSpy('filterChanged');
    component.filterChanged.subscribe(spy);

    component.searchControl.setValue('éàü');
    tick(300);

    expect(spy.calls.mostRecent().args[0].searchTerm).toBe('eau');
  }));

  it('should lowercase emitted searchTerm', fakeAsync(() => {
    const spy = jasmine.createSpy('filterChanged');
    component.filterChanged.subscribe(spy);

    component.searchControl.setValue('BUGHA');
    tick(300);

    expect(spy.calls.mostRecent().args[0].searchTerm).toBe('bugha');
  }));

  // ===== DRAFT MODE LOCKING =====

  it('should lock region select in draft mode when currentRegion is set', () => {
    component.mode = 'draft';
    component.currentRegion = 'EU';
    fixture.detectChanges();

    const regionSelect = fixture.nativeElement.querySelector('[data-testid="region-select"]');
    expect(regionSelect?.hasAttribute('disabled') || regionSelect?.getAttribute('aria-disabled')).toBeTruthy();
  });

  it('should lock tranche select in draft mode when currentTranche is set', () => {
    component.mode = 'draft';
    component.currentTranche = 'expert';
    fixture.detectChanges();

    const trancheSelect = fixture.nativeElement.querySelector('[data-testid="tranche-select"]');
    expect(trancheSelect?.hasAttribute('disabled') || trancheSelect?.getAttribute('aria-disabled')).toBeTruthy();
  });

  it('should enable region select in browse mode', () => {
    component.mode = 'browse';
    fixture.detectChanges();

    const regionSelect = fixture.nativeElement.querySelector('[data-testid="region-select"]');
    expect(regionSelect?.getAttribute('aria-disabled')).not.toBe('true');
  });

  // ===== DEFAULT TOGGLES =====

  it('should default hideTaken to true in draft mode', () => {
    component.mode = 'draft';
    fixture.detectChanges();

    expect(component.hideTaken).toBe(true);
  });

  it('should default hideUnavailable to true in draft mode', () => {
    component.mode = 'draft';
    fixture.detectChanges();

    expect(component.hideUnavailable).toBe(true);
  });

  // ===== FILTER STRUCTURE =====

  it('should emit complete PlayerFilter structure', fakeAsync(() => {
    const spy = jasmine.createSpy('filterChanged');
    component.filterChanged.subscribe(spy);

    component.searchControl.setValue('test');
    tick(300);

    const filter = spy.calls.mostRecent().args[0];
    expect(filter.searchTerm).toBe('test');
    expect(Object.keys(filter)).toContain('region');
    expect(Object.keys(filter)).toContain('tranche');
    expect(typeof filter.hideUnavailable).toBe('boolean');
    expect(typeof filter.hideTaken).toBe('boolean');
  }));

  it('should emit immediately when toggle changes', () => {
    const spy = jasmine.createSpy('filterChanged');
    component.filterChanged.subscribe(spy);

    component.onToggleChange('hideTaken', false);

    expect(spy).toHaveBeenCalledTimes(1);
    expect(spy.calls.mostRecent().args[0].hideTaken).toBe(false);
  });

  // ===== ARIA =====

  it('should have aria-live="polite" on result counter', () => {
    component.totalResults = 42;
    fixture.detectChanges();

    const counter = fixture.nativeElement.querySelector('.result-counter');
    expect(counter).not.toBeNull();
    expect(counter.getAttribute('aria-live')).toBe('polite');
    expect(counter.textContent).toContain('42');
  });

  it('should display totalResults count', () => {
    component.totalResults = 17;
    fixture.detectChanges();

    const counter = fixture.nativeElement.querySelector('.result-counter');
    expect(counter.textContent).toContain('17');
  });
});
