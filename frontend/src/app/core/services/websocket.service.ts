import { Injectable, OnDestroy } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { filter } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface TradeNotification {
  type: 'TRADE_PROPOSED' | 'TRADE_ACCEPTED' | 'TRADE_REJECTED' | 'TRADE_CANCELLED' | 'TRADE_COUNTERED';
  tradeId: string;
  fromTeamId: string;
  fromTeamName: string;
  toTeamId: string;
  toTeamName: string;
  status: string;
  counterTradeId?: string;
}

@Injectable({
  providedIn: 'root'
})
export class WebSocketService implements OnDestroy {
  private client: Client | null = null;
  private subscriptions: StompSubscription[] = [];

  private connectionStatus$ = new BehaviorSubject<boolean>(false);
  private tradeNotifications$ = new Subject<TradeNotification>();
  private reconnectAttempts = 0;
  private readonly maxReconnectAttempts = 5;
  private readonly reconnectDelay = 3000;

  get isConnected$(): Observable<boolean> {
    return this.connectionStatus$.asObservable();
  }

  get tradeNotifications(): Observable<TradeNotification> {
    return this.tradeNotifications$.asObservable();
  }

  connect(token?: string): void {
    if (this.client?.active) {
      return;
    }

    const wsUrl = this.getWebSocketUrl();

    this.client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      debug: (str) => {
        if (!environment.production) {
          console.log('[WebSocket]', str);
        }
      },
      reconnectDelay: this.reconnectDelay,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        this.connectionStatus$.next(true);
        this.reconnectAttempts = 0;
        this.subscribeToUserQueue();
      },
      onDisconnect: () => {
        this.connectionStatus$.next(false);
      },
      onStompError: (frame) => {
        console.error('[WebSocket] STOMP error:', frame.headers['message']);
        this.handleReconnect();
      },
      onWebSocketError: (event) => {
        console.error('[WebSocket] Connection error:', event);
        this.handleReconnect();
      }
    });

    this.client.activate();
  }

  disconnect(): void {
    this.unsubscribeAll();
    if (this.client?.active) {
      this.client.deactivate();
    }
    this.connectionStatus$.next(false);
  }

  subscribeToGameTrades(gameId: string): void {
    if (!this.client?.active) {
      console.warn('[WebSocket] Not connected, cannot subscribe to game trades');
      return;
    }

    const destination = `/topic/games/${gameId}/trades`;
    const subscription = this.client.subscribe(destination, (message: IMessage) => {
      this.handleTradeMessage(message);
    });

    this.subscriptions.push(subscription);
  }

  private subscribeToUserQueue(): void {
    if (!this.client?.active) {
      return;
    }

    const subscription = this.client.subscribe('/user/queue/trades', (message: IMessage) => {
      this.handleTradeMessage(message);
    });

    this.subscriptions.push(subscription);
  }

  private handleTradeMessage(message: IMessage): void {
    try {
      const notification: TradeNotification = JSON.parse(message.body);
      this.tradeNotifications$.next(notification);
    } catch (error) {
      console.error('[WebSocket] Failed to parse trade notification:', error);
    }
  }

  private handleReconnect(): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`[WebSocket] Reconnect attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts}`);
    } else {
      console.error('[WebSocket] Max reconnect attempts reached');
      this.disconnect();
    }
  }

  private unsubscribeAll(): void {
    this.subscriptions.forEach(sub => {
      try {
        sub.unsubscribe();
      } catch (e) {
        // Ignore unsubscribe errors
      }
    });
    this.subscriptions = [];
  }

  private getWebSocketUrl(): string {
    const baseUrl = environment.apiUrl || '';
    return `${baseUrl}/ws`;
  }

  ngOnDestroy(): void {
    this.disconnect();
    this.tradeNotifications$.complete();
    this.connectionStatus$.complete();
  }
}
