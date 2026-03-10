import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { LoginComponent } from './login.component';
import { UserContextService, UserProfile } from '../../../core/services/user-context.service';
import { TranslationService } from '../../../core/services/translation.service';
import { LoggerService } from '../../../core/services/logger.service';
import { AccessibilityAnnouncerService } from '../../../shared/services/accessibility-announcer.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let router: jasmine.SpyObj<Router>;
  let userContextService: jasmine.SpyObj<UserContextService>;
  let translationService: jasmine.SpyObj<TranslationService>;
  let loggerService: jasmine.SpyObj<LoggerService>;
  let accessibilityService: jasmine.SpyObj<AccessibilityAnnouncerService>;
  let activatedRoute: any;

  const mockProfiles: UserProfile[] = [
    { username: 'Thibaut', id: '1', email: 'thibaut@test.com' },
    { username: 'Alex', id: '2', email: 'alex@test.com' }
  ];

  beforeEach(async () => {
    router = jasmine.createSpyObj('Router', ['navigate', 'navigateByUrl']);
    userContextService = jasmine.createSpyObj('UserContextService', [
      'getCurrentUser',
      'attemptAutoLogin',
      'login',
      'getAvailableProfiles'
    ]);
    translationService = jasmine.createSpyObj('TranslationService', ['t', 'setLanguage'], {
      currentLanguage: 'fr'
    });
    translationService.t.and.callFake((key: string) => key);
    loggerService = jasmine.createSpyObj('LoggerService', ['info', 'warn', 'error']);
    accessibilityService = jasmine.createSpyObj('AccessibilityAnnouncerService', [
      'announceLoading',
      'announceSuccess',
      'announceNavigation',
      'announceFormErrors'
    ]);

    activatedRoute = {
      snapshot: {
        queryParams: {}
      },
      queryParams: of({})
    };

    userContextService.getAvailableProfiles.and.returnValue(mockProfiles);
    userContextService.getCurrentUser.and.returnValue(null);
    userContextService.attemptAutoLogin.and.returnValue(null);
    userContextService.login.and.returnValue(of(undefined));

    await TestBed.configureTestingModule({
      imports: [LoginComponent, ReactiveFormsModule]
    })
    .overrideProvider(Router, { useValue: router })
    .overrideProvider(ActivatedRoute, { useValue: activatedRoute })
    .overrideProvider(UserContextService, { useValue: userContextService })
    .overrideProvider(TranslationService, { useValue: translationService })
    .overrideProvider(LoggerService, { useValue: loggerService })
    .overrideProvider(AccessibilityAnnouncerService, { useValue: accessibilityService })
    .compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with default state', () => {
    expect(component.isLoading).toBeFalse();
    expect(component.showAlternative).toBeFalse();
    expect(component.isSwitchingUser).toBeFalse();
  });

  it('should render a compact language combobox on login page', () => {
    fixture.detectChanges();

    const select = fixture.nativeElement.querySelector('mat-select');
    const languageButtons = fixture.nativeElement.querySelectorAll('.lang-btn');

    expect(select).toBeTruthy();
    expect(languageButtons.length).toBe(0);
  });

  it('should expose current language option for select trigger', () => {
    fixture.detectChanges();

    expect(component.currentLanguageOption.code).toBe('fr');
    expect(component.currentLanguageOption.flagAsset.startsWith('data:image/svg+xml')).toBeTrue();
  });

  it('should render selected language label in trigger to avoid duplicated FR/EN code', () => {
    fixture.detectChanges();

    const triggerCode = fixture.nativeElement.querySelector('.lang-trigger .lang-code');

    expect(component.currentLanguageOption.label).toBe('Français');
    expect(component.currentLanguageOption.flagAsset.startsWith('data:image/svg+xml')).toBeTrue();
    expect(triggerCode).toBeNull();
  });

  it('should load available profiles on init', () => {
    fixture.detectChanges();

    expect(component.availableProfiles).toEqual(mockProfiles);
    expect(userContextService.getAvailableProfiles).toHaveBeenCalled();
  });

  it('should redirect if user already logged in', () => {
    userContextService.getCurrentUser.and.returnValue(mockProfiles[0]);

    fixture.detectChanges();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/games');
  });

  it('should attempt auto-login if not switching user', () => {
    userContextService.attemptAutoLogin.and.returnValue(mockProfiles[0]);

    fixture.detectChanges();

    expect(userContextService.attemptAutoLogin).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(
      ['/games'],
      { queryParams: { autoLogin: 'true', user: 'Thibaut' } }
    );
  });

  it('should not attempt auto-login if switching user', () => {
    activatedRoute.queryParams = of({ switchUser: 'true' });

    fixture.detectChanges();

    expect(component.isSwitchingUser).toBeTrue();
    expect(userContextService.attemptAutoLogin).not.toHaveBeenCalled();
  });

  it('should handle user selection and login', () => {
    fixture.detectChanges();

    component.selectUser(mockProfiles[0]);

    expect(userContextService.login).toHaveBeenCalledWith(mockProfiles[0]);
    expect(component.isLoading).toBeFalse();
    expect(accessibilityService.announceSuccess).toHaveBeenCalledWith('Login successful for Thibaut');
    expect(router.navigate).toHaveBeenCalledWith(
      ['/games'],
      { queryParams: { welcome: 'true', user: 'Thibaut' } }
    );
  });

  it('should handle custom return URL after login', () => {
    activatedRoute.snapshot.queryParams = { returnUrl: '/dashboard' };
    fixture.detectChanges();

    component.selectUser(mockProfiles[0]);

    expect(router.navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('should reject unsafe return URLs', () => {
    activatedRoute.snapshot.queryParams = { returnUrl: '//external.com/phishing' };
    fixture.detectChanges();

    component.selectUser(mockProfiles[0]);

    // Navigates to /games because unsafe returnUrl is rejected
    expect(router.navigate).toHaveBeenCalledWith(
      ['/games'],
      { queryParams: { welcome: 'true', user: 'Thibaut' } }
    );
  });

  it('should toggle alternative login form', () => {
    expect(component.showAlternative).toBeFalse();

    component.toggleAlternative();

    expect(component.showAlternative).toBeTrue();

    component.toggleAlternative();

    expect(component.showAlternative).toBeFalse();
  });

  it('should initialize quick form with validator', () => {
    expect(component.quickForm).toBeDefined();
    expect(component.quickForm.get('identifier')?.hasError('required')).toBeTrue();
  });

  it('should handle quick form submission with valid identifier', () => {
    fixture.detectChanges();
    component.quickForm.patchValue({ identifier: 'thibaut' });

    component.onQuickSubmit();

    expect(accessibilityService.announceLoading).toHaveBeenCalledWith(true, 'login attempt for thibaut');
    expect(userContextService.login).toHaveBeenCalled();
    expect(component.isLoading).toBeFalse();
    expect(accessibilityService.announceSuccess).toHaveBeenCalledWith('Login successful');
    expect(router.navigate).toHaveBeenCalledWith(['/games']);
  });

  it('should find matching profile on quick submit', () => {
    fixture.detectChanges();
    component.quickForm.patchValue({ identifier: 'alex' });

    component.onQuickSubmit();

    expect(userContextService.login).toHaveBeenCalledWith(mockProfiles[1]);
  });

  it('should use first profile as fallback on quick submit', () => {
    fixture.detectChanges();
    component.quickForm.patchValue({ identifier: 'nonexistent' });

    component.onQuickSubmit();

    expect(userContextService.login).toHaveBeenCalledWith(mockProfiles[0]);
  });

  it('should not submit invalid quick form', () => {
    component.quickForm.patchValue({ identifier: '' });

    component.onQuickSubmit();

    expect(component.isLoading).toBeFalse();
    expect(userContextService.login).not.toHaveBeenCalled();
  });

  it('should announce form errors on invalid submit', () => {
    component.quickForm.patchValue({ identifier: '' });
    component.quickForm.get('identifier')?.markAsTouched();

    component.onFormError();

    expect(accessibilityService.announceFormErrors).toHaveBeenCalledWith(['Identifiant: Un identifiant est requis']);
  });

  it('should handle default return URL when empty', () => {
    activatedRoute.snapshot.queryParams = { returnUrl: '' };
    fixture.detectChanges();

    const safeUrl = component['getSafeReturnUrl']();

    expect(safeUrl).toBe('/games');
  });

  it('should log page load', () => {
    fixture.detectChanges();

    expect(loggerService.info).toHaveBeenCalledWith('Login: page loaded');
  });

  it('should log user selection', () => {
    fixture.detectChanges();

    component.selectUser(mockProfiles[0]);

    expect(loggerService.info).toHaveBeenCalledWith('Login: user selected', { username: 'Thibaut' });
    expect(loggerService.info).toHaveBeenCalledWith('Login: login succeeded', { username: 'Thibaut' });
  });

  it('should switch language via translation service', () => {
    component.switchLanguage('en');

    expect(translationService.setLanguage).toHaveBeenCalledWith('en');
  });
});
