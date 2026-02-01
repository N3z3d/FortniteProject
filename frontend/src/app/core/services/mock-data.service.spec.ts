import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MockDataService } from './mock-data.service';
import { LoggerService } from './logger.service';
import { MOCK_GAMES } from '../data/mock-game-data';

describe('MockDataService', () => {
  let service: MockDataService;
  let logger: jasmine.SpyObj<LoggerService>;

  beforeEach(() => {
    logger = jasmine.createSpyObj('LoggerService', ['debug']);

    TestBed.configureTestingModule({
      providers: [
        MockDataService,
        { provide: LoggerService, useValue: logger }
      ]
    });

    service = TestBed.inject(MockDataService);
  });

  it('logs initialization on construction', () => {
    expect(logger.debug).toHaveBeenCalled();
  });

  it('returns mock games after delay', fakeAsync(() => {
    let result: unknown;

    service.getMockGames().subscribe((data) => {
      result = data;
    });

    expect(result).toBeUndefined();
    tick(800);
    expect(result).toBe(MOCK_GAMES);
  }));

  it('returns mock leaderboard data after delay', fakeAsync(() => {
    let result: any[] | undefined;

    service.getMockLeaderboard().subscribe((data) => {
      result = data;
    });

    tick(800);

    expect(result?.length).toBe(2);
    expect(result?.[0].teamName).toBe('Alpha Squad');
    expect(result?.[0].players.length).toBe(2);
  }));

  it('returns mock statistics after delay', fakeAsync(() => {
    let result: any;

    service.getMockStatistics().subscribe((data) => {
      result = data;
    });

    tick(600);

    expect(result?.totalTeams).toBe(2);
    expect(result?.seasonProgress).toBe(75);
  }));

  it('returns mock region distribution after delay', fakeAsync(() => {
    let result: Record<string, number> | undefined;

    service.getMockRegionDistribution().subscribe((data) => {
      result = data;
    });

    tick(400);

    expect(result?.['EU']).toBe(2);
    expect(result?.['NAE']).toBe(2);
    expect(result?.['ME']).toBe(0);
  }));

  it('returns mock dashboard data and logs game id', fakeAsync(() => {
    let result: any;

    service.getMockDashboardData('game-123').subscribe((data) => {
      result = data;
    });

    tick(800);

    expect(logger.debug).toHaveBeenCalledWith(
      jasmine.stringMatching('Loading basic mock data for gameId:'),
      'game-123'
    );
    expect(result?.statistics?.totalTeams).toBe(2);
    expect(result?.leaderboard?.[0].teamId).toBe('team-1');
  }));

  it('reports backend availability as false', fakeAsync(() => {
    let result: boolean | undefined;

    service.checkBackendAvailability().subscribe((data) => {
      result = data;
    });

    tick(1000);

    expect(result).toBeFalse();
  }));
});
