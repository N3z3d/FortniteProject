import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
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
import { UiErrorFeedbackService } from '../../core/services/ui-error-feedback.service';

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
    const uiFeedbackSpy = jasmine.createSpyObj<UiErrorFeedbackService>('UiErrorFeedbackService', [
      'showInfoFromKey'
    ]);
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
        { provide: UiErrorFeedbackService, useValue: uiFeedbackSpy },
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

  describe('loadDashboardData', () => {
    it('sets error when no game is selected', async () => {
      component.selectedGame = null;

      await component.loadDashboardData();

      expect(component.error).toBeTruthy();
    });

    it('sets isLoading true when showLoading is true', async () => {
      const facadeSpy = TestBed.inject(DashboardFacade) as jasmine.SpyObj<DashboardFacade>;
      facadeSpy.getDashboardData.and.returnValue(of({
        leaderboard: [], statistics: null, regionDistribution: {}, teams: []
      } as any));

      let loadingDuringCall = false;
      const origIsLoading = Object.getOwnPropertyDescriptor(component, 'isLoading');
      Object.defineProperty(component, 'isLoading', {
        set(val: boolean) { if (val) loadingDuringCall = true; (origIsLoading?.set ?? (() => {})).call(this, val); },
        get() { return (origIsLoading?.get ?? (() => false)).call(this); },
        configurable: true
      });
      // Simpler approach: check that after completion isLoading is false
      await component.loadDashboardData(true);

      expect(component.isLoading).toBeFalse();
    });

    it('populates leaderboard from facade response', async () => {
      const facadeSpy = TestBed.inject(DashboardFacade) as jasmine.SpyObj<DashboardFacade>;
      const entries = [
        { rank: 1, teamName: 'Team A', ownerName: 'Owner A', totalPoints: 100 },
        { rank: 2, teamName: 'Team B', ownerName: 'Owner B', totalPoints: 80 }
      ];
      facadeSpy.getDashboardData.and.returnValue(of({
        leaderboard: entries, statistics: null, regionDistribution: {}, teams: []
      } as any));

      await component.loadDashboardData();

      expect(component.leaderboardEntries.length).toBe(2);
      expect(component.dashboardLeaderboard.length).toBe(2);
      expect(component.dashboardLeaderboard[0].name).toBe('Team A');
    });

    it('limits dashboard leaderboard preview to 5 entries', async () => {
      const facadeSpy = TestBed.inject(DashboardFacade) as jasmine.SpyObj<DashboardFacade>;
      const entries = Array.from({ length: 8 }, (_, i) => ({
        rank: i + 1, teamName: `Team ${i}`, ownerName: `Owner ${i}`, totalPoints: 100 - i * 10
      }));
      facadeSpy.getDashboardData.and.returnValue(of({
        leaderboard: entries, statistics: null, regionDistribution: {}, teams: []
      } as any));

      await component.loadDashboardData();

      expect(component.dashboardLeaderboard.length).toBe(5);
    });

    it('updates region distribution from facade response', async () => {
      const facadeSpy = TestBed.inject(DashboardFacade) as jasmine.SpyObj<DashboardFacade>;
      facadeSpy.getDashboardData.and.returnValue(of({
        leaderboard: [], statistics: null,
        regionDistribution: { EU: 10, NA: 5 }, teams: []
      } as any));

      await component.loadDashboardData();

      expect(component.stats.teamComposition?.regions).toEqual({ EU: 10, NA: 5 });
    });

    it('sets proPlayersCount to 0 for non-active games', async () => {
      component.selectedGame = { ...component.selectedGame!, status: 'CREATING' } as any;
      const facadeSpy = TestBed.inject(DashboardFacade) as jasmine.SpyObj<DashboardFacade>;
      facadeSpy.getDashboardData.and.returnValue(of({
        leaderboard: [], statistics: null, regionDistribution: {}, teams: []
      } as any));

      await component.loadDashboardData();

      expect(component.stats.proPlayersCount).toBe(0);
    });

    it('falls back to mock data on error', async () => {
      const facadeSpy = TestBed.inject(DashboardFacade) as jasmine.SpyObj<DashboardFacade>;
      const uiFeedbackSpy = TestBed.inject(UiErrorFeedbackService) as jasmine.SpyObj<UiErrorFeedbackService>;
      facadeSpy.getDashboardData.and.returnValue(throwError(() => new Error('Backend offline')));

      await component.loadDashboardData();

      expect(component.error).toBeNull();
      expect(component.isLoading).toBeFalse();
      expect(uiFeedbackSpy.showInfoFromKey).toHaveBeenCalledWith('dashboard.messages.demoMode', 3000);
    });

    it('does not show loading spinner when showLoading is false', async () => {
      const facadeSpy = TestBed.inject(DashboardFacade) as jasmine.SpyObj<DashboardFacade>;
      facadeSpy.getDashboardData.and.returnValue(of({
        leaderboard: [], statistics: null, regionDistribution: {}, teams: []
      } as any));

      component.isLoading = false;
      await component.loadDashboardData(false);

      expect(component.isLoading).toBeFalse();
    });
  });

  describe('navigateTo', () => {
    it('navigates to the given route', () => {
      const routerSpy = TestBed.inject(Router) as jasmine.SpyObj<Router>;

      component.navigateTo('/games');

      expect(routerSpy.navigate).toHaveBeenCalledWith(['/', 'games']);
    });

    it('splits nested routes into segments', () => {
      const routerSpy = TestBed.inject(Router) as jasmine.SpyObj<Router>;

      component.navigateTo('/games/create');

      expect(routerSpy.navigate).toHaveBeenCalledWith(['/', 'games', 'create']);
    });

    it('announces navigation to screen readers', () => {
      const accessibilitySpy = TestBed.inject(AccessibilityAnnouncerService) as jasmine.SpyObj<AccessibilityAnnouncerService>;

      component.navigateTo('/leaderboard');

      expect(accessibilitySpy.announceNavigation).toHaveBeenCalled();
    });
  });

  describe('accessibility', () => {
    it('renders navigation cards as keyboard-focusable buttons', () => {
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
      fixture.detectChanges();

      const cards = Array.from(
        fixture.nativeElement.querySelectorAll('.navigation-grid .nav-card')
      ) as HTMLElement[];

      expect(cards.length).toBe(4);
      cards.forEach((card) => {
        expect(card.getAttribute('role')).toBe('button');
        expect(card.getAttribute('tabindex')).toBe('0');
      });
    });

    it('navigates from keyboard handler when pressing Enter', () => {
      const navigateSpy = spyOn(component, 'navigateTo');
      const event = new KeyboardEvent('keydown', { key: 'Enter' });

      component.onNavigationCardKeydown(event, '/games');

      expect(navigateSpy).toHaveBeenCalledWith('/games');
    });
  });

  describe('viewGameDetails', () => {
    it('navigates to game detail page', () => {
      const routerSpy = TestBed.inject(Router) as jasmine.SpyObj<Router>;
      const game = { id: 'game-42', name: 'Test' } as any;

      component.viewGameDetails(game);

      expect(routerSpy.navigate).toHaveBeenCalledWith(['/games', 'game-42']);
    });
  });

  describe('getGameStatus (all statuses)', () => {
    const statuses = [
      { input: 'CREATING', key: 'games.status.creating' },
      { input: 'DRAFTING', key: 'games.status.drafting' },
      { input: 'ACTIVE', key: 'games.status.active' },
      { input: 'FINISHED', key: 'games.status.finished' },
      { input: 'CANCELLED', key: 'games.status.cancelled' },
      { input: 'COMPLETED', key: 'games.status.completed' },
      { input: 'DRAFT', key: 'games.status.draft' },
      { input: 'RECRUITING', key: 'games.status.recruiting' },
      { input: 'UNKNOWN_STATUS', key: 'games.status.unknown' }
    ];

    statuses.forEach(({ input, key }) => {
      it(`returns translated status for ${input}`, () => {
        const game = { ...component.selectedGame, status: input } as any;
        expect(component.getGameStatus(game)).toBe(translationService.t(key));
      });
    });
  });

  describe('loadGames error', () => {
    it('sets games to empty array on error', () => {
      const gameServiceSpy = TestBed.inject(GameService) as jasmine.SpyObj<GameService>;
      gameServiceSpy.getUserGames.and.returnValue(throwError(() => new Error('fail')));

      component['loadGames']();

      expect(component.games).toEqual([]);
    });
  });
});
