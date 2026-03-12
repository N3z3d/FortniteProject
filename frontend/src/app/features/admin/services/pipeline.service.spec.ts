import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PipelineService } from './pipeline.service';
import { environment } from '../../../../environments/environment';
import { PlayerIdentityEntry, PipelineAlertStatus, ScrapeLogEntry } from '../models/admin.models';

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
});
