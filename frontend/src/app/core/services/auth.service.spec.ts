import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService, LoginApiResponse } from './auth.service';
import { environment } from '../../../environments/environment';

const MOCK_RESPONSE: LoginApiResponse = {
  token: 'eyJhbGciOiJIUzI1NiJ9.test',
  refreshToken: 'refresh-token',
  user: { id: 'uuid-2', email: 'thibaut@fortnite-pronos.com', role: 'USER' }
};

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  describe('login()', () => {
    it('calls POST /api/auth/login with username and password', () => {
      service.login('thibaut', 'Admin1234').subscribe();

      const req = httpMock.expectOne(`${environment.apiUrl}/api/auth/login`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ username: 'thibaut', password: 'Admin1234' });
      req.flush(MOCK_RESPONSE);
    });

    it('stores token and user in sessionStorage on success', () => {
      service.login('thibaut', 'Admin1234').subscribe();

      const req = httpMock.expectOne(`${environment.apiUrl}/api/auth/login`);
      req.flush(MOCK_RESPONSE);

      expect(sessionStorage.getItem('jwt_token')).toBe(MOCK_RESPONSE.token);
      expect(JSON.parse(sessionStorage.getItem('jwt_user')!)).toEqual(MOCK_RESPONSE.user);
    });

    it('propagates HTTP 401 error without storing token', () => {
      let errorThrown = false;
      service.login('thibaut', 'wrong').subscribe({
        error: () => { errorThrown = true; }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/auth/login`);
      req.flush({ message: 'Identifiants invalides' }, { status: 401, statusText: 'Unauthorized' });

      expect(errorThrown).toBe(true);
      expect(sessionStorage.getItem('jwt_token')).toBeNull();
    });

    it('propagates network error', () => {
      let errorThrown = false;
      service.login('thibaut', 'Admin1234').subscribe({
        error: () => { errorThrown = true; }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/auth/login`);
      req.error(new ProgressEvent('error'));

      expect(errorThrown).toBe(true);
      expect(sessionStorage.getItem('jwt_token')).toBeNull();
    });
  });

  describe('getToken()', () => {
    it('returns null when sessionStorage is empty', () => {
      expect(service.getToken()).toBeNull();
    });

    it('returns token stored in sessionStorage', () => {
      sessionStorage.setItem('jwt_token', 'my-token');
      expect(service.getToken()).toBe('my-token');
    });
  });

  describe('storeToken()', () => {
    it('stores token and user in sessionStorage', () => {
      service.storeToken('tok', MOCK_RESPONSE.user);
      expect(sessionStorage.getItem('jwt_token')).toBe('tok');
      expect(JSON.parse(sessionStorage.getItem('jwt_user')!)).toEqual(MOCK_RESPONSE.user);
    });
  });

  describe('clearToken()', () => {
    it('removes jwt_token and jwt_user from sessionStorage', () => {
      sessionStorage.setItem('jwt_token', 'tok');
      sessionStorage.setItem('jwt_user', '{}');
      service.clearToken();
      expect(sessionStorage.getItem('jwt_token')).toBeNull();
      expect(sessionStorage.getItem('jwt_user')).toBeNull();
    });
  });

  describe('getStoredUser()', () => {
    it('returns null when no user stored', () => {
      expect(service.getStoredUser()).toBeNull();
    });

    it('returns parsed user object', () => {
      sessionStorage.setItem('jwt_user', JSON.stringify(MOCK_RESPONSE.user));
      expect(service.getStoredUser()).toEqual(MOCK_RESPONSE.user);
    });

    it('returns null on malformed JSON', () => {
      sessionStorage.setItem('jwt_user', 'not-json');
      expect(service.getStoredUser()).toBeNull();
    });
  });
});
