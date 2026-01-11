import { TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';

import { gamesResolver } from './games.resolver';
import { UserContextService } from '../../../core/services/user-context.service';
import { UserGamesStore } from '../../../core/services/user-games.store';
import { LoggerService } from '../../../core/services/logger.service';
import { Game } from '../models/game.interface';

describe('gamesResolver', () => {
  let userContextService: jasmine.SpyObj<UserContextService>;
  let userGamesStore: jasmine.SpyObj<UserGamesStore>;
  let logger: jasmine.SpyObj<LoggerService>;

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
    userContextService = jasmine.createSpyObj<UserContextService>('UserContextService', ['isLoggedIn']);
    userGamesStore = jasmine.createSpyObj<UserGamesStore>('UserGamesStore', ['loadGames', 'clear']);
    logger = jasmine.createSpyObj<LoggerService>('LoggerService', ['info', 'warn', 'error', 'debug']);

    TestBed.configureTestingModule({
      providers: [
        { provide: UserContextService, useValue: userContextService },
        { provide: UserGamesStore, useValue: userGamesStore },
        { provide: LoggerService, useValue: logger }
      ]
    });
  });

  it('returns an empty array when no user is logged in', (done) => {
    userContextService.isLoggedIn.and.returnValue(false);
    userGamesStore.clear.and.stub();

    const result$ = TestBed.runInInjectionContext(
      () => gamesResolver({} as any, {} as any)
    ) as unknown as Observable<Game[]>;

    result$.subscribe((games: Game[]) => {
      expect(games).toEqual([]);
      expect(userGamesStore.clear).toHaveBeenCalled();
      expect(userGamesStore.loadGames).not.toHaveBeenCalled();
      done();
    });
  });

  it('loads user games when a user is logged in', (done) => {
    userContextService.isLoggedIn.and.returnValue(true);
    userGamesStore.loadGames.and.returnValue(of(mockGames));

    const result$ = TestBed.runInInjectionContext(
      () => gamesResolver({} as any, {} as any)
    ) as unknown as Observable<Game[]>;

    result$.subscribe((games: Game[]) => {
      expect(userGamesStore.loadGames).toHaveBeenCalled();
      expect(games).toEqual(mockGames);
      done();
    });
  });

  it('falls back to an empty array on error', (done) => {
    userContextService.isLoggedIn.and.returnValue(true);
    userGamesStore.loadGames.and.returnValue(throwError(() => new Error('network error')));

    const result$ = TestBed.runInInjectionContext(
      () => gamesResolver({} as any, {} as any)
    ) as unknown as Observable<Game[]>;

    result$.subscribe((games: Game[]) => {
      expect(games).toEqual([]);
      done();
    });
  });
});
