import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, forkJoin, takeUntil, timer } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { AdminService } from '../services/admin.service';
import { TranslationService } from '../../../core/services/translation.service';
import {
  AdminAlert,
  DashboardSummary,
  DbTableInfo,
  RealTimeAnalytics,
  RecentActivity,
  SystemHealth,
  SystemMetrics,
  VisitAnalytics
} from '../models/admin.models';

const REAL_TIME_POLLING_INTERVAL_MS = 15_000;
const MILLIS_PER_HOUR = 3_600_000;
const MILLIS_PER_MINUTE = 60_000;
const BYTES_PER_KILOBYTE = 1_024;

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatButtonModule,
    MatTooltipModule
  ],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit, OnDestroy {
  public readonly t = inject(TranslationService);
  private readonly adminService = inject(AdminService);
  private readonly destroy$ = new Subject<void>();

  loading = true;
  error = false;

  summary: DashboardSummary | null = null;
  health: SystemHealth | null = null;
  activity: RecentActivity | null = null;
  metrics: SystemMetrics | null = null;
  alerts: AdminAlert[] = [];
  visitAnalytics: VisitAnalytics | null = null;
  realTimeAnalytics: RealTimeAnalytics | null = null;
  dbTables: DbTableInfo[] = [];
  dbTablesLoading = true;

  ngOnInit(): void {
    this.loadDashboard();
    this.startRealTimePolling();
    this.loadDatabaseTables();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadDatabaseTables(): void {
    this.adminService
      .getDatabaseTables()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (tables) => {
          this.dbTables = tables;
          this.dbTablesLoading = false;
        },
        error: () => {
          this.dbTablesLoading = false;
        }
      });
  }

  private startRealTimePolling(): void {
    timer(0, REAL_TIME_POLLING_INTERVAL_MS)
      .pipe(
        switchMap(() => this.adminService.getRealTimeAnalytics()),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (data) => { this.realTimeAnalytics = data; },
        error: () => {} // supplementary data — silent failure
      });
  }

  loadDashboard(): void {
    this.loading = true;
    this.error = false;

    forkJoin({
      summary: this.adminService.getDashboardSummary(),
      health: this.adminService.getSystemHealth(),
      activity: this.adminService.getRecentActivity(),
      metrics: this.adminService.getSystemMetrics(),
      alerts: this.adminService.getAlerts(),
      visitAnalytics: this.adminService.getVisitAnalytics()
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.summary = data.summary;
          this.health = data.health;
          this.activity = data.activity;
          this.metrics = data.metrics;
          this.alerts = data.alerts;
          this.visitAnalytics = data.visitAnalytics;
          this.loading = false;
        },
        error: () => {
          this.error = true;
          this.loading = false;
        }
      });
  }

  formatUptime(millis: number): string {
    const hours = Math.floor(millis / MILLIS_PER_HOUR);
    const minutes = Math.floor((millis % MILLIS_PER_HOUR) / MILLIS_PER_MINUTE);
    return `${hours}h ${minutes}m`;
  }

  formatBytes(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = BYTES_PER_KILOBYTE;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'UP': return 'status-up';
      case 'DOWN': return 'status-down';
      default: return 'status-unknown';
    }
  }

  getGameStatusEntries(): { key: string; value: number }[] {
    if (!this.summary?.gamesByStatus) return [];
    return Object.entries(this.summary.gamesByStatus).map(([key, value]) => ({ key, value }));
  }

  getAlertSeverityClass(alert: AdminAlert): string {
    return `severity-${alert.severity.toLowerCase()}`;
  }

  getAlertSeverityIcon(alert: AdminAlert): string {
    if (alert.severity === 'CRITICAL') {
      return 'error';
    }
    if (alert.severity === 'WARNING') {
      return 'warning';
    }
    return 'info';
  }

  formatDuration(seconds: number): string {
    if (seconds <= 0) {
      return '0m';
    }
    const minutes = Math.round(seconds / 60);
    return `${minutes}m`;
  }
}
