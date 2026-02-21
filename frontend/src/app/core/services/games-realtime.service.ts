import { Injectable, OnDestroy } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoggerService } from './logger.service';
import { UserContextService } from './user-context.service';

export interface GamesRealtimeEvent {
  type: string;
  gameId: string | null;
  timestamp: string;
}

@Injectable({
  providedIn: 'root'
})
export class GamesRealtimeService implements OnDestroy {
  private static readonly INITIAL_RECONNECT_DELAY_MS = 1000;
  private static readonly MAX_RECONNECT_DELAY_MS = 30000;

  private readonly eventsSubject = new Subject<GamesRealtimeEvent>();
  private eventSource: EventSource | null = null;
  private reconnectTimeout: ReturnType<typeof setTimeout> | null = null;
  private reconnectAttempt = 0;
  private isStarted = false;

  constructor(
    private readonly userContextService: UserContextService,
    private readonly logger: LoggerService
  ) {}

  get events$(): Observable<GamesRealtimeEvent> {
    return this.eventsSubject.asObservable();
  }

  start(): void {
    if (this.isStarted) {
      return;
    }

    this.isStarted = true;
    this.openStream();
  }

  stop(): void {
    this.isStarted = false;
    this.clearReconnectTimeout();
    this.closeStream();
  }

  ngOnDestroy(): void {
    this.stop();
    this.eventsSubject.complete();
  }

  protected createEventSource(url: string): EventSource {
    return new EventSource(url);
  }

  private openStream(): void {
    const username = this.userContextService.getCurrentUser()?.username;
    if (!username) {
      this.logger.warn('GamesRealtimeService: skipped realtime stream (no current user)');
      return;
    }

    const streamUrl = this.buildStreamUrl(username);
    this.closeStream();

    const source = this.createEventSource(streamUrl);
    source.onopen = () => {
      this.reconnectAttempt = 0;
      this.logger.debug('GamesRealtimeService: stream connected', { username });
    };
    source.onmessage = (event) => this.emitFromRawData(event.data);
    source.addEventListener('game-event', (event) => {
      const messageEvent = event as MessageEvent<string>;
      this.emitFromRawData(messageEvent.data);
    });
    source.onerror = () => this.handleStreamError(username);

    this.eventSource = source;
  }

  private handleStreamError(username: string): void {
    if (!this.isStarted) {
      return;
    }

    this.logger.warn('GamesRealtimeService: stream disconnected, scheduling reconnect', {
      username,
      attempt: this.reconnectAttempt + 1
    });
    this.closeStream();
    this.scheduleReconnect();
  }

  private scheduleReconnect(): void {
    this.clearReconnectTimeout();
    const delay =
      GamesRealtimeService.INITIAL_RECONNECT_DELAY_MS * Math.pow(2, this.reconnectAttempt);
    const cappedDelay = Math.min(delay, GamesRealtimeService.MAX_RECONNECT_DELAY_MS);
    this.reconnectAttempt += 1;

    this.reconnectTimeout = setTimeout(() => {
      if (this.isStarted) {
        this.openStream();
      }
    }, cappedDelay);
  }

  private emitFromRawData(rawData: string): void {
    try {
      const parsed = JSON.parse(rawData) as Partial<GamesRealtimeEvent>;
      if (!parsed.type || !parsed.timestamp) {
        return;
      }
      this.eventsSubject.next({
        type: parsed.type,
        gameId: parsed.gameId ?? null,
        timestamp: parsed.timestamp
      });
    } catch (error) {
      this.logger.warn('GamesRealtimeService: invalid SSE payload ignored', { rawData, error });
    }
  }

  private buildStreamUrl(username: string): string {
    const baseUrl = environment.apiUrl ? environment.apiUrl.replace(/\/$/, '') : '';
    const encodedUsername = encodeURIComponent(username);
    return `${baseUrl}/api/games/events?user=${encodedUsername}`;
  }

  private clearReconnectTimeout(): void {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }
  }

  private closeStream(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }
}
