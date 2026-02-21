import { fakeAsync, TestBed, tick } from '@angular/core/testing';

import { GamesRealtimeEvent, GamesRealtimeService } from './games-realtime.service';
import { LoggerService } from './logger.service';
import { UserContextService } from './user-context.service';

class FakeEventSource {
  onopen: ((event: Event) => void) | null = null;
  onmessage: ((event: MessageEvent<string>) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;
  readonly listeners = new Map<string, (event: MessageEvent<string>) => void>();
  close = jasmine.createSpy('close');

  addEventListener(type: string, listener: EventListenerOrEventListenerObject): void {
    this.listeners.set(type, listener as (event: MessageEvent<string>) => void);
  }
}

describe('GamesRealtimeService', () => {
  let service: GamesRealtimeService;
  let userContextService: jasmine.SpyObj<UserContextService>;

  beforeEach(() => {
    userContextService = jasmine.createSpyObj<UserContextService>('UserContextService', ['getCurrentUser']);
    const logger = jasmine.createSpyObj<LoggerService>('LoggerService', ['debug', 'warn']);

    userContextService.getCurrentUser.and.returnValue({
      id: '1',
      username: 'Thibaut',
      email: 'thibaut@test.com'
    });

    TestBed.configureTestingModule({
      providers: [
        GamesRealtimeService,
        { provide: UserContextService, useValue: userContextService },
        { provide: LoggerService, useValue: logger }
      ]
    });

    service = TestBed.inject(GamesRealtimeService);
  });

  it('should connect to user stream on start', () => {
    const eventSource = new FakeEventSource();
    const createSpy = spyOn<any>(service, 'createEventSource').and.returnValue(
      eventSource as unknown as EventSource
    );

    service.start();

    expect(createSpy).toHaveBeenCalledWith(
      'http://localhost:8080/api/games/events?user=Thibaut'
    );
  });

  it('should emit parsed events from named game-event stream', () => {
    const eventSource = new FakeEventSource();
    spyOn<any>(service, 'createEventSource').and.returnValue(
      eventSource as unknown as EventSource
    );

    const receivedEvents: GamesRealtimeEvent[] = [];
    service.events$.subscribe((event) => receivedEvents.push(event));

    service.start();
    eventSource.listeners.get('game-event')?.({
      data: JSON.stringify({
        type: 'GAME_DELETED',
        gameId: 'game-42',
        timestamp: '2026-02-18T06:00:00Z'
      })
    } as MessageEvent<string>);

    expect(receivedEvents).toEqual([
      {
        type: 'GAME_DELETED',
        gameId: 'game-42',
        timestamp: '2026-02-18T06:00:00Z'
      }
    ]);
  });

  it('should reconnect with backoff when stream fails', fakeAsync(() => {
    const firstEventSource = new FakeEventSource();
    const secondEventSource = new FakeEventSource();
    const createSpy = spyOn<any>(service, 'createEventSource').and.returnValues(
      firstEventSource as unknown as EventSource,
      secondEventSource as unknown as EventSource
    );

    service.start();
    firstEventSource.onerror?.(new Event('error'));
    tick(1000);

    expect(firstEventSource.close).toHaveBeenCalled();
    expect(createSpy).toHaveBeenCalledTimes(2);
  }));

  it('should stop stream and cancel reconnect', fakeAsync(() => {
    const firstEventSource = new FakeEventSource();
    const createSpy = spyOn<any>(service, 'createEventSource').and.returnValue(
      firstEventSource as unknown as EventSource
    );

    service.start();
    firstEventSource.onerror?.(new Event('error'));
    service.stop();
    tick(1000);

    expect(firstEventSource.close).toHaveBeenCalled();
    expect(createSpy).toHaveBeenCalledTimes(1);
  }));

  it('should skip connection when no current user is available', () => {
    userContextService.getCurrentUser.and.returnValue(null);
    const createSpy = spyOn<any>(service, 'createEventSource');

    service.start();

    expect(createSpy).not.toHaveBeenCalled();
  });
});
