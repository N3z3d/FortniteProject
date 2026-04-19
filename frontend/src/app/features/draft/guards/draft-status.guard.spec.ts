import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, UrlTree } from '@angular/router';
import { of, throwError } from 'rxjs';
import { firstValueFrom } from 'rxjs';

import { draftStatusGuard } from './draft-status.guard';
import { UserGamesStore } from '../../../core/services/user-games.store';
import { GameService } from '../../game/services/game.service';
import { Game } from '../../game/models/game.interface';

const makeRoute = (id: string | null): ActivatedRouteSnapshot => {
  const route = { paramMap: { get: (key: string) => (key === 'id' ? id : null) } };
  return route as unknown as ActivatedRouteSnapshot;
};

const makeGame = (status: Game['status'] = 'DRAFTING'): Game => ({
  id: 'game-1',
  name: 'Test Game',
  creatorName: 'Thibaut',
  maxParticipants: 5,
  status,
  createdAt: new Date().toISOString(),
  participantCount: 2,
  canJoin: false,
});

describe('draftStatusGuard', () => {
  let router: jasmine.SpyObj<Router>;
  let userGamesStore: jasmine.SpyObj<UserGamesStore>;
  let gameService: jasmine.SpyObj<GameService>;

  const gameDetailUrlTree = { commands: 'game-detail' } as unknown as UrlTree;
  const gamesUrlTree = { commands: 'games' } as unknown as UrlTree;

  beforeEach(() => {
    router = jasmine.createSpyObj<Router>('Router', ['createUrlTree']);
    userGamesStore = jasmine.createSpyObj<UserGamesStore>('UserGamesStore', ['findGameById']);
    gameService = jasmine.createSpyObj<GameService>('GameService', ['getGameById']);

    router.createUrlTree.and.callFake((commands: unknown[]) => {
      const path = (commands as string[]).join('/');
      if (path === '/games') return gamesUrlTree;
      return gameDetailUrlTree;
    });

    userGamesStore.findGameById.and.returnValue(undefined);

    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: router },
        { provide: UserGamesStore, useValue: userGamesStore },
        { provide: GameService, useValue: gameService },
      ],
    });
  });

  const runGuard = (route: ActivatedRouteSnapshot) =>
    firstValueFrom(
      TestBed.runInInjectionContext(() => draftStatusGuard(route, {} as any)) as any
    );

  describe('when gameId is missing', () => {
    it('should redirect to /games', async () => {
      const result = await runGuard(makeRoute(null));

      expect(result).toBe(gamesUrlTree);
      expect(router.createUrlTree).toHaveBeenCalledWith(['/games']);
    });
  });

  describe('when game is found in store with status DRAFTING', () => {
    it('should allow access (return true)', async () => {
      userGamesStore.findGameById.and.returnValue(makeGame('DRAFTING'));

      const result = await runGuard(makeRoute('game-1'));

      expect(result).toBe(true);
    });
  });

  describe('when game is found in store with status other than DRAFTING', () => {
    it('should redirect to /games/:id for CREATING status', async () => {
      userGamesStore.findGameById.and.returnValue(makeGame('CREATING'));

      const result = await runGuard(makeRoute('game-1'));

      expect(result).toBe(gameDetailUrlTree);
      expect(router.createUrlTree).toHaveBeenCalledWith(['/games', 'game-1']);
    });

    it('should redirect to /games/:id for ACTIVE status', async () => {
      userGamesStore.findGameById.and.returnValue(makeGame('ACTIVE'));

      const result = await runGuard(makeRoute('game-1'));

      expect(result).toBe(gameDetailUrlTree);
    });

    it('should redirect to /games/:id for FINISHED status', async () => {
      userGamesStore.findGameById.and.returnValue(makeGame('FINISHED'));

      const result = await runGuard(makeRoute('game-1'));

      expect(result).toBe(gameDetailUrlTree);
    });
  });

  describe('when game is not in store — API fallback', () => {
    it('should allow access when API returns DRAFTING status', async () => {
      gameService.getGameById.and.returnValue(of(makeGame('DRAFTING')));

      const result = await runGuard(makeRoute('game-1'));

      expect(result).toBe(true);
      expect(gameService.getGameById).toHaveBeenCalledWith('game-1');
    });

    it('should redirect to /games/:id when API returns non-DRAFTING status', async () => {
      gameService.getGameById.and.returnValue(of(makeGame('CREATING')));

      const result = await runGuard(makeRoute('game-1'));

      expect(result).toBe(gameDetailUrlTree);
      expect(router.createUrlTree).toHaveBeenCalledWith(['/games', 'game-1']);
    });

    it('should redirect to /games on API error', async () => {
      gameService.getGameById.and.returnValue(throwError(() => new Error('Network error')));

      const result = await runGuard(makeRoute('game-1'));

      expect(result).toBe(gamesUrlTree);
      expect(router.createUrlTree).toHaveBeenCalledWith(['/games']);
    });
  });
});
