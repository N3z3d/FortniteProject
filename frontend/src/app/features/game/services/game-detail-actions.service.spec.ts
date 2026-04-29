import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';

import { GameDetailActionsService } from './game-detail-actions.service';
import { GameService } from './game.service';
import { UserGamesStore } from '../../../core/services/user-games.store';
import { TranslationService } from '../../../core/services/translation.service';
import { UiErrorFeedbackService } from '../../../core/services/ui-error-feedback.service';
import { LoggerService } from '../../../core/services/logger.service';
import { Game } from '../models/game.interface';
import { RenameGameDialogComponent } from '../components/rename-game-dialog/rename-game-dialog.component';

describe('GameDetailActionsService', () => {
  let service: GameDetailActionsService;
  let gameServiceSpy: jasmine.SpyObj<GameService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;
  let userGamesStoreSpy: jasmine.SpyObj<UserGamesStore>;
  let translationSpy: jasmine.SpyObj<TranslationService>;
  let uiFeedbackSpy: jasmine.SpyObj<UiErrorFeedbackService>;
  let loggerSpy: jasmine.SpyObj<LoggerService>;

  const mockGame: Game = {
    id: 'game1',
    name: 'Test Game',
    creatorName: 'TestUser',
    maxParticipants: 10,
    status: 'CREATING',
    createdAt: new Date().toISOString(),
    participantCount: 5,
    canJoin: true,
    regionRules: { EU: 2, NAW: 3 },
    invitationCode: 'ABC123',
    invitationCodeExpiresAt: undefined,
    isInvitationCodeExpired: false
  };

  beforeEach(() => {
    gameServiceSpy = jasmine.createSpyObj('GameService', [
      'startDraft',
      'archiveGame',
      'leaveGame',
      'deleteGame',
      'joinGame',
      'regenerateInvitationCode',
      'deleteInvitationCode',
      'renameGame'
    ]);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
    userGamesStoreSpy = jasmine.createSpyObj('UserGamesStore', ['removeGame', 'refreshGames']);
    translationSpy = jasmine.createSpyObj('TranslationService', ['t']);
    uiFeedbackSpy = jasmine.createSpyObj('UiErrorFeedbackService', [
      'showSuccessFromKey',
      'showSuccessMessage',
      'showError'
    ]);
    loggerSpy = jasmine.createSpyObj('LoggerService', ['debug', 'info', 'warn', 'error']);

    userGamesStoreSpy.refreshGames.and.returnValue(of([]));
    dialogSpy.open.and.returnValue({ afterClosed: () => of(undefined) } as any);
    translationSpy.t.and.callFake((key: string) => {
      const templates: Record<string, string> = {
        'games.detail.actions.invitationCodeGenerateVerb': 'Generate invitation code',
        'games.detail.actions.invitationCodeRegenerateVerb': 'Regenerate invitation code',
        'games.detail.actions.invitationCodeWithValue': '{verb}: {code}',
        'games.detail.actions.invitationCodeWithoutValue': '{verb}.',
        'games.detail.actions.invitationCodeDeleteSuccess': 'Invitation code deleted.'
      };
      return templates[key] || key;
    });

    TestBed.configureTestingModule({
      providers: [
        GameDetailActionsService,
        { provide: GameService, useValue: gameServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: MatDialog, useValue: dialogSpy },
        { provide: UserGamesStore, useValue: userGamesStoreSpy },
        { provide: TranslationService, useValue: translationSpy },
        { provide: UiErrorFeedbackService, useValue: uiFeedbackSpy },
        { provide: LoggerService, useValue: loggerSpy }
      ]
    });

    service = TestBed.inject(GameDetailActionsService);
  });

  it('starts draft and shows translated success message', () => {
    const onSuccess = jasmine.createSpy('onSuccess');
    const onSettled = jasmine.createSpy('onSettled');
    gameServiceSpy.startDraft.and.returnValue(of(true));

    service.startDraft('game1', onSuccess, onSettled);

    expect(gameServiceSpy.startDraft).toHaveBeenCalledWith('game1');
    expect(uiFeedbackSpy.showSuccessFromKey).toHaveBeenCalledWith('games.detail.actions.draftStarted');
    expect(onSuccess).toHaveBeenCalled();
    expect(onSettled).toHaveBeenCalled();
  });

  it('shows translated blocked message when startDraft returns false', () => {
    const onSettled = jasmine.createSpy('onSettled');
    gameServiceSpy.startDraft.and.returnValue(of(false));

    service.startDraft('game1', undefined, onSettled);

    expect(uiFeedbackSpy.showError).toHaveBeenCalledWith({}, 'games.detail.actions.draftStartBlocked');
    expect(onSettled).toHaveBeenCalled();
  });

  it('passes backend error and fallback key when startDraft throws', () => {
    const error = new Error('Draft failure');
    const onSettled = jasmine.createSpy('onSettled');
    gameServiceSpy.startDraft.and.returnValue(throwError(() => error));

    service.startDraft('game1', undefined, onSettled);

    expect(uiFeedbackSpy.showError).toHaveBeenCalledWith(
      error,
      'games.detail.actions.draftStartError',
      jasmine.objectContaining({
        rules: jasmine.any(Array)
      })
    );
    expect(onSettled).toHaveBeenCalled();
  });

  it('logs structured context when startDraft throws', () => {
    const error = new Error('Draft failure');
    gameServiceSpy.startDraft.and.returnValue(throwError(() => error));

    service.startDraft('game1');

    expect(loggerSpy.error).toHaveBeenCalledWith(
      'GameDetailActionsService: startDraft failed',
      jasmine.objectContaining({
        gameId: 'game1',
        error
      })
    );
  });

  it('configures creator-missing rule for startDraft errors', () => {
    const error = new Error('Createur de la partie introuvable');
    gameServiceSpy.startDraft.and.returnValue(throwError(() => error));

    service.startDraft('game1');

    expect(uiFeedbackSpy.showError).toHaveBeenCalledWith(
      error,
      'games.detail.actions.draftStartError',
      jasmine.objectContaining({
        rules: [
          jasmine.objectContaining({
            translationKey: 'games.detail.actions.draftCreatorMissing'
          })
        ]
      })
    );
  });

  it('archives game, refreshes store and navigates home', () => {
    gameServiceSpy.archiveGame.and.returnValue(of(true));

    service.archiveGame('game1');

    expect(userGamesStoreSpy.removeGame).toHaveBeenCalledWith('game1');
    expect(userGamesStoreSpy.refreshGames).toHaveBeenCalled();
    expect(uiFeedbackSpy.showSuccessFromKey).toHaveBeenCalledWith('games.detail.actions.archiveSuccess');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
  });

  it('shows translated leave success and updates store', () => {
    gameServiceSpy.leaveGame.and.returnValue(of(true));

    service.leaveGame('game1');

    expect(userGamesStoreSpy.removeGame).toHaveBeenCalledWith('game1');
    expect(userGamesStoreSpy.refreshGames).toHaveBeenCalled();
    expect(uiFeedbackSpy.showSuccessFromKey).toHaveBeenCalledWith('games.detail.actions.leaveSuccess');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
  });

  it('joins game successfully and calls callback', () => {
    const onSuccess = jasmine.createSpy('onSuccess');
    gameServiceSpy.joinGame.and.returnValue(of(true));

    service.joinGame('game1', onSuccess);

    expect(userGamesStoreSpy.refreshGames).toHaveBeenCalled();
    expect(uiFeedbackSpy.showSuccessFromKey).toHaveBeenCalledWith('games.detail.actions.joinSuccess');
    expect(onSuccess).toHaveBeenCalled();
  });

  it('copies invitation code and shows translated success message', async () => {
    const clipboardSpy = jasmine.createSpyObj('Clipboard', ['writeText']);
    clipboardSpy.writeText.and.returnValue(Promise.resolve());
    Object.defineProperty(navigator, 'clipboard', {
      writable: true,
      value: clipboardSpy
    });

    service.copyInvitationCode('ABC123');
    await clipboardSpy.writeText.calls.mostRecent().returnValue;

    expect(uiFeedbackSpy.showSuccessFromKey).toHaveBeenCalledWith('games.detail.actions.codeCopied', 2000);
  });

  it('regenerates invitation code and formats message with generated code', () => {
    const onSuccess = jasmine.createSpy('onSuccess');
    gameServiceSpy.regenerateInvitationCode.and.returnValue(of(mockGame));

    service.regenerateInvitationCode('game1', 'permanent', 'regenerate', onSuccess);

    expect(uiFeedbackSpy.showSuccessMessage).toHaveBeenCalledWith(
      'Regenerate invitation code: ABC123',
      3500
    );
    expect(onSuccess).toHaveBeenCalledWith(mockGame);
  });

  it('deletes invitation code and forwards updated game', () => {
    const onSuccess = jasmine.createSpy('onSuccess');
    const updatedGame = {
      ...mockGame,
      invitationCode: undefined,
      invitationCodeExpiresAt: undefined,
      isInvitationCodeExpired: false
    };
    gameServiceSpy.deleteInvitationCode.and.returnValue(of(updatedGame));

    service.deleteInvitationCode('game1', onSuccess);

    expect(uiFeedbackSpy.showSuccessFromKey).toHaveBeenCalledWith(
      'games.detail.actions.invitationCodeDeleteSuccess'
    );
    expect(onSuccess).toHaveBeenCalledWith(updatedGame);
  });

  it('renames game and shows translated success message', () => {
    const onSuccess = jasmine.createSpy('onSuccess');
    const renamedGame = { ...mockGame, name: 'Renamed Game' };
    gameServiceSpy.renameGame.and.returnValue(of(renamedGame));

    service.renameGame('game1', 'Renamed Game', onSuccess);

    expect(uiFeedbackSpy.showSuccessFromKey).toHaveBeenCalledWith('games.detail.actions.renameSuccess');
    expect(onSuccess).toHaveBeenCalledWith(renamedGame);
  });

  it('opens confirm dialog and archives when confirmed', () => {
    dialogSpy.open.and.returnValue({ afterClosed: () => of(true) } as any);
    gameServiceSpy.archiveGame.and.returnValue(of(true));

    service.confirmArchive('game1');

    expect(dialogSpy.open).toHaveBeenCalled();
    expect(gameServiceSpy.archiveGame).toHaveBeenCalledWith('game1');
  });

  it('resets pending state when start-draft dialog is cancelled', () => {
    const onSettled = jasmine.createSpy('onSettled');
    dialogSpy.open.and.returnValue({ afterClosed: () => of(false) } as any);

    service.confirmStartDraft('game1', undefined, onSettled);

    expect(onSettled).toHaveBeenCalled();
    expect(gameServiceSpy.startDraft).not.toHaveBeenCalled();
  });

  it('starts draft when start-draft dialog is confirmed', () => {
    const onSuccess = jasmine.createSpy('onSuccess');
    const onSettled = jasmine.createSpy('onSettled');
    dialogSpy.open.and.returnValue({ afterClosed: () => of(true) } as any);
    gameServiceSpy.startDraft.and.returnValue(of(true));

    service.confirmStartDraft('game1', onSuccess, onSettled);

    expect(gameServiceSpy.startDraft).toHaveBeenCalledWith('game1');
    expect(onSuccess).toHaveBeenCalled();
    expect(onSettled).toHaveBeenCalled();
  });

  it('opens code duration dialog in generate mode when no code exists', () => {
    const regenerateSpy = spyOn(service, 'regenerateInvitationCode').and.stub();
    dialogSpy.open.and.returnValue({ afterClosed: () => of('7d') } as any);

    service.promptRegenerateCode('game1', false);

    expect(dialogSpy.open).toHaveBeenCalled();
    expect(regenerateSpy).toHaveBeenCalledWith('game1', '7d', 'generate', undefined);
  });


  it('opens confirmation dialog and deletes invitation code when confirmed', () => {
    const deleteSpy = spyOn(service, 'deleteInvitationCode').and.stub();
    dialogSpy.open.and.returnValue({ afterClosed: () => of(true) } as any);

    service.confirmDeleteInvitationCode('game1');

    expect(dialogSpy.open).toHaveBeenCalled();
    expect(deleteSpy).toHaveBeenCalledWith('game1', undefined);
  });
  it('opens rename dialog and renames game when a new name is submitted', () => {
    const renameSpy = spyOn(service, 'renameGame').and.stub();
    dialogSpy.open.and.returnValue({ afterClosed: () => of('New Game Name') } as any);

    service.promptRenameGame('game1', 'Old Name');

    expect(dialogSpy.open).toHaveBeenCalledWith(
      RenameGameDialogComponent,
      jasmine.objectContaining({
        data: { currentName: 'Old Name' }
      })
    );
    expect(renameSpy).toHaveBeenCalledWith('game1', 'New Game Name', undefined);
  });

  it('does not rename when rename dialog is cancelled', () => {
    const renameSpy = spyOn(service, 'renameGame').and.stub();
    dialogSpy.open.and.returnValue({ afterClosed: () => of(undefined) } as any);

    service.promptRenameGame('game1', 'Old Name');

    expect(renameSpy).not.toHaveBeenCalled();
  });
});
