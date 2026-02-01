import { Component, OnInit, OnDestroy, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject, takeUntil } from 'rxjs';
import { AccessibilityAnnouncerService } from '../../services/accessibility-announcer.service';
import { FocusManagementService } from '../../services/focus-management.service';
import { BrowserNavigationService } from '../../services/browser-navigation.service';

export interface AccessibleErrorInfo {
  title: string;
  message: string;
  timestamp: Date;
  status?: number;
  code?: string;
  path?: string;
  validationErrors?: { [key: string]: string };
  requestId?: string;
  recoveryActions?: Array<{
    label: string;
    action: () => void;
    keyboardShortcut?: string;
  }>;
}

@Component({
  selector: 'app-accessible-error-handler',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './accessible-error-handler.component.html',
  styleUrls: ['./accessible-error-handler.component.scss']
})
export class AccessibleErrorHandlerComponent implements OnInit, OnDestroy {
  @ViewChild('errorTitle') errorTitle!: ElementRef;
  @ViewChild('recoveryActions') recoveryActions!: ElementRef;

  currentError: AccessibleErrorInfo | null = null;
  detailsExpanded = false;
  errorTitleId = 'error-title-' + Math.random().toString(36).substr(2, 9);
  errorDescriptionId = 'error-desc-' + Math.random().toString(36).substr(2, 9);

  private destroy$ = new Subject<void>();

  constructor(
    private accessibilityAnnouncer: AccessibilityAnnouncerService,
    private focusManagement: FocusManagementService,
    private navigation: BrowserNavigationService
  ) {}

  ngOnInit(): void {
    // Écouter les raccourcis clavier globaux
    document.addEventListener('keydown', this.handleKeydown.bind(this));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    document.removeEventListener('keydown', this.handleKeydown.bind(this));
  }

  /**
   * Affiche une erreur avec informations d'accessibilité
   */
  showError(error: AccessibleErrorInfo): void {
    this.currentError = error;
    
    // Annoncer l'erreur
    this.accessibilityAnnouncer.announceTechnicalError(
      `${error.message}. Dialogue d'erreur ouvert avec actions de récupération`
    );

    // Focus sur le titre après un court délai
    setTimeout(() => {
      this.focusErrorTitle();
    }, 100);
  }

  /**
   * Crée une erreur depuis une HttpErrorResponse
   */
  static fromHttpError(error: HttpErrorResponse): AccessibleErrorInfo {
    const errorInfo: AccessibleErrorInfo = {
      title: 'Erreur de communication avec le serveur',
      message: 'Une erreur est survenue lors de la communication avec le serveur.',
      timestamp: new Date(),
      status: error.status
    };

    // Traitement spécifique selon le type d'erreur
    if (error.error && typeof error.error === 'object') {
      // Erreur du GlobalExceptionHandler backend
      errorInfo.message = error.error.message || errorInfo.message;
      errorInfo.code = error.error.code;
      errorInfo.path = error.error.path;
      errorInfo.requestId = error.error.requestId;
      errorInfo.validationErrors = error.error.validationErrors;
    }

    // Messages spécifiques par code de statut
    switch (error.status) {
      case 0:
        errorInfo.title = 'Problème de connexion';
        errorInfo.message = 'Impossible de contacter le serveur. Vérifiez votre connexion internet.';
        break;
      case 400:
        errorInfo.title = 'Demande invalide';
        break;
      case 401:
        errorInfo.title = 'Authentification requise';
        errorInfo.message = 'Vous devez vous authentifier pour accéder à cette ressource.';
        break;
      case 403:
        errorInfo.title = 'Accès refusé';
        errorInfo.message = 'Vous n\'avez pas les droits pour effectuer cette action.';
        break;
      case 404:
        errorInfo.title = 'Ressource introuvable';
        errorInfo.message = 'La ressource demandée n\'existe pas ou n\'est plus disponible.';
        break;
      case 500:
        errorInfo.title = 'Erreur interne du serveur';
        errorInfo.message = 'Une erreur technique est survenue. L\'équipe technique a été notifiée.';
        break;
    }

    return errorInfo;
  }

  /**
   * Cache l'erreur actuelle
   */
  hideError(): void {
    if (this.currentError) {
      this.accessibilityAnnouncer.announceErrorRecovery('Dialogue d\'erreur fermé');
      this.currentError = null;
      this.detailsExpanded = false;
      
      // Retourner le focus à l'élément précédent
      this.focusManagement.restoreFocus();
    }
  }

  /**
   * Focus sur le titre de l'erreur
   */
  focusErrorTitle(): void {
    if (this.errorTitle) {
      this.focusManagement.focusElement(this.errorTitle.nativeElement);
    }
  }

  /**
   * Focus sur les actions de récupération
   */
  focusRecoveryActions(): void {
    if (this.recoveryActions) {
      const firstButton = this.recoveryActions.nativeElement.querySelector('button');
      if (firstButton) {
        this.focusManagement.focusElement(firstButton);
      }
    }
  }

  /**
   * Vérifie si l'erreur a des informations détaillées
   */
  hasDetailedInfo(): boolean {
    return !!(this.currentError?.status || 
              this.currentError?.code || 
              this.currentError?.path || 
              this.currentError?.requestId || 
              this.currentError?.validationErrors);
  }

  /**
   * Retourne les erreurs de validation sous forme de tableau
   */
  getValidationErrorsArray(): Array<{field: string, message: string}> {
    if (!this.currentError?.validationErrors) {
      return [];
    }

    return Object.entries(this.currentError.validationErrors)
      .map(([field, message]) => ({ field, message }));
  }

  /**
   * Actions par défaut de récupération
   */
  getDefaultRecoveryActions() {
    return [
      {
        label: 'Réessayer',
        action: () => this.navigation.reload(),
        keyboardShortcut: 'R'
      },
      {
        label: 'Retour à l\'accueil',
        action: () => this.navigation.navigateHome(),
        keyboardShortcut: 'H'
      },
      {
        label: 'Fermer',
        action: () => this.hideError(),
        keyboardShortcut: 'Échap'
      }
    ];
  }

  /**
   * Exécute une action de récupération
   */
  executeRecoveryAction(action: any): void {
    this.accessibilityAnnouncer.announcePolite(`Exécution de l'action : ${action.label}`);
    action.action();
  }

  /**
   * Gestion des raccourcis clavier
   */
  private handleKeydown(event: KeyboardEvent): void {
    if (!this.currentError) return;

    switch (event.key) {
      case 'Escape':
        event.preventDefault();
        this.hideError();
        break;
      case 'r':
      case 'R':
        if (event.altKey) {
          event.preventDefault();
          this.navigation.reload();
        }
        break;
      case 'h':
      case 'H':
        if (event.altKey) {
          event.preventDefault();
          this.navigation.navigateHome();
        }
        break;
    }
  }
}

