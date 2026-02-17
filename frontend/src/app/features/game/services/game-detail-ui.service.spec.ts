import { TestBed } from '@angular/core/testing';
import { GameDetailUIService } from './game-detail-ui.service';
import { GameDataService } from './game-data.service';
import { TranslationService } from '../../../core/services/translation.service';
import { Game, GameParticipant } from '../models/game.interface';

describe('GameDetailUIService', () => {
  let service: GameDetailUIService;
  let gameDataServiceSpy: jasmine.SpyObj<GameDataService>;
  let translationServiceSpy: jasmine.SpyObj<TranslationService>;

  const mockGame: Game = {
    id: 'game1',
    name: 'Test Game',
    creatorName: 'TestUser',
    maxParticipants: 10,
    status: 'CREATING',
    createdAt: new Date().toISOString(),
    participantCount: 5,
    canJoin: true,
    regionRules: { EU: 2, NAW: 3 }
  };

  const mockParticipants: GameParticipant[] = [
    {
      id: 'u1',
      username: 'Creator',
      joinedAt: new Date().toISOString(),
      isCreator: true
    },
    {
      id: 'u2',
      username: 'Player1',
      joinedAt: new Date().toISOString(),
      isCreator: false
    },
    {
      id: 'u3',
      username: 'Player2',
      joinedAt: new Date().toISOString(),
      isCreator: false
    }
  ];

  beforeEach(() => {
    gameDataServiceSpy = jasmine.createSpyObj('GameDataService', ['calculateGameStatistics']);
    translationServiceSpy = jasmine.createSpyObj('TranslationService', ['t']);

    translationServiceSpy.t.and.callFake((key: string) => key);
    gameDataServiceSpy.calculateGameStatistics.and.returnValue({
      fillPercentage: 50,
      availableSlots: 5,
      isNearlyFull: false,
      canAcceptMoreParticipants: true
    });

    TestBed.configureTestingModule({
      providers: [
        GameDetailUIService,
        { provide: GameDataService, useValue: gameDataServiceSpy },
        { provide: TranslationService, useValue: translationServiceSpy }
      ]
    });

    service = TestBed.inject(GameDetailUIService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getStatusColor', () => {
    it('should return primary for CREATING status', () => {
      expect(service.getStatusColor('CREATING')).toBe('primary');
    });

    it('should return accent for DRAFTING status', () => {
      expect(service.getStatusColor('DRAFTING')).toBe('accent');
    });

    it('should return warn for ACTIVE status', () => {
      expect(service.getStatusColor('ACTIVE')).toBe('warn');
    });

    it('should return default for FINISHED status', () => {
      expect(service.getStatusColor('FINISHED')).toBe('default');
    });

    it('should return default for CANCELLED status', () => {
      expect(service.getStatusColor('CANCELLED')).toBe('default');
    });

    it('should return default for unknown status', () => {
      expect(service.getStatusColor('UNKNOWN' as any)).toBe('default');
    });
  });

  describe('getStatusLabel', () => {
    it('should return translated label for CREATING', () => {
      expect(service.getStatusLabel('CREATING')).toBe('games.home.statusCreating');
      expect(translationServiceSpy.t).toHaveBeenCalledWith('games.home.statusCreating');
    });

    it('should return translated label for DRAFTING', () => {
      expect(service.getStatusLabel('DRAFTING')).toBe('games.home.statusDrafting');
      expect(translationServiceSpy.t).toHaveBeenCalledWith('games.home.statusDrafting');
    });

    it('should return translated label for ACTIVE', () => {
      expect(service.getStatusLabel('ACTIVE')).toBe('games.home.statusActive');
      expect(translationServiceSpy.t).toHaveBeenCalledWith('games.home.statusActive');
    });

    it('should return translated label for FINISHED', () => {
      expect(service.getStatusLabel('FINISHED')).toBe('games.home.statusFinished');
      expect(translationServiceSpy.t).toHaveBeenCalledWith('games.home.statusFinished');
    });

    it('should return translated label for CANCELLED', () => {
      expect(service.getStatusLabel('CANCELLED')).toBe('games.home.statusCancelled');
      expect(translationServiceSpy.t).toHaveBeenCalledWith('games.home.statusCancelled');
    });

    it('should return status as-is for unknown status', () => {
      expect(service.getStatusLabel('UNKNOWN' as any)).toBe('UNKNOWN');
    });
  });

  describe('getParticipantPercentage', () => {
    it('should return fill percentage from game statistics', () => {
      const result = service.getParticipantPercentage(mockGame);
      expect(result).toBe(50);
      expect(gameDataServiceSpy.calculateGameStatistics).toHaveBeenCalledWith(mockGame);
    });

    it('should return 0 when game is null', () => {
      expect(service.getParticipantPercentage(null)).toBe(0);
    });
  });

  describe('getGameStatistics', () => {
    it('should return statistics from game data service', () => {
      const result = service.getGameStatistics(mockGame);
      expect(result).toEqual({
        fillPercentage: 50,
        availableSlots: 5,
        isNearlyFull: false,
        canAcceptMoreParticipants: true
      });
      expect(gameDataServiceSpy.calculateGameStatistics).toHaveBeenCalledWith(mockGame);
    });

    it('should return null when game is null', () => {
      expect(service.getGameStatistics(null)).toBeNull();
    });
  });

  describe('getParticipantColor', () => {
    it('should return warn for >=90% fill', () => {
      gameDataServiceSpy.calculateGameStatistics.and.returnValue({
        fillPercentage: 90,
        availableSlots: 1,
        isNearlyFull: true,
        canAcceptMoreParticipants: true
      });

      expect(service.getParticipantColor(mockGame)).toBe('warn');
    });

    it('should return accent for >=70% fill', () => {
      gameDataServiceSpy.calculateGameStatistics.and.returnValue({
        fillPercentage: 75,
        availableSlots: 2,
        isNearlyFull: true,
        canAcceptMoreParticipants: true
      });

      expect(service.getParticipantColor(mockGame)).toBe('accent');
    });

    it('should return primary for <70% fill', () => {
      gameDataServiceSpy.calculateGameStatistics.and.returnValue({
        fillPercentage: 50,
        availableSlots: 5,
        isNearlyFull: false,
        canAcceptMoreParticipants: true
      });

      expect(service.getParticipantColor(mockGame)).toBe('primary');
    });

    it('should handle null game', () => {
      expect(service.getParticipantColor(null)).toBe('primary');
    });
  });

  describe('getTimeAgo', () => {
    it('should return formatted relative time for string date', () => {
      const result = service.getTimeAgo('2024-01-01T00:00:00Z');
      expect(result).toBeDefined();
      expect(typeof result).toBe('string');
    });

    it('should return formatted relative time for Date object', () => {
      const date = new Date();
      const result = service.getTimeAgo(date);
      expect(result).toBeDefined();
      expect(typeof result).toBe('string');
    });

    it('should return translated fallback for null date', () => {
      expect(service.getTimeAgo(null)).toBe('common.invalidDate');
      expect(translationServiceSpy.t).toHaveBeenCalledWith('common.invalidDate');
    });

    it('should return translated fallback for undefined date', () => {
      expect(service.getTimeAgo(undefined)).toBe('common.invalidDate');
      expect(translationServiceSpy.t).toHaveBeenCalledWith('common.invalidDate');
    });
  });

  describe('getInvitationCodeExpiry', () => {
    it('should return translated permanent label when no expiration', () => {
      const game = { ...mockGame };
      expect(service.getInvitationCodeExpiry(game)).toBe('games.detail.permanent');
      expect(translationServiceSpy.t).toHaveBeenCalledWith('games.detail.permanent');
    });

    it('should return translated expired label when code is expired', () => {
      const game = {
        ...mockGame,
        invitationCodeExpiresAt: '2024-01-01T00:00:00Z',
        isInvitationCodeExpired: true
      };
      expect(service.getInvitationCodeExpiry(game)).toBe('games.detail.expired');
      expect(translationServiceSpy.t).toHaveBeenCalledWith('games.detail.expired');
    });

    it('should return time ago when not expired', () => {
      const futureDate = new Date();
      futureDate.setDate(futureDate.getDate() + 1);

      const game = {
        ...mockGame,
        invitationCodeExpiresAt: futureDate.toISOString(),
        isInvitationCodeExpired: false
      };

      const result = service.getInvitationCodeExpiry(game);
      expect(result).toBeDefined();
      expect(result).not.toBe('games.detail.permanent');
      expect(result).not.toBe('games.detail.expired');
    });

    it('should handle null game', () => {
      expect(service.getInvitationCodeExpiry(null)).toBe('games.detail.permanent');
      expect(translationServiceSpy.t).toHaveBeenCalledWith('games.detail.permanent');
    });
  });

  describe('getCreator', () => {
    it('should return creator participant', () => {
      const creator = service.getCreator(mockParticipants);
      expect(creator).toEqual(mockParticipants[0]);
      expect(creator?.username).toBe('Creator');
      expect(creator?.isCreator).toBeTrue();
    });

    it('should return null when no creator exists', () => {
      const noCreator = mockParticipants.filter(p => !p.isCreator);
      expect(service.getCreator(noCreator)).toBeNull();
    });

    it('should return null for empty array', () => {
      expect(service.getCreator([])).toBeNull();
    });
  });

  describe('getNonCreatorParticipants', () => {
    it('should return only non-creator participants', () => {
      const result = service.getNonCreatorParticipants(mockParticipants);
      expect(result.length).toBe(2);
      expect(result[0].username).toBe('Player1');
      expect(result[1].username).toBe('Player2');
      expect(result.every(p => !p.isCreator)).toBeTrue();
    });

    it('should return empty array when all are creators', () => {
      const allCreators = mockParticipants.map(p => ({ ...p, isCreator: true }));
      expect(service.getNonCreatorParticipants(allCreators)).toEqual([]);
    });

    it('should return empty array for empty input', () => {
      expect(service.getNonCreatorParticipants([])).toEqual([]);
    });
  });

  describe('getParticipantStatusIcon', () => {
    it('should return star for creator', () => {
      expect(service.getParticipantStatusIcon(mockParticipants[0])).toBe('star');
    });

    it('should return person for non-creator', () => {
      expect(service.getParticipantStatusIcon(mockParticipants[1])).toBe('person');
      expect(service.getParticipantStatusIcon(mockParticipants[2])).toBe('person');
    });
  });

  describe('getParticipantStatusColor', () => {
    it('should return accent for creator', () => {
      expect(service.getParticipantStatusColor(mockParticipants[0])).toBe('accent');
    });

    it('should return primary for non-creator', () => {
      expect(service.getParticipantStatusColor(mockParticipants[1])).toBe('primary');
      expect(service.getParticipantStatusColor(mockParticipants[2])).toBe('primary');
    });
  });

  describe('getParticipantStatusLabel', () => {
    it('should return translated creator label for creator', () => {
      expect(service.getParticipantStatusLabel(mockParticipants[0])).toBe('games.detail.creator');
      expect(translationServiceSpy.t).toHaveBeenCalledWith('games.detail.creator');
    });

    it('should return translated participant label for non-creator', () => {
      expect(service.getParticipantStatusLabel(mockParticipants[1])).toBe('games.detail.participant');
      expect(service.getParticipantStatusLabel(mockParticipants[2])).toBe('games.detail.participant');
      expect(translationServiceSpy.t).toHaveBeenCalledWith('games.detail.participant');
    });
  });

  describe('edge cases', () => {
    it('should handle game with all fields populated', () => {
      const completeGame: Game = {
        id: 'game1',
        name: 'Complete Game',
        creatorName: 'Admin',
        maxParticipants: 10,
        status: 'ACTIVE',
        createdAt: new Date().toISOString(),
        participantCount: 10,
        canJoin: false,
        regionRules: { EU: 3, NAW: 3, NAC: 2, BR: 2 },
        invitationCode: 'CODE123',
        invitationCodeExpiresAt: new Date(Date.now() + 86400000).toISOString(),
        isInvitationCodeExpired: false
      };

      expect(() => service.getStatusColor(completeGame.status)).not.toThrow();
      expect(() => service.getStatusLabel(completeGame.status)).not.toThrow();
      expect(() => service.getParticipantPercentage(completeGame)).not.toThrow();
      expect(() => service.getParticipantColor(completeGame)).not.toThrow();
      expect(() => service.getInvitationCodeExpiry(completeGame)).not.toThrow();
    });

    it('should handle participant with minimal data', () => {
      const minimalParticipant: GameParticipant = {
        id: 'p1',
        username: 'Minimal',
        joinedAt: new Date().toISOString(),
        isCreator: false
      };

      expect(service.getParticipantStatusIcon(minimalParticipant)).toBe('person');
      expect(service.getParticipantStatusColor(minimalParticipant)).toBe('primary');
      expect(service.getParticipantStatusLabel(minimalParticipant)).toBe('games.detail.participant');
    });
  });
});
