import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { AppComponent } from './app.component';
import { UserContextService } from './core/services/user-context.service';
import { TranslationService } from './core/services/translation.service';

describe('App', () => {
  let userContextService: jasmine.SpyObj<UserContextService>;
  let translationService: jasmine.SpyObj<TranslationService>;

  beforeEach(async () => {
    userContextService = jasmine.createSpyObj('UserContextService', [
      'getCurrentUser',
      'getAvailableProfiles',
      'login',
      'logout'
    ], {
      userChanged$: of(null)
    });
    translationService = jasmine.createSpyObj('TranslationService', ['setCurrentUserId']);

    await TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        AppComponent
      ],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: UserContextService, useValue: userContextService },
        { provide: TranslationService, useValue: translationService }
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
