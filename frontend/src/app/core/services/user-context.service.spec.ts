import { TestBed } from '@angular/core/testing';
import { of, skip, take, throwError } from 'rxjs';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { UserContextService, UserProfile } from './user-context.service';
import { AuthService, LoginApiResponse } from './auth.service';

const MOCK_LOGIN_RESPONSE: LoginApiResponse = {
  token: 'test-jwt-token',
  user: { id: 'uuid-2', email: 'thibaut@fortnite-pronos.com', role: 'USER' }
};

const MOCK_ADMIN_LOGIN_RESPONSE: LoginApiResponse = {
  token: 'admin-jwt-token',
  user: { id: 'uuid-1', email: 'admin@fortnite-pronos.com', role: 'ADMIN' }
};

describe('UserContextService', () => {
  let service: UserContextService;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj('AuthService', [
      'login', 'storeToken', 'clearToken', 'getToken', 'getStoredUser'
    ]);
    authServiceSpy.getToken.and.returnValue(null);
    authServiceSpy.getStoredUser.and.returnValue(null);
    authServiceSpy.login.and.returnValue(of(MOCK_LOGIN_RESPONSE));

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        UserContextService,
        { provide: AuthService, useValue: authServiceSpy }
      ]
    });
    service = TestBed.inject(UserContextService);
  });

  afterEach(() => {
    sessionStorage.clear();
    localStorage.clear();
  });

  describe('getAvailableProfiles', () => {
    it('returns local profiles aligned with seeded backend accounts', () => {
      const profiles = service.getAvailableProfiles();
      expect(profiles.length).toBe(4);
      expect(profiles[0].username).toBe('admin');
      expect(profiles[1].username).toBe('thibaut');
    });
  });

  describe('getCurrentUser', () => {
    it('returns null when no user is logged in', () => {
      expect(service.getCurrentUser()).toBeNull();
    });

    it('returns the logged in user from session storage', () => {
      const testUser: UserProfile = { id: '1', username: 'admin', email: 'admin@fortnite-pronos.com' };
      sessionStorage.setItem('currentUser', JSON.stringify(testUser));
      expect(service.getCurrentUser()).toEqual(testUser);
    });
  });

  describe('login', () => {
    it('calls authService.login with username and devUserPassword', () => {
      const testUser: UserProfile = { id: '2', username: 'thibaut', email: 'thibaut@fortnite-pronos.com' };

      service.login(testUser).subscribe();

      expect(authServiceSpy.login).toHaveBeenCalledWith('thibaut', jasmine.any(String));
    });

    it('sets the current user in session storage on success', done => {
      const testUser: UserProfile = { id: '2', username: 'thibaut', email: 'thibaut@fortnite-pronos.com' };
      spyOn(service as any, 'generateBrowserFingerprint').and.returnValue('fp-123');

      service.login(testUser).subscribe(() => {
        const storedUser = sessionStorage.getItem('currentUser');
        expect(storedUser).toBeTruthy();
        const parsed = JSON.parse(storedUser!);
        expect(parsed.username).toBe('thibaut');
        expect(parsed.browserFingerprint).toBe('fp-123');
        done();
      });
    });

    it('emits user change event on success', done => {
      const testUser: UserProfile = { id: '3', username: 'marcel', email: 'marcel@fortnite-pronos.com' };

      service.userChanged$.pipe(skip(1), take(1)).subscribe(user => {
        expect(user?.username).toBe('marcel');
        done();
      });

      service.login(testUser).subscribe();
    });

    it('propagates error when authService.login fails', done => {
      authServiceSpy.login.and.returnValue(throwError(() => new Error('401 Unauthorized')));
      const testUser: UserProfile = { id: '2', username: 'thibaut', email: 'thibaut@fortnite-pronos.com' };

      service.login(testUser).subscribe({
        next: () => fail('should not succeed'),
        error: err => {
          expect(err.message).toContain('401');
          expect(sessionStorage.getItem('currentUser')).toBeNull();
          done();
        }
      });
    });
  });

  describe('logout', () => {
    it('clears session storage and calls authService.clearToken', () => {
      sessionStorage.setItem('currentUser', '{"id":"1","username":"admin","email":"admin@fortnite-pronos.com"}');
      service.logout();
      expect(sessionStorage.getItem('currentUser')).toBeNull();
      expect(authServiceSpy.clearToken).toHaveBeenCalled();
    });

    it('emits null user change event', done => {
      sessionStorage.setItem('currentUser', '{"id":"1","username":"admin","email":"admin@fortnite-pronos.com"}');

      service.userChanged$.pipe(skip(1), take(1)).subscribe(user => {
        expect(user).toBeNull();
        done();
      });

      service.logout();
    });
  });

  describe('isLoggedIn', () => {
    it('returns false when no JWT token', () => {
      authServiceSpy.getToken.and.returnValue(null);
      expect(service.isLoggedIn()).toBeFalse();
    });

    it('returns true when JWT token is present', () => {
      authServiceSpy.getToken.and.returnValue('valid-jwt');
      expect(service.isLoggedIn()).toBeTrue();
    });
  });

  describe('isAdmin', () => {
    it('returns true when JWT user has role ADMIN', () => {
      authServiceSpy.getStoredUser.and.returnValue(MOCK_ADMIN_LOGIN_RESPONSE.user);
      expect(service.isAdmin()).toBeTrue();
    });

    it('returns false when JWT user has role USER', () => {
      authServiceSpy.getStoredUser.and.returnValue(MOCK_LOGIN_RESPONSE.user);
      expect(service.isAdmin()).toBeFalse();
    });

    it('falls back to profile role Administrateur when no JWT user stored', () => {
      authServiceSpy.getStoredUser.and.returnValue(null);
      sessionStorage.setItem('currentUser', JSON.stringify({
        id: '1', username: 'admin', email: 'admin@fortnite-pronos.com', role: 'Administrateur'
      }));
      expect(service.isAdmin()).toBeTrue();
    });

    it('returns false when no JWT user and profile has role Joueur', () => {
      authServiceSpy.getStoredUser.and.returnValue(null);
      sessionStorage.setItem('currentUser', JSON.stringify({
        id: '2', username: 'thibaut', email: 'thibaut@fortnite-pronos.com', role: 'Joueur'
      }));
      expect(service.isAdmin()).toBeFalse();
    });

    it('returns false when no user at all', () => {
      authServiceSpy.getStoredUser.and.returnValue(null);
      expect(service.isAdmin()).toBeFalse();
    });
  });

  describe('getUserById', () => {
    it('returns the correct user profile by ID', () => {
      expect(service.getUserById('1')?.username).toBe('admin');
    });

    it('returns undefined for non-existent ID', () => {
      expect(service.getUserById('999')).toBeUndefined();
    });
  });

  describe('getUserByUsername', () => {
    it('returns the correct user profile by username', () => {
      expect(service.getUserByUsername('admin')?.id).toBe('1');
    });

    it('returns undefined for non-existent username', () => {
      expect(service.getUserByUsername('NonExistent')).toBeUndefined();
    });
  });
});
