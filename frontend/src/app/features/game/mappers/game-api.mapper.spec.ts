import { GameApiMapper } from './game-api.mapper';
import { Game, GameParticipant } from '../models/game.interface';

describe('GameApiMapper', () => {
  
  describe('mapApiResponseToGame', () => {
    it('should map complete API response correctly', () => {
      // Arrange
      const apiResponse = {
        id: 'game-123',
        name: 'Test Game',
        creatorUsername: 'TestUser',
        maxParticipants: 10,
        currentParticipantCount: 5,
        status: 'ACTIVE',
        createdAt: '2025-01-15T10:30:00Z',
        availableToJoin: true,
        invitationCode: 'ABC123'
      };

      // Act
      const game = GameApiMapper.mapApiResponseToGame(apiResponse);

      // Assert
      expect(game.id).toBe('game-123');
      expect(game.name).toBe('Test Game');
      expect(game.creatorName).toBe('TestUser');
      expect(game.maxParticipants).toBe(10);
      expect(game.participantCount).toBe(5);
      expect(game.status).toBe('ACTIVE');
      expect(game.canJoin).toBe(true);
      expect(game.invitationCode).toBe('ABC123');
    });

    it('should handle missing creatorUsername by using creatorName', () => {
      // Arrange
      const apiResponse = {
        id: 'game-123',
        name: 'Test Game',
        creatorName: 'FallbackUser',
        maxParticipants: 10,
        currentParticipantCount: 5,
        status: 'ACTIVE',
        createdAt: '2025-01-15T10:30:00Z'
      };

      // Act
      const game = GameApiMapper.mapApiResponseToGame(apiResponse);

      // Assert
      expect(game.creatorName).toBe('FallbackUser');
    });

    it('should use default creator name when both are missing', () => {
      // Arrange
      const apiResponse = {
        id: 'game-123',
        name: 'Test Game',
        maxParticipants: 10,
        status: 'ACTIVE',
        createdAt: '2025-01-15T10:30:00Z'
      };

      // Act
      const game = GameApiMapper.mapApiResponseToGame(apiResponse);

      // Assert
      expect(game.creatorName).toBe('Créateur inconnu');
    });

    it('should handle currentParticipantCount vs participantCount', () => {
      // Arrange - API uses currentParticipantCount
      const apiResponse1 = {
        id: 'game-123',
        name: 'Test Game',
        currentParticipantCount: 7,
        maxParticipants: 10,
        status: 'ACTIVE',
        createdAt: '2025-01-15T10:30:00Z'
      };

      // Arrange - API uses participantCount
      const apiResponse2 = {
        id: 'game-456',
        name: 'Test Game 2',
        participantCount: 3,
        maxParticipants: 10,
        status: 'ACTIVE',
        createdAt: '2025-01-15T10:30:00Z'
      };

      // Act
      const game1 = GameApiMapper.mapApiResponseToGame(apiResponse1);
      const game2 = GameApiMapper.mapApiResponseToGame(apiResponse2);

      // Assert
      expect(game1.participantCount).toBe(7);
      expect(game2.participantCount).toBe(3);
    });

    it('should throw error for null API response', () => {
      // Act & Assert
      expect(() => GameApiMapper.mapApiResponseToGame(null)).toThrowError('API response is null or undefined');
    });
  });

  describe('mapApiParticipants', () => {
    it('should map participants array correctly', () => {
      // Arrange
      const apiParticipants = [
        {
          id: 'participant-1',
          username: 'User1',
          joinedAt: '2025-01-15T10:30:00Z',
          isCreator: true,
          draftOrder: 1
        },
        {
          id: 'participant-2',
          username: 'User2',
          joinedAt: '2025-01-15T10:35:00Z',
          isCreator: false,
          draftOrder: 2
        }
      ];

      // Act
      const participants = GameApiMapper.mapApiParticipants(apiParticipants);

      // Assert
      expect(participants).toHaveSize(2);
      expect(participants[0].id).toBe('participant-1');
      expect(participants[0].username).toBe('User1');
      expect(participants[0].isCreator).toBe(true);
      expect(participants[1].isCreator).toBe(false);
    });

    it('should return empty array for non-array input', () => {
      // Act
      const result1 = GameApiMapper.mapApiParticipants(null as any);
      const result2 = GameApiMapper.mapApiParticipants(undefined as any);
      const result3 = GameApiMapper.mapApiParticipants({} as any);

      // Assert
      expect(result1).toEqual([]);
      expect(result2).toEqual([]);
      expect(result3).toEqual([]);
    });
  });

  describe('calculateFillPercentage', () => {
    it('should calculate percentage correctly', () => {
      // Arrange
      const game: Game = {
        id: 'test',
        name: 'Test',
        creatorName: 'Creator',
        maxParticipants: 10,
        participantCount: 7,
        status: 'ACTIVE',
        createdAt: '2025-01-15T10:30:00Z',
        canJoin: true
      };

      // Act
      const percentage = GameApiMapper.calculateFillPercentage(game);

      // Assert
      expect(percentage).toBe(70);
    });

    it('should return 0 for null game', () => {
      // Act
      const percentage = GameApiMapper.calculateFillPercentage(null as any);

      // Assert
      expect(percentage).toBe(0);
    });

    it('should return 0 when maxParticipants is 0', () => {
      // Arrange
      const game: Game = {
        id: 'test',
        name: 'Test',
        creatorName: 'Creator',
        maxParticipants: 0,
        participantCount: 5,
        status: 'ACTIVE',
        createdAt: '2025-01-15T10:30:00Z',
        canJoin: true
      };

      // Act
      const percentage = GameApiMapper.calculateFillPercentage(game);

      // Assert
      expect(percentage).toBe(0);
    });
  });

  describe('formatRelativeTime', () => {
    beforeEach(() => {
      // Mock de la date courante pour des tests prévisibles
      jasmine.clock().install();
      jasmine.clock().mockDate(new Date('2025-01-15T12:00:00Z'));
    });

    afterEach(() => {
      jasmine.clock().uninstall();
    });

    it('should format recent time correctly', () => {
      // Arrange - Il y a 30 minutes
      const dateString = '2025-01-15T11:30:00Z';

      // Act
      const result = GameApiMapper.formatRelativeTime(dateString);

      // Assert
      expect(result).toBe('Il y a 30 min');
    });

    it('should format hours correctly', () => {
      // Arrange - Il y a 2 heures
      const dateString = '2025-01-15T10:00:00Z';

      // Act
      const result = GameApiMapper.formatRelativeTime(dateString);

      // Assert
      expect(result).toBe('Il y a 2h');
    });

    it('should format days correctly', () => {
      // Arrange - Il y a 2 jours
      const dateString = '2025-01-13T12:00:00Z';

      // Act
      const result = GameApiMapper.formatRelativeTime(dateString);

      // Assert
      expect(result).toBe('Il y a 2j');
    });

    it('should return "Date invalide" for invalid date string', () => {
      // Act
      const result = GameApiMapper.formatRelativeTime('invalid-date');

      // Assert
      expect(result).toBe('Date invalide');
    });

    it('should return "À l\'instant" for very recent time', () => {
      // Arrange - Il y a 30 secondes
      const dateString = '2025-01-15T11:59:30Z';

      // Act
      const result = GameApiMapper.formatRelativeTime(dateString);

      // Assert
      expect(result).toBe('À l\'instant');
    });
  });
});