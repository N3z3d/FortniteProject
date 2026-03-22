import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';

import { GameLeaderboardPageComponent } from './game-leaderboard-page.component';
import { LeaderboardService } from '../../../core/services/leaderboard.service';
import { TranslationService } from '../../../core/services/translation.service';
import { TeamDeltaLeaderboardEntry } from '../models/team-delta-leaderboard.model';

const MOCK_ENTRIES: TeamDeltaLeaderboardEntry[] = [
  {
    rank: 1,
    participantId: 'p1',
    username: 'Alpha',
    deltaPr: 500,
    periodStart: '2025-01-01',
    periodEnd: '2025-01-31',
    computedAt: '2025-01-31T23:59:00Z',
  },
  {
    rank: 2,
    participantId: 'p2',
    username: 'Beta',
    deltaPr: -200,
    periodStart: '2025-01-01',
    periodEnd: '2025-01-31',
    computedAt: '2025-01-31T23:59:00Z',
  },
];

describe('GameLeaderboardPageComponent', () => {
  let component: GameLeaderboardPageComponent;
  let fixture: ComponentFixture<GameLeaderboardPageComponent>;
  let leaderboardSpy: jasmine.SpyObj<LeaderboardService>;

  beforeEach(async () => {
    leaderboardSpy = jasmine.createSpyObj('LeaderboardService', [
      'getGameDeltaLeaderboard',
    ]);
    leaderboardSpy.getGameDeltaLeaderboard.and.returnValue(of(MOCK_ENTRIES));

    const translationStub = { t: (key: string) => key };

    await TestBed.configureTestingModule({
      imports: [GameLeaderboardPageComponent],
      providers: [
        { provide: LeaderboardService, useValue: leaderboardSpy },
        { provide: TranslationService, useValue: translationStub },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { params: { id: 'game123' } } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(GameLeaderboardPageComponent);
    component = fixture.componentInstance;
  });

  it('charge les entrées depuis le service au démarrage', () => {
    fixture.detectChanges();

    expect(leaderboardSpy.getGameDeltaLeaderboard).toHaveBeenCalledWith(
      'game123'
    );
    expect(component.entries.length).toBe(2);
    expect(component.loading).toBeFalse();
    expect(component.error).toBeFalse();
  });

  it('affiche un état de chargement puis le tableau une fois les données reçues', () => {
    fixture.detectChanges();
    fixture.detectChanges();

    const table = fixture.nativeElement.querySelector('.leaderboard-table');
    expect(table).toBeTruthy();
  });

  it('affiche le rang de chaque entrée dans le tableau', () => {
    fixture.detectChanges();
    fixture.detectChanges();

    const rankCells = fixture.nativeElement.querySelectorAll('.rank-cell');
    expect(rankCells.length).toBe(2);
    expect(rankCells[0].textContent.trim()).toBe('1');
    expect(rankCells[1].textContent.trim()).toBe('2');
  });

  it('affiche le username de chaque entrée', () => {
    fixture.detectChanges();
    fixture.detectChanges();

    const usernameCells =
      fixture.nativeElement.querySelectorAll('.username-cell');
    expect(usernameCells[0].textContent.trim()).toBe('Alpha');
    expect(usernameCells[1].textContent.trim()).toBe('Beta');
  });

  it('formate un deltaPr positif avec le signe +', () => {
    expect(component.formatDelta(500)).toBe('+500 PR');
  });

  it('formate un deltaPr négatif sans signe supplémentaire', () => {
    expect(component.formatDelta(-200)).toBe('-200 PR');
  });

  it('affiche l\'état vide quand il n\'y a pas d\'entrées', () => {
    leaderboardSpy.getGameDeltaLeaderboard.and.returnValue(of([]));

    fixture.detectChanges();
    fixture.detectChanges();

    const emptyEl = fixture.nativeElement.querySelector('.leaderboard-empty');
    expect(emptyEl).toBeTruthy();
    expect(component.entries.length).toBe(0);
  });

  it('affiche l\'état d\'erreur et le bouton réessayer en cas d\'échec', () => {
    leaderboardSpy.getGameDeltaLeaderboard.and.returnValue(
      throwError(() => new Error('Network error'))
    );

    fixture.detectChanges();
    fixture.detectChanges();

    expect(component.error).toBeTrue();
    const errorEl = fixture.nativeElement.querySelector('.leaderboard-error');
    expect(errorEl).toBeTruthy();
    const retryBtn = fixture.nativeElement.querySelector('.retry-btn');
    expect(retryBtn).toBeTruthy();
  });

  // BUG-13 — rank medal classes
  it('applique rank-gold au rang 1, rank-silver au rang 2, rank-bronze au rang 3', () => {
    const entries3: TeamDeltaLeaderboardEntry[] = [
      { rank: 1, participantId: 'a', username: 'A', deltaPr: 300, periodStart: '2025-01-01', periodEnd: '2025-01-31', computedAt: '2025-01-31T00:00:00Z' },
      { rank: 2, participantId: 'b', username: 'B', deltaPr: 200, periodStart: '2025-01-01', periodEnd: '2025-01-31', computedAt: '2025-01-31T00:00:00Z' },
      { rank: 3, participantId: 'c', username: 'C', deltaPr: 100, periodStart: '2025-01-01', periodEnd: '2025-01-31', computedAt: '2025-01-31T00:00:00Z' },
    ];
    leaderboardSpy.getGameDeltaLeaderboard.and.returnValue(of(entries3));
    fixture.detectChanges();
    fixture.detectChanges();

    const rankCells = fixture.nativeElement.querySelectorAll('.rank-cell');
    expect(rankCells[0].classList.contains('rank-gold')).toBeTrue();
    expect(rankCells[1].classList.contains('rank-silver')).toBeTrue();
    expect(rankCells[2].classList.contains('rank-bronze')).toBeTrue();
  });

  it('getRankClass retourne rank-gold/silver/bronze selon le rang', () => {
    expect(component.getRankClass(1)).toBe('rank-gold');
    expect(component.getRankClass(2)).toBe('rank-silver');
    expect(component.getRankClass(3)).toBe('rank-bronze');
    expect(component.getRankClass(4)).toBe('');
  });
});
