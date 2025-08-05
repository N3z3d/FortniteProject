import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { GameDetailComponent } from './game-detail.component';
import { GameService } from '../services/game.service';
import { Game, GameParticipant, GameStatus } from '../models/game.interface';

describe('GameDetailComponent - TDD Tests', () => {
  let component: GameDetailComponent;
  let fixture: ComponentFixture<GameDetailComponent>;
  let mockGameService: jasmine.SpyObj<GameService>;
  let mockRouter: jasmine.SpyObj<Router>;
  let mockSnackBar: jasmine.SpyObj<MatSnackBar>;
  let mockActivatedRoute: any;

  const mockGameFromApi = {
    id: 'test-game-id',
    name: 'Test Game',
    creatorId: 'creator-id',
    creatorUsername: 'TestCreator', // API uses creatorUsername
    maxParticipants: 10,
    currentParticipantCount: 3, // API uses currentParticipantCount
    status: 'ACTIVE' as GameStatus,
    createdAt: '2025-01-15T10:30:00Z',
    description: 'Test game description',
    invitationCode: 'TEST123',
    active: true,
    cancelled: false,
    full: false,
    finished: false,
    availableToJoin: true
  };

  const mockParticipants: GameParticipant[] = [
    {
      id: 'participant-1',
      username: 'TestCreator',
      joinedAt: '2025-01-15T10:30:00Z',
      isCreator: true,
      draftOrder: 1
    },
    {
      id: 'participant-2', 
      username: 'Player2',
      joinedAt: '2025-01-15T10:35:00Z',
      isCreator: false,
      draftOrder: 2
    }
  ];

  beforeEach(async () => {
    const gameServiceSpy = jasmine.createSpyObj('GameService', [
      'getGameById',
      'getGameParticipants', 
      'startDraft',
      'deleteGame',
      'joinGame'
    ]);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    mockActivatedRoute = {
      params: of({ id: 'test-game-id' })
    };

    await TestBed.configureTestingModule({
      imports: [GameDetailComponent],
      providers: [
        { provide: GameService, useValue: gameServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: ActivatedRoute, useValue: mockActivatedRoute }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(GameDetailComponent);
    component = fixture.componentInstance;
    mockGameService = TestBed.inject(GameService) as jasmine.SpyObj<GameService>;
    mockRouter = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    mockSnackBar = TestBed.inject(MatSnackBar) as jasmine.SpyObj<MatSnackBar>;
  });

  describe('Data Mapping from API', () => {
    it('should correctly map API response to Game interface', () => {
      // Arrange
      mockGameService.getGameById.and.returnValue(of(mockGameFromApi as any));
      mockGameService.getGameParticipants.and.returnValue(of(mockParticipants));

      // Act
      component.ngOnInit();

      // Assert
      expect(component.game).toBeTruthy();
      expect(component.game!.name).toBe('Test Game');
      expect(component.game!.creatorName).toBe('TestCreator'); // Should map from creatorUsername
      expect(component.game!.participantCount).toBe(3); // Should map from currentParticipantCount
      expect(component.game!.canJoin).toBe(true); // Should map from availableToJoin
    });

    it('should load participants from API', () => {
      // Arrange
      mockGameService.getGameById.and.returnValue(of(mockGameFromApi as any));
      mockGameService.getGameParticipants.and.returnValue(of(mockParticipants));

      // Act
      component.ngOnInit();

      // Assert
      expect(component.participants).toEqual(mockParticipants);
      expect(component.participants.length).toBe(2);
    });
  });

  describe('Participant Percentage Calculation', () => {
    beforeEach(() => {
      mockGameService.getGameById.and.returnValue(of(mockGameFromApi as any));
      mockGameService.getGameParticipants.and.returnValue(of(mockParticipants));
      component.ngOnInit();
    });

    it('should calculate correct participant percentage', () => {
      // Act
      const percentage = component.getParticipantPercentage();

      // Assert
      expect(percentage).toBe(30); // 3/10 * 100 = 30%
    });

    it('should return 0 when no game is loaded', () => {
      // Arrange
      component.game = null;

      // Act
      const percentage = component.getParticipantPercentage();

      // Assert
      expect(percentage).toBe(0);
    });

    it('should return 0 when maxParticipants is 0', () => {
      // Arrange
      component.game!.maxParticipants = 0;

      // Act
      const percentage = component.getParticipantPercentage();

      // Assert
      expect(percentage).toBe(0);
    });
  });

  describe('Date Handling', () => {
    beforeEach(() => {
      mockGameService.getGameById.and.returnValue(of(mockGameFromApi as any));
      mockGameService.getGameParticipants.and.returnValue(of(mockParticipants));
    });

    it('should handle valid ISO date strings', () => {
      // Arrange
      const validDate = '2025-01-15T10:30:00Z';
      
      // Act
      const result = component.getTimeAgo(validDate);

      // Assert
      expect(result).toBeTruthy();
      expect(result).not.toBe('Invalid Date');
    });

    it('should handle null or undefined dates gracefully', () => {
      // Act & Assert
      expect(() => component.getTimeAgo(null as any)).not.toThrow();
      expect(() => component.getTimeAgo(undefined as any)).not.toThrow();
      expect(() => component.getTimeAgo('')).not.toThrow();
    });

    it('should handle invalid date strings gracefully', () => {
      // Act
      const result = component.getTimeAgo('invalid-date-string');

      // Assert
      expect(result).toBe('Date invalide');
    });
  });

  describe('Game Actions', () => {
    beforeEach(() => {
      mockGameService.getGameById.and.returnValue(of(mockGameFromApi as any));
      mockGameService.getGameParticipants.and.returnValue(of(mockParticipants));
      component.ngOnInit();
    });

    it('should determine if user can start draft correctly', () => {
      // Arrange
      component.game!.status = 'CREATING';
      component.game!.participantCount = 3;

      // Act
      const canStart = component.canStartDraft();

      // Assert
      expect(canStart).toBe(true);
    });

    it('should prevent starting draft with insufficient participants', () => {
      // Arrange
      component.game!.status = 'CREATING';
      component.game!.participantCount = 1;

      // Act
      const canStart = component.canStartDraft();

      // Assert
      expect(canStart).toBe(false);
    });

    it('should determine if user can join game correctly', () => {
      // Arrange
      component.game!.canJoin = true;
      component.game!.participantCount = 3;
      component.game!.maxParticipants = 10;

      // Act
      const canJoin = component.canJoinGame();

      // Assert
      expect(canJoin).toBe(true);
    });
  });

  describe('Error Handling', () => {
    it('should handle game loading errors gracefully', () => {
      // Arrange
      const errorMessage = 'Game not found';
      mockGameService.getGameById.and.returnValue(throwError(() => new Error(errorMessage)));
      mockGameService.getGameParticipants.and.returnValue(of([]));

      // Act
      component.ngOnInit();

      // Assert
      expect(component.error).toBe('Erreur lors du chargement des détails de la game');
      expect(component.loading).toBe(false);
    });

    it('should handle participants loading errors gracefully', () => {
      // Arrange
      mockGameService.getGameById.and.returnValue(of(mockGameFromApi as any));
      mockGameService.getGameParticipants.and.returnValue(throwError(() => new Error('Participants not found')));

      // Act
      component.ngOnInit();

      // Assert
      expect(component.error).toBe('Erreur lors du chargement des participants');
    });
  });
});