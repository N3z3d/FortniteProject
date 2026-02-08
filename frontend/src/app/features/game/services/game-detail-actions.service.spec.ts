import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';

import { GameDetailActionsService } from './game-detail-actions.service';
import { GameService } from './game.service';
import { UserGamesStore } from '../../../core/services/user-games.store';
import { Game } from '../models/game.interface';

describe('GameDetailActionsService', () => {
  let service: GameDetailActionsService;
  let gameServiceSpy: jasmine.SpyObj<GameService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;
  let userGamesStoreSpy: jasmine.SpyObj<UserGamesStore>;

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
      'renameGame'
    ]);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
    userGamesStoreSpy = jasmine.createSpyObj('UserGamesStore', ['removeGame', 'refreshGames']);
    userGamesStoreSpy.refreshGames.and.returnValue(of([]));
    dialogSpy.open.and.returnValue({ afterClosed: () => of(undefined) } as any);

    TestBed.configureTestingModule({
      providers: [
        GameDetailActionsService,
        { provide: GameService, useValue: gameServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: MatDialog, useValue: dialogSpy },
        { provide: UserGamesStore, useValue: userGamesStoreSpy }
      ]
    });

    service = TestBed.inject(GameDetailActionsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('startDraft', () => {
    it('should start draft successfully and call onSuccess', () => {
      const onSuccess = jasmine.createSpy('onSuccess');
      gameServiceSpy.startDraft.and.returnValue(of(true));

      service.startDraft('game1', onSuccess);

      expect(gameServiceSpy.startDraft).toHaveBeenCalledWith('game1');
      expect(snackBarSpy.open).toHaveBeenCalledWith(
        'Draft démarré avec succès!',
        'Fermer',
        { duration: 3000 }
      );
      expect(onSuccess).toHaveBeenCalled();
    });

    it('should show error message when draft start fails', () => {
      gameServiceSpy.startDraft.and.returnValue(of(false));

      service.startDraft('game1');

      expect(snackBarSpy.open).toHaveBeenCalledWith(
        'Impossible de demarrer le draft. Verifiez que vous etes le createur et qu il y a au moins 2 participants.',
        'Fermer',
        { duration: 5000 }
      );
    });

    it('should handle error when starting draft', () => {
      gameServiceSpy.startDraft.and.returnValue(throwError(() => new Error('Test error')));

      service.startDraft('game1');

      expect(snackBarSpy.open).toHaveBeenCalledWith(
        'Test error',
        'Fermer',
        { duration: 5000 }
      );
    });
  });

  describe('archiveGame', () => {
    it('should archive game successfully, remove from store, and navigate home', () => {
      gameServiceSpy.archiveGame.and.returnValue(of(true));

      service.archiveGame('game1');

      expect(gameServiceSpy.archiveGame).toHaveBeenCalledWith('game1');
      expect(userGamesStoreSpy.removeGame).toHaveBeenCalledWith('game1');
      expect(snackBarSpy.open).toHaveBeenCalledWith(
        'Game archivée avec succès!',
        'Fermer',
        { duration: 3000 }
      );
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
    });

    it('should show error message when archive fails', () => {
      gameServiceSpy.archiveGame.and.returnValue(of(false));

      service.archiveGame('game1');

      expect(snackBarSpy.open).toHaveBeenCalledWith(
        'Impossible d\'archiver la game',
        'Fermer',
        { duration: 3000 }
      );
      expect(routerSpy.navigate).not.toHaveBeenCalled();
    });

    it('should handle error when archiving game', () => {
      gameServiceSpy.archiveGame.and.returnValue(throwError(() => new Error('Test error')));

      service.archiveGame('game1');

      expect(snackBarSpy.open).toHaveBeenCalledWith(
        'Erreur lors de l\'archivage de la game',
        'Fermer',
        { duration: 3000 }
      );
    });
  });

  describe('leaveGame', () => {
    it('should leave game successfully, remove from store, and navigate home', () => {
      gameServiceSpy.leaveGame.and.returnValue(of(true));

      service.leaveGame('game1');

      expect(gameServiceSpy.leaveGame).toHaveBeenCalledWith('game1');
      expect(userGamesStoreSpy.removeGame).toHaveBeenCalledWith('game1');
      expect(snackBarSpy.open).toHaveBeenCalledWith(
        'Vous avez quitté la game',
        'Fermer',
        { duration: 3000 }
      );
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
    });

    it('should show error message when leave fails', () => {
      gameServiceSpy.leaveGame.and.returnValue(of(false));

      service.leaveGame('game1');

      expect(snackBarSpy.open).toHaveBeenCalledWith(
        'Impossible de quitter la game',
        'Fermer',
        { duration: 3000 }
      );
    });

    it('should handle error when leaving game', () => {
      gameServiceSpy.leaveGame.and.returnValue(throwError(() => new Error('Test error')));

      service.leaveGame('game1');

      expect(snackBarSpy.open).toHaveBeenCalledWith(
        'Erreur lors de la sortie de la game',
        'Fermer',
        { duration: 3000 }
      );
    });
  });

  describe('permanentlyDeleteGame', () => {
    it('should delete game successfully and remove from store', () => {
      gameServiceSpy.deleteGame.and.returnValue(of(true));

      service.permanentlyDeleteGame('game1');

      expect(gameServiceSpy.deleteGame).toHaveBeenCalledWith('game1');
      expect(userGamesStoreSpy.removeGame).toHaveBeenCalledWith('game1');
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
    });

    it('should not navigate if delete fails', () => {
      gameServiceSpy.deleteGame.and.returnValue(of(false));

      service.permanentlyDeleteGame('game1');

      expect(routerSpy.navigate).not.toHaveBeenCalled();
    });

    it('should handle error when deleting game', () => {
      gameServiceSpy.deleteGame.and.returnValue(throwError(() => new Error('Test error')));

      service.permanentlyDeleteGame('game1');

      expect(routerSpy.navigate).not.toHaveBeenCalled();
    });
  });

  describe('joinGame', () => {
    it('should join game successfully, refresh store, and call onSuccess', () => {
      const onSuccess = jasmine.createSpy('onSuccess');
      gameServiceSpy.joinGame.and.returnValue(of(true));

      service.joinGame('game1', onSuccess);

      expect(gameServiceSpy.joinGame).toHaveBeenCalledWith('game1');
      expect(userGamesStoreSpy.refreshGames).toHaveBeenCalled();
      expect(snackBarSpy.open).toHaveBeenCalledWith(
        jasmine.stringMatching(/rejoint/i),
        'Fermer',
        { duration: 3000 }
      );
      expect(onSuccess).toHaveBeenCalled();
    });

    it('should show error message when join fails', () => {
      gameServiceSpy.joinGame.and.returnValue(of(false));

      service.joinGame('game1');

      expect(snackBarSpy.open).toHaveBeenCalledWith(
        'Impossible de rejoindre la game',
        'Fermer',
        { duration: 3000 }
      );
    });

    it('should handle error when joining game', () => {
      gameServiceSpy.joinGame.and.returnValue(throwError(() => new Error('Test error')));

      service.joinGame('game1');

      expect(snackBarSpy.open).toHaveBeenCalledWith(
        jasmine.stringMatching(/Erreur/i),
        'Fermer',
        { duration: 3000 }
      );
    });
  });

  describe('confirmArchive', () => {
    it('should open dialog and archive if confirmed', () => {
      dialogSpy.open.and.returnValue({ afterClosed: () => of(true) } as any);
      gameServiceSpy.archiveGame.and.returnValue(of(true));

      service.confirmArchive('game1');

      expect(dialogSpy.open).toHaveBeenCalled();
      expect(gameServiceSpy.archiveGame).toHaveBeenCalledWith('game1');
    });

    it('should not archive if user cancels dialog', () => {
      dialogSpy.open.and.returnValue({ afterClosed: () => of(false) } as any);

      service.confirmArchive('game1');

      expect(dialogSpy.open).toHaveBeenCalled();
      expect(gameServiceSpy.archiveGame).not.toHaveBeenCalled();
    });
  });

  describe('confirmLeave', () => {
    it('should open dialog and leave if confirmed', () => {
      dialogSpy.open.and.returnValue({ afterClosed: () => of(true) } as any);
      gameServiceSpy.leaveGame.and.returnValue(of(true));

      service.confirmLeave('game1');

      expect(dialogSpy.open).toHaveBeenCalled();
      expect(gameServiceSpy.leaveGame).toHaveBeenCalledWith('game1');
    });

    it('should not leave if user cancels dialog', () => {
      dialogSpy.open.and.returnValue({ afterClosed: () => of(false) } as any);

      service.confirmLeave('game1');

      expect(dialogSpy.open).toHaveBeenCalled();
      expect(gameServiceSpy.leaveGame).not.toHaveBeenCalled();
    });
  });

  describe('confirmDelete', () => {
    it('should open dialog and delete if confirmed', () => {
      dialogSpy.open.and.returnValue({ afterClosed: () => of(true) } as any);
      gameServiceSpy.deleteGame.and.returnValue(of(true));

      service.confirmDelete('game1');

      expect(dialogSpy.open).toHaveBeenCalled();
      expect(gameServiceSpy.deleteGame).toHaveBeenCalledWith('game1');
    });

    it('should not delete if user cancels dialog', () => {
      dialogSpy.open.and.returnValue({ afterClosed: () => of(false) } as any);

      service.confirmDelete('game1');

      expect(dialogSpy.open).toHaveBeenCalled();
      expect(gameServiceSpy.deleteGame).not.toHaveBeenCalled();
    });
  });

  describe('confirmStartDraft', () => {
    it('should open dialog and start draft if confirmed', () => {
      const onSuccess = jasmine.createSpy('onSuccess');
      dialogSpy.open.and.returnValue({ afterClosed: () => of(true) } as any);
      gameServiceSpy.startDraft.and.returnValue(of(true));

      service.confirmStartDraft('game1', onSuccess);

      expect(dialogSpy.open).toHaveBeenCalled();
      expect(gameServiceSpy.startDraft).toHaveBeenCalledWith('game1');
    });

    it('should not start draft if user cancels dialog', () => {
      dialogSpy.open.and.returnValue({ afterClosed: () => of(false) } as any);

      service.confirmStartDraft('game1');

      expect(dialogSpy.open).toHaveBeenCalled();
      expect(gameServiceSpy.startDraft).not.toHaveBeenCalled();
    });
  });

  describe('copyInvitationCode', () => {
    it('should copy code to clipboard and show success message', async () => {
      const clipboardSpy = jasmine.createSpyObj('Clipboard', ['writeText']);
      clipboardSpy.writeText.and.returnValue(Promise.resolve());
      Object.defineProperty(navigator, 'clipboard', {
        writable: true,
        value: clipboardSpy
      });

      service.copyInvitationCode('ABC123');
      await clipboardSpy.writeText.calls.mostRecent().returnValue;

      expect(clipboardSpy.writeText).toHaveBeenCalledWith('ABC123');
      expect(snackBarSpy.open).toHaveBeenCalledWith(
        'Code copié dans le presse-papier !',
        'OK',
        { duration: 2000 }
      );
    });
  });

  describe('regenerateInvitationCode', () => {
    it('should regenerate code and call onSuccess', () => {
      const onSuccess = jasmine.createSpy('onSuccess');
      gameServiceSpy.regenerateInvitationCode.and.returnValue(of(mockGame));

      service.regenerateInvitationCode('game1', 'permanent', onSuccess);

      expect(gameServiceSpy.regenerateInvitationCode).toHaveBeenCalledWith('game1', 'permanent');
      expect(onSuccess).toHaveBeenCalledWith(mockGame);
      expect(snackBarSpy.open).toHaveBeenCalledWith(
        "Code d'invitation regenere: ABC123",
        'Fermer',
        { duration: 3500 }
      );
    });

    it('should handle error when regenerating code', () => {
      gameServiceSpy.regenerateInvitationCode.and.returnValue(throwError(() => new Error('Test error')));
      const consoleSpy = spyOn(console, 'error');

      service.regenerateInvitationCode('game1', '24h');

      expect(consoleSpy).toHaveBeenCalled();
      expect(snackBarSpy.open).toHaveBeenCalledWith(
        'Test error',
        'Fermer',
        { duration: 5000 }
      );
    });
  });

  describe('renameGame', () => {
    it('should rename game successfully and show confirmation', () => {
      const onSuccess = jasmine.createSpy('onSuccess');
      const renamedGame = { ...mockGame, name: 'New Name' };
      gameServiceSpy.renameGame.and.returnValue(of(renamedGame));

      service.renameGame('game1', 'New Name', onSuccess);

      expect(gameServiceSpy.renameGame).toHaveBeenCalledWith('game1', 'New Name');
      expect(snackBarSpy.open).toHaveBeenCalledWith(
        'Partie renommée avec succès!',
        'Fermer',
        { duration: 3000 }
      );
      expect(onSuccess).toHaveBeenCalledWith(renamedGame);
    });

    it('should show error message when rename fails', () => {
      gameServiceSpy.renameGame.and.returnValue(throwError(() => new Error('Forbidden')));

      service.renameGame('game1', 'New Name');

      expect(snackBarSpy.open).toHaveBeenCalledWith(
        'Forbidden',
        'Fermer',
        { duration: 5000 }
      );
    });
  });

  describe('promptRegenerateCode', () => {
    it('should open dialog and regenerate when duration is selected', () => {
      const regenerateSpy = spyOn(service, 'regenerateInvitationCode');
      dialogSpy.open.and.returnValue({ afterClosed: () => of('7d') } as any);

      service.promptRegenerateCode('game1');

      expect(dialogSpy.open).toHaveBeenCalled();
      expect(regenerateSpy).toHaveBeenCalledWith('game1', '7d', undefined);
    });

    it('should not regenerate when dialog is closed without selection', () => {
      const regenerateSpy = spyOn(service, 'regenerateInvitationCode');
      dialogSpy.open.and.returnValue({ afterClosed: () => of(undefined) } as any);

      service.promptRegenerateCode('game1');

      expect(dialogSpy.open).toHaveBeenCalled();
      expect(regenerateSpy).not.toHaveBeenCalled();
    });
  });
});
