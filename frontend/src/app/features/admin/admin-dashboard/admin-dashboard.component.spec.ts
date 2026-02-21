import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { AdminDashboardComponent } from './admin-dashboard.component';
import { AdminService } from '../services/admin.service';
import { TranslationService } from '../../../core/services/translation.service';
import {
  AdminAlert,
  DashboardSummary,
  RecentActivity,
  SystemHealth,
  SystemMetrics,
  VisitAnalytics
} from '../models/admin.models';

describe('AdminDashboardComponent', () => {
  let component: AdminDashboardComponent;
  let fixture: ComponentFixture<AdminDashboardComponent>;
  let adminService: jasmine.SpyObj<AdminService>;

  const mockSummary: DashboardSummary = {
    totalUsers: 42,
    totalGames: 15,
    totalTrades: 8,
    gamesByStatus: { CREATING: 3, ACTIVE: 5, DRAFTING: 2, FINISHED: 4, CANCELLED: 1 }
  };

  const mockHealth: SystemHealth = {
    status: 'UP',
    uptimeMillis: 3600000,
    databasePool: { activeConnections: 2, idleConnections: 8, totalConnections: 10, maxConnections: 20 },
    disk: { totalSpaceBytes: 100000000000, freeSpaceBytes: 50000000000, usagePercent: 50 }
  };

  const mockActivity: RecentActivity = {
    recentGamesCount: 3,
    recentTradesCount: 1,
    recentUsersCount: 5,
    recentGames: [
      { id: '1', name: 'TestGame', status: 'CREATING', createdAt: '2026-02-09T10:00:00' }
    ],
    recentTrades: []
  };

  const mockMetrics: SystemMetrics = {
    jvm: { heapUsedBytes: 256000000, heapMaxBytes: 512000000, heapUsagePercent: 50, threadCount: 25 },
    http: { totalRequests: 1000, errorRate: 1.5 }
  };

  const mockAlerts: AdminAlert[] = [
    {
      code: 'HTTP_ERROR_RATE_HIGH',
      severity: 'WARNING',
      title: 'High error rate',
      message: 'Error rate exceeded threshold',
      currentValue: 10,
      thresholdValue: 5,
      triggeredAt: '2026-02-21T08:00:00'
    }
  ];

  const mockVisitAnalytics: VisitAnalytics = {
    pageViews: 120,
    uniqueVisitors: 32,
    activeSessions: 18,
    averageSessionDurationSeconds: 140,
    bounceRatePercent: 42.5,
    topPages: [{ path: '/api/games', views: 40 }],
    topNavigationFlows: [{ fromPath: '/api/games', toPath: '/api/trades', transitions: 12 }]
  };

  beforeEach(async () => {
    const adminSpy = jasmine.createSpyObj('AdminService', [
      'getDashboardSummary',
      'getSystemHealth',
      'getRecentActivity',
      'getSystemMetrics',
      'getAlerts',
      'getVisitAnalytics'
    ]);

    const translationSpy = jasmine.createSpyObj('TranslationService', ['t']);
    translationSpy.t.and.callFake((key: string) => key);

    await TestBed.configureTestingModule({
      imports: [AdminDashboardComponent, NoopAnimationsModule],
      providers: [
        { provide: AdminService, useValue: adminSpy },
        { provide: TranslationService, useValue: translationSpy }
      ]
    }).compileComponents();

    adminService = TestBed.inject(AdminService) as jasmine.SpyObj<AdminService>;
  });

  function createComponent(): void {
    adminService.getDashboardSummary.and.returnValue(of(mockSummary));
    adminService.getSystemHealth.and.returnValue(of(mockHealth));
    adminService.getRecentActivity.and.returnValue(of(mockActivity));
    adminService.getSystemMetrics.and.returnValue(of(mockMetrics));
    adminService.getAlerts.and.returnValue(of(mockAlerts));
    adminService.getVisitAnalytics.and.returnValue(of(mockVisitAnalytics));

    fixture = TestBed.createComponent(AdminDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('should create', () => {
    createComponent();
    expect(component).toBeTruthy();
  });

  it('should load dashboard data on init', () => {
    createComponent();
    expect(component.loading).toBeFalse();
    expect(component.summary).toEqual(mockSummary);
    expect(component.health).toEqual(mockHealth);
    expect(component.activity).toEqual(mockActivity);
    expect(component.metrics).toEqual(mockMetrics);
    expect(component.alerts).toEqual(mockAlerts);
    expect(component.visitAnalytics).toEqual(mockVisitAnalytics);
  });

  it('should set error flag when API fails', () => {
    adminService.getDashboardSummary.and.returnValue(throwError(() => new Error('fail')));
    adminService.getSystemHealth.and.returnValue(of(mockHealth));
    adminService.getRecentActivity.and.returnValue(of(mockActivity));
    adminService.getSystemMetrics.and.returnValue(of(mockMetrics));
    adminService.getAlerts.and.returnValue(of(mockAlerts));
    adminService.getVisitAnalytics.and.returnValue(of(mockVisitAnalytics));

    fixture = TestBed.createComponent(AdminDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.error).toBeTrue();
    expect(component.loading).toBeFalse();
  });

  it('should show loading state initially', () => {
    adminService.getDashboardSummary.and.returnValue(of(mockSummary));
    adminService.getSystemHealth.and.returnValue(of(mockHealth));
    adminService.getRecentActivity.and.returnValue(of(mockActivity));
    adminService.getSystemMetrics.and.returnValue(of(mockMetrics));
    adminService.getAlerts.and.returnValue(of(mockAlerts));
    adminService.getVisitAnalytics.and.returnValue(of(mockVisitAnalytics));

    fixture = TestBed.createComponent(AdminDashboardComponent);
    component = fixture.componentInstance;

    expect(component.loading).toBeTrue();
  });

  it('should reload dashboard when refresh is called', () => {
    createComponent();
    adminService.getDashboardSummary.calls.reset();
    adminService.getSystemHealth.calls.reset();

    component.loadDashboard();

    expect(adminService.getDashboardSummary).toHaveBeenCalledTimes(1);
    expect(adminService.getSystemHealth).toHaveBeenCalledTimes(1);
  });

  describe('formatUptime', () => {
    beforeEach(() => createComponent());

    it('should format milliseconds to hours and minutes', () => {
      expect(component.formatUptime(3600000)).toBe('1h 0m');
      expect(component.formatUptime(5400000)).toBe('1h 30m');
      expect(component.formatUptime(90000)).toBe('0h 1m');
    });
  });

  describe('formatBytes', () => {
    beforeEach(() => createComponent());

    it('should format bytes to human readable', () => {
      expect(component.formatBytes(0)).toBe('0 B');
      expect(component.formatBytes(1024)).toBe('1 KB');
      expect(component.formatBytes(1048576)).toBe('1 MB');
      expect(component.formatBytes(1073741824)).toBe('1 GB');
    });
  });

  describe('getStatusColor', () => {
    beforeEach(() => createComponent());

    it('should return correct class for UP status', () => {
      expect(component.getStatusColor('UP')).toBe('status-up');
    });

    it('should return correct class for DOWN status', () => {
      expect(component.getStatusColor('DOWN')).toBe('status-down');
    });

    it('should return unknown class for other statuses', () => {
      expect(component.getStatusColor('UNKNOWN')).toBe('status-unknown');
    });
  });

  describe('getGameStatusEntries', () => {
    beforeEach(() => createComponent());

    it('should return entries from summary', () => {
      const entries = component.getGameStatusEntries();
      expect(entries.length).toBe(5);
      expect(entries.find(e => e.key === 'CREATING')?.value).toBe(3);
    });

    it('should return empty array when summary is null', () => {
      component.summary = null;
      expect(component.getGameStatusEntries()).toEqual([]);
    });
  });

  describe('alert helpers', () => {
    beforeEach(() => createComponent());

    it('should map severity classes', () => {
      expect(component.getAlertSeverityClass(mockAlerts[0])).toBe('severity-warning');
    });

    it('should map severity icons', () => {
      expect(component.getAlertSeverityIcon(mockAlerts[0])).toBe('warning');
      expect(component.getAlertSeverityIcon({ ...mockAlerts[0], severity: 'CRITICAL' })).toBe('error');
      expect(component.getAlertSeverityIcon({ ...mockAlerts[0], severity: 'INFO' })).toBe('info');
    });
  });

  describe('formatDuration', () => {
    beforeEach(() => createComponent());

    it('should convert seconds to rounded minutes', () => {
      expect(component.formatDuration(0)).toBe('0m');
      expect(component.formatDuration(30)).toBe('1m');
      expect(component.formatDuration(120)).toBe('2m');
    });
  });

  it('should load visit analytics with bounce and flows', () => {
    createComponent();
    expect(component.visitAnalytics?.bounceRatePercent).toBe(42.5);
    expect(component.visitAnalytics?.topNavigationFlows.length).toBe(1);
  });
});
