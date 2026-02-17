import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { BreakpointObserver } from '@angular/cdk/layout';
import { BehaviorSubject, of, throwError } from 'rxjs';

import { MainLayoutComponent } from './main-layout.component';
import { UserContextService, UserProfile } from '../../../core/services/user-context.service';
import { GameService } from '../../../features/game/services/game.service';
import { Game } from '../../../features/game/models/game.interface';
import { GameSelectionService } from '../../../core/services/game-selection.service';
import { LoggerService } from '../../../core/services/logger.service';
import { UserGamesState, UserGamesStore } from '../../../core/services/user-games.store';
import { UiErrorFeedbackService } from '../../../core/services/ui-error-feedback.service';
import { AccessibilityAnnouncerService } from '../../services/accessibility-announcer.service';
import { FocusManagementService } from '../../services/focus-management.service';

describe('MainLayoutComponent', () => {
  let component: MainLayoutComponent;
  let fixture: ComponentFixture<MainLayoutComponent>;
  let router: Router;
  let gameService: jasmine.SpyObj<GameService>;
  let userContextService: jasmine.SpyObj<UserContextService>;
  let userGamesStore: jasmine.SpyObj<UserGamesStore>;
  let gameSelectionService: jasmine.SpyObj<GameSelectionService>;
  let uiFeedback: jasmine.SpyObj<UiErrorFeedbackService>;
  let stateSubject: BehaviorSubject<UserGamesState>;

  const mockUser: UserProfile = {
    id: '1',
    username: 'Thibaut',
    email: 'thibaut@test.com'
  };

  const mockGames: Game[] = [
    {
      id: '1',
      name: 'Championnat Saison 1',
      creatorName: 'Thibaut',
      maxParticipants: 10,
      status: 'CREATING',
      createdAt: '2024-01-15T10:30:00',
      participantCount: 3,
      canJoin: true,
      draftRules: {
        maxPlayersPerTeam: 5,
        timeLimitPerPick: 60,
        autoPickEnabled: true,
        regionQuotas: {
          EU: 7,
          NAC: 7,
          BR: 7,
          ASIA: 7,
          OCE: 7,
          NAW: 7,
          ME: 7
        }
      }
    },
    {
      id: '2',
      name: 'Tournoi Amical',
      creatorName: 'Marcel',
      maxParticipants: 10,
      status: 'DRAFTING',
      createdAt: '2024-01-14T15:45:00',
      participantCount: 5,
      canJoin: false,
      draftRules: {
        maxPlayersPerTeam: 5,
        timeLimitPerPick: 60,
        autoPickEnabled: true,
        regionQuotas: {
          EU: 7,
          NAC: 7,
          BR: 7,
          ASIA: 7,
          OCE: 7,
          NAW: 7,
          ME: 7
        }
      }
    }
  ];

  const initialState: UserGamesState = {
    games: [],
    loading: false,
    error: null,
    lastLoaded: null
  };

  beforeEach(async () => {
    const breakpointObserver = jasmine.createSpyObj('BreakpointObserver', ['observe']);
    gameService = jasmine.createSpyObj('GameService', ['joinGameWithCode']);
    const logger = jasmine.createSpyObj('LoggerService', ['debug', 'info', 'warn', 'error']);
    uiFeedback = jasmine.createSpyObj('UiErrorFeedbackService', ['showSuccessWithAction']);
    const accessibilityService = jasmine.createSpyObj('AccessibilityAnnouncerService', ['announceNavigation']);
    const focusManagementService = jasmine.createSpyObj('FocusManagementService', ['restoreFocusAfterNavigation']);

    userContextService = jasmine.createSpyObj('UserContextService', ['getCurrentUser', 'logout']);
    gameSelectionService = jasmine.createSpyObj(
      'GameSelectionService',
      ['setSelectedGame'],
      { selectedGame$: of(null) }
    );

    stateSubject = new BehaviorSubject<UserGamesState>(initialState);
    userGamesStore = jasmine.createSpyObj<UserGamesStore>(
      'UserGamesStore',
      ['loadGames', 'refreshGames', 'clear', 'startAutoRefresh', 'stopAutoRefresh'],
      { state$: stateSubject.asObservable() }
    );

    breakpointObserver.observe.and.returnValue(of({ matches: false }));
    userGamesStore.loadGames.and.returnValue(of([]));
    userGamesStore.refreshGames.and.returnValue(of([]));
    userContextService.getCurrentUser.and.returnValue(mockUser);

    await TestBed.configureTestingModule({
      imports: [MainLayoutComponent, RouterTestingModule, NoopAnimationsModule],
      providers: [
        { provide: BreakpointObserver, useValue: breakpointObserver },
        { provide: GameService, useValue: gameService },
        { provide: LoggerService, useValue: logger },
        { provide: UiErrorFeedbackService, useValue: uiFeedback },
        { provide: AccessibilityAnnouncerService, useValue: accessibilityService },
        { provide: FocusManagementService, useValue: focusManagementService },
        { provide: UserContextService, useValue: userContextService },
        { provide: GameSelectionService, useValue: gameSelectionService },
        { provide: UserGamesStore, useValue: userGamesStore }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(MainLayoutComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load current user and games on init', () => {
    component.ngOnInit();
    stateSubject.next({ ...initialState, games: mockGames });

    expect(userContextService.getCurrentUser).toHaveBeenCalled();
    expect(userGamesStore.loadGames).toHaveBeenCalled();
    expect(userGamesStore.startAutoRefresh).toHaveBeenCalledWith(15000);
    expect(component.currentUser).toEqual(mockUser);
    expect(component.userGames).toEqual(mockGames);
  });

  it('should refresh user games', () => {
    component.reloadUserGames();

    expect(userGamesStore.refreshGames).toHaveBeenCalled();
  });

  it('should navigate to draft when selecting a drafting game', () => {
    component.selectGame(mockGames[1]);

    expect(router.navigate).toHaveBeenCalledWith(['/games', mockGames[1].id, 'draft']);
  });

  it('should navigate to game details when selecting a non-drafting game', () => {
    component.selectGame(mockGames[0]);

    expect(router.navigate).toHaveBeenCalledWith(['/games', mockGames[0].id]);
  });

  it('should clear local data when logging out', () => {
    component.ngOnInit();
    stateSubject.next({ ...initialState, games: mockGames });

    component.logout();

    expect(userContextService.logout).toHaveBeenCalled();
    expect(userGamesStore.clear).toHaveBeenCalled();
    expect(component.currentUser).toBeNull();
    expect(component.userGames).toEqual([]);
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should stop auto-refresh on destroy', () => {
    component.ngOnInit();

    component.ngOnDestroy();

    expect(userGamesStore.stopAutoRefresh).toHaveBeenCalled();
  });

  it('should refresh games on window focus', () => {
    component.ngOnInit();
    userGamesStore.refreshGames.calls.reset();

    window.dispatchEvent(new Event('focus'));

    expect(userGamesStore.refreshGames).toHaveBeenCalled();
  });

  it('should refresh games when page becomes visible', () => {
    component.ngOnInit();
    userGamesStore.refreshGames.calls.reset();
    spyOnProperty(document, 'visibilityState', 'get').and.returnValue('visible');

    document.dispatchEvent(new Event('visibilitychange'));

    expect(userGamesStore.refreshGames).toHaveBeenCalled();
  });

  it('should display welcome message in template', () => {
    component.ngOnInit();
    stateSubject.next({ ...initialState, games: mockGames });
    fixture.detectChanges();

    const welcomeText = fixture.nativeElement.querySelector('.toolbar-title');
    expect(welcomeText.textContent).toContain('Thibaut');
  });

  it('should display join game button text in sidebar', () => {
    component.ngOnInit();
    stateSubject.next({ ...initialState, games: mockGames });
    fixture.detectChanges();

    const joinButton = fixture.nativeElement.querySelector('.join-game-btn');
    expect(joinButton).toBeTruthy();
    expect(joinButton.textContent.trim()).toContain(component.t.t('games.home.joinWithCode'));
  });

  it('should expose explicit aria-labels for profile and settings icon buttons', () => {
    component.ngOnInit();
    stateSubject.next({ ...initialState, games: mockGames });
    fixture.detectChanges();

    const iconButtons = fixture.nativeElement.querySelectorAll(
      '.user-section button[mat-icon-button]'
    ) as NodeListOf<HTMLButtonElement>;

    expect(iconButtons[0].getAttribute('aria-label')).toBe(component.t.t('navigation.profile'));
    expect(iconButtons[1].getAttribute('aria-label')).toBe(component.t.t('navigation.settings'));
  });

  it('should expose explicit aria-labels for sidebar icon controls', () => {
    component.ngOnInit();
    stateSubject.next({ ...initialState, games: mockGames });
    fixture.detectChanges();

    const toggleButton = fixture.nativeElement.querySelector(
      '.toggle-sidebar-btn'
    ) as HTMLButtonElement;
    const createButton = fixture.nativeElement.querySelector(
      '.create-game-sidebar-btn'
    ) as HTMLButtonElement;

    expect(toggleButton.getAttribute('aria-label')).toBe(component.t.t('layout.collapse'));
    expect(createButton.getAttribute('aria-label')).toBe(component.t.t('games.createGame'));
  });

  it('should render sidebar game items as keyboard-focusable buttons', () => {
    component.ngOnInit();
    stateSubject.next({ ...initialState, games: mockGames });
    fixture.detectChanges();

    const gameItem = fixture.nativeElement.querySelector('.game-item') as HTMLElement;

    expect(gameItem).toBeTruthy();
    expect(gameItem.getAttribute('role')).toBe('button');
    expect(gameItem.getAttribute('tabindex')).toBe('0');
    expect(gameItem.getAttribute('aria-label')).toContain(component.t.t('games.home.viewDetails'));
  });

  it('should select game from keyboard Enter on sidebar game item', () => {
    const selectGameSpy = spyOn(component, 'selectGame');
    const event = new KeyboardEvent('keydown', { key: 'Enter' });

    component.onGameItemKeyDown(event, mockGames[0]);

    expect(selectGameSpy).toHaveBeenCalledWith(mockGames[0]);
  });

  it('should show empty state when no games', () => {
    component.ngOnInit();
    stateSubject.next({ ...initialState, games: [] });
    fixture.detectChanges();

    const emptyState = fixture.nativeElement.querySelector('.sidebar-empty');
    expect(emptyState).toBeTruthy();
    expect(emptyState.textContent).toContain(component.t.t('games.noGames'));
  });

  it('should map already-in-game errors to explicit message key', () => {
    const result = (component as any).extractJoinErrorMessage(
      new Error('User is already participating in this game')
    );

    expect(result).toBe('games.joinDialog.alreadyInGame');
  });

  it('should not expose explicit backend message and fallback to i18n', () => {
    const result = (component as any).extractJoinErrorMessage({
      error: { message: 'Code expired' }
    });

    expect(result).toBe('games.joinDialog.invalidCode');
  });

  it('should map not-found errors to invalid-or-unavailable message key', () => {
    const result = (component as any).extractJoinErrorMessage(
      new Error('Ressource non trouvee')
    );

    expect(result).toBe('games.joinDialog.invalidOrUnavailableCode');
  });

  it('should map RESOURCE_NOT_FOUND code to invalid-or-unavailable message key', () => {
    const result = (component as any).extractJoinErrorMessage({
      error: { code: 'RESOURCE_NOT_FOUND', message: 'Not found' }
    });

    expect(result).toBe('games.joinDialog.invalidOrUnavailableCode');
  });

  it('should map invalid input parameters to invalid-or-unavailable message key', () => {
    const result = (component as any).extractJoinErrorMessage({
      error: { code: 'INVALID_INPUT_PARAMETERS', message: 'Invalid input parameters' }
    });

    expect(result).toBe('games.joinDialog.invalidOrUnavailableCode');
  });

  it('should map rate-limit errors to too-many-attempts message key', () => {
    const result = (component as any).extractJoinErrorMessage({
      error: { code: 'SYS_004', message: 'Too many attempts' }
    });

    expect(result).toBe('games.joinDialog.tooManyAttempts');
  });

  it('should map mojibake not-found message to invalid-or-unavailable message key', () => {
    const result = (component as any).extractJoinErrorMessage(
      new Error('Ressource non trouvÃ©e')
    );

    expect(result).toBe('games.joinDialog.invalidOrUnavailableCode');
  });

  it('should show inline feedback when join code is empty', () => {
    component.showJoinCodeInput = true;
    component.invitationCode = '   ';

    component.joinWithCode();
    fixture.detectChanges();

    const feedback = fixture.nativeElement.querySelector('.join-code-section .join-feedback');
    expect(component.joinFeedbackMessage).toBe(component.t.t('games.joinDialog.enterCodeError'));
    expect(feedback?.textContent).toContain(component.t.t('games.joinDialog.enterCodeError'));
    expect(uiFeedback.showSuccessWithAction).not.toHaveBeenCalled();
  });

  it('should show inline format feedback when join code contains invalid characters', () => {
    component.showJoinCodeInput = true;
    component.invitationCode = '@@';

    component.joinWithCode();
    fixture.detectChanges();

    const feedback = fixture.nativeElement.querySelector('.join-code-section .join-feedback');
    expect(component.joinFeedbackMessage).toBe(component.t.t('games.joinDialog.invalidCodeFormat'));
    expect(feedback?.textContent).toContain(component.t.t('games.joinDialog.invalidCodeFormat'));
    expect(gameService.joinGameWithCode).not.toHaveBeenCalled();
  });

  it('should show inline feedback for already-in-game error in sidebar flow', () => {
    component.showJoinCodeInput = true;
    component.invitationCode = 'DUPLICATE';
    gameService.joinGameWithCode.and.returnValue(
      throwError(() => ({ error: { code: 'USER_ALREADY_IN_GAME', message: 'User is already participating in this game' } }))
    );

    component.joinWithCode();

    expect(component.joinFeedbackMessage).toBe(component.t.t('games.joinDialog.alreadyInGame'));
    expect(uiFeedback.showSuccessWithAction).not.toHaveBeenCalled();
  });

  describe('openJoinDialog / cancelJoin', () => {
    it('opens join code input and clears feedback', () => {
      component.joinFeedbackMessage = 'old error';

      component.openJoinDialog();

      expect(component.showJoinCodeInput).toBeTrue();
      expect(component.joinFeedbackMessage).toBeNull();
    });

    it('cancels join and resets state', () => {
      component.showJoinCodeInput = true;
      component.invitationCode = 'ABC123';
      component.joinFeedbackMessage = 'some error';

      component.cancelJoin();

      expect(component.showJoinCodeInput).toBeFalse();
      expect(component.invitationCode).toBe('');
      expect(component.joinFeedbackMessage).toBeNull();
    });
  });

  describe('joinWithCode success', () => {
    it('joins game, shows success, clears input and refreshes games', () => {
      component.showJoinCodeInput = true;
      component.invitationCode = 'VALIDCODE';
      const joinedGame = { id: 'g-99', name: 'Joined Game' } as any;
      gameService.joinGameWithCode.and.returnValue(of(joinedGame));

      component.joinWithCode();

      expect(gameService.joinGameWithCode).toHaveBeenCalledWith('VALIDCODE');
      expect(uiFeedback.showSuccessWithAction).toHaveBeenCalled();
      expect(component.invitationCode).toBe('');
      expect(component.showJoinCodeInput).toBeFalse();
      expect(component.joiningGame).toBeFalse();
      expect(userGamesStore.refreshGames).toHaveBeenCalled();
    });
  });

  describe('goToHome', () => {
    it('deselects game and navigates to /games', () => {
      component.selectedGame = mockGames[0];

      component.goToHome();

      expect(component.selectedGame).toBeNull();
      expect(gameSelectionService.setSelectedGame).toHaveBeenCalledWith(null);
      expect(router.navigate).toHaveBeenCalledWith(['/games']);
    });
  });

  describe('switchProfile', () => {
    it('logs out and navigates to login with switchUser param', () => {
      component.switchProfile();

      expect(userContextService.logout).toHaveBeenCalled();
      expect(router.navigate).toHaveBeenCalledWith(['/login'], {
        queryParams: { switchUser: 'true' }
      });
    });
  });

  describe('canManageDraft', () => {
    it('returns true when game is DRAFTING', () => {
      component.selectedGame = { ...mockGames[0], status: 'DRAFTING' } as any;
      expect(component.canManageDraft()).toBeTrue();
    });

    it('returns false when game is not DRAFTING', () => {
      component.selectedGame = { ...mockGames[0], status: 'ACTIVE' } as any;
      expect(component.canManageDraft()).toBeFalse();
    });

    it('returns false when no game selected', () => {
      component.selectedGame = null;
      expect(component.canManageDraft()).toBeFalse();
    });
  });

  describe('getStatusColor', () => {
    it('returns primary for CREATING', () => {
      expect(component.getStatusColor('CREATING')).toBe('primary');
    });

    it('returns accent for DRAFTING', () => {
      expect(component.getStatusColor('DRAFTING')).toBe('accent');
    });

    it('returns warn for FINISHED', () => {
      expect(component.getStatusColor('FINISHED')).toBe('warn');
    });

    it('returns warn for CANCELLED', () => {
      expect(component.getStatusColor('CANCELLED')).toBe('warn');
    });

    it('returns primary for unknown status', () => {
      expect(component.getStatusColor('UNKNOWN' as any)).toBe('primary');
    });
  });

  describe('getStatusLabel', () => {
    it('returns label for each known status', () => {
      expect(component.getStatusLabel('CREATING')).toBe('En création');
      expect(component.getStatusLabel('DRAFTING')).toBe('Draft en cours');
      expect(component.getStatusLabel('ACTIVE')).toBe('Active');
      expect(component.getStatusLabel('FINISHED')).toBe('Terminée');
      expect(component.getStatusLabel('CANCELLED')).toBe('Annulée');
    });

    it('returns raw status for unknown', () => {
      expect(component.getStatusLabel('UNKNOWN' as any)).toBe('UNKNOWN');
    });
  });

  describe('getStatusIcon', () => {
    it('returns icon for each known status', () => {
      expect(component.getStatusIcon('CREATING')).toBe('build');
      expect(component.getStatusIcon('DRAFTING')).toBe('how_to_vote');
      expect(component.getStatusIcon('ACTIVE')).toBe('play_arrow');
      expect(component.getStatusIcon('COMPLETED')).toBe('emoji_events');
    });

    it('returns default icon for unknown status', () => {
      expect(component.getStatusIcon('UNKNOWN' as any)).toBe('sports_esports');
    });
  });

  describe('getCurrentUserInitials', () => {
    it('returns first 2 chars uppercased from username', () => {
      component.currentUser = mockUser;
      expect(component.getCurrentUserInitials()).toBe('TH');
    });

    it('returns U when no user', () => {
      component.currentUser = null;
      expect(component.getCurrentUserInitials()).toBe('U');
    });

    it('returns U when username is empty', () => {
      component.currentUser = { ...mockUser, username: '' };
      expect(component.getCurrentUserInitials()).toBe('U');
    });
  });

  describe('utility methods', () => {
    it('showAllGames navigates to /games', () => {
      component.showAllGames();
      expect(router.navigate).toHaveBeenCalledWith(['/games']);
    });

    it('joinGame navigates to /games/join', () => {
      component.joinGame();
      expect(router.navigate).toHaveBeenCalledWith(['/games/join']);
    });

    it('createGame navigates to /games/create', () => {
      component.createGame();
      expect(router.navigate).toHaveBeenCalledWith(['/games/create']);
    });

    it('quickCreateGame navigates with quick param', () => {
      component.quickCreateGame();
      expect(router.navigate).toHaveBeenCalledWith(['/games/create'], { queryParams: { quick: true } });
    });

    it('trackByGameId returns game id', () => {
      expect(component.trackByGameId(0, mockGames[0])).toBe('1');
    });

    it('hasGames returns true when games loaded', () => {
      component.userGames = mockGames;
      expect(component.hasGames()).toBeTrue();
    });

    it('hasGames returns false when empty', () => {
      component.userGames = [];
      expect(component.hasGames()).toBeFalse();
    });

    it('getGameCount returns number of games', () => {
      component.userGames = mockGames;
      expect(component.getGameCount()).toBe(2);
    });
  });

  describe('toggleSidebar / toggleSidebarCollapse', () => {
    it('toggleSidebar flips sidebarOpen and saves to localStorage', () => {
      component.sidebarOpen = true;

      component.toggleSidebar();

      expect(component.sidebarOpen).toBeFalse();
      expect(localStorage.getItem('sidebarOpen')).toBe('false');
    });

    it('toggleSidebarCollapse flips sidebarCollapsed', () => {
      component.sidebarCollapsed = false;

      component.toggleSidebarCollapse();

      expect(component.sidebarCollapsed).toBeTrue();
    });
  });

  describe('onNavigationKeyDown', () => {
    it('navigates on Enter key', () => {
      const event = new KeyboardEvent('keydown', { key: 'Enter' });
      spyOn(event, 'preventDefault');

      component.onNavigationKeyDown(event, '/games', 'Games');

      expect(event.preventDefault).toHaveBeenCalled();
      expect(router.navigate).toHaveBeenCalledWith(['/games']);
    });

    it('navigates on Space key', () => {
      const event = new KeyboardEvent('keydown', { key: ' ' });
      spyOn(event, 'preventDefault');

      component.onNavigationKeyDown(event, '/teams', 'Teams');

      expect(router.navigate).toHaveBeenCalledWith(['/teams']);
    });

    it('does nothing on other keys', () => {
      const event = new KeyboardEvent('keydown', { key: 'Tab' });

      component.onNavigationKeyDown(event, '/games', 'Games');

      expect(router.navigate).not.toHaveBeenCalled();
    });
  });

  describe('prepareRoute', () => {
    it('returns * when outlet has animation data', () => {
      const outlet = { activatedRouteData: { animation: 'fade' } } as any;
      expect(component.prepareRoute(outlet)).toBe('*');
    });

    it('returns empty string when no animation data', () => {
      const outlet = { activatedRouteData: {} } as any;
      expect(component.prepareRoute(outlet)).toBe('');
    });

    it('returns empty string when outlet is null', () => {
      expect(component.prepareRoute(null as any)).toBe('');
    });
  });
});
