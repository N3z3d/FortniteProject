import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { GameListComponent } from './game-list.component';
import { GameService } from '../services/game.service';
import { Game, GameStatus } from '../models/game.interface';

describe('GameListComponent', () => {
  let component: GameListComponent;
  let fixture: ComponentFixture<GameListComponent>;
  let gameService: jasmine.SpyObj<GameService>;
  let router: jasmine.SpyObj<Router>;

  // Données de test
  const mockGames: Game[] = [
    {
      id: 'game-1',
      name: 'Test Game 1',
      creatorName: 'User1',
      maxParticipants: 4,
      status: 'CREATING',
      createdAt: '2025-01-20T10:00:00Z',
      participantCount: 2,
      canJoin: true,
      invitationCode: 'ABC123',
      draftRules: {
        maxPlayersPerTeam: 7,
        timeLimitPerPick: 300,
        autoPickEnabled: true,
        regionQuotas: { 'EU': 3, 'NAW': 2, 'BR': 2 }
      },
      regionRules: { 'EU': 3, 'NAW': 2, 'BR': 2 }
    },
    {
      id: 'game-2',
      name: 'Test Game 2',
      creatorName: 'User2',
      maxParticipants: 6,
      status: 'DRAFTING',
      createdAt: '2025-01-20T11:00:00Z',
      participantCount: 4,
      canJoin: false,
      invitationCode: 'DEF456',
      draftRules: {
        maxPlayersPerTeam: 5,
        timeLimitPerPick: 240,
        autoPickEnabled: false,
        regionQuotas: { 'EU': 2, 'NAC': 2, 'BR': 1 }
      },
      regionRules: { 'EU': 2, 'NAC': 2, 'BR': 1 }
    }
  ];

  beforeEach(async () => {
    const gameServiceSpy = jasmine.createSpyObj('GameService', ['getUserGames', 'getAvailableGames']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [
        GameListComponent,
        RouterTestingModule,
        NoopAnimationsModule,
        HttpClientTestingModule,
        MatSnackBarModule,
        MatDialogModule
      ],
      providers: [
        { provide: GameService, useValue: gameServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(GameListComponent);
    component = fixture.componentInstance;
    gameService = TestBed.inject(GameService) as jasmine.SpyObj<GameService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should load user games on initialization', () => {
      gameService.getUserGames.and.returnValue(of(mockGames));

      component.ngOnInit();

      expect(gameService.getUserGames).toHaveBeenCalled();
      expect(component.games).toEqual(mockGames);
      expect(component.loading).toBeFalse();
    });

    it('should handle loading state correctly', () => {
      gameService.getUserGames.and.returnValue(of(mockGames));

      component.ngOnInit();

      expect(component.loading).toBeFalse();
    });

    it('should handle error when loading games fails', () => {
      const errorMessage = 'Erreur réseau';
      gameService.getUserGames.and.returnValue(throwError(() => new Error(errorMessage)));

      component.ngOnInit();

      expect(component.error).toBeTruthy();
      expect(component.loading).toBeFalse();
    });
  });

  describe('loadGames', () => {
    it('should load games successfully', () => {
      gameService.getUserGames.and.returnValue(of(mockGames));

      component.loadGames();

      expect(gameService.getUserGames).toHaveBeenCalled();
      expect(component.games).toEqual(mockGames);
      expect(component.loading).toBeFalse();
      expect(component.error).toBeNull();
    });

    it('should handle empty games list', () => {
      gameService.getUserGames.and.returnValue(of([]));

      component.loadGames();

      expect(component.games).toEqual([]);
      expect(component.loading).toBeFalse();
    });

    it('should handle network errors', () => {
      gameService.getUserGames.and.returnValue(throwError(() => new Error('Network error')));

      component.loadGames();

      expect(component.error).toBeTruthy();
      expect(component.loading).toBeFalse();
    });
  });

  describe('loadAvailableGames', () => {
    it('should load available games successfully', () => {
      gameService.getAvailableGames.and.returnValue(of(mockGames));

      component.loadAvailableGames();

      expect(gameService.getAvailableGames).toHaveBeenCalled();
      expect(component.availableGames).toEqual(mockGames);
    });

    it('should handle error when loading available games fails', () => {
      gameService.getAvailableGames.and.returnValue(throwError(() => new Error('Error')));

      component.loadAvailableGames();

      expect(component.error).toBeTruthy();
    });
  });

  describe('joinGame', () => {
    it('should join game successfully', () => {
      const gameId = 'game-1';
      gameService.joinGame.and.returnValue(of(true));

      component.joinGame(gameId);

      expect(gameService.joinGame).toHaveBeenCalledWith(gameId);
    });

    it('should handle error when joining game fails', () => {
      const gameId = 'game-1';
      gameService.joinGame.and.returnValue(throwError(() => new Error('Join failed')));

      component.joinGame(gameId);

      expect(component.error).toBeTruthy();
    });
  });

  describe('startDraft', () => {
    it('should start draft successfully', () => {
      const gameId = 'game-1';
      gameService.startDraft.and.returnValue(of(true));

      component.startDraft(gameId);

      expect(gameService.startDraft).toHaveBeenCalledWith(gameId);
    });

    it('should handle error when starting draft fails', () => {
      const gameId = 'game-1';
      gameService.startDraft.and.returnValue(throwError(() => new Error('Start draft failed')));

      component.startDraft(gameId);

      expect(component.error).toBeTruthy();
    });
  });

  describe('getStatusColor', () => {
    it('should return correct color for CREATING status', () => {
      const color = component.getStatusColor('CREATING');
      expect(color).toBe('primary');
    });

    it('should return correct color for DRAFTING status', () => {
      const color = component.getStatusColor('DRAFTING');
      expect(color).toBe('accent');
    });

    it('should return correct color for ACTIVE status', () => {
      const color = component.getStatusColor('ACTIVE');
      expect(color).toBe('primary');
    });

    it('should return correct color for FINISHED status', () => {
      const color = component.getStatusColor('FINISHED');
      expect(color).toBe('accent');
    });

    it('should return correct color for CANCELLED status', () => {
      const color = component.getStatusColor('CANCELLED');
      expect(color).toBe('warn');
    });
  });

  describe('getStatusLabel', () => {
    it('should return correct label for CREATING status', () => {
      const label = component.getStatusLabel('CREATING');
      expect(label).toBe('En création');
    });

    it('should return correct label for DRAFTING status', () => {
      const label = component.getStatusLabel('DRAFTING');
      expect(label).toBe('Draft en cours');
    });

    it('should return correct label for ACTIVE status', () => {
      const label = component.getStatusLabel('ACTIVE');
      expect(label).toBe('En cours');
    });

    it('should return correct label for FINISHED status', () => {
      const label = component.getStatusLabel('FINISHED');
      expect(label).toBe('Terminé');
    });

    it('should return correct label for CANCELLED status', () => {
      const label = component.getStatusLabel('CANCELLED');
      expect(label).toBe('Annulé');
    });
  });



  describe('deleteGame', () => {
    it('should delete game successfully', () => {
      const gameId = 'game-1';
      spyOn(component, 'loadGames');
      gameService.deleteGame.and.returnValue(of(true));

      component.deleteGame(gameId);

      expect(gameService.deleteGame).toHaveBeenCalledWith(gameId);
      expect(component.loadGames).toHaveBeenCalled();
    });

    it('should handle error when deleting game fails', () => {
      const gameId = 'game-1';
      gameService.deleteGame.and.returnValue(throwError(() => new Error('Delete failed')));

      component.deleteGame(gameId);

      expect(component.error).toBeTruthy();
    });
  });

  describe('Template rendering', () => {
    it('should display games when loaded successfully', () => {
      gameService.getUserGames.and.returnValue(of(mockGames));
      component.ngOnInit();
      fixture.detectChanges();

      const gameElements = fixture.nativeElement.querySelectorAll('.game-card');
      expect(gameElements.length).toBe(2);
    });

    it('should display loading spinner when loading', () => {
      component.loading = true;
      fixture.detectChanges();

      const spinner = fixture.nativeElement.querySelector('mat-spinner');
      expect(spinner).toBeTruthy();
    });

    it('should display error message when error occurs', () => {
      component.error = 'Test error message';
      fixture.detectChanges();

      const errorElement = fixture.nativeElement.querySelector('.error-message');
      expect(errorElement.textContent).toContain('Test error message');
    });

    it('should display empty state when no games', () => {
      component.games = [];
      component.loading = false;
      fixture.detectChanges();

      const emptyState = fixture.nativeElement.querySelector('.empty-state');
      expect(emptyState).toBeTruthy();
    });
  });
}); 