import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UserStatsDialogComponent } from './user-stats-dialog.component';
import { TranslationService } from '../../../core/services/translation.service';
import { UserProfile } from '../../../core/services/user-context.service';

describe('UserStatsDialogComponent', () => {
  let component: UserStatsDialogComponent;
  let fixture: ComponentFixture<UserStatsDialogComponent>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<UserStatsDialogComponent>>;
  let translationService: jasmine.SpyObj<TranslationService>;

  const user: UserProfile = {
    id: 'user-1',
    username: 'PlayerOne',
    email: 'player@game.test',
    role: 'Player'
  };

  beforeEach(async () => {
    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
    translationService = jasmine.createSpyObj('TranslationService', ['t', 'translate']);
    translationService.t.and.callFake((key: string) => key);
    translationService.translate.and.callFake((key: string) => key);

    TestBed.configureTestingModule({
      imports: [UserStatsDialogComponent]
    });
    TestBed.overrideComponent(UserStatsDialogComponent, {
      set: {
        providers: [
          { provide: MatDialogRef, useValue: dialogRef },
          { provide: MAT_DIALOG_DATA, useValue: { user } },
          { provide: TranslationService, useValue: translationService }
        ]
      }
    });

    await TestBed.compileComponents();

    fixture = TestBed.createComponent(UserStatsDialogComponent);
    component = fixture.componentInstance;
  });

  it('should create and load stats', async () => {
    vi.useFakeTimers();
    spyOn(Math, 'random').and.returnValue(0.1);

    fixture.detectChanges();
    expect(component.loading).toBeTrue();

    vi.advanceTimersByTime(800);
    await Promise.resolve();

    expect(component.loading).toBeFalse();
    expect(component.stats).toBeTruthy();
    vi.useRealTimers();
  });

  it('closes dialog on close', () => {
    component.onClose();

    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('formats title with username', () => {
    const title = component.getStatsTitle();
    expect(title).toBe('profile.statsDialog.title');
  });

  it('returns games played label', () => {
    const label = component.getGamesPlayedLabel(12);
    expect(label).toBe('profile.statsDialog.gamesPlayedSuffix');
  });

  it('returns favorite region label', () => {
    expect(component.getFavoriteRegionLabel('EU')).toBe('leaderboard.regions.EU');
  });

  it('returns win rate color by threshold', () => {
    component.stats = { winRate: 35 } as any;
    expect(component.getWinRateColor()).toBe('excellent');

    component.stats = { winRate: 25 } as any;
    expect(component.getWinRateColor()).toBe('good');

    component.stats = { winRate: 15 } as any;
    expect(component.getWinRateColor()).toBe('average');

    component.stats = { winRate: 5 } as any;
    expect(component.getWinRateColor()).toBe('low');
  });

  it('returns rank badge class', () => {
    expect(component.getRankBadgeClass(1)).toBe('gold');
    expect(component.getRankBadgeClass(2)).toBe('silver');
    expect(component.getRankBadgeClass(3)).toBe('bronze');
    expect(component.getRankBadgeClass(4)).toBe('default');
  });

  it('returns region flag fallback', () => {
    expect(component.getRegionFlag('EU')).toBeTruthy();
    expect(component.getRegionFlag('UNKNOWN')).toBe('🌍');
  });
});
