import { TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';

import { gamesResolver } from './games.resolver';
import { GameService } from '../services/game.service';
import { UserContextService } from '../../../core/services/user-context.service';
import { Game } from '../models/game.interface';

describe('gamesResolver', () => {
  let gameService: jasmine.SpyObj<GameService>;
  let userContextService: jasmine.SpyObj<UserContextService>;

  const mockGames: Game[] = [
    {
      id: '1',
      name: 'Mock Game',
      creatorName: 'Thibaut',
      maxParticipants: 10,
      status: 'CREATING',
      createdAt: '2024-01-15T10:30:00',
      participantCount: 1,
      canJoin: true
    }
  ];

  beforeEach(() => {
    gameService = jasmine.createSpyObj<GameService>('GameService', ['getUserGames']);
    userContextService = jasmine.createSpyObj<UserContextService>('UserContextService', ['isLoggedIn']);

    TestBed.configureTestingModule({
      providers: [
        { provide: GameService, useValue: gameService },
        { provide: UserContextService, useValue: userContextService }
      ]
    });
  });

  it('returns an empty array when no user is logged in', (done) => {
    userContextService.isLoggedIn.and.returnValue(false);

    const result$ = TestBed.runInInjectionContext(
      () => gamesResolver({} as any, {} as any)
    ) as unknown as Observable<Game[]>;

    result$.subscribe((games: Game[]) => {
      expect(games).toEqual([]);
      expect(gameService.getUserGames).not.toHaveBeenCalled();
      done();
    });
  });

  it('loads user games when a user is logged in', (done) => {
    userContextService.isLoggedIn.and.returnValue(true);
    gameService.getUserGames.and.returnValue(of(mockGames));

    const result$ = TestBed.runInInjectionContext(
      () => gamesResolver({} as any, {} as any)
    ) as unknown as Observable<Game[]>;

    result$.subscribe((games: Game[]) => {
      expect(gameService.getUserGames).toHaveBeenCalled();
      expect(games).toEqual(mockGames);
      done();
    });
  });

  it('falls back to an empty array on error', (done) => {
    userContextService.isLoggedIn.and.returnValue(true);
    gameService.getUserGames.and.returnValue(throwError(() => new Error('network error')));

    const result$ = TestBed.runInInjectionContext(
      () => gamesResolver({} as any, {} as any)
    ) as unknown as Observable<Game[]>;

    result$.subscribe((games: Game[]) => {
      expect(games).toEqual([]);
      done();
    });
  });
});
