import { Component, OnInit, OnDestroy, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject, takeUntil } from 'rxjs';
import { AccessibilityAnnouncerService } from '../../services/accessibility-announcer.service';
import { FocusManagementService } from '../../services/focus-management.service';

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
  template: `
    <div 
      *ngIf="currentError" 
      class="accessible-error-container"
      role="alert"
      aria-live="assertive"
      aria-atomic="true"
      [attr.aria-labelledby]="errorTitleId"
      [attr.aria-describedby]="errorDescriptionId">
      
      <!-- Skip link pour aller directement aux actions de récupération -->
      <a 
        class="skip-link"
        href="#error-recovery-actions"
        (click)="focusRecoveryActions()"
        [attr.aria-label]="'Aller aux actions de récupération pour : ' + currentError.title">
        Aller aux actions de récupération
      </a>

      <div class="error-content">
        <!-- Titre de l'erreur -->
        <h2 
          [id]="errorTitleId"
          class="error-title"
          tabindex="-1"
          #errorTitle>
          <span class="error-icon" aria-hidden="true">⚠️</span>
          {{ currentError.title }}
        </h2>

        <!-- Message principal -->
        <div 
          [id]="errorDescriptionId"
          class="error-description">
          <p>{{ currentError.message }}</p>
          
          <!-- Informations techniques (collapsibles) -->
          <details 
            *ngIf="hasDetailedInfo()"
            class="error-details">
            <summary 
              class="error-details-toggle"
              [attr.aria-expanded]="detailsExpanded"
              (click)="detailsExpanded = !detailsExpanded">
              Informations techniques
              <span aria-hidden="true">{{ detailsExpanded ? '▼' : '▶' }}</span>
            </summary>
            <div class="error-details-content">
              <dl>
                <dt *ngIf="currentError.status">Code de statut :</dt>
                <dd *ngIf="currentError.status">{{ currentError.status }}</dd>
                
                <dt *ngIf="currentError.code">Code d'erreur :</dt>
                <dd *ngIf="currentError.code">{{ currentError.code }}</dd>
                
                <dt *ngIf="currentError.path">Chemin :</dt>
                <dd *ngIf="currentError.path">{{ currentError.path }}</dd>
                
                <dt *ngIf="currentError.requestId">ID de requête :</dt>
                <dd *ngIf="currentError.requestId">{{ currentError.requestId }}</dd>
                
                <dt>Horodatage :</dt>
                <dd>{{ currentError.timestamp | date:'medium' }}</dd>
              </dl>
              
              <!-- Erreurs de validation -->
              <div *ngIf="currentError.validationErrors" class="validation-errors">
                <h3>Erreurs de validation :</h3>
                <ul>
                  <li *ngFor="let error of getValidationErrorsArray()">
                    <strong>{{ error.field }} :</strong> {{ error.message }}
                  </li>
                </ul>
              </div>
            </div>
          </details>
        </div>

        <!-- Actions de récupération -->
        <div 
          id="error-recovery-actions"
          class="error-recovery"
          #recoveryActions>
          <h3>Actions possibles :</h3>
          <ul class="recovery-actions-list">
            <li *ngFor="let action of currentError.recoveryActions || getDefaultRecoveryActions(); let i = index">
              <button 
                type="button"
                class="recovery-action-btn"
                (click)="executeRecoveryAction(action)"
                [attr.aria-describedby]="action.keyboardShortcut ? 'shortcut-' + i : null">
                {{ action.label }}
              </button>
              <span 
                *ngIf="action.keyboardShortcut"
                [id]="'shortcut-' + i"
                class="keyboard-shortcut"
                aria-label="Raccourci clavier">
                ({{ action.keyboardShortcut }})
              </span>
            </li>
          </ul>
        </div>

        <!-- Instructions d'accessibilité -->
        <div class="accessibility-instructions" aria-label="Instructions de navigation">
          <p>
            <span class="sr-only">Instructions : </span>
            Utilisez Tab et Shift+Tab pour naviguer, Entrée ou Espace pour activer les boutons.
            Échap pour fermer cette erreur si possible.
          </p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .accessible-error-container {
      position: fixed;
      top: 20px;
      left: 50%;
      transform: translateX(-50%);
      max-width: 600px;
      width: 90%;
      background: #fee;
      border: 3px solid #c53030;
      border-radius: 8px;
      padding: 20px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
      z-index: 10000;
      font-family: Arial, sans-serif;
    }

    .skip-link {
      position: absolute;
      top: -40px;
      left: 6px;
      background: #000;
      color: #fff;
      padding: 8px;
      text-decoration: none;
      border-radius: 4px;
      font-size: 14px;
      z-index: 10001;
    }

    .skip-link:focus {
      top: 6px;
    }

    .error-title {
      color: #c53030;
      font-size: 1.25rem;
      font-weight: bold;
      margin: 0 0 12px 0;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .error-title:focus {
      outline: 2px solid #2563eb;
      outline-offset: 2px;
    }

    .error-icon {
      font-size: 1.5rem;
    }

    .error-description {
      margin-bottom: 16px;
      line-height: 1.5;
    }

    .error-details {
      margin-top: 12px;
      border: 1px solid #ccc;
      border-radius: 4px;
    }

    .error-details-toggle {
      padding: 12px;
      background: #f7f7f7;
      border: none;
      cursor: pointer;
      width: 100%;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-weight: bold;
    }

    .error-details-toggle:focus {
      outline: 2px solid #2563eb;
      outline-offset: -2px;
    }

    .error-details-content {
      padding: 12px;
      background: #fff;
    }

    .error-details-content dl {
      margin: 0;
      display: grid;
      grid-template-columns: max-content 1fr;
      gap: 8px 16px;
    }

    .error-details-content dt {
      font-weight: bold;
      color: #666;
    }

    .error-details-content dd {
      margin: 0;
      word-break: break-word;
    }

    .validation-errors {
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid #eee;
    }

    .validation-errors h3 {
      font-size: 1rem;
      color: #c53030;
      margin: 0 0 8px 0;
    }

    .validation-errors ul {
      margin: 0;
      padding-left: 20px;
    }

    .validation-errors li {
      margin-bottom: 4px;
    }

    .error-recovery {
      margin-top: 20px;
      padding-top: 16px;
      border-top: 2px solid #c53030;
    }

    .error-recovery h3 {
      font-size: 1rem;
      color: #c53030;
      margin: 0 0 12px 0;
    }

    .recovery-actions-list {
      list-style: none;
      padding: 0;
      margin: 0;
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .recovery-actions-list li {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .recovery-action-btn {
      background: #2563eb;
      color: white;
      border: none;
      padding: 10px 16px;
      border-radius: 4px;
      cursor: pointer;
      font-size: 14px;
      font-weight: 500;
    }

    .recovery-action-btn:hover {
      background: #1d4ed8;
    }

    .recovery-action-btn:focus {
      outline: 2px solid #93c5fd;
      outline-offset: 2px;
    }

    .keyboard-shortcut {
      font-size: 12px;
      color: #666;
      font-style: italic;
    }

    .accessibility-instructions {
      margin-top: 16px;
      padding-top: 12px;
      border-top: 1px solid #ddd;
      font-size: 14px;
      color: #666;
    }

    .sr-only {
      position: absolute;
      width: 1px;
      height: 1px;
      padding: 0;
      margin: -1px;
      overflow: hidden;
      clip: rect(0, 0, 0, 0);
      white-space: nowrap;
      border: 0;
    }

    /* Responsive */
    @media (max-width: 640px) {
      .accessible-error-container {
        width: 95%;
        top: 10px;
        padding: 16px;
      }

      .recovery-actions-list {
        flex-direction: column;
      }

      .recovery-actions-list li {
        width: 100%;
      }

      .recovery-action-btn {
        width: 100%;
      }
    }

    /* Mode sombre */
    @media (prefers-color-scheme: dark) {
      .accessible-error-container {
        background: #1a1a1a;
        border-color: #ef4444;
        color: #e5e5e5;
      }

      .error-details-toggle {
        background: #2a2a2a;
        color: #e5e5e5;
      }

      .error-details-content {
        background: #1a1a1a;
      }
    }
  `]
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
    private focusManagement: FocusManagementService
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
        action: () => window.location.reload(),
        keyboardShortcut: 'R'
      },
      {
        label: 'Retour à l\'accueil',
        action: () => window.location.href = '/',
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
          window.location.reload();
        }
        break;
      case 'h':
      case 'H':
        if (event.altKey) {
          event.preventDefault();
          window.location.href = '/';
        }
        break;
    }
  }
}