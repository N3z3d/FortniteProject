import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { DashboardFacade } from './dashboard.facade';
import {
  HttpDashboardRepository,
  MockDashboardRepository,
  DashboardData,
  DashboardStats
} from '../repositories/dashboard.repository';
import {
  DataSourceStrategy,
  DataSourceStatus,
  DataSourceType
} from '../strategies/data-source.strategy';
import { environment } from '../../../environments/environment';

describe('DashboardFacade', () => {
  let facade: DashboardFacade;
  let httpRepo: jasmine.SpyObj<HttpDashboardRepository>;
  let mockRepo: jasmine.SpyObj<MockDashboardRepository>;
  let strategy: jasmine.SpyObj<DataSourceStrategy>;

  beforeEach(() => {
    const status: DataSourceStatus = {
      type: DataSourceType.MOCK,
      isAvailable: false,
      lastChecked: new Date(0),
      message: 'mock'
    };

    httpRepo = jasmine.createSpyObj('HttpDashboardRepository', [
      'getDashboardData',
      'getStatistics',
      'getLeaderboard',
      'getRegionDistribution'
    ]);
    mockRepo = jasmine.createSpyObj('MockDashboardRepository', [
      'getDashboardData',
      'getStatistics',
      'getLeaderboard',
      'getRegionDistribution'
    ]);
    strategy = jasmine.createSpyObj('DataSourceStrategy', ['executeWithFallback', 'isDatabaseAvailable'], {
      currentSource$: of(status)
    });

    TestBed.configureTestingModule({
      providers: [
        DashboardFacade,
        { provide: HttpDashboardRepository, useValue: httpRepo },
        { provide: MockDashboardRepository, useValue: mockRepo },
        { provide: DataSourceStrategy, useValue: strategy }
      ]
    });

    facade = TestBed.inject(DashboardFacade);
  });

  it('should delegate getDashboardData through strategy', (done) => {
    const primary$ = of({ statistics: {} as DashboardStats, leaderboard: [], regionDistribution: {}, teams: [] } as DashboardData);
    const fallback$ = of({ statistics: {} as DashboardStats, leaderboard: [], regionDistribution: {}, teams: [] } as DashboardData);
    const result = { statistics: {} as DashboardStats, leaderboard: [], regionDistribution: {}, teams: [] } as DashboardData;

    httpRepo.getDashboardData.and.returnValue(primary$);
    mockRepo.getDashboardData.and.returnValue(fallback$);
    strategy.executeWithFallback.and.returnValue(of(result));

    facade.getDashboardData('game-1').subscribe(data => {
      expect(data).toBe(result);
      done();
    });

    expect(httpRepo.getDashboardData).toHaveBeenCalledWith('game-1');
    expect(mockRepo.getDashboardData).toHaveBeenCalledWith('game-1');
    expect(strategy.executeWithFallback).toHaveBeenCalledWith(
      primary$,
      fallback$,
      'Dashboard Data',
      jasmine.objectContaining({ allowFallback: environment.enableFallbackData })
    );
  });

  it('should delegate getStatistics through strategy', (done) => {
    const primary$ = of({ totalTeams: 1 } as DashboardStats);
    const fallback$ = of({ totalTeams: 0 } as DashboardStats);

    httpRepo.getStatistics.and.returnValue(primary$);
    mockRepo.getStatistics.and.returnValue(fallback$);
    strategy.executeWithFallback.and.returnValue(of({ totalTeams: 1 } as DashboardStats));

    facade.getStatistics('game-1').subscribe(result => {
      expect(result.totalTeams).toBe(1);
      done();
    });

    expect(strategy.executeWithFallback).toHaveBeenCalledWith(
      primary$,
      fallback$,
      'Dashboard Statistics',
      jasmine.objectContaining({ allowFallback: environment.enableFallbackData })
    );
  });

  it('should delegate getLeaderboard through strategy', (done) => {
    const primary$ = of([{ teamId: 't1' }]);
    const fallback$ = of([{ teamId: 't2' }]);

    httpRepo.getLeaderboard.and.returnValue(primary$);
    mockRepo.getLeaderboard.and.returnValue(fallback$);
    strategy.executeWithFallback.and.returnValue(of([{ teamId: 't1' }]));

    facade.getLeaderboard('game-1').subscribe(result => {
      expect(result.length).toBe(1);
      done();
    });

    expect(strategy.executeWithFallback).toHaveBeenCalledWith(
      primary$,
      fallback$,
      'Dashboard Leaderboard',
      jasmine.objectContaining({ allowFallback: environment.enableFallbackData })
    );
  });

  it('should delegate getRegionDistribution through strategy', (done) => {
    const primary$ = of({ EU: 1 });
    const fallback$ = of({ EU: 0 });

    httpRepo.getRegionDistribution.and.returnValue(primary$);
    mockRepo.getRegionDistribution.and.returnValue(fallback$);
    strategy.executeWithFallback.and.returnValue(of({ EU: 1 }));

    facade.getRegionDistribution('game-1').subscribe(result => {
      expect(result['EU']).toBe(1);
      done();
    });

    expect(strategy.executeWithFallback).toHaveBeenCalledWith(
      primary$,
      fallback$,
      'Region Distribution',
      jasmine.objectContaining({ allowFallback: environment.enableFallbackData })
    );
  });

  it('should proxy database availability', () => {
    strategy.isDatabaseAvailable.and.returnValue(true);

    expect(facade.isDatabaseAvailable()).toBeTrue();
    expect(strategy.isDatabaseAvailable).toHaveBeenCalled();
  });

  it('should expose data source status observable', () => {
    expect(facade.getDataSourceStatus$()).toBe(strategy.currentSource$);
  });
});
