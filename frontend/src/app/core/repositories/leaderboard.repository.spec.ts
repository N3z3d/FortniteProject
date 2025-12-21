import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import {
  HttpLeaderboardRepository,
  MockLeaderboardRepository
} from './leaderboard.repository';
import { LoggerService } from '../services/logger.service';
import { environment } from '../../../environments/environment';

describe('LeaderboardRepository', () => {
  let httpMock: HttpTestingController;
  let httpRepo: HttpLeaderboardRepository;
  let mockRepo: MockLeaderboardRepository;
  let loggerService: jasmine.SpyObj<LoggerService>;

  beforeEach(() => {
    const loggerSpy = jasmine.createSpyObj('LoggerService', ['debug', 'error', 'warn']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        HttpLeaderboardRepository,
        MockLeaderboardRepository,
        { provide: LoggerService, useValue: loggerSpy }
      ]
    });

    httpMock = TestBed.inject(HttpTestingController);
    httpRepo = TestBed.inject(HttpLeaderboardRepository);
    mockRepo = TestBed.inject(MockLeaderboardRepository);
    loggerService = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('HttpLeaderboardRepository', () => {
    it('should fetch leaderboard from API', (done) => {
      const mockData = [
        {
          rank: 1,
          userId: 'user1',
          username: 'Team A',
          totalPoints: 100,
          teamName: 'Team A',
          isSpecial: false,
          pointsByRegion: {},
          pointsByTranche: {},
          winRate: 0,
          averagePoints: 100,
          recentMovements: []
        }
      ];

      httpRepo.getLeaderboard({ season: 2025 }).subscribe(data => {
        expect(data.length).toBe(1);
        expect(data[0].rank).toBe(1);
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard?season=2025`);
      expect(req.request.method).toBe('GET');
      req.flush(mockData);
    });

    it('should throw error when API fails', (done) => {
      httpRepo.getLeaderboard({ season: 2025 }).subscribe({
        next: () => fail('Should have failed'),
        error: (error) => {
          expect(error).toBeDefined();
          expect(loggerService.error).toHaveBeenCalled();
          done();
        }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard?season=2025`);
      req.error(new ProgressEvent('Network error'));
    });

    it('should fetch stats with season parameter', (done) => {
      const mockStats = { totalTeams: 5, totalPlayers: 50 };

      httpRepo.getStats(2025).subscribe(data => {
        expect(data).toEqual(mockStats);
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/leaderboard/stats?season=2025`);
      expect(req.request.method).toBe('GET');
      req.flush(mockStats);
    });
  });

  describe('MockLeaderboardRepository', () => {
    it('should return empty leaderboard', (done) => {
      mockRepo.getLeaderboard({ season: 2025 }).subscribe(data => {
        expect(data).toEqual([]);
        expect(loggerService.warn).toHaveBeenCalled();
        done();
      });
    });

    it('should return empty stats', (done) => {
      mockRepo.getStats(2025).subscribe(data => {
        expect(data.totalTeams).toBe(0);
        expect(data.totalPlayers).toBe(0);
        done();
      });
    });

    it('should return empty region distribution', (done) => {
      mockRepo.getRegionDistribution().subscribe(data => {
        expect(data['EU']).toBe(0);
        expect(data['NAC']).toBe(0);
        done();
      });
    });
  });
});
