import {
  Component,
  Input,
  OnChanges,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { BaseChartDirective } from 'ng2-charts';
import {
  Chart,
  ChartData,
  ChartOptions,
  LineController,
  LineElement,
  PointElement,
  LinearScale,
  CategoryScale,
} from 'chart.js';

import { ResponsiveService } from '../../../core/services/responsive.service';

// Register only the elements needed for a line sparkline
Chart.register(LineController, LineElement, PointElement, LinearScale, CategoryScale);

export interface RankSnapshot {
  date: string | Date;
  rank: number;
}

type Trend = 'up' | 'down' | 'flat';

const COLOR_UP = 'var(--color-pipeline-ok)';   // green â€” rank improved
const COLOR_DOWN = 'var(--color-trend-down)'; // red â€” rank degraded
const COLOR_FLAT = 'rgba(255,255,255,0.4)';

/**
 * Mini sparkline showing rank evolution over the last N days.
 * Hidden when fewer than 2 snapshots are available or on small screens.
 * Axe Y is reversed: rank 1 appears at the top.
 */
@Component({
  selector: 'app-sparkline-chart',
  standalone: true,
  imports: [CommonModule, BaseChartDirective],
  templateUrl: './sparkline-chart.component.html',
  styleUrls: ['./sparkline-chart.component.scss'],
})
export class SparklineChartComponent implements OnChanges {
  private readonly responsive = inject(ResponsiveService);

  @Input() snapshots: RankSnapshot[] = [];
  @Input() defaultDays = 14;

  filteredSnapshots: RankSnapshot[] = [];
  trend: Trend = 'flat';
  chartData: ChartData<'line'> = { labels: [], datasets: [] };

  readonly chartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      y: {
        reverse: true,
        display: false,
      },
      x: {
        display: false,
      },
    },
    elements: {
      point: { radius: 0, hoverRadius: 0 },
      line: { tension: 0.4, borderWidth: 2 },
    },
    plugins: {
      legend: { display: false },
      tooltip: { enabled: false },
    },
    animation: false,
  };

  get isVisible(): boolean {
    return this.filteredSnapshots.length >= 2 && !this.responsive.hideSparkline();
  }

  get trendColor(): string {
    if (this.trend === 'up') return COLOR_UP;
    if (this.trend === 'down') return COLOR_DOWN;
    return COLOR_FLAT;
  }

  get ariaLabel(): string {
    if (this.trend === 'up') return `Rang en hausse sur ${this.defaultDays} jours`;
    if (this.trend === 'down') return `Rang en baisse sur ${this.defaultDays} jours`;
    return `Rang stable sur ${this.defaultDays} jours`;
  }

  ngOnChanges(): void {
    this.filteredSnapshots = this.computeFiltered();
    this.trend = this.computeTrend();
    this.chartData = this.buildChartData();
  }

  private computeFiltered(): RankSnapshot[] {
    if (this.snapshots.length <= this.defaultDays) return [...this.snapshots];
    return this.snapshots.slice(-this.defaultDays);
  }

  private computeTrend(): Trend {
    if (this.filteredSnapshots.length < 2) return 'flat';
    const first = this.filteredSnapshots[0].rank;
    const last = this.filteredSnapshots[this.filteredSnapshots.length - 1].rank;
    if (last < first) return 'up';
    if (last > first) return 'down';
    return 'flat';
  }

  private buildChartData(): ChartData<'line'> {
    return {
      labels: this.filteredSnapshots.map(s => String(s.date)),
      datasets: [
        {
          data: this.filteredSnapshots.map(s => s.rank),
          borderColor: this.trendColor,
          backgroundColor: 'transparent',
          fill: false,
        },
      ],
    };
  }
}

