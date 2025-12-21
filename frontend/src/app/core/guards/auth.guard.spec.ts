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
});

