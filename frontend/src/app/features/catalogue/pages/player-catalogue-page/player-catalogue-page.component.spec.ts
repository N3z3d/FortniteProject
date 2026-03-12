import { Component, Input, Output, EventEmitter } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import {
  PlayerCataloguePageComponent,
} from './player-catalogue-page.component';
import { PlayerCatalogueService } from '../../services/player-catalogue.service';
import { AvailablePlayer } from '../../../draft/models/draft.interface';
import {
  PlayerFilter,
  PlayerSearchFilterComponent,
} from '../../../../shared/components/player-search-filter/player-search-filter.component';
import { PlayerCardComponent } from '../../../../shared/components/player-card/player-card.component';
import { SparklineChartComponent } from '../../../../shared/components/sparkline-chart/sparkline-chart.component';

// ===== STUBS =====

@Component({
  selector: 'app-player-search-filter',
  standalone: true,
  template: '',
})
class MockPlayerSearchFilter {
  @Input() mode = 'browse';
  @Input() availableRegions: string[] = [];
  @Input() availableTranches: string[] = [];
  @Input() totalResults = 0;
  @Output() readonly filterChanged = new EventEmitter<PlayerFilter>();
}

@Component({
  selector: 'app-player-card',
  standalone: true,
  template: '<div class="player-card-stub">{{ player?.username }}</div>',
})
class MockPlayerCard {
  @Input() player!: AvailablePlayer;
  @Input() mode = 'browse';
  @Input() selected = false;
  @Output() readonly cardSelected = new EventEmitter<AvailablePlayer>();
  @Output() readonly reportPlayer = new EventEmitter<AvailablePlayer>();
}

@Component({
  selector: 'app-sparkline-chart',
  standalone: true,
  template: '',
})
class MockSparklineChart {
  @Input() snapshots: unknown[] = [];
}

// ===== FIXTURES =====

const buildPlayer = (id: string, username: string, region = 'EU', tranche = '1'): AvailablePlayer => ({
  id,
  username,
  nickname: username,
  region,
  tranche,
  currentSeason: 2025,
  available: true,
});

const PLAYERS: AvailablePlayer[] = [
  buildPlayer('1', 'Alpha', 'EU', '1'),
  buildPlayer('2', 'Bravo', 'NAC', '2'),
  buildPlayer('3', 'Charlie', 'EU', '1'),
];

const CATALOGUE_DEBOUNCE_SETTLE_MS = 250;

