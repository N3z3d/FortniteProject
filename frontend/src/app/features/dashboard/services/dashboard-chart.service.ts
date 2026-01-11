import { Injectable } from '@angular/core';
import { Chart, ChartConfiguration } from 'chart.js';
import { LoggerService } from '../../../core/services/logger.service';

/**
 * Dashboard Chart Service (SRP - Single Responsibility Principle)
 * Responsible ONLY for creating and updating Chart.js charts
 * Extracted from DashboardComponent to reduce complexity
 *
 * Max lines: ~150 (well under 500-line limit)
 */
@Injectable({
  providedIn: 'root'
})
export class DashboardChartService {
  private readonly ALL_REGIONS: string[] = ['EU', 'NAC', 'NAW', 'BR', 'ASIA', 'OCE', 'ME'];

  private readonly REGION_COLORS: { [key: string]: string } = {
    'EU': '#3b82f6',      // Blue
    'NAC': '#10b981',     // Green
    'NAW': '#f59e0b',     // Orange
    'BR': '#ef4444',      // Red
    'ASIA': '#8b5cf6',    // Purple
    'OCE': '#06b6d4',     // Cyan
    'ME': '#ec4899'       // Pink
  };

  constructor(private logger: LoggerService) {
    this.logger.debug('📊 DashboardChartService initialized');
  }

