import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { GameService } from './game.service';
import { GameQueryService } from './game-query.service';
import { GameCommandService } from './game-command.service';
import {
  Game,
  CreateGameRequest,
  DraftState,
  DraftStatistics,
  DraftHistoryEntry,
  GameParticipant,
  InvitationCode
} from '../models/game.interface';
import { buildBalancedRegionRules } from '../create-game/create-game-region-rules.util';

describe('GameService', () => {
  let service: GameService;
  let querySpy: jasmine.SpyObj<GameQueryService>;
  let commandSpy: jasmine.SpyObj<GameCommandService>;

  const mockGame: Game = {
    id: 'game-1',
    name: 'Test Game',
    creatorName: 'Tester',
    maxParticipants: 4,
    status: 'CREATING',
    createdAt: '2025-01-01T00:00:00Z',
    participantCount: 1,
    canJoin: true
  };

  const draftState: DraftState = {
    gameId: 'game-1',
    status: 'NOT_STARTED',
    currentRound: 0,
    totalRounds: 1,
    currentPick: 0,
    participants: [],
    availablePlayers: [],
    rules: {
      maxPlayersPerTeam: 1,
      timeLimitPerPick: 60,
      autoPickEnabled: false,
      regionQuotas: {}
    },
    lastUpdated: '2025-01-01T00:00:00Z'
  };

  const draftHistory: DraftHistoryEntry[] = [
    {
      playerId: 'player-1',
      playerName: 'Player One',
      selectedBy: 'user-1',
      selectionTime: '2025-01-01T00:00:00Z',
      round: 1,
      pick: 1,
      autoPick: false,
      timeTaken: 12
    }
  ];

  const draftStatistics: DraftStatistics = {
    totalPicks: 1,
    autoPicks: 0,
    manualPicks: 1,
    picksByRegion: { EU: 1 },
    averagePickTime: 12,
    fastestPick: 12,
    slowestPick: 12
  };

  const participants: GameParticipant[] = [
    {
      id: 'participant-1',
      username: 'user-1',
      joinedAt: '2025-01-01T00:00:00Z'
    }
  ];

  const invitationCode: InvitationCode = {
    code: 'INVITE',
    gameId: 'game-1',
    gameName: 'Test Game',
    creatorName: 'Tester',
    currentUses: 0
  };

  beforeEach(() => {
    querySpy = jasmine.createSpyObj('GameQueryService', [
      'getAllGames',
      'getUserGames',
      'getAvailableGames',
      'getGameById',
      'getGameDetails',
      'validateInvitationCode',
      'getDraftState',
      'getDraftHistory',
      'getDraftStatistics',
      'getGameParticipants',
      'canJoinGame',
      'isGameHost',
      'getArchivedGameIds',
      'filterArchivedGames'
    ]);

    commandSpy = jasmine.createSpyObj('GameCommandService', [
      'createGame',
      'joinGame',
      'joinGameWithCode',
      'generateInvitationCode',
      'regenerateInvitationCode',
      'renameGame',
      'deleteGame',
      'startDraft',
      'finishDraft',
      'archiveGame',
      'leaveGame',
      'initializeDraft',
      'makePlayerSelection',
      'pauseDraft',
      'resumeDraft',
      'cancelDraft'
    ]);

    TestBed.configureTestingModule({
      providers: [
        GameService,
        { provide: GameQueryService, useValue: querySpy },
        { provide: GameCommandService, useValue: commandSpy }
      ]
    });

    service = TestBed.inject(GameService);
  });

  it('delegates query operations to GameQueryService', () => {
    const queryCases = [
      { method: 'getAllGames', args: [], returnValue: of([mockGame]) },
      { method: 'getUserGames', args: [], returnValue: of([mockGame]) },
      { method: 'getAvailableGames', args: [], returnValue: of([mockGame]) },
      { method: 'getGameById', args: ['game-1'], returnValue: of(mockGame) },
      { method: 'getGameDetails', args: ['game-1'], returnValue: of(mockGame) },
      { method: 'validateInvitationCode', args: ['INVITE'], returnValue: of(true) },
      { method: 'getDraftState', args: ['game-1'], returnValue: of(draftState) },
      { method: 'getDraftHistory', args: ['game-1'], returnValue: of(draftHistory) },
      { method: 'getDraftStatistics', args: ['game-1'], returnValue: of(draftStatistics) },
      { method: 'getGameParticipants', args: ['game-1'], returnValue: of(participants) },
      { method: 'canJoinGame', args: ['game-1'], returnValue: of(true) }
    ];

    queryCases.forEach(({ method, args, returnValue }) => {
      (querySpy as any)[method].and.returnValue(returnValue);

      const result = (service as any)[method](...args);

      expect((querySpy as any)[method]).toHaveBeenCalledWith(...args);
      expect(result).toBe(returnValue);
    });
  });

  it('delegates command operations to GameCommandService', () => {
    const createRequest: CreateGameRequest = {
      name: 'New Game',
      maxParticipants: 8,
      regionRules: buildBalancedRegionRules(8)
    };

    const commandCases = [
      { method: 'createGame', args: [createRequest], returnValue: of(mockGame) },
      { method: 'joinGame', args: ['game-1'], returnValue: of(true) },
      { method: 'joinGameWithCode', args: ['INVITE'], returnValue: of(mockGame) },
      { method: 'generateInvitationCode', args: ['game-1'], returnValue: of(invitationCode) },
      { method: 'regenerateInvitationCode', args: ['game-1', '24h'], returnValue: of(mockGame) },
      { method: 'renameGame', args: ['game-1', 'Renamed'], returnValue: of(mockGame) },
      { method: 'deleteGame', args: ['game-1'], returnValue: of(true) },
      { method: 'startDraft', args: ['game-1'], returnValue: of(true) },
      { method: 'finishDraft', args: ['game-1'], returnValue: of(mockGame) },
      { method: 'archiveGame', args: ['game-1'], returnValue: of(true) },
      { method: 'leaveGame', args: ['game-1'], returnValue: of(true) },
      { method: 'initializeDraft', args: ['game-1'], returnValue: of(draftState) },
      { method: 'makePlayerSelection', args: ['game-1', 'player-1'], returnValue: of(true) },
      { method: 'pauseDraft', args: ['game-1'], returnValue: of(true) },
      { method: 'resumeDraft', args: ['game-1'], returnValue: of(true) },
      { method: 'cancelDraft', args: ['game-1'], returnValue: of(true) }
    ];

    commandCases.forEach(({ method, args, returnValue }) => {
      (commandSpy as any)[method].and.returnValue(returnValue);

      const result = (service as any)[method](...args);

      expect((commandSpy as any)[method]).toHaveBeenCalledWith(...args);
      expect(result).toBe(returnValue);
    });
  });

  it('delegates sync helpers to GameQueryService', () => {
    const archivedIds = ['game-1'];

    querySpy.getArchivedGameIds.and.returnValue(archivedIds);
    querySpy.filterArchivedGames.and.returnValue([mockGame]);
    querySpy.isGameHost.and.returnValue(true);

    expect(service.getArchivedGameIds()).toBe(archivedIds);
    expect(querySpy.getArchivedGameIds).toHaveBeenCalled();

    expect(service.filterArchivedGames([mockGame])).toEqual([mockGame]);
    expect(querySpy.filterArchivedGames).toHaveBeenCalledWith([mockGame]);

    expect(service.isGameHost(mockGame, 'user-1')).toBe(true);
    expect(querySpy.isGameHost).toHaveBeenCalledWith(mockGame, 'user-1');
  });
});
