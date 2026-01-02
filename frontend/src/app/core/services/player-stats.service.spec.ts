import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { PlayerStatsService } from './player-stats.service';
import { LoggerService } from './logger.service';
import { environment } from '../../../environments/environment';

describe('PlayerStatsService', () => {
  let service: PlayerStatsService;
  let httpMock: HttpTestingController;
  let logger: jasmine.SpyObj<LoggerService>;
  const statsUrl = `${environment.apiUrl}/players/stats`;

  beforeEach(() => {
    logger = jasmine.createSpyObj('LoggerService', ['debug', 'error', 'warn']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        PlayerStatsService,
        { provide: LoggerService, useValue: logger }
      ]
    });

    service = TestBed.inject(PlayerStatsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should return normalized stats', () => {
    const payload = { totalPlayers: 147 };

    service.getPlayerStats().subscribe(stats => {
      expect(stats.totalPlayers).toBe(147);
      expect(stats.playersByRegion).toEqual({});
      expect(stats.playersByTranche).toEqual({});
    });

    const req = httpMock.expectOne(statsUrl);
    expect(req.request.method).toBe('GET');
    req.flush(payload);
  });

  it('should derive totalPlayers from region data when missing', () => {
    const payload = { totalPlayers: 0, playersByRegion: { EU: 140, NAC: 7 } };

    service.getPlayerStats().subscribe(stats => {
      expect(stats.totalPlayers).toBe(147);
      expect(logger.warn).toHaveBeenCalled();
    });

    const req = httpMock.expectOne(statsUrl);
    req.flush(payload);
  });

  it('should surface http errors', () => {
    service.getPlayerStats().subscribe({
      next: () => fail('should not succeed'),
      error: (error) => {
        expect(error).toBeTruthy();
      }
    });

    const req = httpMock.expectOne(statsUrl);
    req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
  });
});
