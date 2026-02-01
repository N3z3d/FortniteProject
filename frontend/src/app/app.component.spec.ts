import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AppComponent } from './app.component';
import { UserContextService } from './core/services/user-context.service';
import { environment } from '../environments/environment';

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let userContextService: jasmine.SpyObj<UserContextService>;

  beforeEach(async () => {
    userContextService = jasmine.createSpyObj('UserContextService', ['getCurrentUser', 'login', 'logout']);

    await TestBed.configureTestingModule({
      imports: [AppComponent, RouterTestingModule],
      providers: [
        { provide: UserContextService, useValue: userContextService }
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
});
