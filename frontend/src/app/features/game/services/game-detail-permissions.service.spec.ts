import { TestBed } from '@angular/core/testing';
import { GameDetailPermissionsService } from './game-detail-permissions.service';
import { GameService } from './game.service';
import { UserContextService } from '../../../core/services/user-context.service';
import { Game } from '../models/game.interface';

describe('GameDetailPermissionsService', () => {
  let service: GameDetailPermissionsService;
  let gameServiceSpy: jasmine.SpyObj<GameService>;
  let userContextServiceSpy: jasmine.SpyObj<UserContextService>;

  const mockUser = {
    id: 'user1',
    username: 'testuser',
    email: 'test@example.com'
  };

  const mockGame: Game = {
    id: 'game1',
    name: 'Test Game',
    creatorName: 'testuser',
    maxParticipants: 10,
    status: 'CREATING',
    createdAt: new Date().toISOString(),
    participantCount: 5,
    canJoin: true,
    regionRules: { EU: 2, NAW: 3 },
    participants: [
      { id: 'u1', username: 'testuser', joinedAt: new Date().toISOString(), isCreator: true },
      { id: 'u2', username: 'player1', joinedAt: new Date().toISOString(), isCreator: false },
      { id: 'u3', username: 'player2', joinedAt: new Date().toISOString(), isCreator: false }
    ]
  };

  beforeEach(() => {
    gameServiceSpy = jasmine.createSpyObj('GameService', ['isGameHost']);
    userContextServiceSpy = jasmine.createSpyObj('UserContextService', ['getCurrentUser']);

    userContextServiceSpy.getCurrentUser.and.returnValue(mockUser);
    gameServiceSpy.isGameHost.and.returnValue(true);

    TestBed.configureTestingModule({
      providers: [
        GameDetailPermissionsService,
        { provide: GameService, useValue: gameServiceSpy },
        { provide: UserContextService, useValue: userContextServiceSpy }
      ]
    });

    service = TestBed.inject(GameDetailPermissionsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('canStartDraft', () => {
    it('should return true when game is CREATING with >=2 participants', () => {
      const game = { ...mockGame, status: 'CREATING' as const, participantCount: 2 };
      expect(service.canStartDraft(game)).toBeTrue();
    });

    it('should return true when game has more than 2 participants', () => {
      const game = { ...mockGame, status: 'CREATING' as const, participantCount: 5 };
      expect(service.canStartDraft(game)).toBeTrue();
    });

    it('should return false when game has less than 2 participants', () => {
      const game = { ...mockGame, status: 'CREATING' as const, participantCount: 1 };
      expect(service.canStartDraft(game)).toBeFalse();
    });

    it('should return false when game status is not CREATING', () => {
      const game = { ...mockGame, status: 'DRAFTING' as const, participantCount: 5 };
      expect(service.canStartDraft(game)).toBeFalse();
    });

    it('should return false when game is null', () => {
      expect(service.canStartDraft(null)).toBeFalse();
    });
  });

  describe('canArchiveGame', () => {
    it('should return true when user is host', () => {
      gameServiceSpy.isGameHost.and.returnValue(true);
      expect(service.canArchiveGame(mockGame)).toBeTrue();
      expect(gameServiceSpy.isGameHost).toHaveBeenCalledWith(mockGame, 'testuser');
    });

    it('should return false when user is not host', () => {
      gameServiceSpy.isGameHost.and.returnValue(false);
      expect(service.canArchiveGame(mockGame)).toBeFalse();
    });

    it('should return false when no current user', () => {
      userContextServiceSpy.getCurrentUser.and.returnValue(null);
      expect(service.canArchiveGame(mockGame)).toBeFalse();
    });

    it('should return false when game is null', () => {
      expect(service.canArchiveGame(null)).toBeFalse();
    });
  });

  describe('canLeaveGame', () => {
    it('should return true when user is participant but not host', () => {
      gameServiceSpy.isGameHost.and.returnValue(false);
      const game = {
        ...mockGame,
        participants: [
          { id: 'u1', username: 'host', joinedAt: '', isCreator: true },
          { id: 'u2', username: 'testuser', joinedAt: '', isCreator: false }
        ]
      };

      expect(service.canLeaveGame(game)).toBeTrue();
      expect(gameServiceSpy.isGameHost).toHaveBeenCalledWith(game, 'testuser');
    });

    it('should return false when user is host', () => {
      gameServiceSpy.isGameHost.and.returnValue(true);
      const game = {
        ...mockGame,
        participants: [
          { id: 'u1', username: 'testuser', joinedAt: '', isCreator: true }
        ]
      };

      expect(service.canLeaveGame(game)).toBeFalse();
    });

    it('should return false when user is not a participant', () => {
      gameServiceSpy.isGameHost.and.returnValue(false);
      const game = {
        ...mockGame,
        participants: [
          { id: 'u1', username: 'otheruser', joinedAt: '', isCreator: true }
        ]
      };

      expect(service.canLeaveGame(game)).toBeFalse();
    });

    it('should return false when no current user', () => {
      userContextServiceSpy.getCurrentUser.and.returnValue(null);
      expect(service.canLeaveGame(mockGame)).toBeFalse();
    });

    it('should return false when game is null', () => {
      expect(service.canLeaveGame(null)).toBeFalse();
    });

    it('should handle undefined participants array', () => {
      gameServiceSpy.isGameHost.and.returnValue(false);
      const game = { ...mockGame, participants: undefined };
      expect(service.canLeaveGame(game as any)).toBeFalse();
    });
  });

  describe('canDeleteGame', () => {
    it('should return true when user is host and status is CREATING', () => {
      gameServiceSpy.isGameHost.and.returnValue(true);
      const game = { ...mockGame, status: 'CREATING' as const };
      expect(service.canDeleteGame(game)).toBeTrue();
      expect(gameServiceSpy.isGameHost).toHaveBeenCalledWith(game, 'testuser');
    });

    it('should return false when user is not host', () => {
      gameServiceSpy.isGameHost.and.returnValue(false);
      const game = { ...mockGame, status: 'CREATING' as const };
      expect(service.canDeleteGame(game)).toBeFalse();
    });

    it('should return false when status is not CREATING', () => {
      gameServiceSpy.isGameHost.and.returnValue(true);
      const game = { ...mockGame, status: 'DRAFTING' as const };
      expect(service.canDeleteGame(game)).toBeFalse();
    });

    it('should return false when status is ACTIVE', () => {
      gameServiceSpy.isGameHost.and.returnValue(true);
      const game = { ...mockGame, status: 'ACTIVE' as const };
      expect(service.canDeleteGame(game)).toBeFalse();
    });

    it('should return false when no current user', () => {
      userContextServiceSpy.getCurrentUser.and.returnValue(null);
      expect(service.canDeleteGame(mockGame)).toBeFalse();
    });

    it('should return false when game is null', () => {
      expect(service.canDeleteGame(null)).toBeFalse();
    });
  });

  describe('canJoinGame', () => {
    it('should return true when game allows joining and has slots', () => {
      const game = { ...mockGame, canJoin: true, participantCount: 5, maxParticipants: 10 };
      expect(service.canJoinGame(game)).toBeTrue();
    });

    it('should return false when game is full', () => {
      const game = { ...mockGame, canJoin: true, participantCount: 10, maxParticipants: 10 };
      expect(service.canJoinGame(game)).toBeFalse();
    });

    it('should return false when canJoin is false', () => {
      const game = { ...mockGame, canJoin: false, participantCount: 5, maxParticipants: 10 };
      expect(service.canJoinGame(game)).toBeFalse();
    });

    it('should return false when game is null', () => {
      expect(service.canJoinGame(null)).toBeFalse();
    });

    it('should return true when game has exactly one slot left', () => {
      const game = { ...mockGame, canJoin: true, participantCount: 9, maxParticipants: 10 };
      expect(service.canJoinGame(game)).toBeTrue();
    });
  });

  describe('canRegenerateCode', () => {
    it('should return true when user is host and status is CREATING', () => {
      gameServiceSpy.isGameHost.and.returnValue(true);
      const game = { ...mockGame, status: 'CREATING' as const };
      expect(service.canRegenerateCode(game)).toBeTrue();
      expect(gameServiceSpy.isGameHost).toHaveBeenCalledWith(game, 'testuser');
    });

    it('should return false when user is not host', () => {
      gameServiceSpy.isGameHost.and.returnValue(false);
      const game = { ...mockGame, status: 'CREATING' as const };
      expect(service.canRegenerateCode(game)).toBeFalse();
    });

    it('should return false when status is not CREATING', () => {
      gameServiceSpy.isGameHost.and.returnValue(true);
      const game = { ...mockGame, status: 'DRAFTING' as const };
      expect(service.canRegenerateCode(game)).toBeFalse();
    });

    it('should return false when no current user', () => {
      userContextServiceSpy.getCurrentUser.and.returnValue(null);
      expect(service.canRegenerateCode(mockGame)).toBeFalse();
    });

    it('should return false when game is null', () => {
      expect(service.canRegenerateCode(null)).toBeFalse();
    });
  });

  describe('canRenameGame', () => {
    it('should return true when user is host and status is CREATING', () => {
      gameServiceSpy.isGameHost.and.returnValue(true);
      const game = { ...mockGame, status: 'CREATING' as const };
      expect(service.canRenameGame(game)).toBeTrue();
      expect(gameServiceSpy.isGameHost).toHaveBeenCalledWith(game, 'testuser');
    });

    it('should return false when user is not host', () => {
      gameServiceSpy.isGameHost.and.returnValue(false);
      const game = { ...mockGame, status: 'CREATING' as const };
      expect(service.canRenameGame(game)).toBeFalse();
    });

    it('should return false when status is not CREATING', () => {
      gameServiceSpy.isGameHost.and.returnValue(true);
      const game = { ...mockGame, status: 'DRAFTING' as const };
      expect(service.canRenameGame(game)).toBeFalse();
    });

    it('should return false when no current user', () => {
      userContextServiceSpy.getCurrentUser.and.returnValue(null);
      expect(service.canRenameGame(mockGame)).toBeFalse();
    });

    it('should return false when game is null', () => {
      expect(service.canRenameGame(null)).toBeFalse();
    });
  });

  describe('edge cases and combinations', () => {
    it('should handle all statuses correctly for canDeleteGame', () => {
      gameServiceSpy.isGameHost.and.returnValue(true);

      const statuses: Array<Game['status']> = ['CREATING', 'DRAFTING', 'ACTIVE', 'FINISHED', 'CANCELLED'];
      const expected = [true, false, false, false, false];

      statuses.forEach((status, index) => {
        const game = { ...mockGame, status };
        expect(service.canDeleteGame(game)).toBe(expected[index], `Failed for status: ${status}`);
      });
    });

    it('should handle participant count edge cases', () => {
      const testCases = [
        { count: 0, expected: false },
        { count: 1, expected: false },
        { count: 2, expected: true },
        { count: 10, expected: true }
      ];

      testCases.forEach(({ count, expected }) => {
        const game = { ...mockGame, status: 'CREATING' as const, participantCount: count };
        expect(service.canStartDraft(game)).toBe(expected, `Failed for count: ${count}`);
      });
    });

    it('should handle multiple permission checks on same game', () => {
      gameServiceSpy.isGameHost.and.returnValue(true);
      const game = { ...mockGame, status: 'CREATING' as const };

      expect(service.canArchiveGame(game)).toBeTrue();
      expect(service.canDeleteGame(game)).toBeTrue();
      expect(service.canRegenerateCode(game)).toBeTrue();
      expect(service.canRenameGame(game)).toBeTrue();
      expect(service.canStartDraft(game)).toBeTrue();
    });

    it('should handle non-host user correctly across all methods', () => {
      gameServiceSpy.isGameHost.and.returnValue(false);
      const game = {
        ...mockGame,
        status: 'CREATING' as const,
        participants: [
          { id: 'u1', username: 'host', joinedAt: '', isCreator: true },
          { id: 'u2', username: 'testuser', joinedAt: '', isCreator: false }
        ]
      };

      expect(service.canArchiveGame(game)).toBeFalse();
      expect(service.canDeleteGame(game)).toBeFalse();
      expect(service.canRegenerateCode(game)).toBeFalse();
      expect(service.canRenameGame(game)).toBeFalse();
      expect(service.canLeaveGame(game)).toBeTrue(); // Can leave as non-host participant
    });
  });
});
