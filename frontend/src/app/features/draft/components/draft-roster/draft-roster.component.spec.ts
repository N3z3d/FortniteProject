import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DraftRosterComponent } from './draft-roster.component';
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
  getSlotsRemainingLabel(count: number, t: (key: string) => string): string {
    return `${count} slots remaining`;
  }
}

describe('DraftRosterComponent', () => {
  let component: DraftRosterComponent;
  let fixture: ComponentFixture<DraftRosterComponent>;

  const createPlayer = (id: number, region = 'EU'): Player => ({
    id: `p-${id}`,
    username: `user-${id}`,
    nickname: `Player ${id}`,
    region,
    tranche: 'T1',
    currentSeason: 1
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DraftRosterComponent],
      providers: [
        { provide: TranslationService, useClass: MockTranslationService },
        { provide: DraftStateHelperService, useClass: MockDraftStateHelperService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DraftRosterComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('renders team players', () => {
    component.team = [createPlayer(1), createPlayer(2)];
    fixture.detectChanges();

    const players = fixture.nativeElement.querySelectorAll('.team-player');
    expect(players.length).toBe(2);
  });

  it('displays player nicknames', () => {
    component.team = [createPlayer(1)];
    fixture.detectChanges();

    const playerSpan = fixture.nativeElement.querySelector('.team-player span');
    expect(playerSpan.textContent).toContain('Player 1');
  });

  it('shows remaining slots when count is positive', () => {
    component.team = [createPlayer(1)];
    component.remainingSlots = 3;
    fixture.detectChanges();

    const slotsEl = fixture.nativeElement.querySelector('.slots-remaining');
    expect(slotsEl).not.toBeNull();
    expect(slotsEl.textContent).toContain('3 slots remaining');
  });

  it('hides remaining slots when count is zero', () => {
    component.team = [createPlayer(1)];
    component.remainingSlots = 0;
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.team-slots')).toBeNull();
  });

  it('renders empty state when no team members', () => {
    component.team = [];
    component.remainingSlots = 5;
    fixture.detectChanges();

    const players = fixture.nativeElement.querySelectorAll('.team-player');
    expect(players.length).toBe(0);
  });

  it('returns region label via helper service', () => {
    const label = component.getRegionLabel('EU');
    expect(label).toBe('EU');
  });

  it('returns slots remaining label via helper service', () => {
    component.remainingSlots = 2;
    const label = component.getSlotsRemainingLabel();
    expect(label).toBe('2 slots remaining');
  });
});
