import { TestBed } from '@angular/core/testing';
import { UserContextService, UserProfile } from './user-context.service';
import { skip, take } from 'rxjs';

describe('UserContextService', () => {
  let service: UserContextService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [UserContextService]
    });
    service = TestBed.inject(UserContextService);
  });

  afterEach(() => {
    sessionStorage.clear();
    localStorage.clear();
  });

  describe('getAvailableProfiles', () => {
    it('should return all available profiles including SARAH', () => {
      const profiles = service.getAvailableProfiles();
      
      expect(profiles.length).toBe(4);
      expect(profiles).toEqual([
        { id: '1', username: 'Thibaut', email: 'thibaut@test.com', role: 'Administrateur' },
        { id: '2', username: 'Marcel', email: 'marcel@test.com', role: 'Joueur' },
        { id: '3', username: 'Teddy', email: 'teddy@test.com', role: 'Joueur' },
        { id: '4', username: 'Sarah', email: 'sarah@test.com', role: 'Modérateur' }
      ]);
    });
  });

  describe('getCurrentUser', () => {
    it('should return null when no user is logged in', () => {
      const currentUser = service.getCurrentUser();
      expect(currentUser).toBeNull();
    });

    it('should return the logged in user from session storage', () => {
      const testUser: UserProfile = { id: '1', username: 'Thibaut', email: 'thibaut@test.com' };
      sessionStorage.setItem('currentUser', JSON.stringify(testUser));
      
      const currentUser = service.getCurrentUser();
      expect(currentUser).toEqual(testUser);
    });
  });

  describe('login', () => {
    it('should set the current user in session storage', () => {
      const testUser: UserProfile = { id: '2', username: 'Marcel', email: 'marcel@test.com' };
      
      spyOn(service as any, 'generateBrowserFingerprint').and.returnValue('fingerprint-123');
      service.login(testUser);
      
      const storedUser = sessionStorage.getItem('currentUser');
      expect(storedUser).toBeTruthy();

      const parsedUser = JSON.parse(storedUser!);
      expect(parsedUser).toEqual(jasmine.objectContaining({
        id: '2',
        username: 'Marcel',
        email: 'marcel@test.com',
        browserFingerprint: 'fingerprint-123'
      }));
      expect(parsedUser.lastLoginDate).toBeTruthy();
    });

    it('should emit user change event', (done) => {
      const testUser: UserProfile = { id: '3', username: 'Teddy', email: 'teddy@test.com' };
      
      spyOn(service as any, 'generateBrowserFingerprint').and.returnValue('fingerprint-456');

      service.userChanged$.pipe(skip(1), take(1)).subscribe(user => {
        expect(user).toEqual(jasmine.objectContaining({
          id: '3',
          username: 'Teddy',
          email: 'teddy@test.com',
          browserFingerprint: 'fingerprint-456'
        }));
        expect(user?.lastLoginDate).toEqual(jasmine.any(Date));
        done();
      });
      
      service.login(testUser);
    });
  });

  describe('logout', () => {
    it('should clear the current user from session storage', () => {
      const testUser: UserProfile = { id: '4', username: 'Sarah', email: 'sarah@test.com' };
      sessionStorage.setItem('currentUser', JSON.stringify(testUser));

      service.logout();

      const storedUser = sessionStorage.getItem('currentUser');
      expect(storedUser).toBeNull();
    });

    it('should emit null user change event', (done) => {
      const testUser: UserProfile = { id: '1', username: 'Thibaut', email: 'thibaut@test.com' };
      sessionStorage.setItem('currentUser', JSON.stringify(testUser));

      service.userChanged$.pipe(skip(1), take(1)).subscribe(user => {
        expect(user).toBeNull();
        done();
      });

      service.logout();
    });

    it('should clear all storage including localStorage on logout', () => {
      const testUser: UserProfile = { id: '1', username: 'Thibaut', email: 'thibaut@test.com' };

      // Simulate a complete login (sets both sessionStorage and localStorage)
      service.login(testUser);

      // Verify data is stored
      expect(sessionStorage.getItem('currentUser')).toBeTruthy();
      expect(localStorage.getItem('lastUser')).toBeTruthy();
      expect(localStorage.getItem('autoLogin')).toBe('true');

      // Perform logout
      service.logout();

      // Verify all storage is cleared
      expect(sessionStorage.getItem('currentUser')).toBeNull();
      expect(localStorage.getItem('lastUser')).toBeNull();
      expect(localStorage.getItem('autoLogin')).toBeNull();
    });

    it('should prevent auto-login after logout', () => {
      const testUser: UserProfile = { id: '2', username: 'Marcel', email: 'marcel@test.com' };

      // Login and enable auto-login
      service.login(testUser);
      expect(service.isAutoLoginEnabled()).toBe(true);

      // Logout
      service.logout();

      // Verify auto-login is disabled
      expect(service.isAutoLoginEnabled()).toBe(false);
      expect(service.getLastUser()).toBeNull();
    });

    it('should not allow attemptAutoLogin after logout', () => {
      const testUser: UserProfile = { id: '3', username: 'Teddy', email: 'teddy@test.com' };

      // Login
      service.login(testUser);
      expect(service.getCurrentUser()).toBeTruthy();

      // Logout
      service.logout();

      // Attempt auto-login should return null
      const autoLoginResult = service.attemptAutoLogin();
      expect(autoLoginResult).toBeNull();
      expect(service.getCurrentUser()).toBeNull();
    });
  });

  describe('isLoggedIn', () => {
    it('should return false when no user is logged in', () => {
      const isLoggedIn = service.isLoggedIn();
      expect(isLoggedIn).toBe(false);
    });

    it('should return true when user is logged in', () => {
      const testUser: UserProfile = { id: '2', username: 'Marcel', email: 'marcel@test.com' };
      sessionStorage.setItem('currentUser', JSON.stringify(testUser));
      
      const isLoggedIn = service.isLoggedIn();
      expect(isLoggedIn).toBe(true);
    });
  });

  describe('getUserById', () => {
    it('should return the correct user profile by ID', () => {
      const user = service.getUserById('4');
      expect(user).toEqual({ id: '4', username: 'Sarah', email: 'sarah@test.com', role: 'Modérateur' });
    });

    it('should return undefined for non-existent user ID', () => {
      const user = service.getUserById('999');
      expect(user).toBeUndefined();
    });
  });

  describe('getUserByUsername', () => {
    it('should return the correct user profile by username', () => {
      const user = service.getUserByUsername('Sarah');
      expect(user).toEqual({ id: '4', username: 'Sarah', email: 'sarah@test.com', role: 'Modérateur' });
    });

    it('should return undefined for non-existent username', () => {
      const user = service.getUserByUsername('NonExistent');
      expect(user).toBeUndefined();
    });
  });
}); 
