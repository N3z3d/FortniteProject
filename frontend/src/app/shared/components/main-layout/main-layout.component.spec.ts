import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject, of } from 'rxjs';

import { MainLayoutComponent } from './main-layout.component';
import { UserContextService, UserProfile } from '../../../core/services/user-context.service';
import { GameService } from '../../../features/game/services/game.service';
import { Game } from '../../../features/game/models/game.interface';
import { GameSelectionService } from '../../../core/services/game-selection.service';
import { LoggerService } from '../../../core/services/logger.service';
import { UserGamesState, UserGamesStore } from '../../../core/services/user-games.store';
import { AccessibilityAnnouncerService } from '../../services/accessibility-announcer.service';
import { FocusManagementService } from '../../services/focus-management.service';

describe('MainLayoutComponent', () => {
  let component: MainLayoutComponent;
  let fixture: ComponentFixture<MainLayoutComponent>;
  let router: Router;
  let userContextService: jasmine.SpyObj<UserContextService>;
  let userGamesStore: jasmine.SpyObj<UserGamesStore>;
  let gameSelectionService: jasmine.SpyObj<GameSelectionService>;
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
    const gameService = jasmine.createSpyObj('GameService', ['joinGameWithCode']);
    const logger = jasmine.createSpyObj('LoggerService', ['debug', 'info', 'warn', 'error']);
    const snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
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
      ['loadGames', 'refreshGames', 'clear'],
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
        { provide: MatSnackBar, useValue: snackBar },
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
    expect(joinButton.textContent.trim()).toContain('Rejoindre une partie');
  });

  it('should show empty state when no games', () => {
    component.ngOnInit();
    stateSubject.next({ ...initialState, games: [] });
    fixture.detectChanges();

    const emptyState = fixture.nativeElement.querySelector('.sidebar-empty');
    expect(emptyState).toBeTruthy();
    expect(emptyState.textContent).toContain('Aucune partie');
  });
});
