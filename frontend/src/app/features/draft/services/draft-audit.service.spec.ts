import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { DraftAuditService, DraftAuditEntry } from './draft-audit.service';
import { environment } from '../../../../environments/environment';

const GAME_ID = 'game-abc';
const BASE_URL = `${environment.apiUrl}/api/games/${GAME_ID}/draft/audit`;

const ENTRY: DraftAuditEntry = {
  id: 'audit-1',
  type: 'SWAP_SOLO',
  occurredAt: '2026-03-01T10:00:00',
  participantId: 'part-1',
  proposerParticipantId: null,
  targetParticipantId: null,
  playerOutId: 'player-out',
  playerInId: 'player-in'
};

describe('DraftAuditService', () => {
  let service: DraftAuditService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });
    service = TestBed.inject(DraftAuditService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getAudit returns entries on success', () => {
    let result: DraftAuditEntry[] | undefined;
    service.getAudit(GAME_ID).subscribe(data => (result = data));

    httpMock.expectOne(BASE_URL).flush([ENTRY]);
    expect(result).toEqual([ENTRY]);
  });

  it('getAudit returns empty array on HTTP error', () => {
    let result: DraftAuditEntry[] | undefined;
    service.getAudit(GAME_ID).subscribe(data => (result = data));

    httpMock.expectOne(BASE_URL).error(new ProgressEvent('error'));
    expect(result).toEqual([]);
  });

  it('getAudit calls correct URL', () => {
    service.getAudit(GAME_ID).subscribe();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
  });
});
