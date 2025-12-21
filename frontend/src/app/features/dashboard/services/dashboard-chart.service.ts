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
  createRegionChart(canvas: HTMLCanvasElement, regionDistribution: { [key: string]: number }): Chart {
    const labels = this.ALL_REGIONS;
    const data = labels.map(region => regionDistribution[region] || 0);
    const colors = labels.map(region => this.REGION_COLORS[region] || '#6b7280');

    const config: ChartConfiguration<'doughnut'> = {
      type: 'doughnut',
      data: {
        labels: labels,
        datasets: [{
          data: data,
          backgroundColor: colors,
          borderWidth: 2,
          borderColor: '#ffffff'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              padding: 15,
              font: {
                size: 12,
                family: "'Inter', sans-serif"
              },
              color: '#6b7280'
            }
          },
          tooltip: {
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            padding: 12,
            titleFont: {
              size: 14,
              weight: 'bold'
            },
            bodyFont: {
              size: 13
            },
            callbacks: {
              label: (context) => {
                const label = context.label || '';
                const value = context.parsed || 0;
                const total = context.dataset.data.reduce((a: number, b: number) => a + b, 0);
                const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : '0.0';
                return `${label}: ${value} joueurs (${percentage}%)`;
              }
            }
          }
        }
      }
    };

    return new Chart(canvas, config);
  }

  /**
   * Create top teams points bar chart
   * @param canvas - HTMLCanvasElement reference
   * @param leaderboard - Array of leaderboard entries
   * @param limit - Number of teams to show (default: 10)
   * @returns Chart instance
   */
  createPointsChart(canvas: HTMLCanvasElement, leaderboard: any[], limit: number = 10): Chart {
    const topTeams = leaderboard.slice(0, limit);
    const labels = topTeams.map(entry => entry.teamName || entry.ownerName || 'Unknown');
    const data = topTeams.map(entry => entry.totalPoints || 0);

    const config: ChartConfiguration<'bar'> = {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [{
          label: 'Points totaux',
          data: data,
          backgroundColor: 'rgba(59, 130, 246, 0.8)',
          borderColor: 'rgba(59, 130, 246, 1)',
          borderWidth: 2,
          borderRadius: 6,
          borderSkipped: false
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        indexAxis: 'y', // Horizontal bar chart
        plugins: {
          legend: {
            display: false
          },
          tooltip: {
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            padding: 12,
            titleFont: {
              size: 14,
              weight: 'bold'
            },
            bodyFont: {
              size: 13
            },
            callbacks: {
              label: (context) => {
                return `${context.parsed.x} points`;
              }
            }
          }
        },
        scales: {
          x: {
            beginAtZero: true,
            grid: {
              color: 'rgba(0, 0, 0, 0.05)'
            },
            ticks: {
              font: {
                size: 11,
                family: "'Inter', sans-serif"
              },
              color: '#6b7280'
            }
          },
          y: {
            grid: {
              display: false
            },
            ticks: {
              font: {
                size: 11,
                family: "'Inter', sans-serif"
              },
              color: '#374151',
              callback: function(value, index) {
                const label = this.getLabelForValue(Number(value));
                return label.length > 20 ? label.substring(0, 20) + '...' : label;
              }
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
}
