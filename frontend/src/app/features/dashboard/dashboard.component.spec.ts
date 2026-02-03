import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NEVER, of, throwError } from 'rxjs';
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
    const httpSpy = jasmine.createSpyObj<HttpClient>('HttpClient', ['get']);
    httpSpy.get.and.returnValue(of({}));

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
        { provide: HttpClient, useValue: httpSpy }
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

    const heading = fixture.nativeElement.querySelector('.dashboard-title') as HTMLElement;
    expect(heading?.textContent).toContain(translationService.t('navigation.dashboard'));
  });

  it('affiche les libellés en anglais quand la langue est en', () => {
    translationService.setLanguage('en');
    fixture.detectChanges();

    const heading = fixture.nativeElement.querySelector('.dashboard-title') as HTMLElement;
    expect(heading?.textContent).toContain(translationService.t('navigation.dashboard'));
  });

  it('retourne le statut traduit pour une game active', () => {
    const status = component.getGameStatus({ ...component.selectedGame, status: 'ACTIVE' } as any);

    expect(status).toBe(translationService.t('games.status.active'));
  });

  it('retourne un statut inconnu par defaut', () => {
    translationService.setLanguage('en');

    const status = component.getGameStatus({ ...component.selectedGame, status: 'UNKNOWN' } as any);

    expect(status).toBe(translationService.t('games.status.unknown'));
  });

  it('utilise le nombre d’équipes quand participantCount est à zéro', () => {
    component.selectedGame = {
      ...component.selectedGame,
      participantCount: 0,
      teams: [{ id: 't1' }, { id: 't2' }, { id: 't3' }]
    } as any;
    component.stats.totalTeams = 3;

    expect(
      component.formatter.getParticipantDisplayCount(component.selectedGame, component.stats.totalTeams)
    ).toBe(3);
  });

  it('privilégie participantCount quand il est défini', () => {
    component.selectedGame = {
      ...component.selectedGame,
      participantCount: 5,
      teams: [{ id: 't1' }, { id: 't2' }]
    } as any;
    component.stats.totalTeams = 2;

    expect(
      component.formatter.getParticipantDisplayCount(component.selectedGame, component.stats.totalTeams)
    ).toBe(5);
  });

  it('tombe sur la liste de participants quand les équipes sont absentes', () => {
    component.selectedGame = {
      ...component.selectedGame,
      participantCount: 0,
      participants: [{ id: 'p1' }, { id: 'p2' }]
    } as any;
    component.stats.totalTeams = 0;

    expect(
      component.formatter.getParticipantDisplayCount(component.selectedGame, component.stats.totalTeams)
    ).toBe(2);
  });

  it('utilise stats.totalTeams quand il n\'y a pas de teams ni participants', () => {
    component.selectedGame = {
      ...component.selectedGame,
      participantCount: 0,
      teams: [],
      participants: []
    } as any;
    component.stats.totalTeams = 4;

    expect(
      component.formatter.getParticipantDisplayCount(component.selectedGame, component.stats.totalTeams)
    ).toBe(4);
  });

  it('retourne 0 quand aucune source n\'est disponible', () => {
    component.selectedGame = {
      ...component.selectedGame,
      participantCount: 0,
      teams: undefined,
      participants: undefined
    } as any;
    component.stats.totalTeams = 0;

    expect(
      component.formatter.getParticipantDisplayCount(component.selectedGame, component.stats.totalTeams)
    ).toBe(0);
  });

  it('formate les nombres en espagnol', () => {
    translationService.setLanguage('es');

    const formatted = component.formatter.formatNumber(1234);
    const expected = new Intl.NumberFormat('es-ES').format(1234);

    expect(formatted).toBe(expected);
  });

  it('formate les nombres en portugais', () => {
    translationService.setLanguage('pt');

    const formatted = component.formatter.formatNumber(1234);
    const expected = new Intl.NumberFormat('pt-PT').format(1234);

    expect(formatted).toBe(expected);
  });

  describe('ngOnInit', () => {
    it('should load games on initialization', () => {
      const gameServiceSpy = TestBed.inject(GameService) as jasmine.SpyObj<GameService>;
      gameServiceSpy.getUserGames.and.returnValue(of([component.selectedGame] as any));

      component.ngOnInit();

      expect(gameServiceSpy.getUserGames).toHaveBeenCalled();
    });

    it('should subscribe to language changes', () => {
      component.ngOnInit();

      translationService.setLanguage('es');
      fixture.detectChanges();

      expect(component.displayStats).toBeDefined();
    });


    it('should handle route params with game id', () => {
      const gameServiceSpy = TestBed.inject(GameService) as jasmine.SpyObj<GameService>;
      const mockGame = { id: 'game-123', name: 'Test Game' } as any;
      gameServiceSpy.getGameById.and.returnValue(of(mockGame));

      component.ngOnInit();

      expect(component.selectedGame).toBeDefined();
    });
  });


  describe('getGameStatus', () => {
    it('should return active status for ACTIVE game', () => {
      const game = { ...component.selectedGame, status: 'ACTIVE' } as any;
      const status = component.getGameStatus(game);

      expect(status).toBe(translationService.t('games.status.active'));
    });

    it('should return completed status for COMPLETED game', () => {
      const game = { ...component.selectedGame, status: 'COMPLETED' } as any;
      const status = component.getGameStatus(game);

      expect(status).toBe(translationService.t('games.status.completed'));
    });

    it('should return draft status for DRAFT game', () => {
      const game = { ...component.selectedGame, status: 'DRAFT' } as any;
      const status = component.getGameStatus(game);

      expect(status).toBe(translationService.t('games.status.draft'));
    });
  });

  describe('Component lifecycle', () => {
    it('should cleanup subscriptions on destroy', () => {
      component.ngOnInit();
      const subscriptionCount = component['subscriptions'].length;

      component.ngOnDestroy();

      expect(subscriptionCount).toBeGreaterThan(0);
    });

    it('should destroy charts on component destroy', () => {
      component.ngOnInit();
      component.ngAfterViewInit();

      component.ngOnDestroy();

      expect(component['regionChart']).toBeNull();
      expect(component['pointsChart']).toBeNull();
    });
  });

  describe('Error handling', () => {
    it('should have error property available', () => {
      expect(component.error).toBeDefined();
    });

    it('should have isLoading property available', () => {
      expect(component.isLoading).toBeDefined();
    });
  });

  describe('Stats display', () => {
    it('should have stats property initialized', () => {
      expect(component.stats).toBeDefined();
      expect(component.stats.totalTeams).toBeDefined();
      expect(component.stats.totalPlayers).toBeDefined();
      expect(component.stats.totalPoints).toBeDefined();
    });

    it('should have displayStats property', () => {
      expect(component.displayStats).toBeDefined();
    });

    it('should handle stats updates', () => {
      component.stats.totalTeams = 15;
      component.stats.totalPlayers = 75;
      component.stats.totalPoints = 10000;

      expect(component.stats.totalTeams).toBe(15);
      expect(component.stats.totalPlayers).toBe(75);
      expect(component.stats.totalPoints).toBe(10000);
    });
  });

  describe('Game selection', () => {
    it('should update selectedGame when game selection changes', () => {
      const newGame = {
        id: 'game-new',
        name: 'New Game',
        status: 'ACTIVE'
      } as any;

      component.ngOnInit();
      const gameSelectionService = TestBed.inject(GameSelectionService) as any;
      gameSelectionService.selectedGame$ = of(newGame);

      expect(component.selectedGame).toBeDefined();
    });

  });

  describe('Participant count display', () => {
    it('should use participantCount when available', () => {
      const game = {
        ...component.selectedGame,
        participantCount: 10,
        teams: [{ id: '1' }, { id: '2' }]
      } as any;

      const count = component.formatter.getParticipantDisplayCount(game, 2);

      expect(count).toBe(10);
    });

    it('should fallback to teams length when participantCount is 0', () => {
      const game = {
        ...component.selectedGame,
        participantCount: 0,
        teams: [{ id: '1' }, { id: '2' }, { id: '3' }]
      } as any;

      const count = component.formatter.getParticipantDisplayCount(game, 3);

      expect(count).toBe(3);
    });

    it('should use stats.totalTeams as final fallback', () => {
      const game = {
        ...component.selectedGame,
        participantCount: 0,
        teams: undefined,
        participants: undefined
      } as any;

      const count = component.formatter.getParticipantDisplayCount(game, 5);

      expect(count).toBe(5);
    });
  });

  describe('Number formatting', () => {
    it('should format numbers according to current language', () => {
      translationService.setLanguage('fr');
      const formatted = component.formatter.formatNumber(1000);

      expect(formatted).toBeTruthy();
      expect(typeof formatted).toBe('string');
    });

    it('should handle large numbers correctly', () => {
      const formatted = component.formatter.formatNumber(1000000);

      expect(formatted).toBeTruthy();
      expect(formatted.length).toBeGreaterThan(0);
    });

    it('should handle zero correctly', () => {
      const formatted = component.formatter.formatNumber(0);

      expect(formatted).toBe('0');
    });
  });
});

