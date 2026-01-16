import { Injectable, Inject, Optional } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { LoggerService } from '../../core/services/logger.service';
import { TranslationService } from '../../core/services/translation.service';

// Interface pour MatSnackBar pour éviter l'import direct
interface SnackBarLike {
  open(message: string, action?: string, config?: any): any;
}

// Interface pour la configuration
interface SnackBarConfig {
  duration?: number;
  panelClass?: string[];
  horizontalPosition?: 'start' | 'center' | 'end' | 'left' | 'right';
  verticalPosition?: 'top' | 'bottom';
}

export interface NotificationMessage {
  id: string;
  message: string;
  type: 'success' | 'error' | 'warning' | 'info';
  timestamp: Date;
  duration?: number;
  action?: string;
  persistent?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private notificationsSubject = new BehaviorSubject<NotificationMessage[]>([]);
  private activeSnackBars: Map<string, any> = new Map();

  public notifications$: Observable<NotificationMessage[]> = this.notificationsSubject.asObservable();
  public unreadCount$: Observable<number> = new BehaviorSubject<number>(0);
  public newNotification$: Observable<NotificationMessage | null> = new BehaviorSubject<NotificationMessage | null>(null);
  public connectionStatus$: Observable<boolean> = new BehaviorSubject<boolean>(true);

  constructor(
    private readonly logger: LoggerService,
    private readonly translationService: TranslationService,
    @Optional() @Inject('MatSnackBar') private readonly snackBar?: SnackBarLike
  ) {}

  /**
   * Shows a success notification
   * @param message The success message
   * @param action Optional action button text
   * @param duration Duration in milliseconds (default: 4000)
   */
  showSuccess(message: string, action?: string, duration: number = 4000): string {
    return this.show(message, 'success', action, duration);
  }

  /**
   * Shows an error notification
   * @param message The error message
   * @param action Optional action button text
   * @param duration Duration in milliseconds (default: 6000)
   */
  showError(message: string, action?: string, duration: number = 6000): string {
    return this.show(message, 'error', action, duration);
  }

  /**
   * Shows a warning notification
   * @param message The warning message
   * @param action Optional action button text
   * @param duration Duration in milliseconds (default: 5000)
   */
  showWarning(message: string, action?: string, duration: number = 5000): string {
    return this.show(message, 'warning', action, duration);
  }

  /**
   * Shows an info notification
   * @param message The info message
   * @param action Optional action button text
   * @param duration Duration in milliseconds (default: 4000)
   */
  showInfo(message: string, action?: string, duration: number = 4000): string {
    return this.show(message, 'info', action, duration);
  }

  /**
   * Shows a persistent notification that stays until dismissed
   * @param message The message
   * @param type The notification type
   * @param action Optional action button text
   */
  showPersistent(message: string, type: 'success' | 'error' | 'warning' | 'info', action?: string): string {
    return this.show(message, type, action, 0, true);
  }

  /**
   * Shows a generic notification
   * @param message The message to show
   * @param type The notification type
   * @param action Optional action button text
   * @param duration Duration in milliseconds (0 = persistent)
   * @param persistent Whether the notification is persistent
   */
  private show(
    message: string, 
    type: 'success' | 'error' | 'warning' | 'info', 
    action?: string, 
    duration: number = 4000,
    persistent: boolean = false
  ): string {
    const id = this.generateId();
    
    const notification: NotificationMessage = {
      id,
      message,
      type,
      timestamp: new Date(),
      duration: persistent ? 0 : duration,
      action,
      persistent
    };

    // Add to notifications list
    const currentNotifications = this.notificationsSubject.value;
    this.notificationsSubject.next([...currentNotifications, notification]);

    // Show snackbar if available
    if (this.snackBar) {
      const config: SnackBarConfig = {
        duration: persistent ? 0 : duration,
        panelClass: [`notification-${type}`, 'custom-snackbar'],
        horizontalPosition: 'right',
        verticalPosition: 'top'
      };

      const closeLabel = this.translationService.t('notifications.close', 'Close');
      const snackBarRef = this.snackBar.open(message, action || closeLabel, config);
      this.activeSnackBars.set(id, snackBarRef);

      // Handle snackbar dismissal if methods exist
      if (snackBarRef && snackBarRef.afterDismissed) {
        snackBarRef.afterDismissed().subscribe(() => {
          this.removeNotification(id);
          this.activeSnackBars.delete(id);
        });
      }

      // Handle action clicks if methods exist
      if (action && snackBarRef && snackBarRef.onAction) {
        snackBarRef.onAction().subscribe(() => {
          this.onNotificationAction(id, notification);
        });
      }
    } else {
      // Fallback: log to LoggerService if no snackbar available
      this.logger.info('NotificationService: fallback notification', { type, message });
    }

    return id;
  }

