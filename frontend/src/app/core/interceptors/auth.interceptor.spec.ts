import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthInterceptor } from './auth.interceptor';
import { UserContextService } from '../services/user-context.service';
import { environment } from '../../../environments/environment';

describe('AuthInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let userContextService: jasmine.SpyObj<UserContextService>;

  beforeEach(() => {
    userContextService = jasmine.createSpyObj('UserContextService', ['getCurrentUser', 'getLastUser']);
    userContextService.getLastUser.and.returnValue(null);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([AuthInterceptor])),
        provideHttpClientTesting(),
        { provide: UserContextService, useValue: userContextService },
      ]
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('adds user param and X-Test-User header for API calls when a user is present', () => {
    userContextService.getCurrentUser.and.returnValue({ username: 'Thibaut' } as any);

    http.get('http://localhost:8080/api/games').subscribe();

    const req = httpMock.expectOne(request => request.url === 'http://localhost:8080/api/games');
    expect(req.request.params.get('user')).toBe('Thibaut');
    expect(req.request.headers.get('X-Test-User')).toBe('Thibaut');
    req.flush([]);
  });

  it('keeps existing user param but still sends the X-Test-User header', () => {
    userContextService.getCurrentUser.and.returnValue({ username: 'Marcel' } as any);

    http.get('http://localhost:8080/api/games', { params: { user: 'Existing' } }).subscribe();

    const req = httpMock.expectOne(request => request.url === 'http://localhost:8080/api/games');
    expect(req.request.params.get('user')).toBe('Existing');
    expect(req.request.headers.get('X-Test-User')).toBe('Marcel');
    req.flush([]);
  });

  it('uses last stored user when no current user is logged in', () => {
    userContextService.getCurrentUser.and.returnValue(null);
    userContextService.getLastUser.and.returnValue({ username: 'Sarah' } as any);

    http.get('http://localhost:8080/api/games').subscribe();

    const req = httpMock.expectOne(request => request.url === 'http://localhost:8080/api/games');
    expect(req.request.params.get('user')).toBe('Sarah');
    expect(req.request.headers.get('X-Test-User')).toBe('Sarah');
    req.flush([]);
  });

  it('falls back to default dev user when no user is available and fallback is enabled', () => {
    const originalFallback = environment.enableFallbackData;
    (environment as any).enableFallbackData = true;
    userContextService.getCurrentUser.and.returnValue(null);
    userContextService.getLastUser.and.returnValue(null);

    http.get('http://localhost:8080/api/games').subscribe();

    const req = httpMock.expectOne(request => request.url === 'http://localhost:8080/api/games');
    expect(req.request.params.get('user')).toBe(environment.defaultDevUser);
    expect(req.request.headers.get('X-Test-User')).toBe(environment.defaultDevUser);
    req.flush([]);
    (environment as any).enableFallbackData = originalFallback;
  });

  it('does not add headers when fallback is disabled and no user is available', () => {
    const originalFallback = environment.enableFallbackData;
    (environment as any).enableFallbackData = false;
    userContextService.getCurrentUser.and.returnValue(null);
    userContextService.getLastUser.and.returnValue(null);

    http.get('http://localhost:8080/api/games').subscribe();

    const req = httpMock.expectOne(request => request.url === 'http://localhost:8080/api/games');
    expect(req.request.params.has('user')).toBeFalse();
    expect(req.request.headers.has('X-Test-User')).toBeFalse();
    req.flush([]);
    (environment as any).enableFallbackData = originalFallback;
  });

  it('does not pollute non-API requests', () => {
    userContextService.getCurrentUser.and.returnValue({ username: 'Thibaut' } as any);

    http.get('/assets/logo.png').subscribe();

    const req = httpMock.expectOne(request => request.url === '/assets/logo.png');
    expect(req.request.params.has('user')).toBeFalse();
    expect(req.request.headers.has('X-Test-User')).toBeFalse();
    req.flush('OK', { status: 200, statusText: 'OK' });
  });
});
