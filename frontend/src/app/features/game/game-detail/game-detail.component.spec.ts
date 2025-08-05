import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { GameDetailComponent } from './game-detail.component';
import { GameService } from '../services/game.service';
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
  let gameServiceSpy: jasmine.SpyObj<GameService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let activatedRouteStub: any;

  beforeEach(async () => {
    gameServiceSpy = jasmine.createSpyObj('GameService', [
      'getGameById', 'getGameParticipants', 'deleteGame', 'startDraft', 'joinGame', 'generateInvitationCode', 'getDraftState'
    ]);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    activatedRouteStub = { snapshot: { paramMap: { get: () => '1' } } };

    await TestBed.configureTestingModule({
      declarations: [GameDetailComponent],
      providers: [
        { provide: GameService, useValue: gameServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: ActivatedRoute, useValue: activatedRouteStub },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GameDetailComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load game details on init', fakeAsync(() => {
    gameServiceSpy.getGameById.and.returnValue(of(mockGame));
    gameServiceSpy.getGameParticipants.and.returnValue(of(mockParticipants));
    fixture.detectChanges();
    tick();
    expect(component.game).toEqual(mockGame);
    expect(component.participants).toEqual(mockParticipants);
    expect(gameServiceSpy.getGameById).toHaveBeenCalledWith('1');
    expect(gameServiceSpy.getGameParticipants).toHaveBeenCalledWith('1');
  }));

  it('should handle error when loading game details', fakeAsync(() => {
    gameServiceSpy.getGameById.and.returnValue(throwError(() => new Error('Erreur')));
    fixture.detectChanges();
    tick();
    expect(component.error).toBeTruthy();
    expect(snackBarSpy.open).toHaveBeenCalled();
  }));

  it('should allow joining a game', fakeAsync(() => {
    gameServiceSpy.getGameById.and.returnValue(of(mockGame));
    gameServiceSpy.getGameParticipants.and.returnValue(of(mockParticipants));
    gameServiceSpy.joinGame.and.returnValue(of(true));
    fixture.detectChanges();
    tick();
    component.joinGame();
    tick();
    expect(gameServiceSpy.joinGame).toHaveBeenCalledWith('1');
    expect(snackBarSpy.open).toHaveBeenCalledWith(jasmine.stringMatching(/rejoint/i), 'Fermer', jasmine.any(Object));
  }));

  it('should handle error when joining a game', fakeAsync(() => {
    gameServiceSpy.getGameById.and.returnValue(of(mockGame));
    gameServiceSpy.getGameParticipants.and.returnValue(of(mockParticipants));
    gameServiceSpy.joinGame.and.returnValue(throwError(() => new Error('Erreur')));
    fixture.detectChanges();
    tick();
    component.joinGame();
    tick();
    expect(component.error).toBeTruthy();
    expect(snackBarSpy.open).toHaveBeenCalled();
  }));

  it('should allow deleting a game', fakeAsync(() => {
    gameServiceSpy.getGameById.and.returnValue(of(mockGame));
    gameServiceSpy.getGameParticipants.and.returnValue(of(mockParticipants));
    gameServiceSpy.deleteGame.and.returnValue(of(true));
    fixture.detectChanges();
    tick();
    component.deleteGame();
    tick();
    expect(gameServiceSpy.deleteGame).toHaveBeenCalledWith('1');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/games']);
    expect(snackBarSpy.open).toHaveBeenCalledWith(jasmine.stringMatching(/supprimée/i), 'Fermer', jasmine.any(Object));
  }));

  it('should handle error when deleting a game', fakeAsync(() => {
    gameServiceSpy.getGameById.and.returnValue(of(mockGame));
    gameServiceSpy.getGameParticipants.and.returnValue(of(mockParticipants));
    gameServiceSpy.deleteGame.and.returnValue(throwError(() => new Error('Erreur')));
    fixture.detectChanges();
    tick();
    component.deleteGame();
    tick();
    expect(component.error).toBeTruthy();
    expect(snackBarSpy.open).toHaveBeenCalled();
  }));

  it('should allow starting a draft', fakeAsync(() => {
    gameServiceSpy.getGameById.and.returnValue(of(mockGame));
    gameServiceSpy.getGameParticipants.and.returnValue(of(mockParticipants));
    gameServiceSpy.startDraft.and.returnValue(of(true));
    fixture.detectChanges();
    tick();
    component.startDraft();
    tick();
    expect(gameServiceSpy.startDraft).toHaveBeenCalledWith('1');
    expect(snackBarSpy.open).toHaveBeenCalledWith(jasmine.stringMatching(/draft/i), 'Fermer', jasmine.any(Object));
  }));

  it('should handle error when starting a draft', fakeAsync(() => {
    gameServiceSpy.getGameById.and.returnValue(of(mockGame));
    gameServiceSpy.getGameParticipants.and.returnValue(of(mockParticipants));
    gameServiceSpy.startDraft.and.returnValue(throwError(() => new Error('Erreur')));
    fixture.detectChanges();
    tick();
    component.startDraft();
    tick();
    expect(component.error).toBeTruthy();
    expect(snackBarSpy.open).toHaveBeenCalled();
  }));

  it('should display participants with correct status', fakeAsync(() => {
    gameServiceSpy.getGameById.and.returnValue(of(mockGame));
    gameServiceSpy.getGameParticipants.and.returnValue(of(mockParticipants));
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
    expect(component.getStatusLabel('CREATING')).toBe('En création');
    expect(component.getStatusLabel('DRAFTING')).toBe('En draft');
    expect(component.getStatusLabel('ACTIVE')).toBe('Active');
    expect(component.getStatusLabel('FINISHED')).toBe('Terminée');
    expect(component.getStatusLabel('CANCELLED')).toBe('Annulée');
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
    spyOn(component['router'], 'navigate');
    component.onBack();
    expect(component['router'].navigate).toHaveBeenCalledWith(['/games']);
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

  it('should handle error when loading participants', () => {
    gameServiceSpy.getGameById.and.returnValue(of(mockGame));
    gameServiceSpy.getGameParticipants.and.returnValue(throwError(() => new Error('Erreur')));
    fixture.detectChanges();
    expect(component.error).toBeTruthy();
  });

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
    component.game = { ...mockGame, status: 'CREATING' };
    expect(component.canDeleteGame()).toBeTrue();
  });
  it('should return false for canDeleteGame if status is not CREATING', () => {
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