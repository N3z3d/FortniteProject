import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { AdminGuard } from './admin.guard';
import { UserContextService } from '../services/user-context.service';

describe('AdminGuard', () => {
  let guard: AdminGuard;
  let userContextService: jasmine.SpyObj<UserContextService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    const userContextSpy = jasmine.createSpyObj('UserContextService', ['getCurrentUser']);
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

  it('should allow access for Administrateur role', () => {
    userContextService.getCurrentUser.and.returnValue({
      id: '1', username: 'Thibaut', email: 'thibaut@test.com', role: 'Administrateur'
    });

    expect(guard.canActivate()).toBeTrue();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('should deny access for Joueur role and redirect to /games', () => {
    userContextService.getCurrentUser.and.returnValue({
      id: '2', username: 'Marcel', email: 'marcel@test.com', role: 'Joueur'
    });

    expect(guard.canActivate()).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/games']);
  });

  it('should deny access when user is null', () => {
    userContextService.getCurrentUser.and.returnValue(null);

    expect(guard.canActivate()).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/games']);
  });

  it('should deny access for Modérateur role', () => {
    userContextService.getCurrentUser.and.returnValue({
      id: '4', username: 'Sarah', email: 'sarah@test.com', role: 'Modérateur'
    });

    expect(guard.canActivate()).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/games']);
  });

  it('should deny access when role is undefined', () => {
    userContextService.getCurrentUser.and.returnValue({
      id: '5', username: 'NoRole', email: 'norole@test.com'
    });

    expect(guard.canActivate()).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/games']);
  });
});
