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
import { UserGamesStore } from '../../../core/services/user-games.store';
import { UiErrorFeedbackService } from '../../../core/services/ui-error-feedback.service';
import { LoggerService } from '../../../core/services/logger.service';

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
  let userGamesStoreSpy: jasmine.SpyObj<UserGamesStore>;
  let uiFeedbackSpy: jasmine.SpyObj<UiErrorFeedbackService>;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
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
      'canSeeDeleteGameAction',
      'getDeleteRestrictionReasonKey',
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
    userGamesStoreSpy = jasmine.createSpyObj('UserGamesStore', ['removeGame', 'refreshGames']);
    userGamesStoreSpy.refreshGames.and.returnValue(of([mockGame]));
    uiFeedbackSpy = jasmine.createSpyObj('UiErrorFeedbackService', ['showError']);
    loggerSpy = jasmine.createSpyObj('LoggerService', ['debug', 'info', 'warn', 'error']);
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
    gameDetailPermissionsSpy.canSeeDeleteGameAction.and.returnValue(false);
    gameDetailPermissionsSpy.getDeleteRestrictionReasonKey.and.returnValue(null);
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
        { provide: UserGamesStore, useValue: userGamesStoreSpy },
        { provide: UiErrorFeedbackService, useValue: uiFeedbackSpy },
        { provide: LoggerService, useValue: loggerSpy },
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

  it('should redirect when game is not present in refreshed user games', fakeAsync(() => {
    userGamesStoreSpy.refreshGames.and.returnValue(of([]));

    fixture.detectChanges();
    tick();

    expect(gameDataServiceSpy.getGameById).not.toHaveBeenCalled();
    expect(userGamesStoreSpy.removeGame).toHaveBeenCalledWith('1');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/games']);
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

  it('should remove game from store and redirect when game detail returns 404', fakeAsync(() => {
    const notFoundError = Object.assign(new Error('Ressource non trouvée'), { status: 404 });
    gameDataServiceSpy.getGameById.and.returnValue(throwError(() => notFoundError));

    fixture.detectChanges();
    tick();

    expect(userGamesStoreSpy.removeGame).toHaveBeenCalledWith('1');
    expect(uiFeedbackSpy.showError).toHaveBeenCalledWith(null, 'games.detail.gameUnavailable', { duration: 5000 });
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/games']);
    expect(component.error).toBeNull();
  }));

  it('should not load participants when loading game details fails', fakeAsync(() => {
    gameDataServiceSpy.getGameById.and.returnValue(throwError(() => new Error('Erreur')));

    fixture.detectChanges();
    tick();

    expect(gameDataServiceSpy.getGameParticipants).not.toHaveBeenCalled();
  }));

  it('should log warning when game data validation fails', fakeAsync(() => {
    gameDataServiceSpy.validateGameData.and.returnValue({ isValid: false, errors: ['invalid'] });

    fixture.detectChanges();
    tick();

    expect(loggerSpy.warn).toHaveBeenCalledWith(
      'GameDetailComponent: game data validation failed',
      jasmine.objectContaining({
        gameId: '1',
        errors: ['invalid']
      })
    );
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
    expect(gameDetailActionsSpy.startDraft).toHaveBeenCalledWith(
      '1',
      jasmine.any(Function),
      jasmine.any(Function)
    );
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
    expect(loggerSpy.error).toHaveBeenCalledWith(
      'GameDetailComponent: failed to load participants',
      jasmine.objectContaining({
        gameId: '1'
      })
    );
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
    expect(gameDetailActionsSpy.confirmStartDraft).toHaveBeenCalledWith(
      '1',
      jasmine.any(Function),
      jasmine.any(Function)
    );
  });

  it('should set draft pending flag and reset it when settled callback is called', () => {
    component.gameId = '1';
    component.isStartingDraft = false;
    gameDetailActionsSpy.confirmStartDraft.and.callFake(
      (_gameId: string, _onSuccess?: () => void, onSettled?: () => void) => {
        expect(component.isStartingDraft).toBeTrue();
        onSettled?.();
      }
    );

    component.confirmStartDraft();

    expect(component.isStartingDraft).toBeFalse();
  });

  it('should disable start draft action while request is pending', () => {
    component.loading = false;
    component.isStartingDraft = true;

    expect(component.isStartDraftActionDisabled()).toBeTrue();
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

  it('should delegate canShowDeleteGameAction to permissions service', () => {
    component.game = { ...mockGame };
    gameDetailPermissionsSpy.canSeeDeleteGameAction.and.returnValue(true);

    expect(component.canShowDeleteGameAction()).toBeTrue();
    expect(gameDetailPermissionsSpy.canSeeDeleteGameAction).toHaveBeenCalledWith(component.game);
  });

  it('should delegate getDeleteRestrictionReasonKey to permissions service', () => {
    component.game = { ...mockGame, status: 'ACTIVE' };
    gameDetailPermissionsSpy.getDeleteRestrictionReasonKey.and.returnValue('games.detail.deleteDisabledStatus');

    expect(component.getDeleteRestrictionReasonKey()).toBe('games.detail.deleteDisabledStatus');
    expect(gameDetailPermissionsSpy.getDeleteRestrictionReasonKey).toHaveBeenCalledWith(component.game);
  });

  it('should return default tooltip key when no restriction exists', () => {
    gameDetailPermissionsSpy.getDeleteRestrictionReasonKey.and.returnValue(null);

    expect(component.getDeleteTooltipKey()).toBe('games.detail.deleteTooltip');
  });

  it('should return restriction tooltip key when delete is restricted', () => {
    gameDetailPermissionsSpy.getDeleteRestrictionReasonKey.and.returnValue('games.detail.deleteDisabledStatus');

    expect(component.getDeleteTooltipKey()).toBe('games.detail.deleteDisabledStatus');
  });

  it('should delegate canJoinGame to permissions service', () => {
    component.game = { ...mockGame };
    gameDetailPermissionsSpy.canJoinGame.and.returnValue(true);
    expect(component.canJoinGame()).toBeTrue();
    expect(gameDetailPermissionsSpy.canJoinGame).toHaveBeenCalledWith(component.game);
  });

  it('should delegate copy invitation code to actions service when code exists', () => {
    component.game = { ...mockGame, invitationCode: 'INVITE123' };

    component.copyInvitationCode();

    expect(gameDetailActionsSpy.copyInvitationCode).toHaveBeenCalledWith('INVITE123');
  });

  it('should not call copy invitation code action when code is missing', () => {
    component.game = { ...mockGame, invitationCode: undefined };

    component.copyInvitationCode();

    expect(gameDetailActionsSpy.copyInvitationCode).not.toHaveBeenCalled();
  });

  it('should show generate invitation code button when code is missing and user can regenerate', fakeAsync(() => {
    const gameWithoutCode: Game = { ...mockGame, invitationCode: undefined };
    gameDataServiceSpy.getGameById.and.returnValue(of(gameWithoutCode));
    gameDataServiceSpy.getGameParticipants.and.returnValue(of(mockParticipants));
    gameDetailPermissionsSpy.canRegenerateCode.and.returnValue(true);

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const generateButton = compiled.querySelector('.generate-code-btn');
    expect(generateButton).toBeTruthy();
    expect(generateButton?.textContent).toContain('games.detail.generateCode');
  }));

  it('should render disabled delete button with explicit reason for host when status blocks delete', fakeAsync(() => {
    const nonDeletableHostGame: Game = { ...mockGame, status: 'ACTIVE' };
    gameDataServiceSpy.getGameById.and.returnValue(of(nonDeletableHostGame));
    gameDataServiceSpy.getGameParticipants.and.returnValue(of(mockParticipants));
    gameDetailPermissionsSpy.canSeeDeleteGameAction.and.returnValue(true);
    gameDetailPermissionsSpy.canDeleteGame.and.returnValue(false);
    gameDetailPermissionsSpy.getDeleteRestrictionReasonKey.and.returnValue('games.detail.deleteDisabledStatus');

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const deleteButton = compiled.querySelector('.delete-game-btn') as HTMLButtonElement | null;
    const reason = compiled.querySelector('.delete-disabled-reason');

    expect(deleteButton).toBeTruthy();
    expect(deleteButton?.disabled).toBeTrue();
    expect(reason?.textContent).toContain('games.detail.deleteDisabledStatus');
  }));

  it('should hide delete action for non host users', fakeAsync(() => {
    gameDataServiceSpy.getGameById.and.returnValue(of(mockGame));
    gameDataServiceSpy.getGameParticipants.and.returnValue(of(mockParticipants));
    gameDetailPermissionsSpy.canSeeDeleteGameAction.and.returnValue(false);
    gameDetailPermissionsSpy.canDeleteGame.and.returnValue(false);
    gameDetailPermissionsSpy.getDeleteRestrictionReasonKey.and.returnValue('games.detail.deleteDisabledNotHost');

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.delete-game-btn')).toBeNull();
    expect(compiled.querySelector('.delete-disabled-reason')).toBeNull();
  }));

  it('should render enabled delete button for host when delete is allowed', fakeAsync(() => {
    const deletableHostGame: Game = { ...mockGame, status: 'CREATING' };
    gameDataServiceSpy.getGameById.and.returnValue(of(deletableHostGame));
    gameDataServiceSpy.getGameParticipants.and.returnValue(of(mockParticipants));
    gameDetailPermissionsSpy.canSeeDeleteGameAction.and.returnValue(true);
    gameDetailPermissionsSpy.canDeleteGame.and.returnValue(true);
    gameDetailPermissionsSpy.getDeleteRestrictionReasonKey.and.returnValue(null);

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const deleteButton = compiled.querySelector('.delete-game-btn') as HTMLButtonElement | null;

    expect(deleteButton).toBeTruthy();
    expect(deleteButton?.disabled).toBeFalse();
    expect(compiled.querySelector('.delete-disabled-reason')).toBeNull();
  }));
});


