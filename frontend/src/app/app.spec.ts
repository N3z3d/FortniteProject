import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AppComponent } from './app.component';
import { UserContextService } from './core/services/user-context.service';

describe('App', () => {
  let userContextService: jasmine.SpyObj<UserContextService>;

  beforeEach(async () => {
    userContextService = jasmine.createSpyObj('UserContextService', [
      'getCurrentUser',
      'getAvailableProfiles',
      'login',
      'logout'
    ]);

    await TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        AppComponent
      ],
      providers: [
        provideZonelessChangeDetection(),
        { provide: UserContextService, useValue: userContextService }
      ]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render router outlet', () => {
    const fixture = TestBed.createComponent(AppComponent);
    spyOn(fixture.componentInstance, 'isDevelopment').and.returnValue(false);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('router-outlet')).toBeTruthy();
  });
});
