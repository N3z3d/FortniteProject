import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Subject } from 'rxjs';
import { PipelineService } from './pipeline.service';
import { environment } from '../../../../environments/environment';
import { EpicIdSuggestion, PlayerIdentityEntry, PipelineAlertStatus, ScrapeLogEntry, AdapterInfo } from '../models/admin.models';

const BASE = `${environment.apiUrl}/api/admin/players`;
const SCRAPE_BASE = `${environment.apiUrl}/api/admin/scraping`;

const MOCK_ENTRY: PlayerIdentityEntry = {
  id: 'e1',
  playerId: 'p1',
  playerUsername: 'Bughaboo',
  playerRegion: 'EU',
  epicId: null,
  status: 'UNRESOLVED',
  confidenceScore: null,
  resolvedBy: null,
  resolvedAt: null,
  rejectedAt: null,
  rejectionReason: null,
  createdAt: '2026-01-01T00:00:00',
  correctedUsername: null,
  correctedRegion: null,
  correctedBy: null,
  correctedAt: null
};

describe('PipelineService', () => {
  let service: PipelineService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PipelineService]
    });
    service = TestBed.inject(PipelineService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  describe('correctMetadata', () => {
    it('sends PATCH to /{playerId}/metadata and returns updated entry', () => {
      const playerId = 'p1';
      const body = { newUsername: 'NewName', newRegion: 'NAW' };
      let result: PlayerIdentityEntry | null | undefined;

      service.correctMetadata(playerId, body).subscribe(r => (result = r));

      const req = httpMock.expectOne(`${BASE}/${playerId}/metadata`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual(body);
      req.flush(MOCK_ENTRY);

      expect(result).toEqual(MOCK_ENTRY);
    });

    it('returns null on HTTP error', () => {
      let result: PlayerIdentityEntry | null | undefined;

      service.correctMetadata('p1', { newRegion: 'EU' }).subscribe(r => (result = r));

      httpMock
        .expectOne(`${BASE}/p1/metadata`)
        .flush(null, { status: 500, statusText: 'Server Error' });

      expect(result).toBeNull();
    });
  });

  describe('getScrapeLog', () => {
    it('sends GET to /logs?limit=50 and returns entries', () => {
      const mockLog: ScrapeLogEntry = {
        id: 'l1',
        source: 'EU',
        startedAt: '2026-03-01T05:00:00Z',
        finishedAt: '2026-03-01T06:00:00Z',
        status: 'SUCCESS',
        totalRowsWritten: 200,
        errorMessage: null
      };
      let result: ScrapeLogEntry[] | undefined;

      service.getScrapeLog(50).subscribe(r => (result = r));

      const req = httpMock.expectOne(`${SCRAPE_BASE}/logs?limit=50`);
      expect(req.request.method).toBe('GET');
      req.flush([mockLog]);

      expect(result).toEqual([mockLog]);
    });

    it('returns empty array on HTTP error', () => {
      let result: ScrapeLogEntry[] | undefined;

      service.getScrapeLog().subscribe(r => (result = r));

      httpMock
        .expectOne(`${SCRAPE_BASE}/logs?limit=50`)
        .flush(null, { status: 500, statusText: 'Server Error' });

      expect(result).toEqual([]);
    });
  });

  describe('getUnresolvedAlertStatus', () => {
    it('sends GET to /alert and returns status', () => {
      const mockAlert: PipelineAlertStatus = {
        level: 'CRITICAL',
        unresolvedCount: 5,
        oldestUnresolvedAt: '2026-02-27T10:00:00Z',
        elapsedHours: 72,
        checkedAt: '2026-03-01T12:00:00Z'
      };
      let result: PipelineAlertStatus | null | undefined;

      service.getUnresolvedAlertStatus().subscribe(r => (result = r));

      const req = httpMock.expectOne(`${SCRAPE_BASE}/alert`);
      expect(req.request.method).toBe('GET');
      req.flush(mockAlert);

      expect(result).toEqual(mockAlert);
    });

    it('returns null on HTTP error', () => {
      let result: PipelineAlertStatus | null | undefined;

      service.getUnresolvedAlertStatus().subscribe(r => (result = r));

      httpMock
        .expectOne(`${SCRAPE_BASE}/alert`)
        .flush(null, { status: 503, statusText: 'Service Unavailable' });

      expect(result).toBeNull();
    });
  });

  describe('getSuggestedEpicId retry on 429', () => {
    const url = `${BASE}/p1/suggest-epic-id`;
    const MOCK_SUGGESTION: EpicIdSuggestion = {
      suggestedEpicId: 'epic_retry_ok',
      displayName: 'Bughaboo',
      confidenceScore: 85,
      found: true
    };

    it('retries once on 429 and returns success on 2nd attempt', fakeAsync(() => {
      let result: EpicIdSuggestion | null | undefined;

      service.getSuggestedEpicId('p1').subscribe(r => (result = r));

      httpMock.expectOne(url).flush(null, { status: 429, statusText: 'Too Many Requests' });
      tick(1_000);

      httpMock.expectOne(url).flush(MOCK_SUGGESTION);

      expect(result).toEqual(MOCK_SUGGESTION);
    }));

    it('signals exhausted rate limit once after 3 retries all 429 and returns null', fakeAsync(() => {
      const onRetry = vi.fn();
      const onRateLimitExhausted = vi.fn();
      let result: EpicIdSuggestion | null | undefined;

      service
        .getSuggestedEpicId('p1', { onRetry, onRateLimitExhausted })
        .subscribe(r => (result = r));

      httpMock.expectOne(url).flush(null, { status: 429, statusText: 'TRM' });
      tick(1_000);
      httpMock.expectOne(url).flush(null, { status: 429, statusText: 'TRM' });
      tick(2_000);
      httpMock.expectOne(url).flush(null, { status: 429, statusText: 'TRM' });
      tick(4_000);
      httpMock.expectOne(url).flush(null, { status: 429, statusText: 'TRM' });

      expect(onRetry).toHaveBeenCalledTimes(3);
      expect(onRateLimitExhausted).toHaveBeenCalledTimes(1);
      expect(result).toBeNull();
    }));

    it('does not signal exhausted rate limit when 429 retries eventually succeed', fakeAsync(() => {
      const onRetry = vi.fn();
      const onRateLimitExhausted = vi.fn();
      let result: EpicIdSuggestion | null | undefined;

      service
        .getSuggestedEpicId('p1', { onRetry, onRateLimitExhausted })
        .subscribe(r => (result = r));

      httpMock.expectOne(url).flush(null, { status: 429, statusText: 'TRM' });
      tick(1_000);
      httpMock.expectOne(url).flush(null, { status: 429, statusText: 'TRM' });
      tick(2_000);

      httpMock.expectOne(url).flush(MOCK_SUGGESTION);

      expect(onRetry).toHaveBeenCalledTimes(2);
      expect(onRateLimitExhausted).not.toHaveBeenCalled();
      expect(result).toEqual(MOCK_SUGGESTION);
    }));

    it('signals temporary resolution unavailable on non-429 server errors (500)', fakeAsync(() => {
      const onRetry = vi.fn();
      const onRateLimitExhausted = vi.fn();
      const onResolutionUnavailable = vi.fn();
      let result: EpicIdSuggestion | null | undefined;

      service
        .getSuggestedEpicId('p1', { onRetry, onRateLimitExhausted, onResolutionUnavailable })
        .subscribe(r => (result = r));

      httpMock.expectOne(url).flush(null, { status: 500, statusText: 'Internal Server Error' });

      expect(onRetry).not.toHaveBeenCalled();
      expect(onRateLimitExhausted).not.toHaveBeenCalled();
      expect(onResolutionUnavailable).toHaveBeenCalledTimes(1);
      expect(result).toBeNull();
      httpMock.verify();
    }));

    it('signals temporary resolution unavailable on 503 without rate-limit callback', fakeAsync(() => {
      const onRateLimitExhausted = vi.fn();
      const onResolutionUnavailable = vi.fn();
      let result: EpicIdSuggestion | null | undefined;

      service
        .getSuggestedEpicId('p1', { onRateLimitExhausted, onResolutionUnavailable })
        .subscribe(r => (result = r));

      httpMock.expectOne(url).flush(null, { status: 503, statusText: 'Service Unavailable' });

      expect(onRateLimitExhausted).not.toHaveBeenCalled();
      expect(onResolutionUnavailable).toHaveBeenCalledTimes(1);
      expect(result).toBeNull();
    }));

    it('signals temporary resolution unavailable on network failure without rate-limit callback', fakeAsync(() => {
      const onRateLimitExhausted = vi.fn();
      const onResolutionUnavailable = vi.fn();
      let result: EpicIdSuggestion | null | undefined;

      service
        .getSuggestedEpicId('p1', { onRateLimitExhausted, onResolutionUnavailable })
        .subscribe(r => (result = r));

      httpMock.expectOne(url).flush(null, { status: 0, statusText: 'Unknown Error' });

      expect(onRateLimitExhausted).not.toHaveBeenCalled();
      expect(onResolutionUnavailable).toHaveBeenCalledTimes(1);
      expect(result).toBeNull();
    }));

    it('calls onRetry callback on each 429 before retry delay', fakeAsync(() => {
      const onRetry = vi.fn();

      service.getSuggestedEpicId('p1', { onRetry }).subscribe();

      httpMock.expectOne(url).flush(null, { status: 429, statusText: 'TRM' });
      tick(1_000);

      expect(onRetry).toHaveBeenCalledTimes(1);

      httpMock.expectOne(url).flush(null, { status: 429, statusText: 'TRM' });
      tick(2_000);

      expect(onRetry).toHaveBeenCalledTimes(2);

      httpMock.expectOne(url).flush(null, { status: 429, statusText: 'TRM' });
      tick(4_000);

      expect(onRetry).toHaveBeenCalledTimes(3);

      httpMock.expectOne(url).flush(MOCK_SUGGESTION);
    }));
  });

  describe('getAdapterInfo', () => {
    it('returns adapter info from GET /pipeline/adapter-info', () => {
      const mockInfo: AdapterInfo = { adapter: 'stub' };
      let result: AdapterInfo | null | undefined;

      service.getAdapterInfo().subscribe(r => (result = r));

      const req = httpMock.expectOne(`${BASE}/pipeline/adapter-info`);
      expect(req.request.method).toBe('GET');
      req.flush(mockInfo);

      expect(result).toEqual({ adapter: 'stub' });
    });

    it('returns null on HTTP error', () => {
      let result: AdapterInfo | null | undefined;

      service.getAdapterInfo().subscribe(r => (result = r));

      httpMock
        .expectOne(`${BASE}/pipeline/adapter-info`)
        .flush(null, { status: 503, statusText: 'Service Unavailable' });

      expect(result).toBeNull();
    });
  });
});
