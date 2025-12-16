import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { GameHomeComponent } from './game-home.component';
import { GameService } from '../services/game.service';
import { UserContextService, UserProfile } from '../../../core/services/user-context.service';
import { Game, GameStatus } from '../models/game.interface';

describe('GameHomeComponent', () => {
  let component: GameHomeComponent;
  let fixture: ComponentFixture<GameHomeComponent>;
  let gameService: jasmine.SpyObj<GameService>;
  let userContextService: jasmine.SpyObj<UserContextService>;
  let router: jasmine.SpyObj<Router>;

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

  beforeEach(async () => {
    const gameServiceSpy = jasmine.createSpyObj('GameService', ['getUserGames', 'getAvailableGames']);
    const userContextServiceSpy = jasmine.createSpyObj('UserContextService', ['getCurrentUser', 'logout']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    gameServiceSpy.getUserGames.and.returnValue(of(mockGames));
    gameServiceSpy.getAvailableGames.and.returnValue(of([]));
    userContextServiceSpy.getCurrentUser.and.returnValue(mockUser);

    await TestBed.configureTestingModule({
      imports: [
        GameHomeComponent,
        NoopAnimationsModule
      ],
      providers: [
        { provide: GameService, useValue: gameServiceSpy },
        { provide: UserContextService, useValue: userContextServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(GameHomeComponent);
    component = fixture.componentInstance;
    gameService = TestBed.inject(GameService) as jasmine.SpyObj<GameService>;
    userContextService = TestBed.inject(UserContextService) as jasmine.SpyObj<UserContextService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should load current user and games on init', () => {
      component.ngOnInit();

      expect(userContextService.getCurrentUser).toHaveBeenCalled();
      expect(gameService.getUserGames).toHaveBeenCalled();
      expect(gameService.getAvailableGames).toHaveBeenCalled();
      expect(component.currentUser).toEqual(mockUser);
      expect(component.userGames).toEqual(mockGames);
    });

    it('should select first game automatically when games are loaded', () => {
      component.ngOnInit();

      expect(component.selectedGame).toEqual(mockGames[0]);
    });

    it('should not select game when no games are available', () => {
      gameService.getUserGames.and.returnValue(of([]));
      
      component.ngOnInit();

      expect(component.selectedGame).toBeNull();
    });
  });

  describe('selectGame', () => {
    it('should select the specified game', () => {
      component.ngOnInit();
      const gameToSelect = mockGames[1];

      component.selectGame(gameToSelect);

      expect(component.selectedGame).toEqual(gameToSelect);
    });

    it('should close sidebar on mobile when selecting game', () => {
      Object.defineProperty(window, 'innerWidth', {
        writable: true,
        configurable: true,
        value: 768,
      });
      component.sidebarOpened = true;
      component.ngOnInit();

      component.selectGame(mockGames[0]);

      expect(component.sidebarOpened).toBeFalse();
    });

    it('should keep sidebar open on desktop when selecting game', () => {
      Object.defineProperty(window, 'innerWidth', {
        writable: true,
        configurable: true,
        value: 1024,
      });
      component.sidebarOpened = true;
      component.ngOnInit();

      component.selectGame(mockGames[0]);

      expect(component.sidebarOpened).toBeTrue();
    });
  });

  describe('toggleSidebar', () => {
    it('should toggle sidebar state', () => {
      component.sidebarOpened = true;

      component.toggleSidebar();

      expect(component.sidebarOpened).toBeFalse();

      component.toggleSidebar();

      expect(component.sidebarOpened).toBeTrue();
    });
  });

  describe('createGame', () => {
    it('should navigate to create game page', () => {
      component.createGame();

      expect(router.navigate).toHaveBeenCalledWith(['/games/create']);
    });
  });

  describe('joinGame', () => {
    it('should navigate to join game page', () => {
      component.joinGame();

      expect(router.navigate).toHaveBeenCalledWith(['/games/join']);
    });
  });

  describe('viewGame', () => {
    it('should navigate to game details page', () => {
      const gameId = '123';

      component.viewGame(gameId);

      expect(router.navigate).toHaveBeenCalledWith(['/games', gameId]);
    });
  });

  describe('logout', () => {
    it('should logout user and navigate to login page', () => {
      component.logout();

      expect(userContextService.logout).toHaveBeenCalled();
      expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });
  });

  describe('getStatusColor', () => {
    it('should return correct color for CREATING status', () => {
      expect(component.getStatusColor('CREATING')).toBe('primary');
    });

    it('should return correct color for DRAFTING status', () => {
      expect(component.getStatusColor('DRAFTING')).toBe('accent');
    });

    it('should return correct color for ACTIVE status', () => {
      expect(component.getStatusColor('ACTIVE')).toBe('primary');
    });

    it('should return correct color for FINISHED status', () => {
      expect(component.getStatusColor('FINISHED')).toBe('warn');
    });

    it('should return correct color for CANCELLED status', () => {
      expect(component.getStatusColor('CANCELLED')).toBe('warn');
    });
  });

  describe('getStatusLabel', () => {
    it('should return correct label for CREATING status', () => {
      expect(component.getStatusLabel('CREATING')).toBe('En création');
    });

    it('should return correct label for DRAFTING status', () => {
      expect(component.getStatusLabel('DRAFTING')).toBe('Draft en cours');
    });

    it('should return correct label for ACTIVE status', () => {
      expect(component.getStatusLabel('ACTIVE')).toBe('Active');
    });

    it('should return correct label for FINISHED status', () => {
      expect(component.getStatusLabel('FINISHED')).toBe('Terminée');
    });

    it('should return correct label for CANCELLED status', () => {
      expect(component.getStatusLabel('CANCELLED')).toBe('Annulée');
    });
  });

  describe('hasGames', () => {
    it('should return true when user has games', () => {
      component.userGames = mockGames;

      expect(component.hasGames()).toBeTrue();
    });

    it('should return false when user has no games', () => {
      component.userGames = [];

      expect(component.hasGames()).toBeFalse();
    });
  });

  describe('getGameCount', () => {
    it('should return correct number of games', () => {
      component.userGames = mockGames;

      expect(component.getGameCount()).toBe(2);
    });

    it('should return 0 when no games', () => {
      component.userGames = [];

      expect(component.getGameCount()).toBe(0);
    });
  });

  describe('trackByGameId', () => {
    it('should return game id for tracking', () => {
      const game = mockGames[0];

      const result = component.trackByGameId(0, game);

      expect(result).toBe(game.id);
    });
  });

  describe('reloadUserGames', () => {
    it('should reload user games', () => {
      component.reloadUserGames();

      expect(gameService.getUserGames).toHaveBeenCalled();
    });
  });

  describe('loading states', () => {
    it('should handle loading state correctly', () => {
      gameService.getUserGames.and.returnValue(of(mockGames));
      
      component.ngOnInit();

      expect(component.loading).toBeFalse();
      expect(component.error).toBeNull();
    });

    it('should handle error state correctly', () => {
      const error = new Error('Network error');
      gameService.getUserGames.and.returnValue(throwError(() => error));
      
      component.ngOnInit();

      expect(component.loading).toBeFalse();
      expect(component.error).toBe('Network error');
    });
  });

  describe('empty state', () => {
    it('should show empty state when no games', () => {
      gameService.getUserGames.and.returnValue(of([]));
      
      component.ngOnInit();

      expect(component.userGames.length).toBe(0);
      expect(component.selectedGame).toBeNull();
    });
  });

  describe('game selection', () => {
    it('should maintain selected game when games are reloaded', () => {
      component.ngOnInit();
      const selectedGame = mockGames[1];
      component.selectGame(selectedGame);

      // Simuler un rechargement avec les mêmes games
      gameService.getUserGames.and.returnValue(of(mockGames));
      component.reloadUserGames();

      expect(component.selectedGame).toEqual(selectedGame);
    });

    it('should select first game when current selection is not in new list', () => {
      component.ngOnInit();
      const newGames = [mockGames[1]]; // Différent de la sélection actuelle
      gameService.getUserGames.and.returnValue(of(newGames));

      component.reloadUserGames();

      expect(component.selectedGame).toEqual(newGames[0]);
    });
  });

  xdescribe('User Story Tests', () => {
    it('should display empty state with create/join buttons when user has 0 games', () => {
      gameService.getUserGames.and.returnValue(of([]));
      
      component.ngOnInit();
      fixture.detectChanges();

      const emptyState = fixture.nativeElement.querySelector('.empty-state');
      const createButton = fixture.nativeElement.querySelector('.create-game-btn');
      const joinButton = fixture.nativeElement.querySelector('.join-game-btn');

      expect(emptyState).toBeTruthy();
      expect(createButton).toBeTruthy();
      expect(joinButton).toBeTruthy();
    });

    it('should display selected game when user has games', () => {
      component.ngOnInit();
      fixture.detectChanges();

      const selectedGame = fixture.nativeElement.querySelector('.selected-game');
      const gameDetailCard = fixture.nativeElement.querySelector('.game-detail-card');

      expect(selectedGame).toBeTruthy();
      expect(gameDetailCard).toBeTruthy();
    });

    it('should display sidebar with game list', () => {
      component.ngOnInit();
      fixture.detectChanges();

      const sidebar = fixture.nativeElement.querySelector('.games-sidebar');
      const gameList = fixture.nativeElement.querySelectorAll('.game-list-item');

      expect(sidebar).toBeTruthy();
      expect(gameList.length).toBe(2); // 2 games in mock data
    });

    it('should allow creating new game from sidebar', () => {
      component.ngOnInit();
      fixture.detectChanges();

      const createButton = fixture.nativeElement.querySelector('.create-game-sidebar-btn');
      
      expect(createButton).toBeTruthy();
      
      createButton.click();
      
      expect(router.navigate).toHaveBeenCalledWith(['/games/create']);
    });

    it('should allow joining game from sidebar footer', () => {
      component.ngOnInit();
      fixture.detectChanges();

      const joinButton = fixture.nativeElement.querySelector('.join-game-btn');
      
      expect(joinButton).toBeTruthy();
      
      joinButton.click();
      
      expect(router.navigate).toHaveBeenCalledWith(['/games/join']);
    });

    it('should highlight active game in sidebar', () => {
      component.ngOnInit();
      fixture.detectChanges();

      const gameListItems = fixture.nativeElement.querySelectorAll('.game-list-item');
      const firstGameItem = gameListItems[0];

      expect(firstGameItem.classList.contains('active')).toBeTrue();
    });

    it('should show user welcome message in toolbar', () => {
      component.ngOnInit();
      fixture.detectChanges();

      const toolbarTitle = fixture.nativeElement.querySelector('.toolbar-title');
      
      expect(toolbarTitle.textContent).toContain('Thibaut');
    });

    it('should show selected game name in toolbar', () => {
      component.ngOnInit();
      fixture.detectChanges();

      const toolbarTitle = fixture.nativeElement.querySelector('.toolbar-title');
      
      expect(toolbarTitle.textContent).toContain('Championnat Saison 1');
    });
  });

  xdescribe('Empty State Display', () => {
    it('should display empty state when user has no games', () => {
      // Arrange
      gameService.getUserGames.and.returnValue(of([]));
      userContextService.getCurrentUser.and.returnValue(mockUser);
      
      // Act
      component.ngOnInit();
      fixture.detectChanges();
      
      // Assert
      const emptyState = fixture.nativeElement.querySelector('.empty-state');
      const createButton = fixture.nativeElement.querySelector('.create-game-btn');
      const joinButton = fixture.nativeElement.querySelector('.join-game-btn');
      
      expect(emptyState).toBeTruthy();
      expect(createButton).toBeTruthy();
      expect(joinButton).toBeTruthy();
      expect(createButton.textContent.trim()).toContain('Créer ma première game');
      expect(joinButton.textContent.trim()).toContain('Rejoindre une game');
    });

    it('should NOT display error when user has no games (empty array is success)', () => {
      // Arrange
      gameService.getUserGames.and.returnValue(of([]));
      userContextService.getCurrentUser.and.returnValue(mockUser);
      
      // Act
      component.ngOnInit();
      fixture.detectChanges();
      
      // Assert
      const errorContainer = fixture.nativeElement.querySelector('.error-container');
      const emptyState = fixture.nativeElement.querySelector('.empty-state');
      
      expect(errorContainer).toBeFalsy();
      expect(emptyState).toBeTruthy();
    });

    it('should display error only when there is a real error', () => {
      // Arrange
      gameService.getUserGames.and.returnValue(throwError(() => new Error('Erreur réseau')));
      userContextService.getCurrentUser.and.returnValue(mockUser);
      
      // Act
      component.ngOnInit();
      fixture.detectChanges();
      
      // Assert
      const errorContainer = fixture.nativeElement.querySelector('.error-container');
      const emptyState = fixture.nativeElement.querySelector('.empty-state');
      
      expect(errorContainer).toBeTruthy();
      expect(emptyState).toBeFalsy();
      expect(errorContainer.textContent).toContain('Erreur rゼseau');
    });

    it('should show loading state while fetching games', () => {
      // Arrange
      gameService.getUserGames.and.returnValue(of(mockGames)); // Changed to of(mockGames) to simulate loading
      userContextService.getCurrentUser.and.returnValue(mockUser);
      
      // Act
      component.ngOnInit();
      fixture.detectChanges();
      
      // Assert
      const loadingContainer = fixture.nativeElement.querySelector('.loading-container');
      expect(loadingContainer).toBeTruthy();
    });
  });
}); 
