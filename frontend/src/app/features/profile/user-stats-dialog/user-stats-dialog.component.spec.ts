import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { UserStatsDialogComponent } from './user-stats-dialog.component';
import { TranslationService } from '../../../core/services/translation.service';
import { UserProfile } from '../../../core/services/user-context.service';

describe('UserStatsDialogComponent', () => {
  let component: UserStatsDialogComponent;
  let fixture: ComponentFixture<UserStatsDialogComponent>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<UserStatsDialogComponent>>;
  let translationService: jasmine.SpyObj<TranslationService>;

  const mockUser: UserProfile = {
    id: 'user1',
    username: 'testuser',
    email: 'test@example.com',
    role: 'Joueur'
  };

  beforeEach(async () => {
    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
    translationService = jasmine.createSpyObj('TranslationService', ['t']);
    translationService.t.and.callFake((key: string, fallback?: string) => fallback || key);

    await TestBed.configureTestingModule({
      imports: [UserStatsDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { user: mockUser } },
        { provide: TranslationService, useValue: translationService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UserStatsDialogComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with loading state', () => {
    expect(component.loading).toBeTrue();
    expect(component.stats).toBeNull();
  });

  it('should load stats on init', fakeAsync(() => {
    fixture.detectChanges();

    expect(component.loading).toBeTrue();
    expect(component.stats).toBeNull();

    tick(800);

    expect(component.loading).toBeFalse();
    expect(component.stats).not.toBeNull();
  }));

  it('should generate valid mock stats', fakeAsync(() => {
    fixture.detectChanges();
    tick(800);

    const stats = component.stats!;
    expect(stats.gamesPlayed).toBeGreaterThanOrEqual(10);
    expect(stats.gamesPlayed).toBeLessThanOrEqual(60);
    expect(stats.gamesWon).toBeLessThanOrEqual(stats.gamesPlayed);
    expect(stats.totalPoints).toBeGreaterThan(0);
    expect(stats.bestRank).toBeGreaterThanOrEqual(1);
    expect(stats.bestRank).toBeLessThanOrEqual(6);
    expect(stats.winRate).toBeGreaterThanOrEqual(0);
    expect(stats.winRate).toBeLessThanOrEqual(100);
    expect(['EU', 'NAW', 'ASIA', 'BR']).toContain(stats.favoriteRegion);
  }));

  it('should close dialog on onClose', () => {
    component.onClose();
    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('should format stats title with username', () => {
    translationService.t.and.returnValue('Stats for {username}');

    const title = component.getStatsTitle();

    expect(title).toBe('Stats for testuser');
  });

  it('should format games played label', () => {
    translationService.t.and.returnValue('{count} games');

    const label = component.getGamesPlayedLabel(42);

    expect(label).toBe('42 games');
  });

  it('should get favorite region label', () => {
    const label = component.getFavoriteRegionLabel('EU');

    expect(translationService.t).toHaveBeenCalledWith('leaderboard.regions.EU', 'EU');
  });

  it('should format numbers with formatPoints', () => {
    const formatted = component.formatNumber(123456);

    // formatPoints from theme.constants returns formatted number
    expect(formatted).toBeDefined();
    expect(typeof formatted).toBe('string');
  });

  it('should return excellent win rate color for >= 30%', fakeAsync(() => {
    fixture.detectChanges();
    tick(800);

    component.stats!.winRate = 35;

    expect(component.getWinRateColor()).toBe('excellent');
  }));

  it('should return good win rate color for >= 20%', fakeAsync(() => {
    fixture.detectChanges();
    tick(800);

    component.stats!.winRate = 25;

    expect(component.getWinRateColor()).toBe('good');
  }));

  it('should return average win rate color for >= 10%', fakeAsync(() => {
    fixture.detectChanges();
    tick(800);

    component.stats!.winRate = 15;

    expect(component.getWinRateColor()).toBe('average');
  }));

  it('should return low win rate color for < 10%', fakeAsync(() => {
    fixture.detectChanges();
    tick(800);

    component.stats!.winRate = 5;

    expect(component.getWinRateColor()).toBe('low');
  }));

  it('should return empty string for win rate color when no stats', () => {
    component.stats = null;

    expect(component.getWinRateColor()).toBe('');
  });

  it('should return gold badge for rank 1', () => {
    expect(component.getRankBadgeClass(1)).toBe('gold');
  });

  it('should return silver badge for rank 2', () => {
    expect(component.getRankBadgeClass(2)).toBe('silver');
  });

  it('should return bronze badge for rank 3', () => {
    expect(component.getRankBadgeClass(3)).toBe('bronze');
  });

  it('should return default badge for rank > 3', () => {
    expect(component.getRankBadgeClass(4)).toBe('default');
    expect(component.getRankBadgeClass(10)).toBe('default');
  });

  it('should return correct flag for EU region', () => {
    expect(component.getRegionFlag('EU')).toBe('🇪🇺');
  });

  it('should return correct flag for NAW region', () => {
    expect(component.getRegionFlag('NAW')).toBe('🇺🇸');
  });

  it('should return correct flag for ASIA region', () => {
    expect(component.getRegionFlag('ASIA')).toBe('🇯🇵');
  });

  it('should return correct flag for BR region', () => {
    expect(component.getRegionFlag('BR')).toBe('🇧🇷');
  });

  it('should return default flag for unknown region', () => {
    expect(component.getRegionFlag('UNKNOWN')).toBe('🌍');
  });

  it('should replace multiple template params', () => {
    translationService.t.and.returnValue('User {name} has {count} points');

    const formatted = component['formatTemplate']('profile.test', {
      name: 'Alice',
      count: 100
    });

    expect(formatted).toBe('User Alice has 100 points');
  });
});
