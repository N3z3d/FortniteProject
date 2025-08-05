import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { JoinGameComponent } from './join-game.component';
import { GameService } from '../services/game.service';
import { Game } from '../models/game.interface';

describe('JoinGameComponent', () => {
  let component: JoinGameComponent;
  let fixture: ComponentFixture<JoinGameComponent>;
  let gameService: jasmine.SpyObj<GameService>;

  const mockGames: Game[] = [
    {
      id: '1',
      name: 'Test Game 1',
      creatorName: 'User1',
      maxParticipants: 5,
      status: 'CREATING' as const,
      createdAt: '2025-01-15T10:30:00',
      participantCount: 2,
      canJoin: true
    },
    {
      id: '2',
      name: 'Test Game 2',
      creatorName: 'User2',
      maxParticipants: 8,
      status: 'DRAFTING' as const,
      createdAt: '2025-01-15T11:30:00',
      participantCount: 5,
      canJoin: false
    },
    {
      id: '3',
      name: 'Test Game 3',
      creatorName: 'User3',
      maxParticipants: 6,
      status: 'CREATING' as const,
      createdAt: '2025-01-15T12:30:00',
      participantCount: 1,
      canJoin: true
    }
  ];

  beforeEach(async () => {
    const spy = jasmine.createSpyObj('GameService', ['getAvailableGames', 'joinGame']);
    
    await TestBed.configureTestingModule({
      imports: [
        JoinGameComponent,
        RouterTestingModule,
        NoopAnimationsModule,
        MatSnackBarModule
      ],
      providers: [
        { provide: GameService, useValue: spy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(JoinGameComponent);
    component = fixture.componentInstance;
    gameService = TestBed.inject(GameService) as jasmine.SpyObj<GameService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should load available games on init', () => {
      gameService.getAvailableGames.and.returnValue(of(mockGames));

      component.ngOnInit();

      expect(gameService.getAvailableGames).toHaveBeenCalled();
      expect(component.games).toEqual(mockGames);
      expect(component.loading).toBeFalse();
    });

    it('should handle error when loading games', () => {
      gameService.getAvailableGames.and.returnValue(throwError(() => new Error('Server error')));

      component.ngOnInit();

      expect(component.loading).toBeFalse();
      expect(component.error).toBeTruthy();
    });
  });

  describe('loadGames', () => {
    it('should load games and set loading state', () => {
      gameService.getAvailableGames.and.returnValue(of(mockGames));

      component.loadGames();

      expect(component.loading).toBeTrue();
      expect(gameService.getAvailableGames).toHaveBeenCalled();
    });

    it('should handle error when loading games', () => {
      gameService.getAvailableGames.and.returnValue(throwError(() => new Error('Server error')));

      component.loadGames();

      expect(component.loading).toBeFalse();
      expect(component.error).toBeTruthy();
    });
  });

  describe('joinGame', () => {
    it('should join game successfully', () => {
      const gameId = '1';
      gameService.joinGame.and.returnValue(of(true));

      component.joinGame(gameId);

      expect(gameService.joinGame).toHaveBeenCalledWith(gameId);
    });

    it('should handle error when joining game', () => {
      const gameId = '1';
      gameService.joinGame.and.returnValue(throwError(() => new Error('Cannot join')));

      component.joinGame(gameId);

      expect(component.error).toBeTruthy();
    });

    it('should reload games after successful join', () => {
      const gameId = '1';
      gameService.joinGame.and.returnValue(of(true));
      gameService.getAvailableGames.and.returnValue(of(mockGames));

      component.joinGame(gameId);

      expect(gameService.getAvailableGames).toHaveBeenCalledTimes(2); // Once in ngOnInit, once after join
    });
  });

  describe('filterGames', () => {
    beforeEach(() => {
      component.games = mockGames;
    });

    it('should filter games by name', () => {
      component.searchTerm = 'Game 1';
      component.filterGames();

      expect(component.filteredGames.length).toBe(1);
      expect(component.filteredGames[0].name).toBe('Test Game 1');
    });

    it('should filter games by creator name', () => {
      component.searchTerm = 'User2';
      component.filterGames();

      expect(component.filteredGames.length).toBe(1);
      expect(component.filteredGames[0].creatorName).toBe('User2');
    });

    it('should return all games when search term is empty', () => {
      component.searchTerm = '';
      component.filterGames();

      expect(component.filteredGames.length).toBe(3);
    });

    it('should be case insensitive', () => {
      component.searchTerm = 'user1';
      component.filterGames();

      expect(component.filteredGames.length).toBe(1);
      expect(component.filteredGames[0].creatorName).toBe('User1');
    });

    it('should filter by status', () => {
      component.selectedStatus = 'CREATING';
      component.filterGames();

      expect(component.filteredGames.length).toBe(2);
      expect(component.filteredGames.every(game => game.status === 'CREATING')).toBeTrue();
    });

    it('should filter by both search term and status', () => {
      component.searchTerm = 'Game';
      component.selectedStatus = 'CREATING';
      component.filterGames();

      expect(component.filteredGames.length).toBe(2);
      expect(component.filteredGames.every(game => 
        game.status === 'CREATING' && 
        game.name.toLowerCase().includes('game')
      )).toBeTrue();
    });
  });

  describe('clearFilters', () => {
    it('should clear search term and status filters', () => {
      component.searchTerm = 'test';
      component.selectedStatus = 'CREATING';

      component.clearFilters();

      expect(component.searchTerm).toBe('');
      expect(component.selectedStatus).toBe('');
    });

    it('should reload filtered games', () => {
      component.games = mockGames;
      component.searchTerm = 'test';
      component.filteredGames = [];

      component.clearFilters();

      expect(component.filteredGames.length).toBe(3);
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
      expect(color).toBe('warn');
    });

    it('should return correct color for FINISHED status', () => {
      const color = component.getStatusColor('FINISHED');
      expect(color).toBe('default');
    });
  });

  describe('getStatusLabel', () => {
    it('should return correct label for CREATING status', () => {
      const label = component.getStatusLabel('CREATING');
      expect(label).toBe('En création');
    });

    it('should return correct label for DRAFTING status', () => {
      const label = component.getStatusLabel('DRAFTING');
      expect(label).toBe('En draft');
    });

    it('should return correct label for ACTIVE status', () => {
      const label = component.getStatusLabel('ACTIVE');
      expect(label).toBe('Active');
    });

    it('should return correct label for FINISHED status', () => {
      const label = component.getStatusLabel('FINISHED');
      expect(label).toBe('Terminée');
    });
  });

  describe('canJoinGame', () => {
    it('should return true for joinable game', () => {
      const game = mockGames[0]; // canJoin: true
      expect(component.canJoinGame(game)).toBeTrue();
    });

    it('should return false for non-joinable game', () => {
      const game = mockGames[1]; // canJoin: false
      expect(component.canJoinGame(game)).toBeFalse();
    });

    it('should return false for full game', () => {
      const fullGame = { ...mockGames[0], participantCount: 5, maxParticipants: 5 };
      expect(component.canJoinGame(fullGame)).toBeFalse();
    });
  });

  describe('getAvailableStatuses', () => {
    it('should return unique statuses from games', () => {
      component.games = mockGames;
      const statuses = component.getAvailableStatuses();

      expect(statuses).toContain('CREATING');
      expect(statuses).toContain('DRAFTING');
      expect(statuses.length).toBe(2);
    });

    it('should return empty array when no games', () => {
      component.games = [];
      const statuses = component.getAvailableStatuses();

      expect(statuses.length).toBe(0);
    });
  });

  describe('onBack', () => {
    it('should navigate back to games list', () => {
      spyOn(component['router'], 'navigate');
      
      component.onBack();
      
      expect(component['router'].navigate).toHaveBeenCalledWith(['/games']);
    });
  });

  describe('getParticipantPercentage', () => {
    it('should calculate correct percentage', () => {
      const game = mockGames[0]; // participantCount: 2, maxParticipants: 5
      const percentage = component.getParticipantPercentage(game);
      expect(percentage).toBe(40);
    });

    it('should return 0 for empty game', () => {
      const game = { ...mockGames[0], participantCount: 0 };
      const percentage = component.getParticipantPercentage(game);
      expect(percentage).toBe(0);
    });

    it('should return 100 for full game', () => {
      const game = { ...mockGames[0], participantCount: 5, maxParticipants: 5 };
      const percentage = component.getParticipantPercentage(game);
      expect(percentage).toBe(100);
    });
  });

  it('should handle error if joinGame throws and show error', () => {
    const gameId = '1';
    gameService.joinGame.and.returnValue(throwError(() => new Error('Erreur')));
    component.joinGame(gameId);
    expect(component.error).toBeTruthy();
  });

  it('should not filter any games if games array is empty', () => {
    component.games = [];
    component.searchTerm = 'Test';
    component.filterGames();
    expect(component.filteredGames.length).toBe(0);
  });

  it('should not fail if clearFilters called with no games', () => {
    component.games = [];
    component.filteredGames = [];
    component.clearFilters();
    expect(component.filteredGames.length).toBe(0);
  });

  it('should return default color for unknown status', () => {
    expect(component.getStatusColor('UNKNOWN' as any)).toBe('default');
  });

  it('should return status label as is for unknown status', () => {
    expect(component.getStatusLabel('UNKNOWN' as any)).toBe('UNKNOWN');
  });

  it('should return 0 for getParticipantPercentage if maxParticipants is 0', () => {
    const game = { ...mockGames[0], maxParticipants: 0 };
    expect(component.getParticipantPercentage(game)).toBe(0);
  });
}); 