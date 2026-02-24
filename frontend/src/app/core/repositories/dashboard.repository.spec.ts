import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import {
  HttpDashboardRepository,
  MockDashboardRepository
} from './dashboard.repository';
import { LoggerService } from '../services/logger.service';
import { environment } from '../../../environments/environment';

describe('DashboardRepository', () => {
  let httpMock: HttpTestingController;
  let httpRepo: HttpDashboardRepository;
  let mockRepo: MockDashboardRepository;
  let loggerService: jasmine.SpyObj<LoggerService>;

  beforeEach(() => {
    const loggerSpy = jasmine.createSpyObj('LoggerService', ['debug', 'error', 'warn']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        HttpDashboardRepository,
        MockDashboardRepository,
        { provide: LoggerService, useValue: loggerSpy }
      ]
    });

    httpMock = TestBed.inject(HttpTestingController);
    httpRepo = TestBed.inject(HttpDashboardRepository);
    mockRepo = TestBed.inject(MockDashboardRepository);
    loggerService = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('HttpDashboardRepository', () => {
    it('should map statistics response and include season progress', (done) => {
      const apiResponse = {
        totalTeams: 10,
        totalPlayers: 20,
        totalPoints: 300,
        averagePointsPerTeam: 30,
        mostActiveTeam: 'Team A'
      };

      httpRepo.getStatistics('game-1').subscribe(stats => {
        expect(stats.totalTeams).toBe(10);
        expect(stats.totalPlayers).toBe(20);
        expect(stats.totalPoints).toBe(300);
        expect(stats.averagePointsPerTeam).toBe(30);
        expect(stats.mostActiveTeam).toBe('Team A');
        expect(stats.seasonProgress).toEqual(jasmine.any(Number));
        expect(stats.seasonProgress).toBeGreaterThanOrEqual(0);
        expect(stats.seasonProgress).toBeLessThanOrEqual(100);
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/stats?season=2025&gameId=game-1`);
      expect(req.request.method).toBe('GET');
      req.flush(apiResponse);
    });

    it('should request stats without gameId when not provided', (done) => {
      httpRepo.getStatistics('').subscribe(stats => {
        expect(stats.totalTeams).toBe(0);
        expect(stats.mostActiveTeam).toBe('');
        expect(stats.seasonProgress).toEqual(jasmine.any(Number));
        expect(stats.seasonProgress).toBeGreaterThanOrEqual(0);
        expect(stats.seasonProgress).toBeLessThanOrEqual(100);
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/stats?season=2025`);
      req.flush({});
    });

    it('should unwrap leaderboard data responses', (done) => {
      const leaderboard = [
        { teamId: 't1', teamName: 'Alpha', totalPoints: 10 }
      ];

      httpRepo.getLeaderboard('game-1').subscribe(data => {
        expect(data).toEqual(leaderboard);
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard?season=2025&gameId=game-1`);
      req.flush({ data: leaderboard });
    });

    it('should normalize region distribution', (done) => {
      httpRepo.getRegionDistribution('').subscribe(data => {
        expect(data['EU']).toBe(2);
        expect(data['NAW']).toBe(1);
        expect(data['NAC']).toBe(0);
        expect(Object.keys(data).length).toBe(7);
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/distribution/regions`);
      req.flush({ EU: 2, NAW: 1 });
    });

    it('should aggregate dashboard data and build teams', (done) => {
      const leaderboard = [
        { teamId: 't1', teamName: 'Alpha', totalPoints: 10, ownerName: 'Owner', players: [{ id: 1 }] },
        { id: 't2', teamName: 'Beta', ownerName: 'Owner 2' }
      ];

      httpRepo.getDashboardData('game-1').subscribe(data => {
        expect(data.teams.length).toBe(2);
        expect(data.teams[0]).toEqual(jasmine.objectContaining({
          id: 't1',
          name: 'Alpha',
          totalPoints: 10,
          ownerName: 'Owner'
        }));
        expect(data.teams[1].id).toBe('t2');
        expect(data.teams[1].totalPoints).toBe(0);
        done();
      });

      const statsReq = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/stats?season=2025&gameId=game-1`);
      const leaderboardReq = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard?season=2025&gameId=game-1`);
      const regionReq = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/distribution/regions?gameId=game-1`);

      statsReq.flush({
        totalTeams: 2,
        totalPlayers: 4,
        totalPoints: 10,
        averagePointsPerTeam: 5,
        mostActiveTeam: 'Alpha'
      });
      leaderboardReq.flush({ data: leaderboard });
      regionReq.flush({ EU: 1 });
    });

    it('should surface errors and log them', (done) => {
      httpRepo.getLeaderboard('game-1').subscribe({
        next: () => fail('Expected error'),
        error: (error) => {
          expect(error).toBeDefined();
          expect(loggerService.error).toHaveBeenCalled();
          done();
        }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard?season=2025&gameId=game-1`);
      req.error(new ProgressEvent('Network error'));
    });
  });

  describe('MockDashboardRepository', () => {
    it('should return empty dashboard data', (done) => {
      mockRepo.getDashboardData('game-1').subscribe(data => {
        expect(data.statistics.seasonProgress).toEqual(jasmine.any(Number));
        expect(data.statistics.seasonProgress).toBeGreaterThanOrEqual(0);
        expect(data.statistics.seasonProgress).toBeLessThanOrEqual(100);
        expect(data.leaderboard).toEqual([]);
        expect(data.regionDistribution['EU']).toBe(0);
        expect(loggerService.warn).toHaveBeenCalled();
        done();
      });
    });

    it('should return empty stats and region distribution', (done) => {
      mockRepo.getStatistics('game-1').subscribe(stats => {
        expect(stats.totalTeams).toBe(0);
        expect(stats.seasonProgress).toEqual(jasmine.any(Number));
        expect(stats.seasonProgress).toBeGreaterThanOrEqual(0);
        expect(stats.seasonProgress).toBeLessThanOrEqual(100);
      });

      mockRepo.getRegionDistribution('game-1').subscribe(distribution => {
        expect(distribution['ME']).toBe(0);
        expect(loggerService.warn).toHaveBeenCalled();
        done();
      });
    });
  });
});
