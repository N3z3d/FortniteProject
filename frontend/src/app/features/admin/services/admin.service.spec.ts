import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AdminService } from './admin.service';
import { environment } from '../../../../environments/environment';
import { DashboardSummary, SystemHealth, RecentActivity, SystemMetrics } from '../models/admin.models';
import { ErrorEntry, ErrorStatistics } from '../models/error-journal.models';

describe('AdminService', () => {
  let service: AdminService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/api/admin`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AdminService]
    });

    service = TestBed.inject(AdminService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getDashboardSummary', () => {
    it('should fetch dashboard summary and unwrap data', () => {
      const mockSummary: DashboardSummary = {
        totalUsers: 42,
        totalGames: 15,
        totalTrades: 8,
        gamesByStatus: { CREATING: 3, ACTIVE: 5 }
      };

      service.getDashboardSummary().subscribe(result => {
        expect(result.totalUsers).toBe(42);
        expect(result.totalGames).toBe(15);
        expect(result.totalTrades).toBe(8);
      });

      const req = httpMock.expectOne(`${baseUrl}/dashboard/summary`);
      expect(req.request.method).toBe('GET');
      req.flush({ success: true, data: mockSummary, message: 'OK', timestamp: '' });
    });
  });

  describe('getSystemHealth', () => {
    it('should fetch system health and unwrap data', () => {
      const mockHealth: SystemHealth = {
        status: 'UP',
        uptimeMillis: 60000,
        databasePool: { activeConnections: 2, idleConnections: 8, totalConnections: 10, maxConnections: 20 },
        disk: { totalSpaceBytes: 100000, freeSpaceBytes: 50000, usagePercent: 50 }
      };

      service.getSystemHealth().subscribe(result => {
        expect(result.status).toBe('UP');
        expect(result.databasePool.activeConnections).toBe(2);
      });

      const req = httpMock.expectOne(`${baseUrl}/dashboard/health`);
      expect(req.request.method).toBe('GET');
      req.flush({ success: true, data: mockHealth, message: 'OK', timestamp: '' });
    });
  });

  describe('getRecentActivity', () => {
    it('should fetch recent activity with default hours', () => {
      const mockActivity: RecentActivity = {
        recentGamesCount: 3,
        recentTradesCount: 1,
        recentUsersCount: 5,
        recentGames: [],
        recentTrades: []
      };

      service.getRecentActivity().subscribe(result => {
        expect(result.recentGamesCount).toBe(3);
      });

      const req = httpMock.expectOne(r => r.url === `${baseUrl}/dashboard/recent-activity`);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('hours')).toBe('24');
      req.flush({ success: true, data: mockActivity, message: 'OK', timestamp: '' });
    });

    it('should fetch recent activity with custom hours', () => {
      const mockActivity: RecentActivity = {
        recentGamesCount: 0,
        recentTradesCount: 0,
        recentUsersCount: 0,
        recentGames: [],
        recentTrades: []
      };

      service.getRecentActivity(1).subscribe(result => {
        expect(result.recentGamesCount).toBe(0);
      });

      const req = httpMock.expectOne(r => r.url === `${baseUrl}/dashboard/recent-activity`);
      expect(req.request.params.get('hours')).toBe('1');
      req.flush({ success: true, data: mockActivity, message: 'OK', timestamp: '' });
    });
  });

  describe('getSystemMetrics', () => {
    it('should fetch system metrics and unwrap data', () => {
      const mockMetrics: SystemMetrics = {
        jvm: { heapUsedBytes: 1000, heapMaxBytes: 2000, heapUsagePercent: 50, threadCount: 10 },
        http: { totalRequests: 100, errorRate: 2 }
      };

      service.getSystemMetrics().subscribe(result => {
        expect(result.jvm.heapUsedBytes).toBe(1000);
        expect(result.http.totalRequests).toBe(100);
      });

      const req = httpMock.expectOne(`${baseUrl}/system/metrics`);
      expect(req.request.method).toBe('GET');
      req.flush({ success: true, data: mockMetrics, message: 'OK', timestamp: '' });
    });
  });

  describe('getErrors', () => {
    const mockErrors: ErrorEntry[] = [
      {
        id: 'uuid-1',
        timestamp: '2026-02-17T10:00:00',
        exceptionType: 'GameNotFoundException',
        message: 'Game not found',
        statusCode: 404,
        errorCode: 'GAME_NOT_FOUND',
        path: '/api/games/123',
        stackTrace: 'stack trace here'
      }
    ];

    it('should fetch errors with default limit', () => {
      service.getErrors().subscribe(result => {
        expect(result.length).toBe(1);
        expect(result[0].exceptionType).toBe('GameNotFoundException');
      });

      const req = httpMock.expectOne(r => r.url === `${baseUrl}/errors`);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('limit')).toBe('50');
      req.flush({ success: true, data: mockErrors, message: 'OK', timestamp: '' });
    });

    it('should pass statusCode and type filters', () => {
      service.getErrors(10, 400, 'Business').subscribe();

      const req = httpMock.expectOne(r => r.url === `${baseUrl}/errors`);
      expect(req.request.params.get('limit')).toBe('10');
      expect(req.request.params.get('statusCode')).toBe('400');
      expect(req.request.params.get('type')).toBe('Business');
      req.flush({ success: true, data: [], message: 'OK', timestamp: '' });
    });
  });

  describe('getErrorStatistics', () => {
    it('should fetch error statistics and unwrap data', () => {
      const mockStats: ErrorStatistics = {
        totalErrors: 15,
        errorsByType: { GameNotFoundException: 10, BusinessException: 5 },
        errorsByStatusCode: { 404: 10, 400: 5 },
        topErrors: [{ type: 'GameNotFoundException', message: 'not found', count: 10, lastOccurrence: '2026-02-17T10:00:00' }]
      };

      service.getErrorStatistics(48).subscribe(result => {
        expect(result.totalErrors).toBe(15);
        expect(result.topErrors.length).toBe(1);
      });

      const req = httpMock.expectOne(r => r.url === `${baseUrl}/errors/stats`);
      expect(req.request.params.get('hours')).toBe('48');
      req.flush({ success: true, data: mockStats, message: 'OK', timestamp: '' });
    });
  });

  describe('getErrorDetail', () => {
    it('should fetch single error entry by id', () => {
      const mockEntry: ErrorEntry = {
        id: 'uuid-1',
        timestamp: '2026-02-17T10:00:00',
        exceptionType: 'BusinessException',
        message: 'rule violation',
        statusCode: 400,
        errorCode: 'BUSINESS_RULE_VIOLATION',
        path: '/api/trades',
        stackTrace: 'detailed stack trace'
      };

      service.getErrorDetail('uuid-1').subscribe(result => {
        expect(result.id).toBe('uuid-1');
        expect(result.exceptionType).toBe('BusinessException');
      });

      const req = httpMock.expectOne(`${baseUrl}/errors/uuid-1`);
      expect(req.request.method).toBe('GET');
      req.flush({ success: true, data: mockEntry, message: 'OK', timestamp: '' });
    });
  });
});
