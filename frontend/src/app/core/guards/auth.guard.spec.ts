import { TestBed } from '@angular/core/testing';
import { Router, RouterStateSnapshot } from '@angular/router';

import { AuthGuard } from './auth.guard';
import { UserContextService } from '../services/user-context.service';

describe('AuthGuard', () => {
  let router: jasmine.SpyObj<Router>;
  let userContextService: jasmine.SpyObj<UserContextService>;
  let guard: AuthGuard;

  beforeEach(() => {
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    userContextService = jasmine.createSpyObj<UserContextService>('UserContextService', ['isLoggedIn']);

    TestBed.configureTestingModule({
      providers: [
        AuthGuard,
        { provide: Router, useValue: router },
        { provide: UserContextService, useValue: userContextService }
      ]
    });

    guard = TestBed.inject(AuthGuard);
  });

  it('redirects to /login with returnUrl when unauthenticated', () => {
    userContextService.isLoggedIn.and.returnValue(false);

    const state = { url: '/games/test-game-id' } as RouterStateSnapshot;

    const result = guard.canActivate(undefined as any, state);

    expect(result).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: '/games/test-game-id' }
    });
  });

  it('allows navigation when authenticated', () => {
    userContextService.isLoggedIn.and.returnValue(true);

    const state = { url: '/games/test-game-id' } as RouterStateSnapshot;

    const result = guard.canActivate(undefined as any, state);

    expect(result).toBe(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  describe('edge cases', () => {
    it('redirects with root returnUrl when accessing root path', () => {
      userContextService.isLoggedIn.and.returnValue(false);

      const state = { url: '/' } as RouterStateSnapshot;

      const result = guard.canActivate(undefined as any, state);

      expect(result).toBe(false);
      expect(router.navigate).toHaveBeenCalledWith(['/login'], {
        queryParams: { returnUrl: '/' }
      });
    });

    it('redirects with empty returnUrl when state url is empty', () => {
      userContextService.isLoggedIn.and.returnValue(false);

      const state = { url: '' } as RouterStateSnapshot;

      const result = guard.canActivate(undefined as any, state);

      expect(result).toBe(false);
      expect(router.navigate).toHaveBeenCalledWith(['/login'], {
        queryParams: { returnUrl: '' }
      });
    });

    it('redirects with complex returnUrl containing query params', () => {
      userContextService.isLoggedIn.and.returnValue(false);

      const state = { url: '/dashboard?view=stats&filter=active' } as RouterStateSnapshot;

      const result = guard.canActivate(undefined as any, state);

      expect(result).toBe(false);
      expect(router.navigate).toHaveBeenCalledWith(['/login'], {
        queryParams: { returnUrl: '/dashboard?view=stats&filter=active' }
      });
    });

    it('allows navigation when authenticated with root path', () => {
      userContextService.isLoggedIn.and.returnValue(true);

      const state = { url: '/' } as RouterStateSnapshot;

      const result = guard.canActivate(undefined as any, state);

      expect(result).toBe(true);
      expect(router.navigate).not.toHaveBeenCalled();
    });

    it('allows navigation when authenticated with path containing special characters', () => {
      userContextService.isLoggedIn.and.returnValue(true);

      const state = { url: '/teams/team-123/players/player-456' } as RouterStateSnapshot;

      const result = guard.canActivate(undefined as any, state);

      expect(result).toBe(true);
      expect(router.navigate).not.toHaveBeenCalled();
    });

    it('redirects with returnUrl containing hash fragment', () => {
      userContextService.isLoggedIn.and.returnValue(false);

      const state = { url: '/dashboard#section-stats' } as RouterStateSnapshot;

      const result = guard.canActivate(undefined as any, state);

      expect(result).toBe(false);
      expect(router.navigate).toHaveBeenCalledWith(['/login'], {
        queryParams: { returnUrl: '/dashboard#section-stats' }
      });
    });

    it('handles multiple consecutive unauthenticated checks consistently', () => {
      userContextService.isLoggedIn.and.returnValue(false);

      const state1 = { url: '/games' } as RouterStateSnapshot;
      const state2 = { url: '/teams' } as RouterStateSnapshot;

      const result1 = guard.canActivate(undefined as any, state1);
      const result2 = guard.canActivate(undefined as any, state2);

      expect(result1).toBe(false);
      expect(result2).toBe(false);
      expect(router.navigate).toHaveBeenCalledTimes(2);
      expect(router.navigate).toHaveBeenCalledWith(['/login'], {
        queryParams: { returnUrl: '/games' }
      });
      expect(router.navigate).toHaveBeenCalledWith(['/login'], {
        queryParams: { returnUrl: '/teams' }
      });
    });

    it('handles multiple consecutive authenticated checks consistently', () => {
      userContextService.isLoggedIn.and.returnValue(true);

      const state1 = { url: '/games' } as RouterStateSnapshot;
      const state2 = { url: '/teams' } as RouterStateSnapshot;

      const result1 = guard.canActivate(undefined as any, state1);
      const result2 = guard.canActivate(undefined as any, state2);

      expect(result1).toBe(true);
      expect(result2).toBe(true);
      expect(router.navigate).not.toHaveBeenCalled();
    });
  });
});

