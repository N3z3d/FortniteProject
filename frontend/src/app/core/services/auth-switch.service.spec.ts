import { fakeAsync, tick, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthSwitchResponse, AuthSwitchService } from './auth-switch.service';
import { LoggerService } from './logger.service';
import { TranslationService } from './translation.service';
import { environment } from '../../../environments/environment';

describe('AuthSwitchService', () => {
  let service: AuthSwitchService;
  let httpMock: HttpTestingController;
  let logger: jasmine.SpyObj<LoggerService>;
  let translationService: jasmine.SpyObj<TranslationService>;
  let originalProduction: boolean;

  beforeEach(() => {
    originalProduction = environment.production;
    logger = jasmine.createSpyObj('LoggerService', ['info', 'warn', 'error', 'debug']);
    translationService = jasmine.createSpyObj('TranslationService', ['t']);
    translationService.t.and.callFake((_key: string, fallback?: string) => fallback || '');

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        { provide: LoggerService, useValue: logger },
        { provide: TranslationService, useValue: translationService }
      ]
    });

    service = TestBed.inject(AuthSwitchService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    environment.production = originalProduction;
    httpMock.verify();
  });

  it('switchUser returns mock response in dev mode', fakeAsync(() => {
    let response: AuthSwitchResponse | undefined;

    service.switchUser('Thibaut').subscribe(result => (response = result));
    tick(300);

    expect(response?.success).toBeTrue();
    expect(response?.username).toBe('Thibaut');
    expect(response?.message).toContain('Thibaut');
    expect(translationService.t).toHaveBeenCalledWith(
      'authSwitch.messages.switchSuccess',
      'Switched to {username} successfully'
    );
    expect(logger.info).toHaveBeenCalled();
  }));

  it('canSwitchToUser validates usernames', () => {
    let result = true;

    service.canSwitchToUser('').subscribe(value => (result = value));
    expect(result).toBeFalse();

    service.canSwitchToUser('Thibaut').subscribe(value => (result = value));
    expect(result).toBeTrue();

    service.canSwitchToUser('Unknown').subscribe(value => (result = value));
    expect(result).toBeFalse();
  });

  it('getAvailableUsers returns mock list in dev mode', fakeAsync(() => {
    let users: string[] = [];

    service.getAvailableUsers().subscribe(result => (users = result));
    tick(200);

    expect(users).toEqual(['Thibaut', 'Marcel', 'Teddy', 'Sarah']);
  }));

  it('notifyUserSwitch returns mock response in dev mode', fakeAsync(() => {
    let response: AuthSwitchResponse | undefined;

    service.notifyUserSwitch('A', 'B').subscribe(result => (response = result));
    tick(100);

    expect(response?.success).toBeTrue();
    expect(response?.message).toContain('A');
    expect(response?.message).toContain('B');
    expect(logger.info).toHaveBeenCalled();
  }));

  it('validateUserSession returns true in dev mode', fakeAsync(() => {
    let valid = false;

    service.validateUserSession('Any').subscribe(result => (valid = result));
    tick(100);

    expect(valid).toBeTrue();
  }));

  it('switchUser falls back to mock response on production error', fakeAsync(() => {
    environment.production = true;

    let response: AuthSwitchResponse | undefined;
    service.switchUser('Marcel').subscribe(result => (response = result));

    const request = httpMock.expectOne('http://localhost:8080/api/auth/switch');
    expect(request.request.method).toBe('POST');
    request.flush({ error: 'boom' }, { status: 500, statusText: 'Server Error' });

    tick(300);

    expect(response?.success).toBeTrue();
    expect(response?.username).toBe('Marcel');
    expect(logger.warn).toHaveBeenCalled();
  }));

  it('getAvailableUsers returns fallback list on production error', () => {
    environment.production = true;

    let users: string[] = [];
    service.getAvailableUsers().subscribe(result => (users = result));

    const request = httpMock.expectOne('http://localhost:8080/api/auth/available-users');
    request.flush('fail', { status: 500, statusText: 'Server Error' });

    expect(users).toEqual(['Thibaut', 'Marcel', 'Teddy', 'Sarah']);
  });

  it('validateUserSession returns true on production error', () => {
    environment.production = true;

    let valid = false;
    service.validateUserSession('Any').subscribe(result => (valid = result));

    const request = httpMock.expectOne('http://localhost:8080/api/auth/validate-session');
    request.flush('fail', { status: 500, statusText: 'Server Error' });

    expect(valid).toBeTrue();
    expect(logger.warn).toHaveBeenCalled();
  });

  it('logSwitchActivity writes a debug entry', () => {
    service.logSwitchActivity({
      action: 'switch',
      username: 'Tester',
      success: true
    });

    expect(logger.debug).toHaveBeenCalled();
  });

  it('handleSwitchError returns fallback response and logs activity', fakeAsync(() => {
    let response: AuthSwitchResponse | undefined;

    (service as any).handleSwitchError({ message: 'boom' }, 'Sarah')
      .subscribe((result: AuthSwitchResponse) => (response = result));

    tick();

    expect(logger.error).toHaveBeenCalled();
    expect(logger.debug).toHaveBeenCalled();
    expect(response?.success).toBeFalse();
    expect(response?.message).toContain('Sarah');
  }));
});
