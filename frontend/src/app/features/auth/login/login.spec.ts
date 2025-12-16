import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';

import { LoginComponent } from './login.component';
import { UserContextService } from '../../../core/services/user-context.service';
import { AccessibilityAnnouncerService } from '../../../shared/services/accessibility-announcer.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        { provide: Router, useValue: { navigate: jasmine.createSpy('navigate') } },
        { provide: ActivatedRoute, useValue: { queryParams: of({}) } },
        {
          provide: UserContextService,
          useValue: {
            getCurrentUser: () => null,
            attemptAutoLogin: () => null,
            getAvailableProfiles: () => []
          }
        },
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
});
