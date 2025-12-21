import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';

import { LoginComponent } from './login.component';
import { UserContextService } from '../../../core/services/user-context.service';
import { AccessibilityAnnouncerService } from '../../../shared/services/accessibility-announcer.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let router: jasmine.SpyObj<Router>;
  let route: ActivatedRoute;
  let userContextService: jasmine.SpyObj<UserContextService>;

  beforeEach(async () => {
    router = jasmine.createSpyObj<Router>('Router', ['navigate', 'navigateByUrl']);
    userContextService = jasmine.createSpyObj<UserContextService>('UserContextService', [
      'getCurrentUser',
      'attemptAutoLogin',
      'getAvailableProfiles',
      'login'
    ]);

    userContextService.getCurrentUser.and.returnValue(null);
    userContextService.attemptAutoLogin.and.returnValue(null);
    userContextService.getAvailableProfiles.and.returnValue([]);

    route = {
      queryParams: of({}),
      snapshot: { queryParams: {} }
    } as unknown as ActivatedRoute;

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: route },
        { provide: UserContextService, useValue: userContextService },
        {
          provide: AccessibilityAnnouncerService,
          useValue: {
            announceLoading: () => {},
            announceSuccess: () => {},
            announceNavigation: () => {},
            announceFormErrors: () => {}
          }
        }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('navigates to returnUrl after login when provided', fakeAsync(() => {
    (route.snapshot as any).queryParams = { returnUrl: '/games/test-game-id' };

    component.selectUser({ id: '1', username: 'Thibaut', email: 'thibaut@test.com' } as any);
    tick(800);

    expect(router.navigateByUrl).toHaveBeenCalledWith('/games/test-game-id');
  }));

  it('defaults to /games after login when returnUrl is missing', fakeAsync(() => {
    (route.snapshot as any).queryParams = {};

    component.selectUser({ id: '1', username: 'Thibaut', email: 'thibaut@test.com' } as any);
    tick(800);

    expect(router.navigate).toHaveBeenCalledWith(['/games'], {
      queryParams: { welcome: 'true', user: 'Thibaut' }
    });
  }));

  it('defaults to /games after login when returnUrl is unsafe', fakeAsync(() => {
    (route.snapshot as any).queryParams = { returnUrl: 'https://example.com/phish' };

    component.selectUser({ id: '1', username: 'Thibaut', email: 'thibaut@test.com' } as any);
    tick(800);

    expect(router.navigate).toHaveBeenCalledWith(['/games'], {
      queryParams: { welcome: 'true', user: 'Thibaut' }
    });
  }));
});
