import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { animate, style, transition, trigger } from '@angular/animations';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';
import { NotificationService, NotificationMessage } from '../../services/notification.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-notification-center',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatBadgeModule, MatButtonModule],
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
  notifications: NotificationMessage[] = [];
  unreadCount = 0;
  isOpen = false;
  connectionStatus = false;
  
  private subscriptions: Subscription[] = [];
  
  constructor(public notificationService: NotificationService) {}
  
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
    
    if (diffMins < 1) return 'À l\'instant';
    if (diffMins < 60) return `Il y a ${diffMins} min`;
    if (diffHours < 24) return `Il y a ${diffHours}h`;
    if (diffDays < 7) return `Il y a ${diffDays}j`;
    
    return notifDate.toLocaleDateString('fr-FR');
  }
  
  private showToastNotification(notification: NotificationMessage | null) {
    if (!notification) return;
    
    // Créer un toast temporaire
    const toast = document.createElement('div');
    toast.className = 'notification-toast';
    toast.innerHTML = `
      <div class="toast-icon">
        <i class="material-icons">${this.getNotificationIcon(notification.type)}</i>
      </div>
      <div class="toast-content">
        <h4>Notification</h4>
        <p>${notification.message}</p>
      </div>
    `;
    
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