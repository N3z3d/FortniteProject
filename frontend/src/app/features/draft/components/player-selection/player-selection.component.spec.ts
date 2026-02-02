import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { PlayerSelectionComponent } from './player-selection.component';
import { Player } from '../../models/draft.interface';
import { TranslationService } from '../../../../core/services/translation.service';

describe('PlayerSelectionComponent', () => {
  let component: PlayerSelectionComponent;
  let fixture: ComponentFixture<PlayerSelectionComponent>;
  let translationService: jasmine.SpyObj<TranslationService>;

  const mockPlayers: Player[] = [
    { id: '1', nickname: 'Ninja', username: 'ninja', region: 'NAE', tranche: 'T1', totalPoints: 5000, currentSeason: 2024 },
    { id: '2', nickname: 'Tfue', username: 'tfue', region: 'NAW', tranche: 'T1', totalPoints: 4500, currentSeason: 2024 },
    { id: '3', nickname: 'Bugha', username: 'bugha', region: 'NAE', tranche: 'T2', totalPoints: 4000, currentSeason: 2024 },
    { id: '4', nickname: 'Aqua', username: 'aqua', region: 'EU', tranche: 'T1', totalPoints: 3500, currentSeason: 2024 }
  ];

  beforeEach(async () => {
    translationService = jasmine.createSpyObj('TranslationService', ['t']);
    translationService.t.and.callFake((key: string, fallback?: string) => fallback || key);

    await TestBed.configureTestingModule({
      imports: [PlayerSelectionComponent],
      providers: [
        { provide: TranslationService, useValue: translationService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PlayerSelectionComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with default filters', () => {
    expect(component.selectedRegion).toBeDefined();
    expect(component.selectedTranche).toBeDefined();
    expect(component.searchTerm).toBe('');
  });

  it('should setup search debouncing on init', fakeAsync(() => {
    spyOn<any>(component, 'updateFilteredPlayers');

    fixture.detectChanges();

    component['searchSubject'].next('test');
    tick(300);

    expect(component['updateFilteredPlayers']).toHaveBeenCalled();
  }));

  it('should update filtered players on init', () => {
    component.players = mockPlayers;

    fixture.detectChanges();

    expect(component.getFilteredPlayers().length).toBeGreaterThan(0);
  });

  it('should filter players by region', () => {
    component.players = mockPlayers;
    component.selectedRegion = 'NAE';

    fixture.detectChanges();

    const filtered = component.getFilteredPlayers();
    expect(filtered.length).toBe(2);
    expect(filtered.every(p => p.region === 'NAE')).toBeTrue();
  });

  it('should filter players by tranche', () => {
    component.players = mockPlayers;
    component.selectedTranche = 'T1';

    fixture.detectChanges();

    const filtered = component.getFilteredPlayers();
    expect(filtered.length).toBe(3);
    expect(filtered.every(p => p.tranche === 'T1')).toBeTrue();
  });

  it('should filter players by search term', fakeAsync(() => {
    component.players = mockPlayers;
    fixture.detectChanges();

    component.searchTerm = 'ninja';
    component['updateFilteredPlayers']();

    const filtered = component.getFilteredPlayers();
    expect(filtered.length).toBe(1);
    expect(filtered[0].nickname).toBe('Ninja');
  }));

  it('should combine multiple filters', fakeAsync(() => {
    component.players = mockPlayers;
    component.selectedRegion = 'NAE';
    component.selectedTranche = 'T1';

    fixture.detectChanges();

    const filtered = component.getFilteredPlayers();
    expect(filtered.length).toBe(1);
    expect(filtered[0].nickname).toBe('Ninja');
  }));

  it('should use cache when filters unchanged', () => {
    component.players = mockPlayers;
    fixture.detectChanges();

    const firstCall = component.getFilteredPlayers();
    const secondCall = component.getFilteredPlayers();

    expect(firstCall).toBe(secondCall); // Same reference = cached
  });

  it('should invalidate cache when filters change', () => {
    component.players = mockPlayers;
    fixture.detectChanges();

    const firstCall = component.getFilteredPlayers();

    component.selectedRegion = 'EU';
    const secondCall = component.getFilteredPlayers();

    expect(firstCall).not.toBe(secondCall);
    expect(secondCall.length).toBe(1);
  });

  it('should get available regions', () => {
    component.players = mockPlayers;

    const regions = component.getAvailableRegions();

    expect(regions.length).toBe(3);
    expect(regions).toContain('NAE');
    expect(regions).toContain('NAW');
    expect(regions).toContain('EU');
  });

  it('should get available tranches', () => {
    component.players = mockPlayers;

    const tranches = component.getAvailableTranches();

    expect(tranches.length).toBe(2);
    expect(tranches).toContain('T1');
    expect(tranches).toContain('T2');
  });

  it('should detect active filters', () => {
    component.players = mockPlayers;
    fixture.detectChanges();

    expect(component.hasActiveFilters()).toBeFalse();

    component.searchTerm = 'test';
    expect(component.hasActiveFilters()).toBeTrue();
  });

  it('should clear all filters', () => {
    component.players = mockPlayers;
    component.selectedRegion = 'EU';
    component.selectedTranche = 'T2';
    component.searchTerm = 'test';

    fixture.detectChanges();

    component.clearFilters();

    expect(component.hasActiveFilters()).toBeFalse();
    expect(component.getFilteredPlayers().length).toBe(mockPlayers.length);
  });

  it('should emit player selected when can select', () => {
    spyOn(component.playerSelected, 'emit');
    component.canSelect = true;

    component.onPlayerSelect(mockPlayers[0]);

    expect(component.playerSelected.emit).toHaveBeenCalledWith(mockPlayers[0]);
  });

  it('should not emit player selected when cannot select', () => {
    spyOn(component.playerSelected, 'emit');
    component.canSelect = false;

    component.onPlayerSelect(mockPlayers[0]);

    expect(component.playerSelected.emit).not.toHaveBeenCalled();
  });

  it('should debounce search changes', fakeAsync(() => {
    fixture.detectChanges();

    const spy = spyOn<any>(component, 'updateFilteredPlayers');

    const event = { target: { value: 'ninja' } } as any;

    component.onSearchChange(event);
    tick(150); // Before debounce (300ms)
    expect(spy).not.toHaveBeenCalled();

    tick(200); // After debounce (total 350ms > 300ms)
    expect(spy).toHaveBeenCalled();
  }));

  it('should get region label from translation', () => {
    translationService.t.and.returnValue('Europe');

    const label = component.getRegionLabel('EU');

    expect(label).toBe('Europe');
  });

  it('should get region color', () => {
    expect(component.getRegionColor('EU')).toBe('#4CAF50');
    expect(component.getRegionColor('NAW')).toBe('#2196F3');
    expect(component.getRegionColor('UNKNOWN')).toBe('#9E9E9E');
  });

  it('should get tranche label', () => {
    translationService.t.and.returnValue('Tier {value}');

    const label = component.getTrancheLabel('T1');

    expect(label).toContain('1');
  });

  it('should get total count', () => {
    component.players = mockPlayers;

    expect(component.getTotalCount()).toBe(4);
  });

  it('should get filtered count', () => {
    component.players = mockPlayers;
    component.selectedRegion = 'NAE';

    fixture.detectChanges();

    expect(component.getFilteredCount()).toBe(2);
  });

  it('should get results summary with translation', () => {
    component.players = mockPlayers;
    translationService.t.and.returnValue('{filtered} of {total} players');

    fixture.detectChanges();

    const summary = component.getResultsSummary();

    expect(summary).toContain('4');
  });

  it('should get list aria label', () => {
    component.players = mockPlayers;
    translationService.t.and.returnValue('{count} players available');

    fixture.detectChanges();

    const label = component.getListAriaLabel();

    expect(label).toContain('4');
  });

  it('should get player aria label', () => {
    translationService.t.and.returnValues('{nickname} from {region} tier {tranche}', 'Europe');

    const label = component.getPlayerAriaLabel(mockPlayers[3]);

    expect(label).toContain('Aqua');
  });

  it('should get pagination info label', () => {
    component.players = mockPlayers;
    translationService.t.and.returnValue('Optimized for {count} players');

    const label = component.getPaginationInfoLabel();

    expect(label).toContain('4');
  });

  it('should track players by id', () => {
    const player = mockPlayers[0];

    const trackId = component.trackByPlayerId(0, player);

    expect(trackId).toBe('1');
  });

  it('should track by index if no id', () => {
    const player = { ...mockPlayers[0], id: undefined } as any;

    const trackId = component.trackByPlayerId(5, player);

    expect(trackId).toBe(5);
  });

  it('should sort players by points descending', () => {
    component.players = [
      { id: '1', nickname: 'Player1', username: 'player1', region: 'EU', tranche: 'T1', totalPoints: 1000, currentSeason: 2024 },
      { id: '2', nickname: 'Player2', username: 'player2', region: 'EU', tranche: 'T1', totalPoints: 3000, currentSeason: 2024 },
      { id: '3', nickname: 'Player3', username: 'player3', region: 'EU', tranche: 'T1', totalPoints: 2000, currentSeason: 2024 }
    ];

    fixture.detectChanges();

    const filtered = component.getFilteredPlayers();

    expect(filtered[0].totalPoints).toBe(3000);
    expect(filtered[1].totalPoints).toBe(2000);
    expect(filtered[2].totalPoints).toBe(1000);
  });

  it('should unsubscribe on destroy', () => {
    fixture.detectChanges();

    spyOn(component['destroy$'], 'next');
    spyOn(component['destroy$'], 'complete');
    spyOn(component['searchSubject'], 'complete');

    component.ngOnDestroy();

    expect(component['destroy$'].next).toHaveBeenCalled();
    expect(component['destroy$'].complete).toHaveBeenCalled();
    expect(component['searchSubject'].complete).toHaveBeenCalled();
  });
});
