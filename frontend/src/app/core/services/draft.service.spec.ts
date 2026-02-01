import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NgZone } from '@angular/core';
import { DraftService } from './draft.service';
import { environment } from '../../../environments/environment';

describe('DraftService', () => {
  let service: DraftService;
  let httpMock: HttpTestingController;
  let zone: NgZone;
  let zoneRunSpy: jasmine.Spy;
  let originalEventSource: any;

  class FakeEventSource {
    static instances: FakeEventSource[] = [];
    listeners = new Map<string, (event: MessageEvent) => void>();
    onerror: ((event: Event) => void) | null = null;

    constructor(public url: string) {
      FakeEventSource.instances.push(this);
    }

    addEventListener(event: string, callback: (e: MessageEvent) => void) {
      this.listeners.set(event, callback);
    }

    emit(event: string, payload: any) {
      const callback = this.listeners.get(event);
      if (callback) {
        callback({ data: JSON.stringify(payload) } as MessageEvent);
      }
    }

    triggerError() {
      if (this.onerror) {
        this.onerror(new Event('error'));
      }
    }
  }

  beforeEach(() => {
    originalEventSource = (window as any).EventSource;
    (window as any).EventSource = FakeEventSource as any;

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });

    zone = TestBed.inject(NgZone);
    zoneRunSpy = spyOn(zone, 'run').and.callFake(((fn: (...args: any[]) => any, applyThis?: any, applyArgs?: any[]) => {
      return fn.apply(applyThis, applyArgs ?? []);
    }) as any);

    service = TestBed.inject(DraftService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    (window as any).EventSource = originalEventSource;
    FakeEventSource.instances = [];
  });

  it('gets the current draft', () => {
    service.getCurrent().subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/api/drafts/current`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 'draft-1' });
  });

  it('starts a draft with season param', () => {
    service.start(2024).subscribe();

    const req = httpMock.expectOne((request) => request.url === `${environment.apiUrl}/api/drafts/start`);
    expect(req.request.method).toBe('POST');
    expect(req.request.params.get('season')).toBe('2024');
    req.flush({ id: 'draft-1' });
  });

  it('submits a pick with params', () => {
    service.pick('draft-1', 'player-1', 'user-1').subscribe();

    const req = httpMock.expectOne((request) => request.url === `${environment.apiUrl}/api/drafts/pick`);
    expect(req.request.method).toBe('POST');
    expect(req.request.params.get('draftId')).toBe('draft-1');
    expect(req.request.params.get('playerId')).toBe('player-1');
    expect(req.request.params.get('participant')).toBe('user-1');
    req.flush({});
  });

  it('streams draft events through NgZone', () => {
    const events: Array<{ event: string; data: any }> = [];

    const subscription = service.stream().subscribe((value) => {
      events.push(value);
    });

    const instance = FakeEventSource.instances[0];
    instance.emit('START', { id: 'draft-1' });
    instance.emit('PICK', { id: 'pick-1' });

    expect(events.length).toBe(2);
    expect(events[0].event).toBe('START');
    expect(events[1].event).toBe('PICK');
    expect(zoneRunSpy).toHaveBeenCalled();

    subscription.unsubscribe();
  });

  it('emits an error on stream failure', (done) => {
    service.stream().subscribe({
      error: (err) => {
        expect(err).toBe('SSE error');
        done();
      }
    });

    const instance = FakeEventSource.instances[0];
    instance.triggerError();
  });
});
