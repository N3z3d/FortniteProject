import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { JoinGameComponent } from './join-game.component';
import { GameService } from '../services/game.service';
import { TranslationService } from '../../../core/services/translation.service';
import { UserGamesStore } from '../../../core/services/user-games.store';
import { Game } from '../models/game.interface';

describe('JoinGameComponent', () => {
  let component: JoinGameComponent;
  let fixture: ComponentFixture<JoinGameComponent>;
  let gameService: jasmine.SpyObj<GameService>;
  let router: jasmine.SpyObj<Router>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;
  let translationService: jasmine.SpyObj<TranslationService>;
  let userGamesStore: jasmine.SpyObj<UserGamesStore>;

  const mockGame: Game = {
    id: 'game1',
    name: 'Test Game',
    creatorName: 'User1',
    maxParticipants: 10,
    status: 'CREATING',
    createdAt: new Date().toISOString(),
    participantCount: 5,
    canJoin: true,
    regionRules: {}
  };

  beforeEach(async () => {
    gameService = jasmine.createSpyObj('GameService', ['joinGameWithCode']);
    router = jasmine.createSpyObj('Router', ['navigate']);
    const snackBarRef = jasmine.createSpyObj('MatSnackBarRef', ['onAction']);
    snackBarRef.onAction.and.returnValue(of(undefined));
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    snackBar.open.and.returnValue(snackBarRef);
    translationService = jasmine.createSpyObj('TranslationService', ['t']);
    translationService.t.and.callFake((key: string) => key);
    userGamesStore = jasmine.createSpyObj('UserGamesStore', ['refreshGames']);
    userGamesStore.refreshGames.and.returnValue(of([]));

    TestBed.configureTestingModule({
      imports: [JoinGameComponent]
    });
    TestBed.overrideComponent(JoinGameComponent, {
      set: {
        providers: [
          { provide: GameService, useValue: gameService },
          { provide: Router, useValue: router },
          { provide: MatSnackBar, useValue: snackBar },
          { provide: TranslationService, useValue: translationService },
          { provide: UserGamesStore, useValue: userGamesStore }
        ]
      }
    });
    await TestBed.compileComponents();

    fixture = TestBed.createComponent(JoinGameComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render join dialog translations namespace', () => {
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.textContent).toContain('games.joinDialog.title');
    expect(compiled.textContent).toContain('games.joinDialog.subtitle');
  });

  it('should show error when code is empty', () => {
    component.invitationCode = '';
    component.joinWithCode();

    expect(snackBar.open).toHaveBeenCalledWith(
      'games.joinDialog.enterCodeError',
      'games.joinDialog.close',
      { duration: 3000 }
    );
    expect(gameService.joinGameWithCode).not.toHaveBeenCalled();
  });

  it('should show error when code is whitespace', () => {
    component.invitationCode = '   ';
    component.joinWithCode();

    expect(snackBar.open).toHaveBeenCalled();
    expect(gameService.joinGameWithCode).not.toHaveBeenCalled();
  });

  it('should join game with valid code', fakeAsync(() => {
    component.invitationCode = 'VALID123';
    gameService.joinGameWithCode.and.returnValue(of(mockGame));

    component.joinWithCode();

    expect(component.joiningGame).toBeFalse();
    expect(gameService.joinGameWithCode).toHaveBeenCalledWith('VALID123');
    expect(snackBar.open).toHaveBeenCalled();

    tick(1000);
    expect(router.navigate).toHaveBeenCalledWith(['/games', 'game1', 'dashboard']);
  }));

  it('should trim invitation code before joining', () => {
    component.invitationCode = '  CODE123  ';
    gameService.joinGameWithCode.and.returnValue(of(mockGame));

    component.joinWithCode();

    expect(gameService.joinGameWithCode).toHaveBeenCalledWith('CODE123');
  });

  it('should normalize invitation code to uppercase before joining', () => {
    component.invitationCode = 'ab12cd';
    gameService.joinGameWithCode.and.returnValue(of(mockGame));

    component.joinWithCode();

    expect(gameService.joinGameWithCode).toHaveBeenCalledWith('AB12CD');
  });

  it('should refresh sidebar games after successful join', () => {
    component.invitationCode = 'VALID123';
    gameService.joinGameWithCode.and.returnValue(of(mockGame));

    component.joinWithCode();

    expect(userGamesStore.refreshGames).toHaveBeenCalled();
  });

  it('should handle join error', () => {
    component.invitationCode = 'INVALID';
    gameService.joinGameWithCode.and.returnValue(
      throwError(() => ({ error: { message: 'Code expired' } }))
    );

    component.joinWithCode();

    expect(component.joiningGame).toBeFalse();
    expect(snackBar.open).toHaveBeenCalledWith(
      'Code expired',
      'games.joinDialog.close',
      { duration: 5000 }
    );
  });

  it('should handle error without message', () => {
    component.invitationCode = 'INVALID';
    gameService.joinGameWithCode.and.returnValue(throwError(() => ({})));

    component.joinWithCode();

    expect(snackBar.open).toHaveBeenCalledWith(
      'games.joinDialog.invalidCode',
      'games.joinDialog.close',
      { duration: 5000 }
    );
  });

  it('should display error message from Error instance', () => {
    component.invitationCode = 'INVALID';
    gameService.joinGameWithCode.and.returnValue(throwError(() => new Error('Game is full')));

    component.joinWithCode();

    expect(snackBar.open).toHaveBeenCalledWith(
      'Game is full',
      'games.joinDialog.close',
      { duration: 5000 }
    );
  });

  it('should navigate to games on cancel', () => {
    component.cancel();

    expect(router.navigate).toHaveBeenCalledWith(['/games']);
  });

  it('should navigate to game dashboard on snackbar action', fakeAsync(() => {
    component.invitationCode = 'CODE';
    gameService.joinGameWithCode.and.returnValue(of(mockGame));

    component.joinWithCode();
    tick();

    expect(router.navigate).toHaveBeenCalledWith(['/games', 'game1', 'dashboard']);
  }));
});
