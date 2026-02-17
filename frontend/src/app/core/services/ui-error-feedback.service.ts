import { Injectable } from '@angular/core';
import { MatSnackBar, MatSnackBarRef, TextOnlySnackBar } from '@angular/material/snack-bar';

import { TranslationService } from './translation.service';
import {
  extractBackendErrorDetails,
  toSafeUserMessage
} from '../utils/user-facing-error-message.util';

export interface UiErrorRule {
  pattern: RegExp;
  translationKey: string;
}

@Injectable({
  providedIn: 'root'
})
export class UiErrorFeedbackService {
  private readonly successPanelClass = ['custom-snackbar', 'notification-success'];
  private readonly errorPanelClass = ['custom-snackbar', 'notification-error'];
  private readonly warningPanelClass = ['custom-snackbar', 'notification-warning'];
  private readonly infoPanelClass = ['custom-snackbar', 'notification-info'];

  constructor(
    private readonly snackBar: MatSnackBar,
    private readonly t: TranslationService
  ) {}

  showSuccessFromKey(translationKey: string, duration = 3000): void {
    this.open(this.t.t(translationKey), 'success', duration);
  }

  showSuccessMessage(message: string, duration = 3000): void {
    this.open(message, 'success', duration);
  }

  showErrorMessage(message: string, duration = 5000): void {
    this.open(message, 'error', duration);
  }

  showErrorFromKey(translationKey: string, duration = 5000): void {
    this.open(this.t.t(translationKey), 'error', duration);
  }

  showInfoFromKey(translationKey: string, duration = 3000): void {
    this.open(this.t.t(translationKey), 'info', duration);
  }

  showInfoMessage(message: string, duration = 3000): void {
    this.open(message, 'info', duration);
  }

  showWarningFromKey(translationKey: string, duration = 5000): void {
    this.open(this.t.t(translationKey), 'warning', duration);
  }

  showWarningMessage(message: string, duration = 5000): void {
    this.open(message, 'warning', duration);
  }

  showSuccessWithAction(
    message: string,
    actionTranslationKey: string,
    onAction: () => void,
    duration = 5000
  ): void {
    this.openWithAction(message, actionTranslationKey, 'success', onAction, duration);
  }

  showInfoWithAction(
    message: string,
    actionTranslationKey: string,
    onAction: () => void,
    duration = 5000
  ): void {
    this.openWithAction(message, actionTranslationKey, 'info', onAction, duration);
  }

  showError(
    error: unknown,
    fallbackTranslationKey: string,
    options?: { duration?: number; rules?: UiErrorRule[] }
  ): string {
    const duration = options?.duration ?? 5000;
    const message = this.resolveMessage(error, fallbackTranslationKey, options?.rules);
    this.open(message, 'error', duration);
    return message;
  }

  resolveMessage(
    error: unknown,
    fallbackTranslationKey: string,
    rules: UiErrorRule[] = []
  ): string {
    const fallback = this.t.t(fallbackTranslationKey);
    const rawMessage = this.extractRawMessage(error);
    const normalizedMessage = this.normalize(rawMessage);

    const matchedRule = rules.find(rule => rule.pattern.test(normalizedMessage));
    if (matchedRule) {
      return this.t.t(matchedRule.translationKey);
    }

    const safeBackendMessage = toSafeUserMessage(rawMessage);
    return safeBackendMessage || fallback;
  }

  private extractRawMessage(error: unknown): string | null {
    if (error instanceof Error && error.message) {
      return error.message;
    }

    if (!error || typeof error !== 'object') {
      return null;
    }

    const backendError = error as {
      message?: string;
      error?: { message?: string } | string;
    };

    if (backendError.message) {
      return backendError.message;
    }

    if (typeof backendError.error === 'string') {
      return backendError.error;
    }

    if (backendError.error?.message) {
      return backendError.error.message;
    }

    const parsedFromHttp = extractBackendErrorDetails(error as never).message;
    return parsedFromHttp;
  }

  private normalize(value: string | null): string {
    return (value || '')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim();
  }

  private open(
    message: string,
    type: 'success' | 'error' | 'warning' | 'info',
    duration: number
  ): void {
    this.snackBar.open(message, this.t.t('common.close'), {
      duration,
      panelClass: this.panelClassFor(type)
    });
  }

  private subscribeToAction(
    snackBarRef: MatSnackBarRef<TextOnlySnackBar>,
    onAction: () => void
  ): void {
    snackBarRef.onAction().subscribe(() => onAction());
  }

  private openWithAction(
    message: string,
    actionTranslationKey: string,
    type: 'success' | 'info',
    onAction: () => void,
    duration: number
  ): void {
    const snackBarRef = this.snackBar.open(message, this.t.t(actionTranslationKey), {
      duration,
      panelClass: this.panelClassFor(type)
    });
    this.subscribeToAction(snackBarRef, onAction);
  }

  private panelClassFor(type: 'success' | 'error' | 'warning' | 'info'): string[] {
    if (type === 'success') {
      return this.successPanelClass;
    }
    if (type === 'error') {
      return this.errorPanelClass;
    }
    if (type === 'warning') {
      return this.warningPanelClass;
    }
    return this.infoPanelClass;
  }
}
