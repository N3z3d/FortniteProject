import { Injectable, Inject, Optional } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { LoggerService } from '../../core/services/logger.service';

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
    private logger: LoggerService,
    @Optional() @Inject('MatSnackBar') private snackBar?: SnackBarLike
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

      const snackBarRef = this.snackBar.open(message, action || 'Fermer', config);
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
  showLoading(message: string = 'Chargement en cours...'): string {
    return this.showInfo(message, undefined, 0);
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
      return this.showLoading(`${operation} en cours...`);
    },
    
    success: (operation: string, loadingId?: string): string => {
      if (loadingId) {
        return this.updateLoadingToSuccess(loadingId, `${operation} réalisé avec succès`);
      }
      return this.showSuccess(`${operation} réalisé avec succès`);
    },
    
    error: (operation: string, error?: string, loadingId?: string): string => {
      const message = error 
        ? `Erreur lors de ${operation}: ${error}`
        : `Erreur lors de ${operation}`;
      
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
      return this.showSuccess(`Jeu "${gameName}" créé avec succès`, 'Voir');
    },
    
    joined: (gameName: string): string => {
      return this.showSuccess(`Vous avez rejoint "${gameName}"`, 'Voir équipe');
    },
    
    left: (gameName: string): string => {
      return this.showInfo(`Vous avez quitté "${gameName}"`);
    },
    
    draftStarted: (gameName: string): string => {
      return this.showInfo(`La draft a commencé pour "${gameName}"`, 'Participer');
    },
    
    yourTurn: (): string => {
      return this.showWarning('C\'est votre tour de drafter !', 'Aller à la draft');
    }
  };

  /**
   * Shows team-specific notifications
   */
  team = {
    playerAdded: (playerName: string): string => {
      return this.showSuccess(`${playerName} ajouté à votre équipe`);
    },
    
    playerRemoved: (playerName: string): string => {
      return this.showInfo(`${playerName} retiré de votre équipe`);
    },
    
    tradeCompleted: (playerOut: string, playerIn: string): string => {
      return this.showSuccess(`Trade effectué: ${playerOut} ↔ ${playerIn}`);
    }
  };

  /**
   * Shows user authentication notifications
   */
  auth = {
    loginSuccess: (username: string): string => {
      return this.showSuccess(`Connexion réussie en tant que ${username}`);
    },
    
    logoutSuccess: (): string => {
      return this.showInfo('Déconnexion réussie');
    },
    
    sessionExpired: (): string => {
      return this.showWarning('Session expirée, veuillez vous reconnecter', 'Se connecter');
    },
    
    profileSwitched: (username: string): string => {
      return this.showInfo(`Profil changé vers ${username}`);
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
