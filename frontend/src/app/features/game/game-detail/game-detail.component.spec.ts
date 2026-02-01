import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { GameDetailComponent } from './game-detail.component';
import { GameDataService } from '../services/game-data.service';
import { GameDetailActionsService } from '../services/game-detail-actions.service';
import { GameDetailPermissionsService } from '../services/game-detail-permissions.service';
import { GameDetailUIService } from '../services/game-detail-ui.service';
import { Game, GameParticipant } from '../models/game.interface';
import { NO_ERRORS_SCHEMA } from '@angular/core';

const mockGame: Game = {
  id: '1',
  name: 'Test Game',
  creatorName: 'Alice',
  maxParticipants: 5,
  status: 'CREATING',
  createdAt: new Date().toISOString(),
  participantCount: 2,
  canJoin: true,
  regionRules: { EU: 2, NAW: 3 },
};

const mockParticipants: GameParticipant[] = [
  { id: 'u1', username: 'Alice', joinedAt: new Date().toISOString(), isCreator: true },
  { id: 'u2', username: 'Bob', joinedAt: new Date().toISOString(), isCreator: false }
];

describe('GameDetailComponent', () => {
  let component: GameDetailComponent;
  let fixture: ComponentFixture<GameDetailComponent>;
  let gameDataServiceSpy: jasmine.SpyObj<GameDataService>;
  let gameDetailActionsSpy: jasmine.SpyObj<GameDetailActionsService>;
  let gameDetailPermissionsSpy: jasmine.SpyObj<GameDetailPermissionsService>;
  let gameDetailUISpy: jasmine.SpyObj<GameDetailUIService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let activatedRouteStub: any;

  beforeEach(async () => {
    gameDataServiceSpy = jasmine.createSpyObj('GameDataService', [
      'getGameById',
      'getGameParticipants',
      'validateGameData',
      'calculateGameStatistics'
    ]);
    gameDetailActionsSpy = jasmine.createSpyObj('GameDetailActionsService', [
      'startDraft',
      'archiveGame',
      'leaveGame',
      'permanentlyDeleteGame',
      'joinGame',
      'confirmArchive',
      'confirmLeave',
      'confirmDelete',
      'confirmStartDraft',
      'copyInvitationCode',
      'regenerateInvitationCode',
      'promptRegenerateCode',
      'promptRenameGame'
    ]);
    gameDetailPermissionsSpy = jasmine.createSpyObj('GameDetailPermissionsService', [
      'canStartDraft',
      'canArchiveGame',
      'canLeaveGame',
      'canDeleteGame',
      'canJoinGame',
      'canRegenerateCode',
      'canRenameGame'
    ]);
    gameDetailUISpy = jasmine.createSpyObj('GameDetailUIService', [
      'getStatusColor',
      'getStatusLabel',
      'getParticipantPercentage',
      'getGameStatistics',
      'getParticipantColor',
      'getTimeAgo',
      'getCreator',
      'getNonCreatorParticipants',
      'getParticipantStatusIcon',
      'getParticipantStatusColor',
      'getParticipantStatusLabel',
      'getInvitationCodeExpiry'
    ]);
    gameDetailUISpy.getNonCreatorParticipants.and.returnValue([]);
    gameDetailUISpy.getCreator.and.returnValue(null);
    gameDetailUISpy.getStatusColor.and.returnValue('primary');
    gameDetailUISpy.getStatusLabel.and.returnValue('status');
    gameDetailUISpy.getParticipantPercentage.and.returnValue(0);
    gameDetailUISpy.getParticipantColor.and.returnValue('primary');
    gameDetailUISpy.getTimeAgo.and.returnValue('now');
    gameDetailUISpy.getParticipantStatusIcon.and.returnValue('person');
    gameDetailUISpy.getParticipantStatusColor.and.returnValue('primary');
    gameDetailUISpy.getParticipantStatusLabel.and.returnValue('Participant');
    gameDetailUISpy.getInvitationCodeExpiry.and.returnValue('');
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    activatedRouteStub = { params: of({ id: '1' }) };

    gameDataServiceSpy.getGameById.and.returnValue(of(mockGame));
    gameDataServiceSpy.getGameParticipants.and.returnValue(of(mockParticipants));
    gameDataServiceSpy.validateGameData.and.returnValue({ isValid: true, errors: [] });
    gameDataServiceSpy.calculateGameStatistics.and.callFake((game: Game) => ({
      fillPercentage: game.maxParticipants ? Math.round((game.participantCount / game.maxParticipants) * 100) : 0,
      availableSlots: game.maxParticipants - game.participantCount,
      isNearlyFull: false,
      canAcceptMoreParticipants: game.canJoin && game.participantCount < game.maxParticipants
    }));
    gameDetailPermissionsSpy.canStartDraft.and.returnValue(false);
    gameDetailPermissionsSpy.canArchiveGame.and.returnValue(false);
    gameDetailPermissionsSpy.canLeaveGame.and.returnValue(false);
    gameDetailPermissionsSpy.canDeleteGame.and.returnValue(false);
    gameDetailPermissionsSpy.canJoinGame.and.returnValue(false);
    gameDetailPermissionsSpy.canRegenerateCode.and.returnValue(false);
    gameDetailPermissionsSpy.canRenameGame.and.returnValue(false);

    TestBed.configureTestingModule({
      imports: [GameDetailComponent],
      providers: [
        { provide: GameDataService, useValue: gameDataServiceSpy },
        { provide: GameDetailActionsService, useValue: gameDetailActionsSpy },
        { provide: GameDetailPermissionsService, useValue: gameDetailPermissionsSpy },
        { provide: GameDetailUIService, useValue: gameDetailUISpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: activatedRouteStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    });

    await TestBed.compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GameDetailComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load game details on init', fakeAsync(() => {
    gameDataServiceSpy.getGameById.and.returnValue(of(mockGame));
    gameDataServiceSpy.getGameParticipants.and.returnValue(of(mockParticipants));
    fixture.detectChanges();
    tick();
    expect(component.game).toEqual(mockGame);
    expect(component.participants).toEqual(mockParticipants);
    expect(gameDataServiceSpy.getGameById).toHaveBeenCalledWith('1');
    expect(gameDataServiceSpy.getGameParticipants).toHaveBeenCalledWith('1');
  }));

  it('should handle error when loading game details', fakeAsync(() => {
    gameDataServiceSpy.getGameById.and.returnValue(throwError(() => new Error('Erreur')));
    gameDataServiceSpy.getGameParticipants.and.returnValue(throwError(() => new Error('Erreur')));
    fixture.detectChanges();
    tick();
    expect(component.error).toBeTruthy();
    expect(component.participantsError).toBeNull();
    expect(component.loading).toBe(false);
  }));

  it('should not load participants when loading game details fails', fakeAsync(() => {
    gameDataServiceSpy.getGameById.and.returnValue(throwError(() => new Error('Erreur')));

    fixture.detectChanges();
    tick();

    expect(gameDataServiceSpy.getGameParticipants).not.toHaveBeenCalled();
  }));

  it('should allow joining a game', fakeAsync(() => {
    gameDataServiceSpy.getGameById.and.returnValue(of(mockGame));
    gameDataServiceSpy.getGameParticipants.and.returnValue(of(mockParticipants));
    fixture.detectChanges();
    tick();
    component.joinGame();
    tick();
    expect(gameDetailActionsSpy.joinGame).toHaveBeenCalledWith('1', jasmine.any(Function));
  }));

  it('should handle error when joining a game', fakeAsync(() => {
    gameDataServiceSpy.getGameById.and.returnValue(of(mockGame));
    gameDataServiceSpy.getGameParticipants.and.returnValue(of(mockParticipants));
    fixture.detectChanges();
    tick();
    component.joinGame();
    tick();
    expect(gameDetailActionsSpy.joinGame).toHaveBeenCalledWith('1', jasmine.any(Function));
  }));

  it('should call permanentlyDeleteGame when deleteGame is invoked', fakeAsync(() => {
    gameDataServiceSpy.getGameById.and.returnValue(of(mockGame));
    gameDataServiceSpy.getGameParticipants.and.returnValue(of(mockParticipants));
    fixture.detectChanges();
    tick();
    component.deleteGame();
    expect(gameDetailActionsSpy.permanentlyDeleteGame).toHaveBeenCalledWith('1');
  }));

  it('should call startDraft action', fakeAsync(() => {
    gameDataServiceSpy.getGameById.and.returnValue(of(mockGame));
    gameDataServiceSpy.getGameParticipants.and.returnValue(of(mockParticipants));
    fixture.detectChanges();
    tick();
    component.startDraft();
    expect(gameDetailActionsSpy.startDraft).toHaveBeenCalledWith('1', jasmine.any(Function));
  }));

  it('should display participants with correct status', fakeAsync(() => {
    gameDataServiceSpy.getGameById.and.returnValue(of(mockGame));
    gameDataServiceSpy.getGameParticipants.and.returnValue(of(mockParticipants));
    fixture.detectChanges();
    tick();
    expect(component.participants.length).toBe(2);
    expect(component.participants[0].username).toBe('Alice');
    expect(component.participants[1].isCreator).toBeFalse();
  }));

  it('should delegate status color to UI service', () => {
    gameDetailUISpy.getStatusColor.and.returnValue('primary');
    expect(component.getStatusColor('CREATING')).toBe('primary');
    expect(gameDetailUISpy.getStatusColor).toHaveBeenCalledWith('CREATING');
  });

  it('should delegate status label to UI service', () => {
    gameDetailUISpy.getStatusLabel.and.returnValue('status-label');
    expect(component.getStatusLabel('CREATING')).toBe('status-label');
    expect(gameDetailUISpy.getStatusLabel).toHaveBeenCalledWith('CREATING');
  });

  it('should delegate participant percentage to UI service', () => {
    component.game = { ...mockGame, participantCount: 2, maxParticipants: 5 };
    gameDetailUISpy.getParticipantPercentage.and.returnValue(40);
    expect(component.getParticipantPercentage()).toBe(40);
    expect(gameDetailUISpy.getParticipantPercentage).toHaveBeenCalledWith(component.game);
  });

  it('should delegate participant color to UI service', () => {
    component.game = { ...mockGame, participantCount: 1, maxParticipants: 5 };
    gameDetailUISpy.getParticipantColor.and.returnValue('primary');
    expect(component.getParticipantColor()).toBe('primary');
    expect(gameDetailUISpy.getParticipantColor).toHaveBeenCalledWith(component.game);
  });

  it('should delegate time ago to UI service', () => {
    const now = new Date().toISOString();
    gameDetailUISpy.getTimeAgo.and.returnValue('just-now');
    expect(component.getTimeAgo(now)).toBe('just-now');
    expect(gameDetailUISpy.getTimeAgo).toHaveBeenCalledWith(now);
  });

  it('should navigate back to games list', () => {
    component.onBack();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/games']);
  });

  it('should delegate creator lookup to UI service', () => {
    const creator = { id: 'u1', username: 'Alice', joinedAt: '', isCreator: true };
    component.participants = [creator];
    gameDetailUISpy.getCreator.and.returnValue(creator);
    expect(component.getCreator()).toEqual(creator);
    expect(gameDetailUISpy.getCreator).toHaveBeenCalledWith(component.participants);
  });

  it('should delegate non-creator lookup to UI service', () => {
    const nonCreators = [{ id: 'u2', username: 'Bob', joinedAt: '', isCreator: false }];
    component.participants = [
      { id: 'u1', username: 'Alice', joinedAt: '', isCreator: true },
      nonCreators[0]
    ];
    gameDetailUISpy.getNonCreatorParticipants.and.returnValue(nonCreators);
    expect(component.getNonCreatorParticipants()).toEqual(nonCreators);
    expect(gameDetailUISpy.getNonCreatorParticipants).toHaveBeenCalledWith(component.participants);
  });

  it('should not set page error when loading participants fails', fakeAsync(() => {
    gameDataServiceSpy.getGameById.and.returnValue(of(mockGame));
    gameDataServiceSpy.getGameParticipants.and.returnValue(throwError(() => new Error('Erreur')));
    fixture.detectChanges();
    tick();

    expect(component.game).toEqual(mockGame);
    expect(component.error).toBeNull();
  }));

  it('should retry loading details and participants when retryLoad is called', fakeAsync(() => {
    gameDataServiceSpy.getGameById.and.returnValues(of(mockGame), of(mockGame));
    gameDataServiceSpy.getGameParticipants.and.returnValues(
      throwError(() => new Error('Accès interdit')),
      of(mockParticipants)
    );

    fixture.detectChanges();
    tick();
    expect(component.error).toBeNull();
    expect(component.participantsError).toBeTruthy();

    component.retryLoad();
    tick();

    expect(gameDataServiceSpy.getGameById).toHaveBeenCalledTimes(2);
    expect(gameDataServiceSpy.getGameParticipants).toHaveBeenCalledTimes(2);
    expect(component.error).toBeNull();
    expect(component.participantsError).toBeNull();
    expect(component.participants).toEqual(mockParticipants);
  }));

  it('should call confirmDelete action', () => {
    component.gameId = '1';
    component.confirmDelete();
    expect(gameDetailActionsSpy.confirmDelete).toHaveBeenCalledWith('1');
  });

  it('should call confirmStartDraft action', () => {
    component.gameId = '1';
    component.confirmStartDraft();
    expect(gameDetailActionsSpy.confirmStartDraft).toHaveBeenCalledWith('1', jasmine.any(Function));
  });

  it('should delegate participant status icon to UI service', () => {
    const participant = { id: 'u1', username: 'Alice', joinedAt: '', isCreator: true } as any;
    gameDetailUISpy.getParticipantStatusIcon.and.returnValue('star');
    expect(component.getParticipantStatusIcon(participant)).toBe('star');
    expect(gameDetailUISpy.getParticipantStatusIcon).toHaveBeenCalledWith(participant);
  });

  it('should delegate participant status color to UI service', () => {
    const participant = { id: 'u2', username: 'Bob', joinedAt: '', isCreator: false } as any;
    gameDetailUISpy.getParticipantStatusColor.and.returnValue('primary');
    expect(component.getParticipantStatusColor(participant)).toBe('primary');
    expect(gameDetailUISpy.getParticipantStatusColor).toHaveBeenCalledWith(participant);
  });

  it('should delegate participant status label to UI service', () => {
    const participant = { id: 'u2', username: 'Bob', joinedAt: '', isCreator: false } as any;
    gameDetailUISpy.getParticipantStatusLabel.and.returnValue('Participant');
    expect(component.getParticipantStatusLabel(participant)).toBe('Participant');
    expect(gameDetailUISpy.getParticipantStatusLabel).toHaveBeenCalledWith(participant);
  });

  it('should delegate canStartDraft to permissions service', () => {
    component.game = { ...mockGame };
    gameDetailPermissionsSpy.canStartDraft.and.returnValue(true);
    expect(component.canStartDraft()).toBeTrue();
    expect(gameDetailPermissionsSpy.canStartDraft).toHaveBeenCalledWith(component.game);
  });

  it('should delegate canDeleteGame to permissions service', () => {
    component.game = { ...mockGame };
    gameDetailPermissionsSpy.canDeleteGame.and.returnValue(false);
    expect(component.canDeleteGame()).toBeFalse();
    expect(gameDetailPermissionsSpy.canDeleteGame).toHaveBeenCalledWith(component.game);
  });

  it('should delegate canJoinGame to permissions service', () => {
    component.game = { ...mockGame };
    gameDetailPermissionsSpy.canJoinGame.and.returnValue(true);
    expect(component.canJoinGame()).toBeTrue();
    expect(gameDetailPermissionsSpy.canJoinGame).toHaveBeenCalledWith(component.game);
  });
}); 


