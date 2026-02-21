import { Component, OnInit, OnDestroy, inject, Renderer2, RendererFactory2 } from '@angular/core';
import { CommonModule } from '@angular/common';
import { animate, style, transition, trigger } from '@angular/animations';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NotificationService, NotificationMessage } from '../../services/notification.service';
import { TranslationService } from '../../../core/services/translation.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-notification-center',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatBadgeModule, MatButtonModule, MatTooltipModule],
  templateUrl: './notification-center.component.html',
  styleUrls: ['./notification-center.component.css'],
  animations: [
    trigger('slideIn', [
      transition(':enter', [
        style({ transform: 'translateX(100%)', opacity: 0 }),
        animate('300ms ease-out', style({ transform: 'translateX(0)', opacity: 1 }))
      ]),
      transition(':leave', [
        animate('300ms ease-in', style({ transform: 'translateX(100%)', opacity: 0 }))
      ])
    ]),
    trigger('fadeIn', [
      transition(':enter', [
        style({ opacity: 0 }),
        animate('200ms', style({ opacity: 1 }))
      ])
    ])
  ]
})
export class NotificationCenterComponent implements OnInit, OnDestroy {
  public readonly t = inject(TranslationService);

  notifications: NotificationMessage[] = [];
  unreadCount = 0;
  isOpen = false;
  connectionStatus = false;

  private subscriptions: Subscription[] = [];
  private readonly renderer: Renderer2;

  constructor(
    public notificationService: NotificationService,
    rendererFactory: RendererFactory2
  ) {
    this.renderer = rendererFactory.createRenderer(null, null);
  }
  
  ngOnInit() {
    // S'abonner aux notifications
    this.subscriptions.push(
      this.notificationService.notifications$.subscribe(notifications => {
        this.notifications = notifications;
      })
    );
    
    // S'abonner au compteur non lu
    this.subscriptions.push(
      this.notificationService.unreadCount$.subscribe(count => {
        this.unreadCount = count;
      })
    );
    
    // S'abonner aux nouvelles notifications
    this.subscriptions.push(
      this.notificationService.newNotification$.subscribe(notification => {
        this.showToastNotification(notification);
      })
    );
    
    // S'abonner au statut de connexion
    this.subscriptions.push(
      this.notificationService.connectionStatus$.subscribe(status => {
        this.connectionStatus = status;
      })
    );
  }
  
  toggleNotificationCenter() {
    this.isOpen = !this.isOpen;
    
    if (this.isOpen && this.unreadCount > 0) {
      // Marquer toutes comme lues après 2 secondes
      setTimeout(() => {
        this.notificationService.dismissAll();
      }, 2000);
    }
  }
  
  closeNotificationCenter() {
    this.isOpen = false;
  }
  
  markAsRead(notification: NotificationMessage) {
    // Pour l'instant on dismiss la notification
    this.notificationService.dismiss(notification.id);
  }

  onNotificationKeydown(event: KeyboardEvent, notification: NotificationMessage): void {
    if (event.key !== 'Enter' && event.key !== ' ') {
      return;
    }

    event.preventDefault();
    this.markAsRead(notification);
  }
  
  deleteNotification(notification: NotificationMessage, event: Event) {
    event.stopPropagation();
    this.notificationService.dismiss(notification.id);
  }
  
  getNotificationIcon(type: string): string {
    const icons: { [key: string]: string } = {
      'trade': 'swap_horiz',
      'score': 'trending_up',
      'system': 'info',
      'achievement': 'emoji_events'
    };
    return icons[type] || 'notifications';
  }
  
  getNotificationClass(type: string): string {
    return `notification-${type}`;
  }
  
  formatTimestamp(date: Date): string {
    const now = new Date();
    const notifDate = new Date(date);
    const diffMs = now.getTime() - notifDate.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return this.t.t('notificationCenter.time.justNow');
    if (diffMins < 60) return this.t.t('notificationCenter.time.minutesAgo').replace('{n}', String(diffMins));
    if (diffHours < 24) return this.t.t('notificationCenter.time.hoursAgo').replace('{n}', String(diffHours));
    if (diffDays < 7) return this.t.t('notificationCenter.time.daysAgo').replace('{n}', String(diffDays));

    const locale = this.resolveLocale(this.t.currentLanguage);
    return notifDate.toLocaleDateString(locale);
  }

  private resolveLocale(language: string): string {
    const locales: Record<string, string> = {
      fr: 'fr-FR',
      es: 'es-ES',
      pt: 'pt-BR'
    };
    return locales[language] ?? 'en-US';
  }
  
  private showToastNotification(notification: NotificationMessage | null) {
    if (!notification) return;

    // Créer un toast temporaire
    const toast = this.createToastElement(notification);
    
    document.body.appendChild(toast);
    
    // Animation d'entrée
    setTimeout(() => toast.classList.add('show'), 10);
    
    // Supprimer après 5 secondes
    setTimeout(() => {
      toast.classList.remove('show');
      setTimeout(() => toast.remove(), 300);
    }, 5000);
    
    // Clic pour fermer
    toast.addEventListener('click', () => {
      toast.classList.remove('show');
      setTimeout(() => toast.remove(), 300);
    });
  }

  private createToastElement(notification: NotificationMessage): HTMLDivElement {
    const toast = this.renderer.createElement('div') as HTMLDivElement;
    this.renderer.addClass(toast, 'notification-toast');
    this.renderer.appendChild(toast, this.createToastIconElement(notification));
    this.renderer.appendChild(toast, this.createToastContentElement(notification));
    return toast;
  }

  private createToastIconElement(notification: NotificationMessage): HTMLElement {
    const iconWrapper = this.renderer.createElement('div');
    this.renderer.addClass(iconWrapper, 'toast-icon');

    const icon = this.renderer.createElement('i');
    this.renderer.addClass(icon, 'material-icons');
    this.renderer.setProperty(icon, 'textContent', this.getNotificationIcon(notification.type));

    this.renderer.appendChild(iconWrapper, icon);
    return iconWrapper;
  }

  private createToastContentElement(notification: NotificationMessage): HTMLElement {
    const contentWrapper = this.renderer.createElement('div');
    this.renderer.addClass(contentWrapper, 'toast-content');

    const title = this.renderer.createElement('h4');
    this.renderer.setProperty(title, 'textContent', this.t.t('notificationCenter.notification'));

    const message = this.renderer.createElement('p');
    this.renderer.setProperty(message, 'textContent', notification.message);

    this.renderer.appendChild(contentWrapper, title);
    this.renderer.appendChild(contentWrapper, message);
    return contentWrapper;
  }
  
  trackByFn(index: number, notification: NotificationMessage): string {
    return notification.id;
  }

  markAllAsRead() {
    this.notificationService.dismissAll();
  }

  ngOnDestroy() {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }
}
