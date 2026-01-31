import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { LeaderboardService } from './leaderboard.service';
import { LoggerService } from './logger.service';
import { environment } from '../../../environments/environment';

describe('LeaderboardService', () => {
  let service: LeaderboardService;
  let httpMock: HttpTestingController;
  let logger: jasmine.SpyObj<LoggerService>;
  const endpoint = `${environment.apiUrl}/api/leaderboard/joueurs`;

  beforeEach(() => {
    logger = jasmine.createSpyObj('LoggerService', ['debug', 'error', 'warn']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        LeaderboardService,
        { provide: LoggerService, useValue: logger }
      ]
    });

    service = TestBed.inject(LeaderboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('maps wrapped player leaderboard payloads with region normalization', () => {
    const payload = [
      {
        playerId: 'p1',
        nickname: 'Alpha',
        username: 'alpha',
        region: 'EU',
        rank: 2,
        totalPoints: 120,
        teamsCount: 1
      }
    ];

    service.getPlayerLeaderboard(2025).subscribe(entries => {
      expect(entries.length).toBe(1);
      expect(entries[0].region).toBe('EU');
      expect(entries[0].rank).toBe(2);
      expect(entries[0].totalPoints).toBe(120);
    });

    const req = httpMock.expectOne(request => {
      return request.url === endpoint && request.params.get('season') === '2025';
    });
    expect(req.request.method).toBe('GET');
    req.flush(payload);
  });

  it('returns empty list for unexpected payloads', () => {
    service.getPlayerLeaderboard(2025).subscribe(entries => {
      expect(entries).toEqual([]);
    });

    const req = httpMock.expectOne(request => {
      return request.url === endpoint && request.params.get('season') === '2025';
    });
    req.flush([]);
  });

  it('returns entries even when required player fields are missing', () => {
    const payload = [
      {
        playerId: '',
        nickname: '',
        region: null,
        totalPoints: 10
      }
    ];

    service.getPlayerLeaderboard(2025).subscribe(entries => {
      expect(entries.length).toBe(1);
    });

    const req = httpMock.expectOne(request => {
      return request.url === endpoint && request.params.get('season') === '2025';
    });
    req.flush(payload);
  });
});
