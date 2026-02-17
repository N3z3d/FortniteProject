import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { JoinGameComponent } from './join-game.component';
import { GameService } from '../services/game.service';
import { TranslationService } from '../../../core/services/translation.service';
import { UserGamesStore } from '../../../core/services/user-games.store';
import { UiErrorFeedbackService } from '../../../core/services/ui-error-feedback.service';
import { Game } from '../models/game.interface';

describe('JoinGameComponent', () => {
  let component: JoinGameComponent;
  let fixture: ComponentFixture<JoinGameComponent>;
  let gameService: jasmine.SpyObj<GameService>;
  let router: jasmine.SpyObj<Router>;
  let translationService: jasmine.SpyObj<TranslationService>;
  let userGamesStore: jasmine.SpyObj<UserGamesStore>;
  let uiFeedback: jasmine.SpyObj<UiErrorFeedbackService>;

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
    translationService = jasmine.createSpyObj('TranslationService', ['t']);
    translationService.t.and.callFake((key: string) => key);
    userGamesStore = jasmine.createSpyObj('UserGamesStore', ['refreshGames']);
    userGamesStore.refreshGames.and.returnValue(of([]));
    uiFeedback = jasmine.createSpyObj('UiErrorFeedbackService', ['showSuccessWithAction']);

    TestBed.configureTestingModule({
      imports: [JoinGameComponent]
    });
    TestBed.overrideComponent(JoinGameComponent, {
      set: {
        providers: [
          { provide: GameService, useValue: gameService },
          { provide: Router, useValue: router },
          { provide: TranslationService, useValue: translationService },
          { provide: UserGamesStore, useValue: userGamesStore },
          { provide: UiErrorFeedbackService, useValue: uiFeedback }
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
    fixture.detectChanges();

    const feedback = fixture.nativeElement.querySelector('.join-feedback');
    expect(feedback).toBeTruthy();
    expect(feedback.textContent).toContain('games.joinDialog.enterCodeError');
    expect(component.joinFeedbackMessage).toBe('games.joinDialog.enterCodeError');
    expect(uiFeedback.showSuccessWithAction).not.toHaveBeenCalled();
    expect(gameService.joinGameWithCode).not.toHaveBeenCalled();
  });

  it('should show error when code is whitespace', () => {
    component.invitationCode = '   ';
    component.joinWithCode();

    expect(component.joinFeedbackMessage).toBe('games.joinDialog.enterCodeError');
    expect(uiFeedback.showSuccessWithAction).not.toHaveBeenCalled();
    expect(gameService.joinGameWithCode).not.toHaveBeenCalled();
  });

  it('should show format error and skip API call when code contains invalid characters', () => {
    component.invitationCode = '@@';

    component.joinWithCode();

    expect(component.joinFeedbackMessage).toBe('games.joinDialog.invalidCodeFormat');
    expect(uiFeedback.showSuccessWithAction).not.toHaveBeenCalled();
    expect(gameService.joinGameWithCode).not.toHaveBeenCalled();
  });

  it('should join game with valid code', fakeAsync(() => {
    component.invitationCode = 'VALID123';
    gameService.joinGameWithCode.and.returnValue(of(mockGame));

    component.joinWithCode();

    expect(component.joiningGame).toBeFalse();
    expect(gameService.joinGameWithCode).toHaveBeenCalledWith('VALID123');
    expect(uiFeedback.showSuccessWithAction).toHaveBeenCalled();

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
    expect(component.joinFeedbackMessage).toBeNull();
  });

  it('should fallback to translated invalid code for unknown backend messages', () => {
    component.invitationCode = 'INVALID';
    gameService.joinGameWithCode.and.returnValue(
      throwError(() => ({ error: { message: 'Code expired' } }))
    );

    component.joinWithCode();

    expect(component.joiningGame).toBeFalse();
    expect(component.joinFeedbackMessage).toBe('games.joinDialog.invalidCode');
    expect(uiFeedback.showSuccessWithAction).not.toHaveBeenCalled();
  });

  it('should handle error without message', () => {
    component.invitationCode = 'INVALID';
    gameService.joinGameWithCode.and.returnValue(throwError(() => ({})));

    component.joinWithCode();

    expect(component.joinFeedbackMessage).toBe('games.joinDialog.invalidCode');
    expect(uiFeedback.showSuccessWithAction).not.toHaveBeenCalled();
  });

  it('should not expose raw technical Error messages from Error instance', () => {
    component.invitationCode = 'INVALID';
    gameService.joinGameWithCode.and.returnValue(throwError(() => new Error('Game is full')));

    component.joinWithCode();

    expect(component.joinFeedbackMessage).toBe('games.joinDialog.invalidCode');
    expect(uiFeedback.showSuccessWithAction).not.toHaveBeenCalled();
  });

  it('should map USER_ALREADY_IN_GAME to explicit already-in-game message', () => {
    component.invitationCode = 'DUPLICATE';
    gameService.joinGameWithCode.and.returnValue(
      throwError(() => ({ error: { code: 'USER_ALREADY_IN_GAME', message: 'User is already participating in this game' } }))
    );

    component.joinWithCode();

    expect(component.joinFeedbackMessage).toBe('games.joinDialog.alreadyInGame');
    expect(uiFeedback.showSuccessWithAction).not.toHaveBeenCalled();
  });

  it('should map already-in-game english message to explicit already-in-game message', () => {
    component.invitationCode = 'DUPLICATE';
    gameService.joinGameWithCode.and.returnValue(
      throwError(() => new Error('User is already participating in this game'))
    );

    component.joinWithCode();

    expect(component.joinFeedbackMessage).toBe('games.joinDialog.alreadyInGame');
    expect(uiFeedback.showSuccessWithAction).not.toHaveBeenCalled();
  });

  it('should map resource-not-found errors to explicit invalid or unavailable message', () => {
    component.invitationCode = 'INVALID';
    gameService.joinGameWithCode.and.returnValue(
      throwError(() => new Error('Ressource non trouvee'))
    );

    component.joinWithCode();

    expect(component.joinFeedbackMessage).toBe('games.joinDialog.invalidOrUnavailableCode');
    expect(uiFeedback.showSuccessWithAction).not.toHaveBeenCalled();
  });

  it('should map RESOURCE_NOT_FOUND backend code to invalid or unavailable message', () => {
    component.invitationCode = 'INVALID';
    gameService.joinGameWithCode.and.returnValue(
      throwError(() => ({ error: { code: 'RESOURCE_NOT_FOUND', message: 'Not found' } }))
    );

    component.joinWithCode();

    expect(component.joinFeedbackMessage).toBe('games.joinDialog.invalidOrUnavailableCode');
    expect(uiFeedback.showSuccessWithAction).not.toHaveBeenCalled();
  });

  it('should map mojibake invalid invitation code message to invalid or unavailable message', () => {
    component.invitationCode = 'INVALID';
    gameService.joinGameWithCode.and.returnValue(
      throwError(() => new Error('cÃ³digo de invitaciÃ³n invÃ¡lido'))
    );

    component.joinWithCode();

    expect(component.joinFeedbackMessage).toBe('games.joinDialog.invalidOrUnavailableCode');
    expect(uiFeedback.showSuccessWithAction).not.toHaveBeenCalled();
  });

  it('should show format message before API call for INVALID_INPUT_PARAMETERS-shaped input', () => {
    component.invitationCode = '@@';

    component.joinWithCode();

    expect(component.joinFeedbackMessage).toBe('games.joinDialog.invalidCodeFormat');
    expect(gameService.joinGameWithCode).not.toHaveBeenCalled();
    expect(uiFeedback.showSuccessWithAction).not.toHaveBeenCalled();
  });

  it('should map INVALID_INPUT_PARAMETERS backend code when input format is valid', () => {
    component.invitationCode = 'VALID123';
    gameService.joinGameWithCode.and.returnValue(
      throwError(() => ({ error: { code: 'INVALID_INPUT_PARAMETERS', message: 'Invalid input parameters' } }))
    );

    component.joinWithCode();

    expect(component.joinFeedbackMessage).toBe('games.joinDialog.invalidOrUnavailableCode');
    expect(uiFeedback.showSuccessWithAction).not.toHaveBeenCalled();
  });

  it('should map rate-limit backend code to too-many-attempts message', () => {
    component.invitationCode = 'VALID123';
    gameService.joinGameWithCode.and.returnValue(
      throwError(() => ({ error: { code: 'SYS_004', message: 'Too many attempts' } }))
    );

    component.joinWithCode();

    expect(component.joinFeedbackMessage).toBe('games.joinDialog.tooManyAttempts');
    expect(uiFeedback.showSuccessWithAction).not.toHaveBeenCalled();
  });

  it('should map French not-found message to translated invalid or unavailable message', () => {
    component.invitationCode = 'INVALID';
    gameService.joinGameWithCode.and.returnValue(
      throwError(() => new Error('Non trouvé'))
    );

    component.joinWithCode();

    expect(component.joinFeedbackMessage).toBe('games.joinDialog.invalidOrUnavailableCode');
    expect(uiFeedback.showSuccessWithAction).not.toHaveBeenCalled();
  });

  it('should map unexpected backend errors to invalid or unavailable message for join-with-code', () => {
    component.invitationCode = 'INVALID';
    gameService.joinGameWithCode.and.returnValue(
      throwError(() => new Error('An unexpected error occurred'))
    );

    component.joinWithCode();

    expect(component.joinFeedbackMessage).toBe('games.joinDialog.invalidOrUnavailableCode');
    expect(uiFeedback.showSuccessWithAction).not.toHaveBeenCalled();
  });

  it('should navigate to games on cancel', () => {
    component.joinFeedbackMessage = 'games.joinDialog.alreadyInGame';
    component.cancel();

    expect(component.joinFeedbackMessage).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/games']);
  });

  it('should navigate to game dashboard on snackbar action', fakeAsync(() => {
    component.invitationCode = 'CODE';
    gameService.joinGameWithCode.and.returnValue(of(mockGame));

    uiFeedback.showSuccessWithAction.and.callFake((message, action, onAction) => {
      onAction();
    });

    component.joinWithCode();
    tick();

    expect(router.navigate).toHaveBeenCalledWith(['/games', 'game1', 'dashboard']);
  }));
});
