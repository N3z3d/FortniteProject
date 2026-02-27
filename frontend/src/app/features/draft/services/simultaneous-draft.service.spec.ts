import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { SimultaneousDraftService, SimultaneousStatusResponse } from './simultaneous-draft.service';
import { environment } from '../../../../environments/environment';

describe('SimultaneousDraftService', () => {
  let service: SimultaneousDraftService;
  let httpMock: HttpTestingController;

  const BASE = `${environment.apiUrl}/api/draft/simultaneous`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [SimultaneousDraftService],
    });
    service = TestBed.inject(SimultaneousDraftService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  describe('getStatus', () => {
    it('should GET /{draftId}/status', () => {
      const expected: SimultaneousStatusResponse = { windowId: 'w1', submitted: 2, total: 5 };

      service.getStatus('draft1').subscribe(res => expect(res).toEqual(expected));

      const req = httpMock.expectOne(`${BASE}/draft1/status`);
      expect(req.request.method).toBe('GET');
      req.flush(expected);
    });

    it('should return null on HTTP error', () => {
      service.getStatus('draft1').subscribe(res => expect(res).toBeNull());

      const req = httpMock.expectOne(`${BASE}/draft1/status`);
      req.flush('error', { status: 404, statusText: 'Not Found' });
    });
  });

  describe('submitSelection', () => {
    it('should POST /{draftId}/submit with correct body', () => {
      service.submitSelection('draft1', 'win1', 'part1', 'player1').subscribe();

      const req = httpMock.expectOne(`${BASE}/draft1/submit`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({
        windowId: 'win1',
        participantId: 'part1',
        playerId: 'player1',
      });
      req.flush(null);
    });
  });
});