  /**
   * Dismisses a specific notification
   * @param id The notification ID
   */
  dismiss(id: string): void {
    const snackBarRef = this.activeSnackBars.get(id);
    if (snackBarRef && snackBarRef.dismiss) {
      snackBarRef.dismiss();
    }
    this.removeNotification(id);
  }

  /**
   * Dismisses all notifications
   */
  dismissAll(): void {
    this.activeSnackBars.forEach(snackBarRef => {
      if (snackBarRef && snackBarRef.dismiss) {
        snackBarRef.dismiss();
      }
    });
    this.activeSnackBars.clear();
    this.notificationsSubject.next([]);
  }

  /**
   * Gets all current notifications
   */
  getNotifications(): NotificationMessage[] {
    return this.notificationsSubject.value;
  }

  /**
   * Gets notifications by type
   * @param type The notification type to filter by
   */
  getNotificationsByType(type: 'success' | 'error' | 'warning' | 'info'): NotificationMessage[] {
    return this.notificationsSubject.value.filter(notification => notification.type === type);
  }

  /**
   * Shows a loading notification
   * @param message The loading message
   * @returns Notification ID for dismissal
   */
  showLoading(message?: string): string {
    const defaultMessage = this.translationService.t('notifications.loadingDefault', 'Loading...');
    return this.showInfo(message || defaultMessage, undefined, 0);
  }

  /**
   * Updates a loading notification to success
   * @param loadingId The ID of the loading notification
   * @param successMessage The success message
   */
  updateLoadingToSuccess(loadingId: string, successMessage: string): string {
    this.dismiss(loadingId);
    return this.showSuccess(successMessage);
  }

  /**
   * Updates a loading notification to error
   * @param loadingId The ID of the loading notification
   * @param errorMessage The error message
   */
  updateLoadingToError(loadingId: string, errorMessage: string): string {
    this.dismiss(loadingId);
    return this.showError(errorMessage);
  }

  /**
   * Shows API operation notifications with consistent messaging
   */
  api = {
    loading: (operation: string): string => {
      const template = this.translationService.t('notifications.apiLoading', '{operation} in progress...');
      return this.showLoading(template.replace('{operation}', operation));
    },

    success: (operation: string, loadingId?: string): string => {
      const template = this.translationService.t('notifications.apiSuccess', '{operation} completed successfully');
      const message = template.replace('{operation}', operation);
      if (loadingId) {
        return this.updateLoadingToSuccess(loadingId, message);
      }
      return this.showSuccess(message);
    },

    error: (operation: string, error?: string, loadingId?: string): string => {
      let message: string;
      if (error) {
        const template = this.translationService.t('notifications.apiErrorWithDetails', 'Error during {operation}: {error}');
        message = template.replace('{operation}', operation).replace('{error}', error);
      } else {
        const template = this.translationService.t('notifications.apiError', 'Error during {operation}');
        message = template.replace('{operation}', operation);
      }

      if (loadingId) {
        return this.updateLoadingToError(loadingId, message);
      }
      return this.showError(message);
    }
  };

