import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NEVER, of } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClient } from '@angular/common/http';

import { DashboardComponent } from './dashboard.component';
import { TranslationService } from '../../core/services/translation.service';
import { GameSelectionService } from '../../core/services/game-selection.service';
import { DashboardFacade } from '../../core/facades/dashboard.facade';
import { DashboardChartService } from './services/dashboard-chart.service';
import { GameService } from '../game/services/game.service';
import { AccessibilityAnnouncerService } from '../../shared/services/accessibility-announcer.service';
import { FocusManagementService } from '../../shared/services/focus-management.service';
import { PremiumInteractionsService } from '../../shared/services/premium-interactions.service';

describe('DashboardComponent (i18n)', () => {
  let fixture: ComponentFixture<DashboardComponent>;
  let component: DashboardComponent;
  let translationService: TranslationService;

  beforeEach(async () => {
    localStorage.removeItem('app_language');
    (window as any).IntersectionObserver = (window as any).IntersectionObserver || class {
      observe() {}
      unobserve() {}
      disconnect() {}
    };

    const routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);
    const snackBarSpy = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    const gameServiceSpy = jasmine.createSpyObj<GameService>('GameService', ['getUserGames', 'getGameById']);
    const dashboardFacadeSpy = jasmine.createSpyObj<DashboardFacade>('DashboardFacade', ['getDashboardData']);
    const chartServiceSpy = jasmine.createSpyObj<DashboardChartService>('DashboardChartService', [
      'createRegionChart',
      'createPointsChart',
      'updateChart',
      'destroyChart'
    ]);
    const accessibilitySpy = jasmine.createSpyObj<AccessibilityAnnouncerService>('AccessibilityAnnouncerService', [
      'announceLoading',
      'announceError',
      'announceNavigation'
    ]);
    const premiumInteractionsSpy = jasmine.createSpyObj<PremiumInteractionsService>('PremiumInteractionsService', [
      'initMagneticButton',
      'initSpringButton',
      'initParallaxCard',
      'initGlowEffect',
      'createRipple',
      'addShimmerEffect',
      'removeShimmerEffect',
      'typewriterEffect'
    ]);

    gameServiceSpy.getUserGames.and.returnValue(of([]));
    gameServiceSpy.getGameById.and.returnValue(of(null as any));
    dashboardFacadeSpy.getDashboardData.and.returnValue(
      of({
        statistics: null,
        leaderboard: [],
        regionDistribution: {},
        teams: []
      } as any)
    );

    await TestBed.configureTestingModule({
      imports: [DashboardComponent, NoopAnimationsModule],
      providers: [
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: { params: of({}) } },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: GameService, useValue: gameServiceSpy },
        { provide: DashboardFacade, useValue: dashboardFacadeSpy },
        { provide: DashboardChartService, useValue: chartServiceSpy },
        { provide: GameSelectionService, useValue: { selectedGame$: NEVER, hasSelectedGame: () => false } },
        { provide: AccessibilityAnnouncerService, useValue: accessibilitySpy },
        { provide: FocusManagementService, useValue: {} },
        { provide: PremiumInteractionsService, useValue: premiumInteractionsSpy },
        { provide: HttpClient, useValue: {} }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    translationService = TestBed.inject(TranslationService);

    component.selectedGame = {
      id: 'game-1',
      name: 'Game Test',
      creatorName: 'Thibaut',
      maxParticipants: 3,
      status: 'ACTIVE',
      createdAt: new Date(),
      participantCount: 1,
      canJoin: true
    } as any;
    component.isLoading = false;
    component.error = null;
  });

  it('affiche les libellés en français par défaut', () => {
    fixture.detectChanges();

    const heading = fixture.nativeElement.querySelector('#stats-heading') as HTMLElement;
    expect(heading?.textContent).toContain('Statistiques générales');
  });

  it('affiche les libellés en anglais quand la langue est en', () => {
    translationService.setLanguage('en');
    fixture.detectChanges();

    const heading = fixture.nativeElement.querySelector('#stats-heading') as HTMLElement;
    expect(heading?.textContent).toContain('General Statistics');
  });

  it('utilise le nombre d’équipes quand participantCount est à zéro', () => {
    component.selectedGame = {
      ...component.selectedGame,
      participantCount: 0,
      teams: [{ id: 't1' }, { id: 't2' }, { id: 't3' }]
    } as any;
    component.stats.totalTeams = 3;

    expect(component.getParticipantDisplayCount()).toBe(3);
  });

  it('privilégie participantCount quand il est défini', () => {
    component.selectedGame = {
      ...component.selectedGame,
      participantCount: 5,
      teams: [{ id: 't1' }, { id: 't2' }]
    } as any;
    component.stats.totalTeams = 2;

    expect(component.getParticipantDisplayCount()).toBe(5);
  });

  it('tombe sur la liste de participants quand les équipes sont absentes', () => {
    component.selectedGame = {
      ...component.selectedGame,
      participantCount: 0,
      participants: [{ id: 'p1' }, { id: 'p2' }]
    } as any;
    component.stats.totalTeams = 0;

    expect(component.getParticipantDisplayCount()).toBe(2);
  });

  it('utilise stats.totalTeams quand il n\'y a pas de teams ni participants', () => {
    component.selectedGame = {
      ...component.selectedGame,
      participantCount: 0,
      teams: [],
      participants: []
    } as any;
    component.stats.totalTeams = 4;

    expect(component.getParticipantDisplayCount()).toBe(4);
  });

  it('retourne 0 quand aucune source n\'est disponible', () => {
    component.selectedGame = {
      ...component.selectedGame,
      participantCount: 0,
      teams: undefined,
      participants: undefined
    } as any;
    component.stats.totalTeams = 0;

    expect(component.getParticipantDisplayCount()).toBe(0);
  });
});
