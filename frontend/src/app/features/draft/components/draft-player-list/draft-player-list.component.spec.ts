import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DraftPlayerListComponent } from './draft-player-list.component';
import { Player } from '../../models/draft.interface';
import { TranslationService } from '../../../../core/services/translation.service';
import { DraftStateHelperService } from '../../services/draft-state-helper.service';

class MockTranslationService {
  t(key: string, fallback?: string): string {
    return fallback || key;
  }
}

class MockDraftStateHelperService {
  getRegionLabelKey(region: string): string {
    return `draft.filters.${region.toLowerCase()}Region`;
  }
  getTrancheLabel(tranche: string, t: (key: string) => string): string {
    const match = /^T(\d+)$/i.exec(tranche);
    return match ? t('draft.selection.trancheValue') : tranche;
  }
  getSearchResultsTitle(count: number, t: (key: string) => string): string {
    return `${count} results`;
  }
  getShowAllResultsLabel(count: number, t: (key: string) => string): string {
    return `Show all ${count}`;
  }
  getSuggestionRankLabel(rank: number, t: (key: string) => string): string {
    return `#${rank}`;
  }
}

describe('DraftPlayerListComponent', () => {
  let component: DraftPlayerListComponent;
  let fixture: ComponentFixture<DraftPlayerListComponent>;

  const createPlayer = (id: number): Player => ({
    id: `p-${id}`,
    username: `user-${id}`,
    nickname: `Player ${id}`,
    region: 'EU',
    tranche: 'T1',
    currentSeason: 1
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DraftPlayerListComponent],
      providers: [
        { provide: TranslationService, useClass: MockTranslationService },
        { provide: DraftStateHelperService, useClass: MockDraftStateHelperService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DraftPlayerListComponent);
    component = fixture.componentInstance;
  });

  it('renders results when search term and players exist', () => {
    component.searchTerm = 'pla';
    component.filteredPlayers = [createPlayer(1), createPlayer(2)];

    fixture.detectChanges();

    const results = fixture.nativeElement.querySelectorAll('.result-item');
    expect(results.length).toBe(2);
  });

  it('treats null search term as no active search', () => {
    component.searchTerm = null;
    component.filteredPlayers = [createPlayer(1)];

    fixture.detectChanges();

    expect(component.hasResults).toBeFalse();
    expect(fixture.nativeElement.querySelector('.search-results')).toBeNull();
  });

  it('limits visible players when showAllResults is false', () => {
    component.searchTerm = 'pla';
    component.filteredPlayers = Array.from({ length: 6 }, (_, index) => createPlayer(index + 1));
    component.showAllResults = false;

    expect(component.visiblePlayers.length).toBe(5);
  });

  it('returns all players when showAllResults is true', () => {
    component.searchTerm = 'pla';
    component.filteredPlayers = Array.from({ length: 6 }, (_, index) => createPlayer(index + 1));
    component.showAllResults = true;

    expect(component.visiblePlayers.length).toBe(6);
  });

  it('shows no-results when search term set and list empty', () => {
    component.searchTerm = 'missing';
    component.filteredPlayers = [];

    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.no-results')).not.toBeNull();
  });

  it('does not emit selection when cannot select', () => {
    const emitSpy = spyOn(component.playerSelected, 'emit');

    component.canSelect = false;
    component.selectPlayer(createPlayer(1));

    expect(emitSpy).not.toHaveBeenCalled();
  });

  it('emits selection when canSelect is true', () => {
    const emitSpy = spyOn(component.playerSelected, 'emit');
    const player = createPlayer(1);

    component.canSelect = true;
    component.selectPlayer(player);

    expect(emitSpy).toHaveBeenCalledWith(player);
  });

  it('emits toggle when show-all is requested', () => {
    const emitSpy = spyOn(component.showAllResultsChange, 'emit');

    component.showAllResults = false;
    component.toggleShowAllResults();

    expect(emitSpy).toHaveBeenCalledWith(true);
  });

  it('shows suggestions when available and no search term', () => {
    component.searchTerm = '';
    component.suggestions = [
      { player: createPlayer(1), rank: 1, score: 100 }
    ];

    expect(component.hasSuggestions).toBeTrue();
  });

  it('hides suggestions when search term is active', () => {
    component.searchTerm = 'test';
    component.suggestions = [
      { player: createPlayer(1), rank: 1, score: 100 }
    ];

    expect(component.hasSuggestions).toBeFalse();
  });

  it('hides suggestions when list is empty', () => {
    component.searchTerm = '';
    component.suggestions = [];

    expect(component.hasSuggestions).toBeFalse();
  });

  it('emits clearSearch on requestClearSearch', () => {
    const emitSpy = spyOn(component.clearSearch, 'emit');

    component.requestClearSearch();

    expect(emitSpy).toHaveBeenCalled();
  });

  it('returns region label via helper service', () => {
    const label = component.getRegionLabel('EU');
    expect(label).toBe('EU');
  });

  it('returns search results title via helper service', () => {
    const title = component.getSearchResultsTitle(5);
    expect(title).toBe('5 results');
  });
});
