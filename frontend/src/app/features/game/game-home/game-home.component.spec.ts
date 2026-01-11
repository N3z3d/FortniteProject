import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { BehaviorSubject, of } from 'rxjs';

import { GameHomeComponent } from './game-home.component';
import { GameService } from '../services/game.service';
import { UserContextService, UserProfile } from '../../../core/services/user-context.service';
import { GameSelectionService } from '../../../core/services/game-selection.service';
import { UserGamesState, UserGamesStore } from '../../../core/services/user-games.store';
import { Game } from '../models/game.interface';

describe('GameHomeComponent', () => {
  let component: GameHomeComponent;
  let fixture: ComponentFixture<GameHomeComponent>;
  let gameService: jasmine.SpyObj<GameService>;
  let userContextService: jasmine.SpyObj<UserContextService>;
  let userGamesStore: jasmine.SpyObj<UserGamesStore>;
  let gameSelectionService: jasmine.SpyObj<GameSelectionService>;
  let router: jasmine.SpyObj<Router>;
  let stateSubject: BehaviorSubject<UserGamesState>;

  const mockUser: UserProfile = {
    id: '1',
    username: 'Thibaut',
    email: 'thibaut@example.com'
  };

  const mockGames: Game[] = [
    {
      id: '1',
      name: 'Championnat Saison 1',
      creatorName: 'Thibaut',
      maxParticipants: 49,
      status: 'CREATING',
      createdAt: '2024-01-15T10:30:00',
      participantCount: 3,
      canJoin: true
    },
    {
      id: '2',
      name: 'Tournoi Amical',
      creatorName: 'Marcel',
      maxParticipants: 20,
      status: 'DRAFTING',
      createdAt: '2024-01-14T15:45:00',
      participantCount: 5,
      canJoin: false
    }
  ];

  const initialState: UserGamesState = {
    games: [],
    loading: false,
    error: null,
    lastLoaded: null
  };

  beforeEach(async () => {
    gameService = jasmine.createSpyObj('GameService', ['getAvailableGames']);
    userContextService = jasmine.createSpyObj('UserContextService', ['getCurrentUser', 'logout']);
    gameSelectionService = jasmine.createSpyObj('GameSelectionService', ['setSelectedGame']);
    router = jasmine.createSpyObj('Router', ['navigate']);

    stateSubject = new BehaviorSubject<UserGamesState>(initialState);
    userGamesStore = jasmine.createSpyObj<UserGamesStore>(
      'UserGamesStore',
      ['loadGames', 'refreshGames'],
      { state$: stateSubject.asObservable() }
    );

    gameService.getAvailableGames.and.returnValue(of([]));
    userContextService.getCurrentUser.and.returnValue(mockUser);
    userGamesStore.loadGames.and.returnValue(of([]));
    userGamesStore.refreshGames.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [GameHomeComponent, NoopAnimationsModule],
      providers: [
        { provide: GameService, useValue: gameService },
        { provide: UserContextService, useValue: userContextService },
        { provide: GameSelectionService, useValue: gameSelectionService },
        { provide: UserGamesStore, useValue: userGamesStore },
        { provide: Router, useValue: router }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(GameHomeComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should load current user and games on init', () => {
      component.ngOnInit();

      expect(userContextService.getCurrentUser).toHaveBeenCalled();
      expect(userGamesStore.loadGames).toHaveBeenCalled();
      expect(gameService.getAvailableGames).toHaveBeenCalled();
      expect(component.currentUser).toEqual(mockUser);
    });

    it('should select first game automatically when games are loaded', () => {
      component.ngOnInit();
      stateSubject.next({ ...initialState, games: mockGames });

      expect(component.selectedGame).toEqual(mockGames[0]);
      expect(gameSelectionService.setSelectedGame).toHaveBeenCalledWith(mockGames[0]);
    });

    it('should clear selection when no games are available', () => {
      component.selectedGame = mockGames[0];
      component.ngOnInit();
      stateSubject.next({ ...initialState, games: [] });

      expect(component.selectedGame).toBeNull();
      expect(gameSelectionService.setSelectedGame).toHaveBeenCalledWith(null);
    });

    it('should expose loading and error from the store state', () => {
      component.ngOnInit();
      stateSubject.next({ ...initialState, loading: true });
      expect(component.loading).toBeTrue();

      stateSubject.next({ ...initialState, loading: false, error: 'Erreur' });
      expect(component.error).toBe('Erreur');
    });
  });

  describe('selectGame', () => {
    it('should select the specified game', () => {
      component.ngOnInit();
      component.selectGame(mockGames[1]);

      expect(component.selectedGame).toEqual(mockGames[1]);
      expect(gameSelectionService.setSelectedGame).toHaveBeenCalledWith(mockGames[1]);
    });

    it('should close sidebar on mobile when selecting game', () => {
      Object.defineProperty(window, 'innerWidth', {
        writable: true,
        configurable: true,
        value: 768
      });
      component.sidebarOpened = true;
      component.selectGame(mockGames[0]);

      expect(component.sidebarOpened).toBeFalse();
    });

    it('should keep sidebar open on desktop when selecting game', () => {
      Object.defineProperty(window, 'innerWidth', {
        writable: true,
        configurable: true,
        value: 1024
      });
      component.sidebarOpened = true;
      component.selectGame(mockGames[0]);

      expect(component.sidebarOpened).toBeTrue();
    });
  });

  describe('navigation actions', () => {
    it('should navigate to create game page', () => {
      component.createGame();

      expect(router.navigate).toHaveBeenCalledWith(['/games/create']);
    });

    it('should navigate to join game page', () => {
      component.joinGame();

      expect(router.navigate).toHaveBeenCalledWith(['/games/join']);
    });

    it('should navigate to game details page and store selection', () => {
      component.ngOnInit();
      stateSubject.next({ ...initialState, games: mockGames });

      component.viewGame(mockGames[0].id);

      expect(router.navigate).toHaveBeenCalledWith(['/games', mockGames[0].id]);
      expect(gameSelectionService.setSelectedGame).toHaveBeenCalledWith(mockGames[0]);
    });
  });

  describe('logout', () => {
    it('should logout user and navigate to login page', () => {
      component.logout();

      expect(userContextService.logout).toHaveBeenCalled();
      expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });
  });

  describe('helpers', () => {
    it('should return correct status color', () => {
      expect(component.getStatusColor('CREATING')).toBe('primary');
      expect(component.getStatusColor('DRAFTING')).toBe('accent');
      expect(component.getStatusColor('ACTIVE')).toBe('primary');
      expect(component.getStatusColor('FINISHED')).toBe('warn');
      expect(component.getStatusColor('CANCELLED')).toBe('warn');
    });

    it('should return correct status label', () => {
      expect(component.getStatusLabel('CREATING')).toBe('En création');
      expect(component.getStatusLabel('DRAFTING')).toBe('Draft en cours');
      expect(component.getStatusLabel('ACTIVE')).toBe('Active');
      expect(component.getStatusLabel('FINISHED')).toBe('Terminée');
      expect(component.getStatusLabel('CANCELLED')).toBe('Annulée');
    });

    it('should expose game count and hasGames', () => {
      component.userGames = mockGames;
      expect(component.getGameCount()).toBe(2);
      expect(component.hasGames()).toBeTrue();
    });

    it('should track games by id', () => {
      const result = component.trackByGameId(0, mockGames[0]);
      expect(result).toBe(mockGames[0].id);
    });
  });

  describe('reloadUserGames', () => {
    it('should refresh games via the store', () => {
      component.reloadUserGames();

      expect(userGamesStore.refreshGames).toHaveBeenCalled();
    });
  });
});
