import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, UrlTree } from '@angular/router';
import { firstValueFrom, of, throwError } from 'rxjs';

import { simultaneousModeGuard } from './simultaneous-mode.guard';
import { UserGamesStore } from '../../../core/services/user-games.store';
import { GameService } from '../../game/services/game.service';
import { Game } from '../../game/models/game.interface';

const makeRoute = (id: string | null): ActivatedRouteSnapshot => {
  const route = { paramMap: { get: (key: string) => (key === 'id' ? id : null) } };
  return route as unknown as ActivatedRouteSnapshot;
};

const makeGame = (draftMode?: 'SNAKE' | 'SIMULTANEOUS'): Game => ({
  id: 'game-1',
  name: 'Test Game',
  creatorName: 'Thibaut',
  maxParticipants: 5,
  status: 'DRAFTING',
  createdAt: new Date().toISOString(),
  participantCount: 2,
  canJoin: false,
  draftMode
});

describe('simultaneousModeGuard', () => {
  let router: jasmine.SpyObj<Router>;
  let userGamesStore: jasmine.SpyObj<UserGamesStore>;
  let gameService: jasmine.SpyObj<GameService>;

  const snakeUrlTree = { commands: 'snake' } as unknown as UrlTree;
  const gamesUrlTree = { commands: 'games' } as unknown as UrlTree;

  beforeEach(() => {
    router = jasmine.createSpyObj<Router>('Router', ['createUrlTree']);
    userGamesStore = jasmine.createSpyObj<UserGamesStore>('UserGamesStore', ['findGameById']);
    gameService = jasmine.createSpyObj<GameService>('GameService', ['getGameById']);

    router.createUrlTree.and.callFake((commands: unknown[]) => {
      const path = (commands as string[]).join('/');
      if (path.includes('snake')) return snakeUrlTree;
      return gamesUrlTree;
    });
    userGamesStore.findGameById.and.returnValue(undefined);

    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: router },
        { provide: UserGamesStore, useValue: userGamesStore },
        { provide: GameService, useValue: gameService }
      ]
    });
  });

  const runGuard = (route: ActivatedRouteSnapshot) =>
    firstValueFrom(
      TestBed.runInInjectionContext(() => simultaneousModeGuard(route, {} as any)) as any
    );

  it('redirects to /games when gameId is missing', async () => {
    const result = await runGuard(makeRoute(null));

    expect(result).toBe(gamesUrlTree);
  });

  it('redirects cached SNAKE games to the snake draft route', async () => {
    userGamesStore.findGameById.and.returnValue(makeGame('SNAKE'));

    const result = await runGuard(makeRoute('game-1'));

    expect(result).toBe(snakeUrlTree);
    expect(router.createUrlTree).toHaveBeenCalledWith(['/games', 'game-1', 'draft', 'snake']);
  });

  it('allows cached SIMULTANEOUS games', async () => {
    userGamesStore.findGameById.and.returnValue(makeGame('SIMULTANEOUS'));

    const result = await runGuard(makeRoute('game-1'));

    expect(result).toBe(true);
  });

  it('redirects cached games without draftMode to the snake draft route', async () => {
    userGamesStore.findGameById.and.returnValue(makeGame(undefined));

    const result = await runGuard(makeRoute('game-1'));

    expect(result).toBe(snakeUrlTree);
  });

  it('falls back to API and redirects SNAKE games to the snake draft route', async () => {
    gameService.getGameById.and.returnValue(of(makeGame('SNAKE')));

    const result = await runGuard(makeRoute('game-1'));

    expect(result).toBe(snakeUrlTree);
  });

  it('falls back to API and allows SIMULTANEOUS games', async () => {
    gameService.getGameById.and.returnValue(of(makeGame('SIMULTANEOUS')));

    const result = await runGuard(makeRoute('game-1'));

    expect(result).toBe(true);
  });

  it('redirects to /games on API error', async () => {
    gameService.getGameById.and.returnValue(throwError(() => new Error('Network error')));

    const result = await runGuard(makeRoute('game-1'));

    expect(result).toBe(gamesUrlTree);
  });

  it('falls back to API and redirects missing draftMode to the snake draft route', async () => {
    gameService.getGameById.and.returnValue(of(makeGame(undefined)));

    const result = await runGuard(makeRoute('game-1'));

    expect(result).toBe(snakeUrlTree);
  });
});
