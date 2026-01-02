import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { GameSelectionGuard } from './game-selection.guard';
import { GameSelectionService } from '../services/game-selection.service';
import { LoggerService } from '../services/logger.service';

describe('GameSelectionGuard', () => {
  let guard: GameSelectionGuard;
  let gameSelectionService: jasmine.SpyObj<GameSelectionService>;
  let router: jasmine.SpyObj<Router>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;
  let logger: jasmine.SpyObj<LoggerService>;

  const mockRoute = {} as ActivatedRouteSnapshot;
  const mockState = { url: '/dashboard' } as RouterStateSnapshot;

  beforeEach(() => {
    const gameSelectionSpy = jasmine.createSpyObj('GameSelectionService', ['hasSelectedGame', 'getSelectedGame']);
    const routerSpy = jasmine.createSpyObj('Router', ['createUrlTree']);
    const snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    const loggerSpy = jasmine.createSpyObj('LoggerService', ['debug', 'warn', 'info', 'error']);

    TestBed.configureTestingModule({
      providers: [
        GameSelectionGuard,
        { provide: GameSelectionService, useValue: gameSelectionSpy },
        { provide: Router, useValue: routerSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: LoggerService, useValue: loggerSpy }
      ]
    });

    guard = TestBed.inject(GameSelectionGuard);
    gameSelectionService = TestBed.inject(GameSelectionService) as jasmine.SpyObj<GameSelectionService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    snackBar = TestBed.inject(MatSnackBar) as jasmine.SpyObj<MatSnackBar>;
    logger = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;
  });

  it('should be created', () => {
    expect(guard).toBeTruthy();
  });

  describe('canActivate', () => {
    it('should allow access when a game is selected', () => {
      gameSelectionService.hasSelectedGame.and.returnValue(true);
      gameSelectionService.getSelectedGame.and.returnValue({ id: 'game-1', name: 'Test Game' } as any);

      const result = guard.canActivate(mockRoute, mockState);

      expect(result).toBe(true);
      expect(logger.debug).toHaveBeenCalledWith(
        'GameSelectionGuard: Access granted',
        jasmine.objectContaining({ gameId: 'game-1' })
      );
    });

    it('should redirect to /games when no game is selected', () => {
      const mockUrlTree = {} as UrlTree;
      gameSelectionService.hasSelectedGame.and.returnValue(false);
      router.createUrlTree.and.returnValue(mockUrlTree);

      const result = guard.canActivate(mockRoute, mockState);

      expect(result).toBe(mockUrlTree);
      expect(router.createUrlTree).toHaveBeenCalledWith(['/games']);
    });

    it('should show snackbar notification when redirecting', () => {
      gameSelectionService.hasSelectedGame.and.returnValue(false);
      router.createUrlTree.and.returnValue({} as UrlTree);

      guard.canActivate(mockRoute, mockState);

      expect(snackBar.open).toHaveBeenCalledWith(
        'Veuillez d\'abord sélectionner une game',
        'Fermer',
        jasmine.objectContaining({ duration: 4000 })
      );
    });

    it('should log warning when redirecting', () => {
      gameSelectionService.hasSelectedGame.and.returnValue(false);
      router.createUrlTree.and.returnValue({} as UrlTree);

      guard.canActivate(mockRoute, mockState);

      expect(logger.warn).toHaveBeenCalledWith(
        'GameSelectionGuard: No game selected, redirecting to games list',
        jasmine.objectContaining({ attemptedUrl: '/dashboard' })
      );
    });
  });
});
