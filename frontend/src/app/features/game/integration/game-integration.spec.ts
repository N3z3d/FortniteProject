import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';

import { GameListComponent } from '../game-list/game-list.component';
import { CreateGameComponent } from '../create-game/create-game.component';
import { JoinGameComponent } from '../join-game/join-game.component';
import { GameDetailComponent } from '../game-detail/game-detail.component';
import { GameService } from '../services/game.service';
import { Game, GameStatus } from '../models/game.interface';

describe('Game Module Integration Tests', () => {
  let gameService: GameService;
  let createGameFixture: ComponentFixture<CreateGameComponent>;
  let gameListFixture: ComponentFixture<GameListComponent>;
  let joinGameFixture: ComponentFixture<JoinGameComponent>;
  let gameDetailFixture: ComponentFixture<GameDetailComponent>;

  const mockGames: Game[] = [
    {
      id: '1',
      name: 'Test Game 1',
      creatorName: 'User1',
      maxParticipants: 8,
      status: 'CREATING' as const,
      createdAt: '2025-01-15T10:30:00',
      participantCount: 2,
      canJoin: true
    },
    {
      id: '2',
      name: 'Test Game 2',
      creatorName: 'User2',
      maxParticipants: 6,
      status: 'DRAFTING' as const,
      createdAt: '2025-01-15T11:30:00',
      participantCount: 4,
      canJoin: false
    }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        GameListComponent,
        CreateGameComponent,
        JoinGameComponent,
        GameDetailComponent,
        RouterTestingModule,
        NoopAnimationsModule,
        HttpClientTestingModule,
        MatSnackBarModule
      ],
      providers: [
        GameService,
        {
          provide: ActivatedRoute,
          useValue: {
            params: of({ id: '1' })
          }
        }
      ]
    }).compileComponents();

    gameService = TestBed.inject(GameService);
    createGameFixture = TestBed.createComponent(CreateGameComponent);
    gameListFixture = TestBed.createComponent(GameListComponent);
    joinGameFixture = TestBed.createComponent(JoinGameComponent);
    gameDetailFixture = TestBed.createComponent(GameDetailComponent);
  });

  describe('Game List Integration', () => {
    it('should load games on init', () => {
      spyOn(gameService, 'getUserGames').and.returnValue(of(mockGames));
      spyOn(gameService, 'getAvailableGames').and.returnValue(of(mockGames));

      gameListFixture.detectChanges();

      expect(gameService.getUserGames).toHaveBeenCalled();
      expect(gameService.getAvailableGames).toHaveBeenCalled();
      expect(gameListFixture.componentInstance.games).toEqual(mockGames);
      expect(gameListFixture.componentInstance.availableGames).toEqual(mockGames);
    });

    it('should handle join game action', () => {
      spyOn(gameService, 'getUserGames').and.returnValue(of(mockGames));
      spyOn(gameService, 'getAvailableGames').and.returnValue(of(mockGames));
      spyOn(gameService, 'joinGame').and.returnValue(of(true));

      gameListFixture.detectChanges();
      const component = gameListFixture.componentInstance;

      component.joinGame('game-1');
      expect(gameService.joinGame).toHaveBeenCalledWith('game-1');
    });

    it('should handle delete game action', () => {
      spyOn(gameService, 'getUserGames').and.returnValue(of(mockGames));
      spyOn(gameService, 'getAvailableGames').and.returnValue(of(mockGames));
      spyOn(gameService, 'deleteGame').and.returnValue(of(true));
      spyOn(window, 'confirm').and.returnValue(true);

      gameListFixture.detectChanges();
      const component = gameListFixture.componentInstance;

      component.deleteGame('game-1');
      expect(gameService.deleteGame).toHaveBeenCalledWith('game-1');
    });

    it('should handle start draft action', () => {
      spyOn(gameService, 'getUserGames').and.returnValue(of(mockGames));
      spyOn(gameService, 'getAvailableGames').and.returnValue(of(mockGames));
      spyOn(gameService, 'startDraft').and.returnValue(of(true));

      gameListFixture.detectChanges();
      const component = gameListFixture.componentInstance;

      component.startDraft('game-1');
      expect(gameService.startDraft).toHaveBeenCalledWith('game-1');
    });
  });

  describe('Create Game Integration', () => {
    it('should create game and navigate back', () => {
      spyOn(gameService, 'createGame').and.returnValue(of(mockGames[0]));
      spyOn(createGameFixture.componentInstance['router'], 'navigate');

      createGameFixture.detectChanges();
      const component = createGameFixture.componentInstance;

      // Set form values
      component.gameForm.patchValue({
        name: 'New Test Game',
        maxParticipants: 8
      });

      // Submit form
      component.onSubmit();
      expect(gameService.createGame).toHaveBeenCalled();
      expect(component['router'].navigate).toHaveBeenCalledWith(['/games']);
    });

    it('should validate form correctly', () => {
      createGameFixture.detectChanges();
      const component = createGameFixture.componentInstance;

      // Test empty form
      expect(component.gameForm.valid).toBeFalse();

      // Test invalid name
      component.gameForm.patchValue({
        name: 'A', // Too short
        maxParticipants: 8
      });
      expect(component.gameForm.get('name')?.valid).toBeFalse();

      // Test invalid participants
      component.gameForm.patchValue({
        name: 'Valid Name',
        maxParticipants: 1 // Too few
      });
      expect(component.gameForm.get('maxParticipants')?.valid).toBeFalse();

      // Test valid form
      component.gameForm.patchValue({
        name: 'Valid Game Name',
        maxParticipants: 8
      });
      expect(component.gameForm.valid).toBeTrue();
    });

    it('should manage region rules correctly', () => {
      createGameFixture.detectChanges();
      const component = createGameFixture.componentInstance;

      // Add region rule
      component.addRegionRule('EU', 2);
      expect(component.getRegionRules()['EU']).toBe(2);

      // Add another region rule
      component.addRegionRule('NAC', 1);
      expect(component.getRegionRules()['NAC']).toBe(1);

      // Remove region rule
      component.removeRegionRule('EU');
      expect(component.getRegionRules()['EU']).toBeUndefined();
    });
  });

  describe('Join Game Integration', () => {
    it('should load available games and allow joining', () => {
      spyOn(gameService, 'getAvailableGames').and.returnValue(of(mockGames));
      spyOn(gameService, 'joinGame').and.returnValue(of(true));

      joinGameFixture.detectChanges();
      const component = joinGameFixture.componentInstance;

      expect(gameService.getAvailableGames).toHaveBeenCalled();
      expect(component.games).toEqual(mockGames);

      // Test joining a game
      component.joinGame('1');
      expect(gameService.joinGame).toHaveBeenCalledWith('1');
    });

    it('should filter and search games correctly', () => {
      spyOn(gameService, 'getAvailableGames').and.returnValue(of(mockGames));
      joinGameFixture.detectChanges();

      const component = joinGameFixture.componentInstance;
      component.games = mockGames;

      // Test search
      component.searchTerm = 'Game 1';
      component.filterGames();
      expect(component.filteredGames.length).toBe(1);

      // Test status filter
      component.searchTerm = '';
      component.selectedStatus = 'CREATING';
      component.filterGames();
      expect(component.filteredGames.length).toBe(1);
      expect(component.filteredGames[0].status).toBe('CREATING');
    });

    it('should clear filters correctly', () => {
      spyOn(gameService, 'getAvailableGames').and.returnValue(of(mockGames));
      joinGameFixture.detectChanges();

      const component = joinGameFixture.componentInstance;
      component.games = mockGames;
      component.searchTerm = 'test';
      component.selectedStatus = 'CREATING';

      component.clearFilters();
      expect(component.searchTerm).toBe('');
      expect(component.selectedStatus).toBe('');
      expect(component.filteredGames.length).toBe(2);
    });
  });

  describe('Game Detail Integration', () => {
    it('should load game details and participants', () => {
      spyOn(gameService, 'getGameDetails').and.returnValue(of(mockGames[0]));
      spyOn(gameService, 'getGameParticipants').and.returnValue(of([
        { id: '1', username: 'User1', joinedAt: '2025-01-15T10:30:00', isCreator: true },
        { id: '2', username: 'User2', joinedAt: '2025-01-15T10:35:00', isCreator: false }
      ]));

      gameDetailFixture.detectChanges();
      const component = gameDetailFixture.componentInstance;

      expect(gameService.getGameDetails).toHaveBeenCalledWith('1');
      expect(gameService.getGameParticipants).toHaveBeenCalledWith('1');
      expect(component.game).toEqual(mockGames[0]);
    });

    it('should handle game actions correctly', () => {
      spyOn(gameService, 'getGameDetails').and.returnValue(of(mockGames[0]));
      spyOn(gameService, 'getGameParticipants').and.returnValue(of([]));
      spyOn(gameService, 'joinGame').and.returnValue(of(true));
      spyOn(gameService, 'startDraft').and.returnValue(of(true));
      spyOn(gameService, 'deleteGame').and.returnValue(of(true));

      gameDetailFixture.detectChanges();
      const component = gameDetailFixture.componentInstance;

      // Test join game
      component.joinGame();
      expect(gameService.joinGame).toHaveBeenCalledWith('1');

      // Test start draft
      component.startDraft();
      expect(gameService.startDraft).toHaveBeenCalledWith('1');

      // Test delete game
      component.deleteGame();
      expect(gameService.deleteGame).toHaveBeenCalledWith('1');
    });

    it('should calculate permissions correctly', () => {
      spyOn(gameService, 'getGameDetails').and.returnValue(of(mockGames[0]));
      spyOn(gameService, 'getGameParticipants').and.returnValue(of([]));

      gameDetailFixture.detectChanges();
      const component = gameDetailFixture.componentInstance;

      // Test can join
      expect(component.canJoinGame()).toBeTrue();

      // Test can start draft (needs 2+ participants)
      expect(component.canStartDraft()).toBeFalse();

      // Test can delete (only in CREATING status)
      expect(component.canDeleteGame()).toBeTrue();
    });

    it('should calculate percentages and colors correctly', () => {
      spyOn(gameService, 'getGameDetails').and.returnValue(of(mockGames[0]));
      spyOn(gameService, 'getGameParticipants').and.returnValue(of([]));

      gameDetailFixture.detectChanges();
      const component = gameDetailFixture.componentInstance;

      // Test participant percentage (2/8 = 25%)
      expect(component.getParticipantPercentage()).toBe(25);

      // Test participant color (25% = primary)
      expect(component.getParticipantColor()).toBe('primary');
    });
  });

  describe('Cross-Component Integration', () => {
    it('should maintain data consistency across components', () => {
      spyOn(gameService, 'getUserGames').and.returnValue(of(mockGames));
      spyOn(gameService, 'getAvailableGames').and.returnValue(of(mockGames));

      // Test that all components can access the same service data
      gameListFixture.detectChanges();
      createGameFixture.detectChanges();
      joinGameFixture.detectChanges();

      expect(gameListFixture.componentInstance.games).toEqual(mockGames);
      expect(gameListFixture.componentInstance.availableGames).toEqual(mockGames);
    });

    it('should handle service errors consistently', () => {
      const errorMessage = 'Network error';
      spyOn(gameService, 'getUserGames').and.returnValue(throwError(() => new Error(errorMessage)));

      gameListFixture.detectChanges();
      const component = gameListFixture.componentInstance;

      expect(component.error).toBe('Erreur lors du chargement des games');
    });
  });

  describe('Data Flow Integration', () => {
    it('should propagate data changes through services', () => {
      spyOn(gameService, 'getUserGames').and.returnValue(of(mockGames));
      spyOn(gameService, 'getAvailableGames').and.returnValue(of(mockGames));

      gameListFixture.detectChanges();
      const component = gameListFixture.componentInstance;

      // Verify that service calls update component state
      expect(component.games).toEqual(mockGames);
      expect(component.availableGames).toEqual(mockGames);
    });
  });
}); 