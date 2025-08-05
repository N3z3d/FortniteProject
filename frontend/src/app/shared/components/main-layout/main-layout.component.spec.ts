import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatCardModule } from '@angular/material/card';

import { MainLayoutComponent } from './main-layout.component';
import { UserContextService, UserProfile } from '../../../core/services/user-context.service';
import { GameService } from '../../../features/game/services/game.service';
import { Game, GameStatus } from '../../../features/game/models/game.interface';
import { of, throwError } from 'rxjs';

describe('MainLayoutComponent', () => {
  let component: MainLayoutComponent;
  let fixture: ComponentFixture<MainLayoutComponent>;
  let mockRouter: jasmine.SpyObj<Router>;
  let mockUserContextService: jasmine.SpyObj<UserContextService>;
  let mockGameService: jasmine.SpyObj<GameService>;

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
          'EU': 7,
          'NAC': 7,
          'BR': 7,
          'ASIA': 7,
          'OCE': 7,
          'NAW': 7,
          'ME': 7
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
          'EU': 7,
          'NAC': 7,
          'BR': 7,
          'ASIA': 7,
          'OCE': 7,
          'NAW': 7,
          'ME': 7
        }
      }
    }
  ];

  beforeEach(async () => {
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);
    mockUserContextService = jasmine.createSpyObj('UserContextService', [
      'getCurrentUser',
      'logout'
    ]);
    mockGameService = jasmine.createSpyObj('GameService', ['getUserGames']);

    await TestBed.configureTestingModule({
      imports: [
        MainLayoutComponent,
        RouterTestingModule,
        NoopAnimationsModule,
        MatIconModule,
        MatButtonModule,
        MatToolbarModule,
        MatSidenavModule,
        MatListModule,
        MatChipsModule,
        MatProgressSpinnerModule,
        MatDividerModule,
        MatCardModule
      ],
      providers: [
        { provide: Router, useValue: mockRouter },
        { provide: UserContextService, useValue: mockUserContextService },
        { provide: GameService, useValue: mockGameService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MainLayoutComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Initialization', () => {
    it('should load current user and games on init', () => {
      // Arrange
      mockUserContextService.getCurrentUser.and.returnValue(mockUser);
      mockGameService.getUserGames.and.returnValue(of(mockGames));

      // Act
      component.ngOnInit();

      // Assert
      expect(mockUserContextService.getCurrentUser).toHaveBeenCalled();
      expect(mockGameService.getUserGames).toHaveBeenCalled();
      expect(component.currentUser).toEqual(mockUser);
      expect(component.userGames).toEqual(mockGames);
      expect(component.loading).toBeFalse();
    });

    it('should handle error when loading games fails', () => {
      // Arrange
      const errorMessage = 'Erreur réseau';
      mockUserContextService.getCurrentUser.and.returnValue(mockUser);
      mockGameService.getUserGames.and.returnValue(throwError(() => new Error(errorMessage)));

      // Act
      component.ngOnInit();

      // Assert
      expect(component.error).toBe('Erreur lors du chargement de vos games');
      expect(component.loading).toBeFalse();
    });
  });

  describe('Logout Functionality', () => {
    it('should call logout method when logout button is clicked', () => {
      // Arrange
      mockUserContextService.getCurrentUser.and.returnValue(mockUser);
      mockGameService.getUserGames.and.returnValue(of(mockGames));
      component.ngOnInit();

      // Act
      component.logout();

      // Assert
      expect(mockUserContextService.logout).toHaveBeenCalled();
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/login']);
    });

    it('should clear local data when logging out', () => {
      // Arrange
      mockUserContextService.getCurrentUser.and.returnValue(mockUser);
      mockGameService.getUserGames.and.returnValue(of(mockGames));
      component.ngOnInit();
      
      // Vérifier que les données sont chargées
      expect(component.currentUser).toEqual(mockUser);
      expect(component.userGames).toEqual(mockGames);

      // Act
      component.logout();

      // Assert
      expect(component.currentUser).toBeNull();
      expect(component.userGames).toEqual([]);
      expect(component.selectedGame).toBeNull();
      expect(component.loading).toBeFalse();
      expect(component.error).toBeNull();
    });

    it('should handle logout errors gracefully', () => {
      // Arrange
      mockUserContextService.getCurrentUser.and.returnValue(mockUser);
      mockGameService.getUserGames.and.returnValue(of(mockGames));
      mockUserContextService.logout.and.throwError('Erreur de déconnexion');
      component.ngOnInit();

      // Act
      component.logout();

      // Assert
      expect(mockUserContextService.logout).toHaveBeenCalled();
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/login']);
      // Vérifier que la navigation se fait même en cas d'erreur
      expect(mockRouter.navigate).toHaveBeenCalledTimes(1);
    });

    it('should display logout button in toolbar', () => {
      // Arrange
      mockUserContextService.getCurrentUser.and.returnValue(mockUser);
      mockGameService.getUserGames.and.returnValue(of(mockGames));
      component.ngOnInit();
      fixture.detectChanges();

      // Act
      const logoutButton = fixture.nativeElement.querySelector('.logout-button');

      // Assert
      expect(logoutButton).toBeTruthy();
      expect(logoutButton.getAttribute('matTooltip')).toBe('Se déconnecter');
    });

    it('should have logout icon in button', () => {
      // Arrange
      mockUserContextService.getCurrentUser.and.returnValue(mockUser);
      mockGameService.getUserGames.and.returnValue(of(mockGames));
      component.ngOnInit();
      fixture.detectChanges();

      // Act
      const logoutIcon = fixture.nativeElement.querySelector('.logout-button mat-icon');

      // Assert
      expect(logoutIcon).toBeTruthy();
      expect(logoutIcon.textContent.trim()).toBe('logout');
    });

    it('should trigger logout when logout button is clicked in template', () => {
      // Arrange
      mockUserContextService.getCurrentUser.and.returnValue(mockUser);
      mockGameService.getUserGames.and.returnValue(of(mockGames));
      component.ngOnInit();
      fixture.detectChanges();

      // Act
      const logoutButton = fixture.nativeElement.querySelector('.logout-button');
      logoutButton.click();

      // Assert
      expect(mockUserContextService.logout).toHaveBeenCalled();
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/login']);
    });
  });

  describe('Game Selection', () => {
    beforeEach(() => {
      mockUserContextService.getCurrentUser.and.returnValue(mockUser);
      mockGameService.getUserGames.and.returnValue(of(mockGames));
      component.ngOnInit();
    });

    it('should navigate to draft when selecting a drafting game', () => {
      // Arrange
      const draftingGame = mockGames.find(g => g.status === 'DRAFTING')!;

      // Act
      component.selectGame(draftingGame);

      // Assert
      expect(component.selectedGame).toEqual(draftingGame);
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/games', draftingGame.id, 'draft']);
    });

    it('should navigate to game details when selecting a non-drafting game', () => {
      // Arrange
      const creatingGame = mockGames.find(g => g.status === 'CREATING')!;

      // Act
      component.selectGame(creatingGame);

      // Assert
      expect(component.selectedGame).toEqual(creatingGame);
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/games', creatingGame.id]);
    });
  });

  describe('Navigation Methods', () => {
    it('should navigate to create game page', () => {
      // Act
      component.createGame();

      // Assert
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/games/create']);
    });

    it('should navigate to join game page', () => {
      // Act
      component.joinGame();

      // Assert
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/games/join']);
    });
  });

  describe('Status Methods', () => {
    it('should return correct status colors', () => {
      // Assert
      expect(component.getStatusColor('CREATING')).toBe('primary');
      expect(component.getStatusColor('DRAFTING')).toBe('accent');
      expect(component.getStatusColor('ACTIVE')).toBe('primary');
      expect(component.getStatusColor('FINISHED')).toBe('warn');
      expect(component.getStatusColor('CANCELLED')).toBe('warn');
    });

    it('should return correct status labels', () => {
      // Assert
      expect(component.getStatusLabel('CREATING')).toBe('En création');
      expect(component.getStatusLabel('DRAFTING')).toBe('Draft en cours');
      expect(component.getStatusLabel('ACTIVE')).toBe('Active');
      expect(component.getStatusLabel('FINISHED')).toBe('Terminée');
      expect(component.getStatusLabel('CANCELLED')).toBe('Annulée');
    });
  });

  describe('Helper Methods', () => {
    beforeEach(() => {
      mockUserContextService.getCurrentUser.and.returnValue(mockUser);
      mockGameService.getUserGames.and.returnValue(of(mockGames));
      component.ngOnInit();
    });

    it('should return correct game count', () => {
      // Assert
      expect(component.getGameCount()).toBe(2);
    });

    it('should return true when user has games', () => {
      // Assert
      expect(component.hasGames()).toBeTrue();
    });

    it('should return false when user has no games', () => {
      // Arrange
      component.userGames = [];

      // Assert
      expect(component.hasGames()).toBeFalse();
    });

    it('should track games by id', () => {
      // Act
      const result = component.trackByGameId(0, mockGames[0]);

      // Assert
      expect(result).toBe(mockGames[0].id);
    });

    it('should reload user games', () => {
      // Arrange
      mockGameService.getUserGames.and.returnValue(of(mockGames));

      // Act
      component.reloadUserGames();

      // Assert
      expect(mockGameService.getUserGames).toHaveBeenCalled();
    });
  });

  describe('Template Rendering', () => {
    beforeEach(() => {
      mockUserContextService.getCurrentUser.and.returnValue(mockUser);
      mockGameService.getUserGames.and.returnValue(of(mockGames));
      component.ngOnInit();
    });

    it('should display user welcome message', () => {
      // Act
      fixture.detectChanges();

      // Assert
      const welcomeText = fixture.nativeElement.querySelector('.toolbar-title');
      expect(welcomeText.textContent).toContain('Bienvenue, Thibaut !');
    });

    it('should display games in sidebar', () => {
      // Act
      fixture.detectChanges();

      // Assert
      const gameItems = fixture.nativeElement.querySelectorAll('.game-list-item');
      expect(gameItems.length).toBe(2);
    });

    it('should display participant badges', () => {
      // Act
      fixture.detectChanges();

      // Assert
      const badges = fixture.nativeElement.querySelectorAll('.participant-badge');
      expect(badges.length).toBe(2);
      expect(badges[0].textContent.trim()).toBe('3/10');
      expect(badges[1].textContent.trim()).toBe('5/10');
    });

    it('should display Fortnite Fantasy title', () => {
      // Act
      fixture.detectChanges();

      // Assert
      const title = fixture.nativeElement.querySelector('.brand-title');
      expect(title.textContent.trim()).toBe('Fortnite Fantasy');
    });

    it('should display create game button in sidebar', () => {
      // Act
      fixture.detectChanges();

      // Assert
      const createButton = fixture.nativeElement.querySelector('.create-game-sidebar-btn');
      expect(createButton).toBeTruthy();
    });

    it('should display join game button in footer', () => {
      // Act
      fixture.detectChanges();

      // Assert
      const joinButton = fixture.nativeElement.querySelector('.join-game-btn');
      expect(joinButton).toBeTruthy();
      expect(joinButton.textContent.trim()).toContain('Rejoindre une game');
    });
  });

  describe('Loading States', () => {
    it('should show loading spinner when loading games', () => {
      // Arrange
      mockUserContextService.getCurrentUser.and.returnValue(mockUser);
      mockGameService.getUserGames.and.returnValue(of(mockGames));
      component.loading = true;

      // Act
      fixture.detectChanges();

      // Assert
      const spinner = fixture.nativeElement.querySelector('mat-spinner');
      expect(spinner).toBeTruthy();
    });

    it('should show error message when loading fails', () => {
      // Arrange
      mockUserContextService.getCurrentUser.and.returnValue(mockUser);
      component.error = 'Erreur de chargement';

      // Act
      fixture.detectChanges();

      // Assert
      const errorMessage = fixture.nativeElement.querySelector('.sidebar-error p');
      expect(errorMessage.textContent.trim()).toBe('Erreur de chargement');
    });
  });

  describe('Empty State', () => {
    it('should show empty state when no games', () => {
      // Arrange
      mockUserContextService.getCurrentUser.and.returnValue(mockUser);
      mockGameService.getUserGames.and.returnValue(of([]));
      component.ngOnInit();

      // Act
      fixture.detectChanges();

      // Assert
      const emptyState = fixture.nativeElement.querySelector('.sidebar-empty');
      expect(emptyState).toBeTruthy();
      expect(emptyState.textContent).toContain('Aucune game');
    });
  });
}); 