  /**
   * Shows game-specific notifications
   */
  game = {
    created: (gameName: string): string => {
      const template = this.translationService.t('notifications.game.created', 'Game "{name}" created successfully');
      const action = this.translationService.t('notifications.game.viewAction', 'View');
      return this.showSuccess(template.replace('{name}', gameName), action);
    },

    joined: (gameName: string): string => {
      const template = this.translationService.t('notifications.game.joined', 'You joined "{name}"');
      const action = this.translationService.t('notifications.game.viewTeamAction', 'View team');
      return this.showSuccess(template.replace('{name}', gameName), action);
    },

    left: (gameName: string): string => {
      const template = this.translationService.t('notifications.game.left', 'You left "{name}"');
      return this.showInfo(template.replace('{name}', gameName));
    },

    draftStarted: (gameName: string): string => {
      const template = this.translationService.t('notifications.game.draftStarted', 'Draft started for "{name}"');
      const action = this.translationService.t('notifications.game.participateAction', 'Participate');
      return this.showInfo(template.replace('{name}', gameName), action);
    },

    yourTurn: (): string => {
      const message = this.translationService.t('notifications.game.yourTurn', 'It\'s your turn to draft!');
      const action = this.translationService.t('notifications.game.gotoDraftAction', 'Go to draft');
      return this.showWarning(message, action);
    }
  };

  /**
   * Shows team-specific notifications
   */
  team = {
    playerAdded: (playerName: string): string => {
      const template = this.translationService.t('notifications.team.playerAdded', '{player} added to your team');
      return this.showSuccess(template.replace('{player}', playerName));
    },

    playerRemoved: (playerName: string): string => {
      const template = this.translationService.t('notifications.team.playerRemoved', '{player} removed from your team');
      return this.showInfo(template.replace('{player}', playerName));
    },

    tradeCompleted: (playerOut: string, playerIn: string): string => {
      const template = this.translationService.t('notifications.team.tradeCompleted', 'Trade completed: {out} ↔ {in}');
      return this.showSuccess(template.replace('{out}', playerOut).replace('{in}', playerIn));
    }
  };

  /**
   * Shows user authentication notifications
   */
  auth = {
    loginSuccess: (username: string): string => {
      const template = this.translationService.t('notifications.auth.loginSuccess', 'Successfully logged in as {username}');
      return this.showSuccess(template.replace('{username}', username));
    },

    logoutSuccess: (): string => {
      const message = this.translationService.t('notifications.auth.logoutSuccess', 'Successfully logged out');
      return this.showInfo(message);
    },

    sessionExpired: (): string => {
      const message = this.translationService.t('notifications.auth.sessionExpired', 'Session expired, please log in again');
      const action = this.translationService.t('notifications.auth.connectAction', 'Log in');
      return this.showWarning(message, action);
    },

    profileSwitched: (username: string): string => {
      const template = this.translationService.t('notifications.auth.profileSwitched', 'Profile switched to {username}');
      return this.showInfo(template.replace('{username}', username));
    }
  };

  /**
   * Removes a notification from the list
   * @param id The notification ID
   */
  private removeNotification(id: string): void {
    const currentNotifications = this.notificationsSubject.value;
    const filteredNotifications = currentNotifications.filter(n => n.id !== id);
    this.notificationsSubject.next(filteredNotifications);
  }

  /**
   * Handles notification action clicks
   * @param id The notification ID
   * @param notification The notification object
   */
  private onNotificationAction(id: string, notification: NotificationMessage): void {
    this.logger.debug('NotificationService: action clicked', { id, notification });
    
    // You can implement specific action handling here based on notification type
    // For example, navigation, API calls, etc.
    
    // Remove the notification after action
    this.removeNotification(id);
  }

  /**
   * Generates a unique ID for notifications
   */
  private generateId(): string {
    return `notification_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  /**
   * Clears old notifications (older than 24 hours)
   */
  clearOldNotifications(): void {
    const oneDayAgo = new Date();
    oneDayAgo.setHours(oneDayAgo.getHours() - 24);
    
    const currentNotifications = this.notificationsSubject.value;
    const recentNotifications = currentNotifications.filter(
      notification => notification.timestamp > oneDayAgo
    );
    
    this.notificationsSubject.next(recentNotifications);
  }
}