describe('PlayerCataloguePageComponent', () => {
  let component: PlayerCataloguePageComponent;
  let fixture: ComponentFixture<PlayerCataloguePageComponent>;
  let catalogueSpy: jasmine.SpyObj<PlayerCatalogueService>;

  const waitForCatalogueDebounce = async (): Promise<void> => {
    await vi.advanceTimersByTimeAsync(CATALOGUE_DEBOUNCE_SETTLE_MS);
    fixture.detectChanges();
  };

  const initializeCatalogue = async (): Promise<void> => {
    fixture.detectChanges();
    await waitForCatalogueDebounce();
  };

  beforeEach(async () => {
    vi.useFakeTimers();
    catalogueSpy = jasmine.createSpyObj('PlayerCatalogueService', ['getPlayers', 'getSparkline']);
    catalogueSpy.getPlayers.and.returnValue(of(PLAYERS));
    catalogueSpy.getSparkline.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [PlayerCataloguePageComponent, NoopAnimationsModule],
      providers: [{ provide: PlayerCatalogueService, useValue: catalogueSpy }],
    })
      .overrideComponent(PlayerCataloguePageComponent, {
        remove: { imports: [PlayerSearchFilterComponent, PlayerCardComponent, SparklineChartComponent] },
        add: { imports: [MockPlayerSearchFilter, MockPlayerCard, MockSparklineChart] },
      })
      .compileComponents();

    fixture = TestBed.createComponent(PlayerCataloguePageComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  // ===== INIT =====

  describe('Initialization', () => {
    it('should load players on init', async () => {
      await initializeCatalogue();

      expect(catalogueSpy.getPlayers).toHaveBeenCalled();
      expect(component.filteredPlayers.length).toBe(3);
    });

    it('should extract unique regions from players', async () => {
      await initializeCatalogue();

      expect(component.availableRegions).toContain('EU');
      expect(component.availableRegions).toContain('NAC');
    });

    it('should extract unique tranches from players', async () => {
      await initializeCatalogue();

      expect(component.availableTranches).toContain('1');
      expect(component.availableTranches).toContain('2');
    });

    it('should set loading=false after data arrives', async () => {
      await initializeCatalogue();

      expect(component.loading).toBeFalse();
    });

    it('should show virtual scroll viewport when players exist', async () => {
      await initializeCatalogue();

      const viewport = fixture.nativeElement.querySelector('cdk-virtual-scroll-viewport');
      expect(viewport).not.toBeNull();
    });
  });

  // ===== FILTER =====

  describe('Filter', () => {
    it('should call getPlayers with region filter', async () => {
      await initializeCatalogue();
      const filter: PlayerFilter = { searchTerm: '', region: 'EU', tranche: null, hideUnavailable: false, hideTaken: false };
      component.onFilterChanged(filter);
      await waitForCatalogueDebounce();

      expect(catalogueSpy.getPlayers).toHaveBeenCalledWith(jasmine.objectContaining({ region: 'EU' }));
    });

    it('should update currentSearchTerm on filter change', async () => {
      await initializeCatalogue();
      const filter: PlayerFilter = { searchTerm: 'alpha', region: null, tranche: null, hideUnavailable: false, hideTaken: false };
      component.onFilterChanged(filter);
      await waitForCatalogueDebounce();

      expect(component.currentSearchTerm).toBe('alpha');
    });

    it('should deduplicate identical consecutive filters (emit once, not twice)', async () => {
      await initializeCatalogue();
      // Emit a NEW filter (different from default) twice in a row
      const filter: PlayerFilter = { searchTerm: 'test', region: 'EU', tranche: null, hideUnavailable: false, hideTaken: false };
      component.onFilterChanged(filter);
      component.onFilterChanged(filter);
      await waitForCatalogueDebounce();
      const countAfterDuplication = catalogueSpy.getPlayers.calls.count();

      // Now emit it a 3rd time — still same: no new call
      component.onFilterChanged(filter);
      await waitForCatalogueDebounce();

      expect(catalogueSpy.getPlayers.calls.count()).toBe(countAfterDuplication);
    });
  });

  // ===== EMPTY STATE =====

  describe('Empty state', () => {
    beforeEach(() => {
      catalogueSpy.getPlayers.and.returnValue(of([]));
    });

    it('should show empty state when no players', async () => {
      await initializeCatalogue();

      const empty = fixture.nativeElement.querySelector('.catalogue-empty');
      expect(empty).not.toBeNull();
    });

    it('should show CTA button in empty state', async () => {
      await initializeCatalogue();

      const cta = fixture.nativeElement.querySelector('.catalogue-empty__cta');
      expect(cta).not.toBeNull();
    });

    it('should not show viewport when no players', async () => {
      await initializeCatalogue();

      const viewport = fixture.nativeElement.querySelector('cdk-virtual-scroll-viewport');
      expect(viewport).toBeNull();
    });

    it('should open snackbar on empty CTA click', async () => {
      await initializeCatalogue();
      component.currentSearchTerm = 'unknown';
      const snackSpy = spyOn((component as any).snackBar, 'open');

      component.onReportEmpty();

      expect(snackSpy).toHaveBeenCalledWith(
        jasmine.stringContaining('unknown'),
        jasmine.any(String),
        jasmine.any(Object)
      );
    });
  });

  // ===== COMPARISON =====

  describe('Comparison', () => {
    it('should add player to comparison on card selected', async () => {
      await initializeCatalogue();

      component.onCardSelected(PLAYERS[0]);

      expect(component.comparedPlayers).toContain(PLAYERS[0]);
    });

    it('should remove player from comparison when selected again', async () => {
      await initializeCatalogue();

      component.onCardSelected(PLAYERS[0]);
      component.onCardSelected(PLAYERS[0]);

      expect(component.comparedPlayers).not.toContain(PLAYERS[0]);
    });

    it('should not add more than 2 players to comparison', async () => {
      await initializeCatalogue();
      const snackSpy = spyOn((component as any).snackBar, 'open');

      component.onCardSelected(PLAYERS[0]);
      component.onCardSelected(PLAYERS[1]);
      component.onCardSelected(PLAYERS[2]);

      expect(component.comparedPlayers.length).toBe(2);
      expect(snackSpy).toHaveBeenCalled();
    });

    it('should clear comparison on clearCompare()', async () => {
      await initializeCatalogue();

      component.onCardSelected(PLAYERS[0]);
      component.onCardSelected(PLAYERS[1]);
      component.clearCompare();

      expect(component.comparedPlayers.length).toBe(0);
    });

    it('should show comparison panel when players are compared', async () => {
      await initializeCatalogue();
      component.onCardSelected(PLAYERS[0]);
      fixture.detectChanges();

      const panel = fixture.nativeElement.querySelector('.comparison-panel');
      expect(panel).not.toBeNull();
    });

    it('should not show comparison panel when no comparison', async () => {
      await initializeCatalogue();

      const panel = fixture.nativeElement.querySelector('.comparison-panel');
      expect(panel).toBeNull();
    });

    it('should mark compared player as selected', async () => {
      await initializeCatalogue();

      component.onCardSelected(PLAYERS[0]);

      expect(component.isCompared(PLAYERS[0])).toBeTrue();
      expect(component.isCompared(PLAYERS[1])).toBeFalse();
    });
  });

  // ===== REPORT =====

  describe('Report', () => {
    it('should open snackbar on report', async () => {
      await initializeCatalogue();
      const snackSpy = spyOn((component as any).snackBar, 'open');

      component.onReport(PLAYERS[0]);

      expect(snackSpy).toHaveBeenCalledWith(
        jasmine.stringContaining('Alpha'),
        jasmine.any(String),
        jasmine.any(Object)
      );
    });
  });

  // ===== TRACK BY =====

  describe('trackById', () => {
    it('should return player id', () => {
      expect(component.trackById(0, PLAYERS[0])).toBe('1');
    });
  });

  // ===== ACCESSIBILITY =====

  describe('Accessibility', () => {
    it('should have a skip link targeting accessible list', () => {
      fixture.detectChanges();

      const skipLink = fixture.nativeElement.querySelector('.skip-link');
      expect(skipLink).not.toBeNull();
      expect(skipLink.getAttribute('href')).toBe('#accessible-list');
    });

    it('should have accessible list with id', async () => {
      await initializeCatalogue();

      const list = fixture.nativeElement.querySelector('#accessible-list');
      expect(list).not.toBeNull();
    });
  });

  // ===== ERROR HANDLING =====

  describe('Error handling', () => {
    it('should show empty state when service returns empty array (after error recovery)', async () => {
      // The real service has catchError(() => of([])), so we simulate that here
      catalogueSpy.getPlayers.and.returnValue(of([]));
      await initializeCatalogue();

      expect(component.filteredPlayers.length).toBe(0);
      const empty = fixture.nativeElement.querySelector('.catalogue-empty');
      expect(empty).not.toBeNull();
    });
  });
});
