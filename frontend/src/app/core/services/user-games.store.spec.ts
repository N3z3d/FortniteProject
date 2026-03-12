import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { UserGamesStore } from './user-games.store';
import { GameService } from '../../features/game/services/game.service';
import { LoggerService } from './logger.service';
import { Game } from '../../features/game/models/game.interface';

describe('UserGamesStore', () => {
  let store: UserGamesStore;
  let gameServiceSpy: jasmine.SpyObj<GameService>;
  let loggerSpy: jasmine.SpyObj<LoggerService>;

  const mockGames: Game[] = [
    {
      id: 'game-1',
      name: 'Game One',
      creatorName: 'Creator1',
      maxParticipants: 10,
      status: 'ACTIVE',
      createdAt: '2025-01-01T00:00:00Z',
      participantCount: 3,
      canJoin: true
    },
    {
      id: 'game-2',
      name: 'Game Two',
      creatorName: 'Creator2',
      maxParticipants: 8,
      status: 'CREATING',
      createdAt: '2025-01-02T00:00:00Z',
      participantCount: 1,
      canJoin: true
    }
  ];

  beforeEach(() => {
    gameServiceSpy = jasmine.createSpyObj('GameService', ['getUserGames']);
    loggerSpy = jasmine.createSpyObj('LoggerService', ['debug', 'info', 'warn', 'error']);

    TestBed.configureTestingModule({
      providers: [
        UserGamesStore,
        { provide: GameService, useValue: gameServiceSpy },
        { provide: LoggerService, useValue: loggerSpy }
      ]
    });

    store = TestBed.inject(UserGamesStore);
  });

  afterEach(() => {
    store.stopAutoRefresh();
  });

  describe('initial state', () => {
    it('starts with empty games', () => {
      expect(store.getGames()).toEqual([]);
    });

    it('starts with zero game count', () => {
      expect(store.getGameCount()).toBe(0);
    });

    it('starts with hasGames false', () => {
      expect(store.hasGames()).toBeFalse();
    });

    it('starts with isLoading false', () => {
      expect(store.isLoading()).toBeFalse();
    });

    it('emits empty games on games$', () => {
      store.games$.subscribe(games => {
        expect(games).toEqual([]);
      });
    });
  });

  describe('loadGames', () => {
    it('fetches games from GameService', () => {
      gameServiceSpy.getUserGames.and.returnValue(of(mockGames));

      store.loadGames().subscribe(games => {
        expect(games).toEqual(mockGames);
        expect(store.getGames()).toEqual(mockGames);
        expect(store.getGameCount()).toBe(2);
        expect(store.hasGames()).toBeTrue();
      });
    });

    it('updates loading state during fetch', () => {
      gameServiceSpy.getUserGames.and.returnValue(of(mockGames));

      store.loadGames().subscribe();

      expect(store.isLoading()).toBeFalse();
    });

    it('returns cached games when cache is valid', () => {
      gameServiceSpy.getUserGames.and.returnValue(of(mockGames));

      store.loadGames().subscribe(() => {
        store.loadGames().subscribe(games => {
          expect(games).toEqual(mockGames);
          expect(gameServiceSpy.getUserGames).toHaveBeenCalledTimes(1);
        });
      });
    });

    it('fetches fresh data when forceRefresh is true', () => {
      gameServiceSpy.getUserGames.and.returnValue(of(mockGames));

      store.loadGames().subscribe(() => {
        store.loadGames(true).subscribe(() => {
          expect(gameServiceSpy.getUserGames).toHaveBeenCalledTimes(2);
        });
      });
    });

    it('handles API errors gracefully', () => {
      const error = new Error('Network error');
      gameServiceSpy.getUserGames.and.returnValue(throwError(() => error));

      store.loadGames().subscribe({
        error: (err) => {
          expect(err).toBe(error);
          expect(store.isLoading()).toBeFalse();
        }
      });
    });

    it('sets error state on API failure', () => {
      const error = new Error('Network error');
      gameServiceSpy.getUserGames.and.returnValue(throwError(() => error));

      store.loadGames().subscribe({
        error: () => {
          store.error$.subscribe(errorMsg => {
            expect(errorMsg).toBe('Network error');
          });
        }
      });
    });

    it('logs info on successful load', () => {
      gameServiceSpy.getUserGames.and.returnValue(of(mockGames));

      store.loadGames().subscribe(() => {
        expect(loggerSpy.info).toHaveBeenCalledWith(
          'UserGamesStore: games loaded from API',
          jasmine.objectContaining({ count: 2 })
        );
      });
    });

    it('logs error on failed load', () => {
      gameServiceSpy.getUserGames.and.returnValue(throwError(() => new Error('fail')));

      store.loadGames().subscribe({
        error: () => {
          expect(loggerSpy.error).toHaveBeenCalledWith(
            'UserGamesStore: failed to load games',
            jasmine.any(Object)
          );
        }
      });
    });
  });

  describe('refreshGames', () => {
    it('forces a fresh fetch', () => {
      gameServiceSpy.getUserGames.and.returnValue(of(mockGames));

      store.loadGames().subscribe(() => {
        store.refreshGames().subscribe(() => {
          expect(gameServiceSpy.getUserGames).toHaveBeenCalledTimes(2);
        });
      });
    });
  });

  describe('updateGames', () => {
    it('updates the store with provided games', () => {
      store.updateGames(mockGames);

      expect(store.getGames()).toEqual(mockGames);
      expect(store.getGameCount()).toBe(2);
    });

    it('emits updated games on games$', () => {
      const sub = store.games$.subscribe(games => {
        if (games.length > 0) {
          expect(games).toEqual(mockGames);
          sub.unsubscribe();
        }
      });

      store.updateGames(mockGames);
    });
  });

  describe('addGame', () => {
    it('adds a game to existing list', () => {
      store.updateGames([mockGames[0]]);
      store.addGame(mockGames[1]);

      expect(store.getGameCount()).toBe(2);
      expect(store.getGames()).toEqual(mockGames);
    });

    it('adds a game to empty store', () => {
      store.addGame(mockGames[0]);

      expect(store.getGameCount()).toBe(1);
      expect(store.getGames()[0].id).toBe('game-1');
    });
  });

  describe('removeGame', () => {
    it('removes a game by ID', () => {
      store.updateGames(mockGames);
      store.removeGame('game-1');

      expect(store.getGameCount()).toBe(1);
      expect(store.getGames()[0].id).toBe('game-2');
    });

    it('does nothing when game ID not found', () => {
      store.updateGames(mockGames);
      store.removeGame('nonexistent');

      expect(store.getGameCount()).toBe(2);
    });
  });

  describe('findGameById', () => {
    it('finds a game by ID', () => {
      store.updateGames(mockGames);

      const found = store.findGameById('game-2');

      expect(found).toBeDefined();
      expect(found!.name).toBe('Game Two');
    });

    it('returns undefined when not found', () => {
      store.updateGames(mockGames);

      expect(store.findGameById('nonexistent')).toBeUndefined();
    });
  });

  describe('clear', () => {
    it('resets the store to initial state', () => {
      store.updateGames(mockGames);
      store.clear();

      expect(store.getGames()).toEqual([]);
      expect(store.getGameCount()).toBe(0);
      expect(store.hasGames()).toBeFalse();
    });

    it('logs the clear action', () => {
      store.clear();

      expect(loggerSpy.debug).toHaveBeenCalledWith('UserGamesStore: cleared');
    });
  });

  describe('startAutoRefresh / stopAutoRefresh', () => {
    it('starts periodic refresh', fakeAsync(() => {
      gameServiceSpy.getUserGames.and.returnValue(of(mockGames));

      store.startAutoRefresh(100);
      tick(250);
      store.stopAutoRefresh();

      expect(gameServiceSpy.getUserGames).toHaveBeenCalledTimes(2);
    }));

    it('does not start duplicate auto-refresh', fakeAsync(() => {
      gameServiceSpy.getUserGames.and.returnValue(of(mockGames));

      store.startAutoRefresh(100);
      store.startAutoRefresh(100);
      tick(150);
      store.stopAutoRefresh();

      expect(gameServiceSpy.getUserGames).toHaveBeenCalledTimes(1);
    }));

    it('stopAutoRefresh stops the periodic refresh', fakeAsync(() => {
      gameServiceSpy.getUserGames.and.returnValue(of(mockGames));

      store.startAutoRefresh(100);
      tick(150);
      store.stopAutoRefresh();
      tick(200);

      expect(gameServiceSpy.getUserGames).toHaveBeenCalledTimes(1);
    }));

    it('stopAutoRefresh is safe to call when not running', () => {
      expect(() => store.stopAutoRefresh()).not.toThrow();
    });
  });

  describe('state$', () => {
    it('emits complete state object', () => {
      gameServiceSpy.getUserGames.and.returnValue(of(mockGames));

      store.loadGames().subscribe(() => {
        store.state$.subscribe(state => {
          expect(state.games).toEqual(mockGames);
          expect(state.loading).toBeFalse();
          expect(state.error).toBeNull();
          expect(state.lastLoaded).toBeTruthy();
        });
      });
    });
  });

  describe('cache invalidation', () => {
    it('refetches after cache expires', fakeAsync(() => {
      gameServiceSpy.getUserGames.and.returnValue(of(mockGames));

      store.loadGames().subscribe();

      tick(31000);

      store.loadGames().subscribe();

      expect(gameServiceSpy.getUserGames).toHaveBeenCalledTimes(2);
    }));
  });
});
