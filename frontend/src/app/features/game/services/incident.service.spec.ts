import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { environment } from '../../../../environments/environment';
import { IncidentService } from './incident.service';
import { IncidentEntry, IncidentReportRequest } from '../../admin/models/admin.models';

describe('IncidentService', () => {
  let service: IncidentService;
  let httpMock: HttpTestingController;

  const mockEntry: IncidentEntry = {
    id: 'entry-uuid',
    gameId: 'game-uuid',
    gameName: 'Test Game',
    reporterId: 'reporter-uuid',
    reporterUsername: 'player1',
    incidentType: 'CHEATING',
    description: 'Player was using aimbots',
    timestamp: '2026-02-27T10:00:00'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [IncidentService]
    });

    service = TestBed.inject(IncidentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('reportIncident', () => {
    const request: IncidentReportRequest = {
      incidentType: 'CHEATING',
      description: 'Player was using aimbots'
    };

    it('should POST to the correct endpoint and unwrap response data', () => {
      service.reportIncident('game-uuid', request).subscribe(result => {
        expect(result.id).toBe('entry-uuid');
        expect(result.incidentType).toBe('CHEATING');
        expect(result.reporterUsername).toBe('player1');
      });

      const req = httpMock.expectOne(
        `${environment.apiUrl}/api/games/game-uuid/incidents`
      );
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush({ success: true, data: mockEntry, message: 'Incident reported successfully. Thank you.', timestamp: '' });
    });

    it('should propagate HTTP errors', () => {
      let caught = false;
      service.reportIncident('unknown-game', request).subscribe({
        next: () => fail('should have errored'),
        error: () => { caught = true; }
      });

      const req = httpMock.expectOne(
        `${environment.apiUrl}/api/games/unknown-game/incidents`
      );
      req.flush({ message: 'Game not found' }, { status: 404, statusText: 'Not Found' });

      expect(caught).toBeTrue();
    });

    it('should propagate 401 errors for non-participants', () => {
      let caught = false;
      service.reportIncident('game-uuid', request).subscribe({
        next: () => fail('should have errored'),
        error: () => { caught = true; }
      });

      const req = httpMock.expectOne(
        `${environment.apiUrl}/api/games/game-uuid/incidents`
      );
      req.flush({}, { status: 401, statusText: 'Unauthorized' });

      expect(caught).toBeTrue();
    });
  });
});
