import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { SimpleLeaderboardComponent } from './simple-leaderboard.component';
import { LeaderboardService, PlayerLeaderboardEntry } from '../../core/services/leaderboard.service';
import { AccessibilityAnnouncerService } from '../../shared/services/accessibility-announcer.service';
import { GameSelectionService } from '../../core/services/game-selection.service';

describe('SimpleLeaderboardComponent', () => {
  let component: SimpleLeaderboardComponent;
  let fixture: ComponentFixture<SimpleLeaderboardComponent>;
  let leaderboardService: jasmine.SpyObj<LeaderboardService>;

  beforeEach(async () => {
    leaderboardService = jasmine.createSpyObj('LeaderboardService', ['getPlayerLeaderboard']);
    const announcer = jasmine.createSpyObj('AccessibilityAnnouncerService', ['announce']);
    const gameSelectionStub = {
      selectedGame$: of(null),
      getSelectedGame: () => null
    };

    await TestBed.configureTestingModule({
      imports: [SimpleLeaderboardComponent],
      providers: [
        { provide: LeaderboardService, useValue: leaderboardService },
        { provide: AccessibilityAnnouncerService, useValue: announcer },
        { provide: GameSelectionService, useValue: gameSelectionStub }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SimpleLeaderboardComponent);
    component = fixture.componentInstance;
  });

  it('charge le classement joueurs depuis l\'API', () => {
    const entries: PlayerLeaderboardEntry[] = [
      {
        playerId: 'p1',
        nickname: 'Alpha',
        username: 'alpha',
        region: 'EU',
        tranche: 'T1',
        rank: 1,
        totalPoints: 120,
        avgPointsPerGame: 0,
        bestScore: 0,
        teamsCount: 0
      },
      {
        playerId: 'p2',
        nickname: 'Beta',
        username: 'beta',
        region: 'NAW',
        tranche: 'T1',
        rank: 2,
        totalPoints: 80,
        avgPointsPerGame: 0,
        bestScore: 0,
        teamsCount: 0
      }
    ];

    leaderboardService.getPlayerLeaderboard.and.returnValue(of(entries));

    component.loadData();

    expect(leaderboardService.getPlayerLeaderboard).toHaveBeenCalledWith(2025);
    expect(component.allPlayers.length).toBe(2);
    expect(component.filteredPlayers.length).toBe(2);
  });

  it('expose une erreur quand le chargement echoue', () => {
    leaderboardService.getPlayerLeaderboard.and.returnValue(
      throwError(() => new Error('fail'))
    );

    component.loadData();

    const expectedError = 'Donn\u00e9es indisponibles (CSV non charg\u00e9)';
    expect(component.error).toBe(expectedError);
    expect(component.allPlayers.length).toBe(0);
    expect(component.loading).toBe(false);
  });
});
