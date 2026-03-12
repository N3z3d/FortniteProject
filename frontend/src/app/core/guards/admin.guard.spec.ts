import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { UserContextService } from '../services/user-context.service';
import { AdminGuard } from './admin.guard';

describe('AdminGuard', () => {
  let guard: AdminGuard;
  let userContextService: jasmine.SpyObj<UserContextService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    const userContextSpy = jasmine.createSpyObj('UserContextService', ['isAdmin']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        AdminGuard,
        { provide: UserContextService, useValue: userContextSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });

    guard = TestBed.inject(AdminGuard);
    userContextService = TestBed.inject(UserContextService) as jasmine.SpyObj<UserContextService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  it('should be created', () => {
    expect(guard).toBeTruthy();
  });

  it('should allow access when the user context is admin', () => {
    userContextService.isAdmin.and.returnValue(true);

    expect(guard.canActivate()).toBeTrue();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('should deny access for non-admin users and redirect to /games', () => {
    userContextService.isAdmin.and.returnValue(false);

    expect(guard.canActivate()).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/games']);
  });
});