  /**
   * Create region distribution doughnut chart
   * @param canvas - HTMLCanvasElement reference
   * @param regionDistribution - Data { EU: 10, NAC: 5, ... }
   * @returns Chart instance
   */
  /**
   * Create region distribution doughnut chart (Premium Redesign)
   */
  createRegionChart(canvas: HTMLCanvasElement, regionDistribution: { [key: string]: number }): Chart {
    const labels = this.ALL_REGIONS;
    const data = labels.map(region => regionDistribution[region] || 0);

    // Premium Palette for Regions
    const regionColors = [
      '#FFD700', // Gold (EU)
      '#00d4ff', // Cyan (NAC)
      '#ff0055', // Neon Red (NAW)
      '#b388ff', // Purple (BR)
      '#00ff9d', // Green (ASIA)
      '#ff9100', // Orange (OCE)
      '#ffffff'  // White (ME)
    ];

    const config: ChartConfiguration<'doughnut'> = {
      type: 'doughnut',
      data: {
        labels: labels,
        datasets: [{
          data: data,
          backgroundColor: regionColors,
          borderWidth: 0,
          hoverOffset: 10
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '70%', // Thinner ring
        plugins: {
          legend: {
            position: 'right',
            labels: {
              padding: 20,
              usePointStyle: true,
              pointStyle: 'circle',
              font: {
                size: 11,
                family: "'Orbitron', sans-serif"
              },
              color: '#e2e8f0' // Light gray
            }
          },
          tooltip: {
            backgroundColor: 'rgba(17, 24, 39, 0.95)',
            titleColor: '#FFD700',
            bodyColor: '#fff',
            borderColor: 'rgba(255, 215, 0, 0.3)',
            borderWidth: 1,
            padding: 12,
            titleFont: { family: "'Orbitron', sans-serif", size: 13 },
            bodyFont: { family: "'Exo 2', sans-serif", size: 12 },
            cornerRadius: 8,
            callbacks: {
              label: (context) => {
                const label = context.label || '';
                const value = context.parsed || 0;
                const total = context.dataset.data.reduce((a: number, b: number) => a + b, 0);
                const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : '0.0';
                return ` ${label}: ${value} (${percentage}%)`;
              }
            }
          }
        }
      }
    };

    return new Chart(canvas, config);
  }

  /**
   * Create top teams points bar chart (Premium Redesign)
   */
  createPointsChart(canvas: HTMLCanvasElement, leaderboard: any[], limit: number = 10): Chart {
    const topTeams = leaderboard.slice(0, limit);
    const labels = topTeams.map(entry => {
      let name = entry.teamName || entry.ownerName || 'Unknown';
      return name.replace('Equipe de ', '');
    });
    const data = topTeams.map(entry => entry.totalPoints || 0);

    const config: ChartConfiguration<'bar'> = {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [{
          label: 'Points',
          data: data,
          backgroundColor: (context) => {
            const index = context.dataIndex;
            // Top 3 get special metallic gradients (simulated with solid colors for now)
            if (index === 0) return '#FFD700'; // Gold
            if (index === 1) return '#C0C0C0'; // Silver
            if (index === 2) return '#CD7F32'; // Bronze
            return 'rgba(255, 255, 255, 0.2)'; // Others ghost white
          },
          borderColor: 'transparent',
          borderRadius: 4,
          borderSkipped: false,
          barPercentage: 0.6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        indexAxis: 'y',
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: 'rgba(17, 24, 39, 0.95)',
            titleColor: '#FFD700',
            bodyColor: '#fff',
            borderColor: 'rgba(255, 215, 0, 0.3)',
            borderWidth: 1,
            padding: 12,
            titleFont: { family: "'Orbitron', sans-serif", size: 13 },
            callbacks: {
              label: (ctx) => ` ${ctx.formattedValue} pts`
            }
          }
        },
        scales: {
          x: {
            grid: { color: 'rgba(255, 255, 255, 0.05)' },
            ticks: {
              color: '#94a3b8',
              font: { family: "'Exo 2', sans-serif", size: 10 }
            }
          },
          y: {
            grid: { display: false },
            ticks: {
              color: '#fff',
              font: { family: "'Orbitron', sans-serif", size: 11 },
              autoSkip: false
            }
          }
        }
      }
    };

    return new Chart(canvas, config);
  }

  /**
   * Update existing chart with new data
   * @param chart - Chart instance to update
   * @param newData - New data values
   * @param newLabels - Optional new labels
   */
  updateChart(chart: Chart, newData: number[], newLabels?: string[]): void {
    if (!chart || !chart.data || !chart.data.datasets[0]) {
      this.logger.warn('⚠️ Cannot update chart: invalid chart instance');
      return;
    }

    chart.data.datasets[0].data = newData;

    if (newLabels && chart.data.labels) {
      chart.data.labels = newLabels;
    }

    chart.update('active');
  }

  /**
   * Destroy chart instance (cleanup)
   * @param chart - Chart instance to destroy
   */
  destroyChart(chart: Chart | null): void {
    if (chart) {
      chart.destroy();
      this.logger.debug('📊 Chart destroyed');
    }
  }

  /**
   * Validate region distribution data
   * @param distribution - Region distribution object
   * @returns boolean
   */
  isValidRegionDistribution(distribution: { [key: string]: number }): boolean {
    if (!distribution || typeof distribution !== 'object') {
      return false;
    }

    // Check if at least one region has data
    return Object.values(distribution).some(value => value > 0);
  }

  /**
   * Validate leaderboard data for points chart
   * @param leaderboard - Leaderboard array
   * @returns boolean
   */
  isValidLeaderboardData(leaderboard: any[]): boolean {
    return Array.isArray(leaderboard) && leaderboard.length > 0;
  }

  /**
   * Check if region distribution has any data
   * Alias for isValidRegionDistribution for backwards compatibility
   * @param distribution - Region distribution object
   * @returns boolean - true if at least one region has data > 0
   */
  hasRegionData(distribution: { [key: string]: number }): boolean {
    return this.isValidRegionDistribution(distribution);
  }

  /**
   * Check if leaderboard has valid points data
   * @param entries - Leaderboard entries
   * @returns boolean - true if at least one entry has points > 0
   */
  hasPointsData(entries: any[]): boolean {
    if (!Array.isArray(entries) || entries.length === 0) {
      return false;
    }
    return entries.some(entry => (entry.totalPoints || 0) > 0);
  }

  /**
   * Get normalized region chart data
   * @param distribution - Region distribution object (can have string or number values)
   * @returns Object with labels and data arrays
   */
  getRegionChartData(distribution: { [key: string]: number | string }): { labels: string[]; data: number[] } {
    const labels = this.ALL_REGIONS;
    const data = labels.map(region => {
      const value = distribution[region];
      return typeof value === 'string' ? Number.parseInt(value, 10) || 0 : (value || 0);
    });

    return { labels, data };
  }

  /**
   * Get sorted and cleaned points chart data
   * @param entries - Leaderboard entries
   * @param limit - Maximum number of entries to return
   * @returns Object with labels and data arrays, sorted by points descending
   */
  getPointsChartData(entries: any[], limit: number = 10): { labels: string[]; data: number[] } {
    // Filter out entries with 0 points and sort by totalPoints descending
    const validEntries = entries
      .filter(entry => (entry.totalPoints || 0) > 0)
      .sort((a, b) => (b.totalPoints || 0) - (a.totalPoints || 0))
      .slice(0, limit);

    const labels = validEntries.map(entry => {
      // Clean up team name: remove "Equipe de " prefix
      let name = entry.teamName || entry.ownerName || 'Unknown';
      if (name.startsWith('Equipe de ')) {
        name = name.replace('Equipe de ', '');
      }
      return name;
    });

    const data = validEntries.map(entry => entry.totalPoints || 0);

    return { labels, data };
  }
}
