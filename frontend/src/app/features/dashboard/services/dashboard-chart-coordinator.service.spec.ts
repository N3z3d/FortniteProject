import { DashboardChartCoordinatorService } from './dashboard-chart-coordinator.service';
import { DashboardChartService } from './dashboard-chart.service';
import { LoggerService } from '../../../core/services/logger.service';

describe('DashboardChartCoordinatorService', () => {
  let service: DashboardChartCoordinatorService;
  let chartService: jasmine.SpyObj<DashboardChartService>;
  let logger: jasmine.SpyObj<LoggerService>;

  beforeEach(() => {
    chartService = jasmine.createSpyObj<DashboardChartService>('DashboardChartService', [
      'createRegionChart',
      'createPointsChart',
      'updateChart',
      'destroyChart',
      'isValidRegionDistribution',
      'isValidLeaderboardData'
    ]);
    logger = jasmine.createSpyObj<LoggerService>('LoggerService', ['warn']);
    service = new DashboardChartCoordinatorService(chartService, logger);
  });

  it('creates charts when none exist', () => {
    const regionCanvas = document.createElement('canvas');
    const pointsCanvas = document.createElement('canvas');
    const regionChart = {} as any;
    const pointsChart = {} as any;

    chartService.isValidRegionDistribution.and.returnValue(true);
    chartService.isValidLeaderboardData.and.returnValue(true);
    chartService.createRegionChart.and.returnValue(regionChart);
    chartService.createPointsChart.and.returnValue(pointsChart);

    const result = service.updateCharts(
      null,
      null,
      regionCanvas,
      pointsCanvas,
      { EU: 1 },
      [{ teamId: '1', teamName: 'Team 1', ownerName: 'Owner 1', totalPoints: 10, rank: 1, players: [], lastUpdate: '2024-01-01' }],
      () => 'Team 1'
    );

    expect(chartService.createRegionChart).toHaveBeenCalledWith(regionCanvas, { EU: 1 });
    expect(chartService.createPointsChart).toHaveBeenCalled();
    expect(result.regionChart).toBe(regionChart);
    expect(result.pointsChart).toBe(pointsChart);
  });

  it('updates charts when they already exist', () => {
    const regionChart = {} as any;
    const pointsChart = {} as any;

    const result = service.updateCharts(
      regionChart,
      pointsChart,
      null,
      null,
      { EU: 2, NAC: 1 },
      [
        { teamId: '1', teamName: 'Team 1', ownerName: 'Owner 1', totalPoints: 20, rank: 1, players: [], lastUpdate: '2024-01-01' },
        { teamId: '2', teamName: 'Team 2', ownerName: 'Owner 2', totalPoints: 5, rank: 2, players: [], lastUpdate: '2024-01-01' }
      ],
      (entry: any) => `Team ${entry.rank}`
    );

    expect(chartService.updateChart).toHaveBeenCalledTimes(2);
    expect(result.regionChart).toBe(regionChart);
    expect(result.pointsChart).toBe(pointsChart);
  });
});
