import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';

describe('AuthInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    authService = jasmine.createSpyObj('AuthService', ['getToken', 'getStoredUser', 'clearToken', 'storeToken']);
    authService.getToken.and.returnValue(null);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([AuthInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authService }
      ]
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('adds Authorization: Bearer header for API calls when a token is present', () => {
    authService.getToken.and.returnValue('my-jwt-token');

    http.get('http://localhost:8080/api/games').subscribe();

    const req = httpMock.expectOne(r => r.url === 'http://localhost:8080/api/games');
    expect(req.request.headers.get('Authorization')).toBe('Bearer my-jwt-token');
    req.flush([]);
  });

  it('does not add Authorization header for API calls when no token', () => {
    authService.getToken.and.returnValue(null);

    http.get('http://localhost:8080/api/games').subscribe();

    const req = httpMock.expectOne(r => r.url === 'http://localhost:8080/api/games');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush([]);
  });

  it('does not add Authorization header for non-API requests', () => {
    authService.getToken.and.returnValue('my-jwt-token');

    http.get('/assets/logo.png').subscribe();

    const req = httpMock.expectOne(r => r.url === '/assets/logo.png');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush('OK', { status: 200, statusText: 'OK' });
  });

  it('does not add X-Test-User header (removed in JWT migration)', () => {
    authService.getToken.and.returnValue('my-jwt-token');

    http.get('http://localhost:8080/api/games').subscribe();

    const req = httpMock.expectOne(r => r.url === 'http://localhost:8080/api/games');
    expect(req.request.headers.has('X-Test-User')).toBeFalse();
    req.flush([]);
  });

  it('does not add user query param (removed in JWT migration)', () => {
    authService.getToken.and.returnValue('my-jwt-token');

    http.get('http://localhost:8080/api/games').subscribe();

    const req = httpMock.expectOne(r => r.url === 'http://localhost:8080/api/games');
    expect(req.request.params.has('user')).toBeFalse();
    req.flush([]);
  });
});
