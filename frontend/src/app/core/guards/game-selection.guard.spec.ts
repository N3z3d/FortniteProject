import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';

import { GameSelectionGuard } from './game-selection.guard';
import { GameSelectionService } from '../services/game-selection.service';
import { LoggerService } from '../services/logger.service';
import { UiErrorFeedbackService } from '../services/ui-error-feedback.service';

describe('GameSelectionGuard', () => {
  let guard: GameSelectionGuard;
  let gameSelectionService: jasmine.SpyObj<GameSelectionService>;
  let router: jasmine.SpyObj<Router>;
  let uiFeedback: jasmine.SpyObj<UiErrorFeedbackService>;
  let logger: jasmine.SpyObj<LoggerService>;

  const mockRoute = {} as ActivatedRouteSnapshot;
  const mockState = { url: '/dashboard' } as RouterStateSnapshot;

  beforeEach(() => {
    const gameSelectionSpy = jasmine.createSpyObj('GameSelectionService', ['hasSelectedGame', 'getSelectedGame']);
    const routerSpy = jasmine.createSpyObj('Router', ['createUrlTree']);
    const uiFeedbackSpy = jasmine.createSpyObj('UiErrorFeedbackService', ['showError']);
    const loggerSpy = jasmine.createSpyObj('LoggerService', ['debug', 'warn', 'info', 'error']);

    TestBed.configureTestingModule({
      providers: [
        GameSelectionGuard,
        { provide: GameSelectionService, useValue: gameSelectionSpy },
        { provide: Router, useValue: routerSpy },
        { provide: UiErrorFeedbackService, useValue: uiFeedbackSpy },
        { provide: LoggerService, useValue: loggerSpy }
      ]
    });

    guard = TestBed.inject(GameSelectionGuard);
    gameSelectionService = TestBed.inject(GameSelectionService) as jasmine.SpyObj<GameSelectionService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    uiFeedback = TestBed.inject(UiErrorFeedbackService) as jasmine.SpyObj<UiErrorFeedbackService>;
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

    it('should show translated error notification when redirecting', () => {
      gameSelectionService.hasSelectedGame.and.returnValue(false);
      router.createUrlTree.and.returnValue({} as UrlTree);

      guard.canActivate(mockRoute, mockState);

      expect(uiFeedback.showError).toHaveBeenCalledWith(null, 'games.validation.selectGameRequired', { duration: 4000 });
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

    it('should handle null game when hasSelectedGame returns true but getSelectedGame returns null', () => {
      gameSelectionService.hasSelectedGame.and.returnValue(true);
      gameSelectionService.getSelectedGame.and.returnValue(null as any);

      const result = guard.canActivate(mockRoute, mockState);

      expect(result).toBe(true);
      expect(logger.debug).toHaveBeenCalled();
    });

    it('should allow access with different game IDs', () => {
      gameSelectionService.hasSelectedGame.and.returnValue(true);
      gameSelectionService.getSelectedGame.and.returnValue({ id: 'different-game', name: 'Different' } as any);

      const result = guard.canActivate(mockRoute, mockState);

      expect(result).toBe(true);
      expect(logger.debug).toHaveBeenCalledWith(
        'GameSelectionGuard: Access granted',
        jasmine.objectContaining({ gameId: 'different-game' })
      );
    });

    it('should redirect consistently across multiple checks when no game selected', () => {
      gameSelectionService.hasSelectedGame.and.returnValue(false);
      const mockUrlTree = {} as UrlTree;
      router.createUrlTree.and.returnValue(mockUrlTree);

      const state1 = { url: '/dashboard' } as RouterStateSnapshot;
      const state2 = { url: '/teams' } as RouterStateSnapshot;

      const result1 = guard.canActivate(mockRoute, state1);
      const result2 = guard.canActivate(mockRoute, state2);

      expect(result1).toBe(mockUrlTree);
      expect(result2).toBe(mockUrlTree);
      expect(uiFeedback.showError).toHaveBeenCalledTimes(2);
    });

    it('should allow access with empty game name', () => {
      gameSelectionService.hasSelectedGame.and.returnValue(true);
      gameSelectionService.getSelectedGame.and.returnValue({ id: 'game-1', name: '' } as any);

      const result = guard.canActivate(mockRoute, mockState);

      expect(result).toBe(true);
    });

    it('should redirect with different attempted URLs', () => {
      gameSelectionService.hasSelectedGame.and.returnValue(false);
      const mockUrlTree = {} as UrlTree;
      router.createUrlTree.and.returnValue(mockUrlTree);

      const complexState = { url: '/dashboard?tab=stats&filter=active' } as RouterStateSnapshot;

      guard.canActivate(mockRoute, complexState);

      expect(logger.warn).toHaveBeenCalledWith(
        'GameSelectionGuard: No game selected, redirecting to games list',
        jasmine.objectContaining({ attemptedUrl: '/dashboard?tab=stats&filter=active' })
      );
    });
  });
});
