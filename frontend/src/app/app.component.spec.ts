import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Subject } from 'rxjs';
import { AppComponent } from './app.component';
import { UserContextService, UserProfile } from './core/services/user-context.service';
import { TranslationService } from './core/services/translation.service';
import { UserGamesStore } from './core/services/user-games.store';
import { environment } from '../environments/environment';

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let userContextService: jasmine.SpyObj<UserContextService>;
  let translationService: jasmine.SpyObj<TranslationService>;
  let userGamesStore: jasmine.SpyObj<UserGamesStore>;
  let userChangedSubject: Subject<UserProfile | null>;

  beforeEach(async () => {
    userChangedSubject = new Subject<UserProfile | null>();
    userContextService = jasmine.createSpyObj('UserContextService', ['getCurrentUser', 'login', 'logout'], {
      userChanged$: userChangedSubject.asObservable()
    });
    translationService = jasmine.createSpyObj('TranslationService', ['setCurrentUserId']);
    userGamesStore = jasmine.createSpyObj<UserGamesStore>('UserGamesStore', ['reset']);

    await TestBed.configureTestingModule({
      imports: [AppComponent, RouterTestingModule],
      providers: [
        { provide: UserContextService, useValue: userContextService },
        { provide: TranslationService, useValue: translationService },
        { provide: UserGamesStore, useValue: userGamesStore }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the app', () => {
    expect(component).toBeTruthy();
  });

  it('should have router outlet in template', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const routerOutlet = compiled.querySelector('router-outlet');

    expect(routerOutlet).toBeTruthy();
  });

  it('should have skip link for accessibility', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const skipLink = compiled.querySelector('.skip-link');

    expect(skipLink).toBeTruthy();
    expect(skipLink?.getAttribute('href')).toBe('#main-content');
  });

  it('should have live region for announcements', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const liveRegion = compiled.querySelector('#announcements');

    expect(liveRegion).toBeTruthy();
    expect(liveRegion?.getAttribute('aria-live')).toBe('polite');
    expect(liveRegion?.getAttribute('aria-atomic')).toBe('true');
  });

  it('should have main content with proper role', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const mainContent = compiled.querySelector('main');

    expect(mainContent).toBeTruthy();
    expect(mainContent?.getAttribute('role')).toBe('main');
    expect(mainContent?.getAttribute('id')).toBe('main-content');
  });

  it('should return false for isDevelopment when production is true', () => {
    const originalProduction = environment.production;
    environment.production = true;

    const result = component.isDevelopment();

    expect(result).toBeFalse();
    environment.production = originalProduction;
  });

  it('should return true for isDevelopment when production is false', () => {
    const originalProduction = environment.production;
    environment.production = false;

    const result = component.isDevelopment();

    expect(result).toBeTrue();
    environment.production = originalProduction;
  });

  it('should call setCurrentUserId when user changes', () => {
    userChangedSubject.next({ id: '1', username: 'Thibaut', email: 'thibaut@test.com' });

    expect(translationService.setCurrentUserId).toHaveBeenCalledWith('1');
  });

  it('should call setCurrentUserId with null on logout', () => {
    userChangedSubject.next(null);

    expect(translationService.setCurrentUserId).toHaveBeenCalledWith(null);
  });

  it('should call userGamesStore.reset() when user becomes null (logout)', () => {
    userChangedSubject.next(null);

    expect(userGamesStore.reset).toHaveBeenCalled();
  });

  it('should not call userGamesStore.reset() when user logs in', () => {
    userChangedSubject.next({ id: '1', username: 'Thibaut', email: 'thibaut@test.com' });

    expect(userGamesStore.reset).not.toHaveBeenCalled();
  });

  it('should unsubscribe on destroy', () => {
    component.ngOnDestroy();

    userChangedSubject.next({ id: '2', username: 'Marcel', email: 'marcel@test.com' });

    expect(translationService.setCurrentUserId).not.toHaveBeenCalledWith('2');
  });
});
