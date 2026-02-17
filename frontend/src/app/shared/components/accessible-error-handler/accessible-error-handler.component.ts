import { Component, OnInit, OnDestroy, ElementRef, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject } from 'rxjs';
import { AccessibilityAnnouncerService } from '../../services/accessibility-announcer.service';
import { FocusManagementService } from '../../services/focus-management.service';
import { BrowserNavigationService } from '../../services/browser-navigation.service';
import { TranslationService } from '../../../core/services/translation.service';

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

  public readonly t = inject(TranslationService);

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
    document.addEventListener('keydown', this.handleKeydown.bind(this));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    document.removeEventListener('keydown', this.handleKeydown.bind(this));
  }

  showError(error: AccessibleErrorInfo): void {
    this.currentError = error;

    this.accessibilityAnnouncer.announceTechnicalError(
      `${error.message}. ${this.t.t('errors.handler.errorDialogOpened')}`
    );

    setTimeout(() => {
      this.focusErrorTitle();
    }, 100);
  }

  fromHttpError(error: HttpErrorResponse): AccessibleErrorInfo {
    const errorInfo: AccessibleErrorInfo = {
      title: this.t.t('errors.handler.serverCommError'),
      message: this.t.t('errors.handler.serverCommMessage'),
      timestamp: new Date(),
      status: error.status
    };

    if (error.error && typeof error.error === 'object') {
      errorInfo.message = error.error.message || errorInfo.message;
      errorInfo.code = error.error.code;
      errorInfo.path = error.error.path;
      errorInfo.requestId = error.error.requestId;
      errorInfo.validationErrors = error.error.validationErrors;
    }

    switch (error.status) {
      case 0:
        errorInfo.title = this.t.t('errors.handler.connectionProblem');
        errorInfo.message = this.t.t('errors.handler.connectionMessage');
        break;
      case 400:
        errorInfo.title = this.t.t('errors.handler.invalidRequest');
        break;
      case 401:
        errorInfo.title = this.t.t('errors.handler.authRequired');
        errorInfo.message = this.t.t('errors.handler.authMessage');
        break;
      case 403:
        errorInfo.title = this.t.t('errors.handler.forbidden');
        errorInfo.message = this.t.t('errors.handler.forbiddenMessage');
        break;
      case 404:
        errorInfo.title = this.t.t('errors.handler.notFound');
        errorInfo.message = this.t.t('errors.handler.notFoundMessage');
        break;
      case 500:
        errorInfo.title = this.t.t('errors.handler.serverError');
        errorInfo.message = this.t.t('errors.handler.serverErrorMessage');
        break;
    }

    return errorInfo;
  }

  hideError(): void {
    if (this.currentError) {
      this.accessibilityAnnouncer.announceErrorRecovery(
        this.t.t('errors.handler.errorDialogClosed')
      );
      this.currentError = null;
      this.detailsExpanded = false;

      this.focusManagement.restoreFocus();
    }
  }

  focusErrorTitle(): void {
    if (this.errorTitle) {
      this.focusManagement.focusElement(this.errorTitle.nativeElement);
    }
  }

  focusRecoveryActions(): void {
    if (this.recoveryActions) {
      const firstButton = this.recoveryActions.nativeElement.querySelector('button');
      if (firstButton) {
        this.focusManagement.focusElement(firstButton);
      }
    }
  }

  hasDetailedInfo(): boolean {
    return !!(this.currentError?.status ||
              this.currentError?.code ||
              this.currentError?.path ||
              this.currentError?.requestId ||
              this.currentError?.validationErrors);
  }

  getValidationErrorsArray(): Array<{field: string; message: string}> {
    if (!this.currentError?.validationErrors) {
      return [];
    }

    return Object.entries(this.currentError.validationErrors)
      .map(([field, message]) => ({ field, message }));
  }

  getDefaultRecoveryActions() {
    return [
      {
        label: this.t.t('errors.handler.retry'),
        action: () => this.navigation.reload(),
        keyboardShortcut: 'R'
      },
      {
        label: this.t.t('errors.handler.goHome'),
        action: () => this.navigation.navigateHome(),
        keyboardShortcut: 'H'
      },
      {
        label: this.t.t('errors.handler.close'),
        action: () => this.hideError(),
        keyboardShortcut: this.t.t('errors.handler.escapeKey')
      }
    ];
  }

  executeRecoveryAction(action: any): void {
    this.accessibilityAnnouncer.announcePolite(
      this.t.t('errors.handler.executingAction') + action.label
    );
    action.action();
  }

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
