import { TestBed, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { AccessibleErrorHandlerComponent, AccessibleErrorInfo } from './accessible-error-handler.component';
import { AccessibilityAnnouncerService } from '../../services/accessibility-announcer.service';
import { FocusManagementService } from '../../services/focus-management.service';
import { BrowserNavigationService } from '../../services/browser-navigation.service';
import { TranslationService } from '../../../core/services/translation.service';
import { HttpErrorResponse } from '@angular/common/http';

describe('AccessibleErrorHandlerComponent', () => {
  let component: AccessibleErrorHandlerComponent;
  let fixture: ComponentFixture<AccessibleErrorHandlerComponent>;
  let mockAccessibilityAnnouncer: jasmine.SpyObj<AccessibilityAnnouncerService>;
  let mockFocusManagement: jasmine.SpyObj<FocusManagementService>;
  let mockNavigation: jasmine.SpyObj<BrowserNavigationService>;
  let mockTranslationService: jasmine.SpyObj<TranslationService>;

  beforeEach(async () => {
    mockAccessibilityAnnouncer = jasmine.createSpyObj('AccessibilityAnnouncerService', [
      'announceTechnicalError',
      'announceErrorRecovery',
      'announcePolite'
    ]);

    mockFocusManagement = jasmine.createSpyObj('FocusManagementService', [
      'focusElement',
      'restoreFocus'
    ]);
    mockNavigation = jasmine.createSpyObj('BrowserNavigationService', [
      'reload',
      'navigateHome'
    ]);
    mockTranslationService = jasmine.createSpyObj('TranslationService', ['t']);
    mockTranslationService.t.and.callFake((key: string) => key);

    await TestBed.configureTestingModule({
      imports: [AccessibleErrorHandlerComponent],
      providers: [
        { provide: AccessibilityAnnouncerService, useValue: mockAccessibilityAnnouncer },
        { provide: FocusManagementService, useValue: mockFocusManagement },
        { provide: BrowserNavigationService, useValue: mockNavigation },
        { provide: TranslationService, useValue: mockTranslationService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AccessibleErrorHandlerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('showError', () => {
    it('should set currentError and announce', fakeAsync(() => {
      const error: AccessibleErrorInfo = {
        title: 'Test Error',
        message: 'Test message',
        timestamp: new Date()
      };

      component.errorTitle = { nativeElement: document.createElement('h2') } as any;
      component.showError(error);

      expect(component.currentError).toBe(error);
      expect(mockAccessibilityAnnouncer.announceTechnicalError).toHaveBeenCalledWith(
        'Test message. errors.handler.errorDialogOpened'
      );

      tick(150);
      expect(mockFocusManagement.focusElement).toHaveBeenCalledWith(component.errorTitle.nativeElement);
    }));
  });

  describe('fromHttpError', () => {
    it('should create error from HttpErrorResponse with status 0', () => {
      const httpError = new HttpErrorResponse({ status: 0 });
      const result = component.fromHttpError(httpError);

      expect(result.title).toBe('errors.handler.connectionProblem');
      expect(result.message).toBe('errors.handler.connectionMessage');
      expect(result.status).toBe(0);
    });

    it('should create error from HttpErrorResponse with status 400', () => {
      const httpError = new HttpErrorResponse({ status: 400 });
      const result = component.fromHttpError(httpError);

      expect(result.title).toBe('errors.handler.invalidRequest');
      expect(result.status).toBe(400);
    });

    it('should create error from HttpErrorResponse with status 401', () => {
      const httpError = new HttpErrorResponse({ status: 401 });
      const result = component.fromHttpError(httpError);

      expect(result.title).toBe('errors.handler.authRequired');
      expect(result.message).toBe('errors.handler.authMessage');
    });

    it('should create error from HttpErrorResponse with status 403', () => {
      const httpError = new HttpErrorResponse({ status: 403 });
      const result = component.fromHttpError(httpError);

      expect(result.title).toBe('errors.handler.forbidden');
      expect(result.message).toBe('errors.handler.forbiddenMessage');
    });

    it('should create error from HttpErrorResponse with status 404', () => {
      const httpError = new HttpErrorResponse({ status: 404 });
      const result = component.fromHttpError(httpError);

      expect(result.title).toBe('errors.handler.notFound');
      expect(result.message).toBe('errors.handler.notFoundMessage');
    });

    it('should create error from HttpErrorResponse with status 500', () => {
      const httpError = new HttpErrorResponse({ status: 500 });
      const result = component.fromHttpError(httpError);

      expect(result.title).toBe('errors.handler.serverError');
      expect(result.message).toBe('errors.handler.serverErrorMessage');
    });

    it('should extract error details from backend response', () => {
      const httpError = new HttpErrorResponse({
        status: 400,
        error: {
          message: 'Custom error message',
          code: 'ERR_400',
          path: '/api/test',
          requestId: 'req-123',
          validationErrors: { field1: 'Invalid value' }
        }
      });

      const result = component.fromHttpError(httpError);

      expect(result.message).toBe('Custom error message');
      expect(result.code).toBe('ERR_400');
      expect(result.path).toBe('/api/test');
      expect(result.requestId).toBe('req-123');
      expect(result.validationErrors).toEqual({ field1: 'Invalid value' });
    });

    it('should use default title and message for unknown status', () => {
      const httpError = new HttpErrorResponse({ status: 418 });
      const result = component.fromHttpError(httpError);

      expect(result.title).toBe('errors.handler.serverCommError');
      expect(result.message).toBe('errors.handler.serverCommMessage');
    });
  });

  describe('hideError', () => {
    it('should clear currentError and announce recovery', () => {
      component.currentError = {
        title: 'Test Error',
        message: 'Test message',
        timestamp: new Date()
      };
      component.detailsExpanded = true;

      component.hideError();

      expect(component.currentError).toBeNull();
      expect(component.detailsExpanded).toBe(false);
      expect(mockAccessibilityAnnouncer.announceErrorRecovery).toHaveBeenCalledWith(
        'errors.handler.errorDialogClosed'
      );
      expect(mockFocusManagement.restoreFocus).toHaveBeenCalled();
    });

    it('should not announce if no current error', () => {
      component.currentError = null;
      component.hideError();

      expect(mockAccessibilityAnnouncer.announceErrorRecovery).not.toHaveBeenCalled();
    });
  });

  describe('hasDetailedInfo', () => {
    it('should return true when error has status', () => {
      component.currentError = {
        title: 'Test',
        message: 'Test',
        timestamp: new Date(),
        status: 400
      };

      expect(component.hasDetailedInfo()).toBe(true);
    });

    it('should return true when error has code', () => {
      component.currentError = {
        title: 'Test',
        message: 'Test',
        timestamp: new Date(),
        code: 'ERR_400'
      };

      expect(component.hasDetailedInfo()).toBe(true);
    });

    it('should return true when error has path', () => {
      component.currentError = {
        title: 'Test',
        message: 'Test',
        timestamp: new Date(),
        path: '/api/test'
      };

      expect(component.hasDetailedInfo()).toBe(true);
    });

    it('should return true when error has requestId', () => {
      component.currentError = {
        title: 'Test',
        message: 'Test',
        timestamp: new Date(),
        requestId: 'req-123'
      };

      expect(component.hasDetailedInfo()).toBe(true);
    });

    it('should return true when error has validation errors', () => {
      component.currentError = {
        title: 'Test',
        message: 'Test',
        timestamp: new Date(),
        validationErrors: { field: 'error' }
      };

      expect(component.hasDetailedInfo()).toBe(true);
    });

    it('should return false when error has no detailed info', () => {
      component.currentError = {
        title: 'Test',
        message: 'Test',
        timestamp: new Date()
      };

      expect(component.hasDetailedInfo()).toBe(false);
    });

    it('should return false when no current error', () => {
      component.currentError = null;
      expect(component.hasDetailedInfo()).toBe(false);
    });
  });

  describe('getValidationErrorsArray', () => {
    it('should convert validation errors to array', () => {
      component.currentError = {
        title: 'Test',
        message: 'Test',
        timestamp: new Date(),
        validationErrors: {
          email: 'Email invalide',
          password: 'Mot de passe trop court'
        }
      };

      const result = component.getValidationErrorsArray();

      expect(result.length).toBe(2);
      expect(result).toContain({ field: 'email', message: 'Email invalide' });
      expect(result).toContain({ field: 'password', message: 'Mot de passe trop court' });
    });

    it('should return empty array when no validation errors', () => {
      component.currentError = {
        title: 'Test',
        message: 'Test',
        timestamp: new Date()
      };

      expect(component.getValidationErrorsArray()).toEqual([]);
    });

    it('should return empty array when no current error', () => {
      component.currentError = null;
      expect(component.getValidationErrorsArray()).toEqual([]);
    });
  });

  describe('getDefaultRecoveryActions', () => {
    it('should return 3 default actions with translated labels', () => {
      const actions = component.getDefaultRecoveryActions();

      expect(actions.length).toBe(3);
      expect(actions[0].label).toBe('errors.handler.retry');
      expect(actions[0].keyboardShortcut).toBe('R');
      expect(actions[1].label).toBe('errors.handler.goHome');
      expect(actions[1].keyboardShortcut).toBe('H');
      expect(actions[2].label).toBe('errors.handler.close');
      expect(actions[2].keyboardShortcut).toBe('errors.handler.escapeKey');
    });

    it('should have working action callbacks', () => {
      spyOn(component, 'hideError');

      const actions = component.getDefaultRecoveryActions();

      actions[0].action();
      expect(mockNavigation.reload).toHaveBeenCalled();

      actions[1].action();
      expect(mockNavigation.navigateHome).toHaveBeenCalled();

      actions[2].action();
      expect(component.hideError).toHaveBeenCalled();
    });

  });

  describe('executeRecoveryAction', () => {
    it('should announce and execute action', () => {
      const mockAction = {
        label: 'Test Action',
        action: jasmine.createSpy('action')
      };

      component.executeRecoveryAction(mockAction);

      expect(mockAccessibilityAnnouncer.announcePolite).toHaveBeenCalledWith(
        'errors.handler.executingActionTest Action'
      );
      expect(mockAction.action).toHaveBeenCalled();
    });
  });

  describe('keyboard shortcuts', () => {
    beforeEach(() => {
      component.currentError = {
        title: 'Test Error',
        message: 'Test message',
        timestamp: new Date()
      };
    });

    it('should hide error on Escape key', () => {
      spyOn(component, 'hideError');
      const event = new KeyboardEvent('keydown', { key: 'Escape' });
      spyOn(event, 'preventDefault');

      component['handleKeydown'](event);

      expect(event.preventDefault).toHaveBeenCalled();
      expect(component.hideError).toHaveBeenCalled();
    });

    it('should reload on Alt+R', () => {
      const event = new KeyboardEvent('keydown', { key: 'r', altKey: true });
      spyOn(event, 'preventDefault');

      component['handleKeydown'](event);

      expect(event.preventDefault).toHaveBeenCalled();
      expect(mockNavigation.reload).toHaveBeenCalled();
    });

    it('should navigate home on Alt+H', () => {
      const event = new KeyboardEvent('keydown', { key: 'h', altKey: true });
      spyOn(event, 'preventDefault');

      component['handleKeydown'](event);

      expect(event.preventDefault).toHaveBeenCalled();
      expect(mockNavigation.navigateHome).toHaveBeenCalled();
    });

    it('should not trigger keyboard shortcuts when no error', () => {
      component.currentError = null;
      spyOn(component, 'hideError');

      const event = new KeyboardEvent('keydown', { key: 'Escape' });
      component['handleKeydown'](event);

      expect(component.hideError).not.toHaveBeenCalled();
    });
  });

  describe('ngOnDestroy', () => {
    it('should complete destroy$ subject', () => {
      spyOn(component['destroy$'], 'next');
      spyOn(component['destroy$'], 'complete');

      component.ngOnDestroy();

      expect(component['destroy$'].next).toHaveBeenCalled();
      expect(component['destroy$'].complete).toHaveBeenCalled();
    });
  });
});
