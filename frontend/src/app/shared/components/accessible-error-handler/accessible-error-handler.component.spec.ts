import { TestBed, ComponentFixture } from '@angular/core/testing';
import { AccessibleErrorHandlerComponent, AccessibleErrorInfo } from './accessible-error-handler.component';
import { AccessibilityAnnouncerService } from '../../services/accessibility-announcer.service';
import { FocusManagementService } from '../../services/focus-management.service';
import { HttpErrorResponse } from '@angular/common/http';

describe('AccessibleErrorHandlerComponent', () => {
  let component: AccessibleErrorHandlerComponent;
  let fixture: ComponentFixture<AccessibleErrorHandlerComponent>;
  let mockAccessibilityAnnouncer: jasmine.SpyObj<AccessibilityAnnouncerService>;
  let mockFocusManagement: jasmine.SpyObj<FocusManagementService>;

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

    await TestBed.configureTestingModule({
      imports: [AccessibleErrorHandlerComponent],
      providers: [
        { provide: AccessibilityAnnouncerService, useValue: mockAccessibilityAnnouncer },
        { provide: FocusManagementService, useValue: mockFocusManagement }
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
    it('should set currentError and announce', (done) => {
      const error: AccessibleErrorInfo = {
        title: 'Test Error',
        message: 'Test message',
        timestamp: new Date()
      };

      component.showError(error);

      expect(component.currentError).toBe(error);
      expect(mockAccessibilityAnnouncer.announceTechnicalError).toHaveBeenCalledWith(
        'Test message. Dialogue d\'erreur ouvert avec actions de récupération'
      );

      setTimeout(() => {
        expect(mockFocusManagement.focusElement).toHaveBeenCalled();
        done();
      }, 150);
    });
  });

  describe('fromHttpError', () => {
    it('should create error from HttpErrorResponse with status 0', () => {
      const httpError = new HttpErrorResponse({ status: 0 });
      const result = AccessibleErrorHandlerComponent.fromHttpError(httpError);

      expect(result.title).toBe('Problème de connexion');
      expect(result.message).toBe('Impossible de contacter le serveur. Vérifiez votre connexion internet.');
      expect(result.status).toBe(0);
    });

    it('should create error from HttpErrorResponse with status 400', () => {
      const httpError = new HttpErrorResponse({ status: 400 });
      const result = AccessibleErrorHandlerComponent.fromHttpError(httpError);

      expect(result.title).toBe('Demande invalide');
      expect(result.status).toBe(400);
    });

    it('should create error from HttpErrorResponse with status 401', () => {
      const httpError = new HttpErrorResponse({ status: 401 });
      const result = AccessibleErrorHandlerComponent.fromHttpError(httpError);

      expect(result.title).toBe('Authentification requise');
      expect(result.message).toBe('Vous devez vous authentifier pour accéder à cette ressource.');
    });

    it('should create error from HttpErrorResponse with status 403', () => {
      const httpError = new HttpErrorResponse({ status: 403 });
      const result = AccessibleErrorHandlerComponent.fromHttpError(httpError);

      expect(result.title).toBe('Accès refusé');
      expect(result.message).toBe('Vous n\'avez pas les droits pour effectuer cette action.');
    });

    it('should create error from HttpErrorResponse with status 404', () => {
      const httpError = new HttpErrorResponse({ status: 404 });
      const result = AccessibleErrorHandlerComponent.fromHttpError(httpError);

      expect(result.title).toBe('Ressource introuvable');
      expect(result.message).toBe('La ressource demandée n\'existe pas ou n\'est plus disponible.');
    });

    it('should create error from HttpErrorResponse with status 500', () => {
      const httpError = new HttpErrorResponse({ status: 500 });
      const result = AccessibleErrorHandlerComponent.fromHttpError(httpError);

      expect(result.title).toBe('Erreur interne du serveur');
      expect(result.message).toBe('Une erreur technique est survenue. L\'équipe technique a été notifiée.');
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

      const result = AccessibleErrorHandlerComponent.fromHttpError(httpError);

      expect(result.message).toBe('Custom error message');
      expect(result.code).toBe('ERR_400');
      expect(result.path).toBe('/api/test');
      expect(result.requestId).toBe('req-123');
      expect(result.validationErrors).toEqual({ field1: 'Invalid value' });
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
        'Dialogue d\'erreur fermé'
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
    it('should return 3 default actions', () => {
      const actions = component.getDefaultRecoveryActions();

      expect(actions.length).toBe(3);
      expect(actions[0].label).toBe('Réessayer');
      expect(actions[0].keyboardShortcut).toBe('R');
      expect(actions[1].label).toBe('Retour à l\'accueil');
      expect(actions[1].keyboardShortcut).toBe('H');
      expect(actions[2].label).toBe('Fermer');
      expect(actions[2].keyboardShortcut).toBe('Échap');
    });

    it('should have working action callbacks', () => {
      spyOn(window.location, 'reload');
      spyOn(component, 'hideError');

      const actions = component.getDefaultRecoveryActions();

      actions[0].action(); // Réessayer
      expect(window.location.reload).toHaveBeenCalled();

      actions[2].action(); // Fermer
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
        'Exécution de l\'action : Test Action'
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
      spyOn(window.location, 'reload');
      const event = new KeyboardEvent('keydown', { key: 'r', altKey: true });
      spyOn(event, 'preventDefault');

      component['handleKeydown'](event);

      expect(event.preventDefault).toHaveBeenCalled();
      expect(window.location.reload).toHaveBeenCalled();
    });

    it('should navigate home on Alt+H', () => {
      const event = new KeyboardEvent('keydown', { key: 'h', altKey: true });
      spyOn(event, 'preventDefault');

      component['handleKeydown'](event);

      expect(event.preventDefault).toHaveBeenCalled();
      expect(window.location.href).toBe('/');
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
