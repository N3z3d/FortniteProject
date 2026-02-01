import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
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
    userGamesStoreSpy = jasmine.createSpyObj('UserGamesStore', ['removeGame']);

    TestBed.configureTestingModule({
      providers: [
        GameDetailActionsService,
        { provide: GameService, useValue: gameServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
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
        'Impossible de démarrer le draft',
        'Fermer',
        { duration: 3000 }
      );
    });

    it('should handle error when starting draft', () => {
      gameServiceSpy.startDraft.and.returnValue(throwError(() => new Error('Test error')));

      service.startDraft('game1');

      expect(snackBarSpy.open).toHaveBeenCalledWith(
        'Erreur lors du démarrage du draft',
        'Fermer',
        { duration: 3000 }
      );
    });
  });

  describe('archiveGame', () => {
    it('should archive game successfully and navigate', () => {
      gameServiceSpy.archiveGame.and.returnValue(of(true));

      service.archiveGame('game1');

      expect(gameServiceSpy.archiveGame).toHaveBeenCalledWith('game1');
      expect(snackBarSpy.open).toHaveBeenCalledWith(
        'Game archivée avec succès!',
        'Fermer',
        { duration: 3000 }
      );
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/games']);
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
    it('should leave game successfully and navigate', () => {
      gameServiceSpy.leaveGame.and.returnValue(of(true));

      service.leaveGame('game1');

      expect(gameServiceSpy.leaveGame).toHaveBeenCalledWith('game1');
      expect(snackBarSpy.open).toHaveBeenCalledWith(
        'Vous avez quitté la game',
        'Fermer',
        { duration: 3000 }
      );
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/games']);
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
    it('should join game successfully and call onSuccess', () => {
      const onSuccess = jasmine.createSpy('onSuccess');
      gameServiceSpy.joinGame.and.returnValue(of(true));

      service.joinGame('game1', onSuccess);

      expect(gameServiceSpy.joinGame).toHaveBeenCalledWith('game1');
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
    it('should show confirm dialog and archive if confirmed', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      gameServiceSpy.archiveGame.and.returnValue(of(true));

      service.confirmArchive('game1');

      expect(window.confirm).toHaveBeenCalled();
      expect(gameServiceSpy.archiveGame).toHaveBeenCalledWith('game1');
    });

    it('should not archive if user cancels', () => {
      spyOn(window, 'confirm').and.returnValue(false);

      service.confirmArchive('game1');

      expect(window.confirm).toHaveBeenCalled();
      expect(gameServiceSpy.archiveGame).not.toHaveBeenCalled();
    });
  });

  describe('confirmLeave', () => {
    it('should show confirm dialog and leave if confirmed', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      gameServiceSpy.leaveGame.and.returnValue(of(true));

      service.confirmLeave('game1');

      expect(window.confirm).toHaveBeenCalled();
      expect(gameServiceSpy.leaveGame).toHaveBeenCalledWith('game1');
    });

    it('should not leave if user cancels', () => {
      spyOn(window, 'confirm').and.returnValue(false);

      service.confirmLeave('game1');

      expect(window.confirm).toHaveBeenCalled();
      expect(gameServiceSpy.leaveGame).not.toHaveBeenCalled();
    });
  });

  describe('confirmDelete', () => {
    it('should show confirm dialog and delete if confirmed', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      gameServiceSpy.deleteGame.and.returnValue(of(true));

      service.confirmDelete('game1');

      expect(window.confirm).toHaveBeenCalled();
      expect(gameServiceSpy.deleteGame).toHaveBeenCalledWith('game1');
    });

    it('should not delete if user cancels', () => {
      spyOn(window, 'confirm').and.returnValue(false);

      service.confirmDelete('game1');

      expect(window.confirm).toHaveBeenCalled();
      expect(gameServiceSpy.deleteGame).not.toHaveBeenCalled();
    });
  });

  describe('confirmStartDraft', () => {
    it('should show confirm dialog and start draft if confirmed', () => {
      const onSuccess = jasmine.createSpy('onSuccess');
      spyOn(window, 'confirm').and.returnValue(true);
      gameServiceSpy.startDraft.and.returnValue(of(true));

      service.confirmStartDraft('game1', onSuccess);

      expect(window.confirm).toHaveBeenCalled();
      expect(gameServiceSpy.startDraft).toHaveBeenCalledWith('game1');
    });

    it('should not start draft if user cancels', () => {
      spyOn(window, 'confirm').and.returnValue(false);

      service.confirmStartDraft('game1');

      expect(window.confirm).toHaveBeenCalled();
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
      expect(snackBarSpy.open).not.toHaveBeenCalled();
    });

    it('should handle error when regenerating code', () => {
      gameServiceSpy.regenerateInvitationCode.and.returnValue(throwError(() => new Error('Test error')));
      const consoleSpy = spyOn(console, 'error');

      service.regenerateInvitationCode('game1', '24h');

      expect(consoleSpy).toHaveBeenCalled();
      expect(snackBarSpy.open).not.toHaveBeenCalled();
    });
  });
});
