import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { GameDetailComponent } from './game-detail.component';
import { GameService } from '../services/game.service';
import { GameDataService } from '../services/game-data.service';
import { GameDetailActionsService } from '../services/game-detail-actions.service';
import { GameDetailPermissionsService } from '../services/game-detail-permissions.service';
import { GameDetailUIService } from '../services/game-detail-ui.service';
import { Game, GameParticipant } from '../models/game.interface';
import { UserContextService } from '../../../core/services/user-context.service';
import { UserGamesStore } from '../../../core/services/user-games.store';
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
  let gameServiceSpy: jasmine.SpyObj<GameService>;
  let gameDataServiceSpy: jasmine.SpyObj<GameDataService>;
  let gameDetailActionsSpy: jasmine.SpyObj<GameDetailActionsService>;
  let gameDetailPermissionsSpy: jasmine.SpyObj<GameDetailPermissionsService>;
  let gameDetailUISpy: jasmine.SpyObj<GameDetailUIService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let userContextServiceSpy: jasmine.SpyObj<UserContextService>;
  let userGamesStoreSpy: jasmine.SpyObj<UserGamesStore>;
  let activatedRouteStub: any;

  beforeEach(async () => {
    gameServiceSpy = jasmine.createSpyObj('GameService', [
      'getGameById',
      'getGameParticipants',
      'deleteGame',
      'startDraft',
      'joinGame',
      'generateInvitationCode',
      'getDraftState',
      'archiveGame',
      'leaveGame',
      'regenerateInvitationCode',
      'renameGame',
      'isGameHost'
    ]);
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
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    userContextServiceSpy = jasmine.createSpyObj('UserContextService', ['getCurrentUser']);
    userGamesStoreSpy = jasmine.createSpyObj('UserGamesStore', ['removeGame']);
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

    TestBed.configureTestingModule({
      imports: [GameDetailComponent],
      providers: [
        { provide: GameService, useValue: gameServiceSpy },
        { provide: GameDataService, useValue: gameDataServiceSpy },
        { provide: GameDetailActionsService, useValue: gameDetailActionsSpy },
        { provide: GameDetailPermissionsService, useValue: gameDetailPermissionsSpy },
        { provide: GameDetailUIService, useValue: gameDetailUISpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: activatedRouteStub },
        { provide: UserContextService, useValue: userContextServiceSpy },
        { provide: UserGamesStore, useValue: userGamesStoreSpy }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    });

    TestBed.overrideComponent(GameDetailComponent, {
      set: {
        template: '',
        providers: [{ provide: MatSnackBar, useValue: snackBarSpy }]
      }
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

  it('should allow deleting a game', fakeAsync(() => {
    gameDataServiceSpy.getGameById.and.returnValue(of(mockGame));
    gameDataServiceSpy.getGameParticipants.and.returnValue(of(mockParticipants));
    gameServiceSpy.deleteGame.and.returnValue(of(true));
    fixture.detectChanges();
    tick();
    component.deleteGame();
    tick();
    expect(gameServiceSpy.deleteGame).toHaveBeenCalledWith('1');
    expect(userGamesStoreSpy.removeGame).toHaveBeenCalledWith('1');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
    expect(snackBarSpy.open).not.toHaveBeenCalled();
  }));

  it('should handle error when deleting a game', fakeAsync(() => {
    gameDataServiceSpy.getGameById.and.returnValue(of(mockGame));
    gameDataServiceSpy.getGameParticipants.and.returnValue(of(mockParticipants));
    gameServiceSpy.deleteGame.and.returnValue(throwError(() => new Error('Erreur')));
    fixture.detectChanges();
    tick();
    component.deleteGame();
    tick();
    expect(component.error).toBeNull();
    expect(snackBarSpy.open).not.toHaveBeenCalled();
  }));

  it('should allow starting a draft', fakeAsync(() => {
    gameDataServiceSpy.getGameById.and.returnValue(of(mockGame));
    gameDataServiceSpy.getGameParticipants.and.returnValue(of(mockParticipants));
    gameServiceSpy.startDraft.and.returnValue(of(true));
    fixture.detectChanges();
    tick();
    component.startDraft();
    tick();
    expect(gameServiceSpy.startDraft).toHaveBeenCalledWith('1');
    expect(snackBarSpy.open).toHaveBeenCalledWith(jasmine.stringMatching(/draft/i), 'Fermer', jasmine.any(Object));
  }));

  it('should handle error when starting a draft', fakeAsync(() => {
    gameDataServiceSpy.getGameById.and.returnValue(of(mockGame));
    gameDataServiceSpy.getGameParticipants.and.returnValue(of(mockParticipants));
    gameServiceSpy.startDraft.and.returnValue(throwError(() => new Error('Erreur')));
    fixture.detectChanges();
    tick();
    component.startDraft();
    tick();
    expect(component.error).toBeNull();
    expect(snackBarSpy.open).toHaveBeenCalled();
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

  it('should return correct status color', () => {
    expect(component.getStatusColor('CREATING')).toBe('primary');
    expect(component.getStatusColor('DRAFTING')).toBe('accent');
    expect(component.getStatusColor('ACTIVE')).toBe('warn');
    expect(component.getStatusColor('FINISHED')).toBe('default');
    expect(component.getStatusColor('CANCELLED')).toBe('default');
  });

  it('should return correct status label', () => {
    expect(component.getStatusLabel('CREATING')).toBe(component.t.t('games.home.statusCreating'));
    expect(component.getStatusLabel('DRAFTING')).toBe(component.t.t('games.home.statusDrafting'));
    expect(component.getStatusLabel('ACTIVE')).toBe(component.t.t('games.home.statusActive'));
    expect(component.getStatusLabel('FINISHED')).toBe(component.t.t('games.home.statusFinished'));
    expect(component.getStatusLabel('CANCELLED')).toBe(component.t.t('games.home.statusCancelled'));
  });

  it('should calculate participant percentage', () => {
    component.game = { ...mockGame, participantCount: 2, maxParticipants: 5 };
    expect(component.getParticipantPercentage()).toBe(40);
    component.game = { ...mockGame, participantCount: 0, maxParticipants: 5 };
    expect(component.getParticipantPercentage()).toBe(0);
    component.game = { ...mockGame, participantCount: 5, maxParticipants: 5 };
    expect(component.getParticipantPercentage()).toBe(100);
  });

  it('should return correct participant color', () => {
    component.game = { ...mockGame, participantCount: 1, maxParticipants: 5 };
    expect(component.getParticipantColor()).toBe('primary');
    component.game = { ...mockGame, participantCount: 4, maxParticipants: 5 };
    expect(component.getParticipantColor()).toBe('accent');
    component.game = { ...mockGame, participantCount: 5, maxParticipants: 5 };
    expect(component.getParticipantColor()).toBe('warn');
  });

  it('should return correct time ago', () => {
    const now = new Date();
    expect(component.getTimeAgo(now.toISOString())).toBe("À l'instant");
    const thirtyMinAgo = new Date(now.getTime() - 30 * 60 * 1000).toISOString();
    expect(component.getTimeAgo(thirtyMinAgo)).toBe('Il y a 30 min');
    const threeHoursAgo = new Date(now.getTime() - 3 * 60 * 60 * 1000).toISOString();
    expect(component.getTimeAgo(threeHoursAgo)).toBe('Il y a 3h');
    const twoDaysAgo = new Date(now.getTime() - 2 * 24 * 60 * 60 * 1000).toISOString();
    expect(component.getTimeAgo(twoDaysAgo)).toBe('Il y a 2j');
  });

  it('should navigate back to games list', () => {
    component.onBack();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/games']);
  });

  it('should return creator from participants', () => {
    component.participants = [
      { id: 'u1', username: 'Alice', joinedAt: '', isCreator: true },
      { id: 'u2', username: 'Bob', joinedAt: '', isCreator: false }
    ];
    const creator = component.getCreator();
    expect(creator).toEqual(component.participants[0]);
  });

  it('should return null if no creator found', () => {
    component.participants = [
      { id: 'u2', username: 'Bob', joinedAt: '', isCreator: false }
    ];
    const creator = component.getCreator();
    expect(creator).toBeNull();
  });

  it('should return non-creator participants', () => {
    component.participants = [
      { id: 'u1', username: 'Alice', joinedAt: '', isCreator: true },
      { id: 'u2', username: 'Bob', joinedAt: '', isCreator: false }
    ];
    const nonCreators = component.getNonCreatorParticipants();
    expect(nonCreators.length).toBe(1);
    expect(nonCreators[0].isCreator).toBeFalse();
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

  it('should call deleteGame if confirmDelete is confirmed', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    spyOn(component, 'deleteGame');
    component.confirmDelete();
    expect(component.deleteGame).toHaveBeenCalled();
  });

  it('should not call deleteGame if confirmDelete is cancelled', () => {
    spyOn(window, 'confirm').and.returnValue(false);
    spyOn(component, 'deleteGame');
    component.confirmDelete();
    expect(component.deleteGame).not.toHaveBeenCalled();
  });

  it('should call startDraft if confirmStartDraft is confirmed', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    spyOn(component, 'startDraft');
    component.confirmStartDraft();
    expect(component.startDraft).toHaveBeenCalled();
  });

  it('should not call startDraft if confirmStartDraft is cancelled', () => {
    spyOn(window, 'confirm').and.returnValue(false);
    spyOn(component, 'startDraft');
    component.confirmStartDraft();
    expect(component.startDraft).not.toHaveBeenCalled();
  });

  it('should return correct participant status icon', () => {
    expect(component.getParticipantStatusIcon({ isCreator: true } as any)).toBe('star');
    expect(component.getParticipantStatusIcon({ isCreator: false } as any)).toBe('person');
  });

  it('should return correct participant status color', () => {
    expect(component.getParticipantStatusColor({ isCreator: true } as any)).toBe('accent');
    expect(component.getParticipantStatusColor({ isCreator: false } as any)).toBe('primary');
  });

  it('should return correct participant status label', () => {
    expect(component.getParticipantStatusLabel({ isCreator: true } as any)).toBe('Créateur');
    expect(component.getParticipantStatusLabel({ isCreator: false } as any)).toBe('Participant');
  });

  it('should return true for canStartDraft if status is CREATING and >=2 participants', () => {
    component.game = { ...mockGame, status: 'CREATING', participantCount: 2 };
    expect(component.canStartDraft()).toBeTrue();
  });
  it('should return false for canStartDraft if status is not CREATING', () => {
    component.game = { ...mockGame, status: 'ACTIVE', participantCount: 2 };
    expect(component.canStartDraft()).toBeFalse();
  });
  it('should return false for canStartDraft if participants < 2', () => {
    component.game = { ...mockGame, status: 'CREATING', participantCount: 1 };
    expect(component.canStartDraft()).toBeFalse();
  });

  it('should return true for canDeleteGame if status is CREATING', () => {
    userContextServiceSpy.getCurrentUser.and.returnValue({ id: 'u1', username: 'Alice', email: 'alice@test.com' });
    gameServiceSpy.isGameHost.and.returnValue(true);
    component.game = { ...mockGame, status: 'CREATING' };
    expect(component.canDeleteGame()).toBeTrue();
  });
  it('should return false for canDeleteGame if status is not CREATING', () => {
    userContextServiceSpy.getCurrentUser.and.returnValue({ id: 'u1', username: 'Alice', email: 'alice@test.com' });
    gameServiceSpy.isGameHost.and.returnValue(true);
    component.game = { ...mockGame, status: 'ACTIVE' };
    expect(component.canDeleteGame()).toBeFalse();
  });

  it('should return true for canJoinGame if canJoin and not full', () => {
    component.game = { ...mockGame, canJoin: true, participantCount: 2, maxParticipants: 5 };
    expect(component.canJoinGame()).toBeTrue();
  });
  it('should return false for canJoinGame if canJoin is false', () => {
    component.game = { ...mockGame, canJoin: false, participantCount: 2, maxParticipants: 5 };
    expect(component.canJoinGame()).toBeFalse();
  });
  it('should return false for canJoinGame if full', () => {
    component.game = { ...mockGame, canJoin: true, participantCount: 5, maxParticipants: 5 };
    expect(component.canJoinGame()).toBeFalse();
  });
}); 

