import { TestBed } from '@angular/core/testing';
import { UserContextService, UserProfile } from './user-context.service';

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
  });

  describe('getAvailableProfiles', () => {
    it('should return all available profiles including SARAH', () => {
      const profiles = service.getAvailableProfiles();
      
      expect(profiles.length).toBe(4);
      expect(profiles).toEqual([
        { id: '1', username: 'Thibaut', email: 'thibaut@test.com' },
        { id: '2', username: 'Marcel', email: 'marcel@test.com' },
        { id: '3', username: 'Teddy', email: 'teddy@test.com' },
        { id: '4', username: 'Sarah', email: 'sarah@test.com' }
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
      
      service.login(testUser);
      
      const storedUser = sessionStorage.getItem('currentUser');
      expect(storedUser).toBe(JSON.stringify(testUser));
    });

    it('should emit user change event', (done) => {
      const testUser: UserProfile = { id: '3', username: 'Teddy', email: 'teddy@test.com' };
      
      service.userChanged$.subscribe(user => {
        expect(user).toEqual(testUser);
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
      
      service.userChanged$.subscribe(user => {
        expect(user).toBeNull();
        done();
      });
      
      service.logout();
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
      expect(user).toEqual({ id: '4', username: 'Sarah', email: 'sarah@test.com' });
    });

    it('should return undefined for non-existent user ID', () => {
      const user = service.getUserById('999');
      expect(user).toBeUndefined();
    });
  });

  describe('getUserByUsername', () => {
    it('should return the correct user profile by username', () => {
      const user = service.getUserByUsername('Sarah');
      expect(user).toEqual({ id: '4', username: 'Sarah', email: 'sarah@test.com' });
    });

    it('should return undefined for non-existent username', () => {
      const user = service.getUserByUsername('NonExistent');
      expect(user).toBeUndefined();
    });
  });
}); 