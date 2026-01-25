import { Injectable } from '@angular/core';
import { Chart } from 'chart.js';
import { LeaderboardEntryDTO } from '../../../core/models/leaderboard.model';
import { LoggerService } from '../../../core/services/logger.service';
import { DashboardChartService } from './dashboard-chart.service';

export interface DashboardChartsResult {
  regionChart: Chart | null;
  pointsChart: Chart | null;
}

@Injectable({
  providedIn: 'root'
})
export class DashboardChartCoordinatorService {
  constructor(
    private chartService: DashboardChartService,
    private logger: LoggerService
  ) {}

  updateCharts(
    regionChart: Chart | null,
    pointsChart: Chart | null,
    regionCanvas: HTMLCanvasElement | null,
    pointsCanvas: HTMLCanvasElement | null,
    regionDistribution: Record<string, number> | undefined,
    leaderboardEntries: LeaderboardEntryDTO[],
    teamLabelBuilder: (entry: LeaderboardEntryDTO) => string
  ): DashboardChartsResult {
    let nextRegionChart = regionChart;
    let nextPointsChart = pointsChart;

    if (!regionDistribution || !leaderboardEntries?.length) {
      return { regionChart: nextRegionChart, pointsChart: nextPointsChart };
    }

    if (nextRegionChart) {
      this.updateRegionChartData(nextRegionChart, regionDistribution);
    } else if (regionCanvas) {
      nextRegionChart = this.createRegionChart(regionCanvas, regionDistribution);
    }

    if (nextPointsChart) {
      this.updatePointsChartData(nextPointsChart, leaderboardEntries, teamLabelBuilder);
    } else if (pointsCanvas) {
      nextPointsChart = this.createPointsChart(pointsCanvas, leaderboardEntries);
    }

    return { regionChart: nextRegionChart, pointsChart: nextPointsChart };
  }

  destroyCharts(regionChart: Chart | null, pointsChart: Chart | null): void {
    this.chartService.destroyChart(regionChart);
    this.chartService.destroyChart(pointsChart);
  }

  private createRegionChart(
    canvas: HTMLCanvasElement,
    regionDistribution: Record<string, number>
  ): Chart | null {
    if (!this.chartService.isValidRegionDistribution(regionDistribution)) {
      this.logger.warn('Invalid region distribution data, skipping chart creation');
      return null;
    }

    return this.chartService.createRegionChart(canvas, regionDistribution);
  }

  private createPointsChart(canvas: HTMLCanvasElement, leaderboardEntries: LeaderboardEntryDTO[]): Chart | null {
    if (!this.chartService.isValidLeaderboardData(leaderboardEntries)) {
      this.logger.warn('Invalid leaderboard data, skipping chart creation');
      return null;
    }

    return this.chartService.createPointsChart(canvas, leaderboardEntries, 10);
  }

  private updateRegionChartData(chart: Chart, regions: Record<string, number>): void {
    const data = Object.values(regions);
    const labels = Object.keys(regions);

    this.chartService.updateChart(chart, data, labels);
  }

  private updatePointsChartData(
    chart: Chart,
    leaderboardEntries: LeaderboardEntryDTO[],
    teamLabelBuilder: (entry: LeaderboardEntryDTO) => string
  ): void {
    const topTeams = leaderboardEntries.slice(0, 10);
    const labels = topTeams.map(teamLabelBuilder);
    const data = topTeams.map(entry => entry.totalPoints);

    this.chartService.updateChart(chart, data, labels);
  }
}
