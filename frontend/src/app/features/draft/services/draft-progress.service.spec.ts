import { TestBed } from '@angular/core/testing';
import { DraftProgressService } from './draft-progress.service';
import { DraftBoardState, GameParticipant, Player } from './draft.service';

describe('DraftProgressService', () => {
  let service: DraftProgressService;

  const createMockState = (overrides: Partial<DraftBoardState> = {}): DraftBoardState => ({
    draft: {
      id: 'draft-1',
      gameId: 'game-1',
      status: 'ACTIVE',
      currentRound: 1,
      currentPick: 5,
      totalRounds: 3,
      pickTimeLimit: 60
    },
    participants: [],
    availablePlayers: [],
    pickHistory: [],
    rules: {
      pickTimeLimit: 60,
      totalRounds: 3,
      regionQuotas: {},
      trancheQuotas: {}
    },
    ...overrides
  });

  const createMockParticipant = (overrides: Partial<GameParticipant> = {}): GameParticipant => ({
    id: 'user-1',
    username: 'testuser',
    selectedPlayers: [],
    pickOrder: 1,
    isCurrentTurn: false,
    ...overrides
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DraftProgressService]
    });
    service = TestBed.inject(DraftProgressService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('calculateProgress', () => {
    it('should return 0 for null state', () => {
      expect(service.calculateProgress(null)).toBe(0);
    });

    it('should calculate progress correctly', () => {
      const state = createMockState({
        draft: {
          id: 'draft-1',
          gameId: 'game-1',
          status: 'ACTIVE',
          currentRound: 1,
          currentPick: 5,
          totalRounds: 3,
          pickTimeLimit: 60
        },
        participants: [
          createMockParticipant({ id: '1' }),
          createMockParticipant({ id: '2' })
        ]
      });

      const progress = service.calculateProgress(state);
      const expectedProgress = ((5 - 1) / (3 * 2)) * 100; // (currentPick-1) / (totalRounds * participants)

      expect(progress).toBeCloseTo(expectedProgress, 2);
    });

    it('should cap progress at 100%', () => {
      const state = createMockState({
        draft: {
          id: 'draft-1',
          gameId: 'game-1',
          status: 'ACTIVE',
          currentRound: 1,
          currentPick: 100,
          totalRounds: 3,
          pickTimeLimit: 60
        },
        participants: [createMockParticipant()]
      });

      expect(service.calculateProgress(state)).toBe(100);
    });
  });

  describe('getProgressText', () => {
    it('should return empty string for null state', () => {
      expect(service.getProgressText(null)).toBe('');
    });

    it('should format progress text correctly', () => {
      const state = createMockState({
        draft: {
          id: 'draft-1',
          gameId: 'game-1',
          status: 'ACTIVE',
          currentRound: 1,
          currentPick: 5,
          totalRounds: 3,
          pickTimeLimit: 60
        },
        participants: [
          createMockParticipant(),
          createMockParticipant()
        ]
      });

      expect(service.getProgressText(state)).toBe('5 / 6');
    });
  });

  describe('formatTime', () => {
    it('should format seconds to MM:SS', () => {
      expect(service.formatTime(65)).toBe('1:05');
      expect(service.formatTime(120)).toBe('2:00');
      expect(service.formatTime(0)).toBe('0:00');
      expect(service.formatTime(599)).toBe('9:59');
    });
  });

  describe('getCurrentUserTeam', () => {
    it('should return empty array for null state', () => {
      expect(service.getCurrentUserTeam(null, 'user-1')).toEqual([]);
    });

    it('should return empty array for null userId', () => {
      const state = createMockState();
      expect(service.getCurrentUserTeam(state, null)).toEqual([]);
    });

    it('should return current user selected players', () => {
      const players: Player[] = [
        { id: 'p1', nickname: 'Player1', username: 'user1', region: 'EUROPE', tranche: 'T1', epicAccountId: 'epic1', isSelected: true }
      ];

      const state = createMockState({
        participants: [
          createMockParticipant({ id: 'user-1', selectedPlayers: players }),
          createMockParticipant({ id: 'user-2', selectedPlayers: [] })
        ]
      });

      const result = service.getCurrentUserTeam(state, 'user-1');

      expect(result.length).toBe(1);
      expect(result[0].id).toBe('p1');
    });
  });

  describe('getRemainingSlots', () => {
    it('should calculate remaining slots', () => {
      const team: Player[] = [
        { id: 'p1', nickname: 'P1', username: 'u1', region: 'EUROPE', tranche: 'T1', epicAccountId: 'e1', isSelected: true },
        { id: 'p2', nickname: 'P2', username: 'u2', region: 'EUROPE', tranche: 'T1', epicAccountId: 'e2', isSelected: true }
      ];

      expect(service.getRemainingSlots(team, 5)).toBe(3);
    });

    it('should return 0 when team is full', () => {
      const team: Player[] = Array(5).fill(null).map((_, i) => ({
        id: `p${i}`,
        nickname: `P${i}`,
        username: `u${i}`,
        region: 'EUROPE' as any,
        tranche: 'T1',
        epicAccountId: `e${i}`,
        isSelected: true
      }));

      expect(service.getRemainingSlots(team, 5)).toBe(0);
    });

    it('should not return negative values', () => {
      const team: Player[] = Array(10).fill(null).map((_, i) => ({
        id: `p${i}`,
        nickname: `P${i}`,
        username: `u${i}`,
        region: 'EUROPE' as any,
        tranche: 'T1',
        epicAccountId: `e${i}`,
        isSelected: true
      }));

      expect(service.getRemainingSlots(team, 5)).toBe(0);
    });
  });

  describe('getCurrentTurnPlayer', () => {
    it('should return null for null state', () => {
      expect(service.getCurrentTurnPlayer(null)).toBeNull();
    });

    it('should return participant with isCurrentTurn=true', () => {
      const state = createMockState({
        participants: [
          createMockParticipant({ id: 'user-1', isCurrentTurn: false }),
          createMockParticipant({ id: 'user-2', isCurrentTurn: true, username: 'currentUser' })
        ]
      });

      const result = service.getCurrentTurnPlayer(state);

      expect(result).not.toBeNull();
      expect(result?.username).toBe('currentUser');
    });

    it('should handle nested participant structure', () => {
      const state = createMockState({
        participants: [
          { participant: createMockParticipant({ id: 'user-1', isCurrentTurn: true, username: 'nestedUser' }) } as any
        ]
      });

      const result = service.getCurrentTurnPlayer(state);

      expect(result).not.toBeNull();
      expect(result?.username).toBe('nestedUser');
    });
  });

  describe('isCurrentUserTurn', () => {
    it('should return false for null state', () => {
      expect(service.isCurrentUserTurn(null, 'user-1')).toBe(false);
    });

    it('should return false for null userId', () => {
      const state = createMockState();
      expect(service.isCurrentUserTurn(state, null)).toBe(false);
    });

    it('should return true when it is current user turn', () => {
      const state = createMockState({
        participants: [
          createMockParticipant({ id: 'user-1', isCurrentTurn: true })
        ]
      });

      expect(service.isCurrentUserTurn(state, 'user-1')).toBe(true);
    });

    it('should return false when it is not current user turn', () => {
      const state = createMockState({
        participants: [
          createMockParticipant({ id: 'user-1', isCurrentTurn: false }),
          createMockParticipant({ id: 'user-2', isCurrentTurn: true })
        ]
      });

      expect(service.isCurrentUserTurn(state, 'user-1')).toBe(false);
    });
  });
});
