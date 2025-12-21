import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';
import { DataSourceStrategy, DataSourceType, DataSourceStatus } from '../../../core/strategies/data-source.strategy';

/**
 * Data Source Indicator Component (FF-201)
 * Displays real-time status of data source (Database or Mock/Fallback)
 *
 * Features:
 * - Visual badge showing current source
 * - Color-coded status (green=DB, yellow=Mock, red=Error)
 * - Tooltip with detailed information
 * - Auto-updates via DataSourceStrategy observable
 */
@Component({
  selector: 'app-data-source-indicator',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './data-source-indicator.html',
  styleUrl: './data-source-indicator.scss'
})
export class DataSourceIndicator implements OnInit, OnDestroy {
  status: DataSourceStatus | null = null;
  DataSourceType = DataSourceType; // Expose enum to template

  private destroy$ = new Subject<void>();

  constructor(private dataSourceStrategy: DataSourceStrategy) {}

  ngOnInit(): void {
    // Subscribe to data source status updates
    this.dataSourceStrategy.currentSource$
      .pipe(takeUntil(this.destroy$))
      .subscribe(status => {
        this.status = status;
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Get CSS class for badge based on current status
   */
  getBadgeClass(): string {
    if (!this.status) return 'badge-unknown';

    switch (this.status.type) {
      case DataSourceType.DATABASE:
        return 'badge-success';
      case DataSourceType.MOCK:
        return 'badge-warning';
      case DataSourceType.INITIALIZING:
        return 'badge-info';
      case DataSourceType.ERROR:
        return 'badge-error';
      default:
        return 'badge-unknown';
    }
  }

  /**
   * Get icon for current status
   */
  getIcon(): string {
    if (!this.status) return '❓';

    switch (this.status.type) {
      case DataSourceType.DATABASE:
        return '✅';
      case DataSourceType.MOCK:
        return '⚠️';
      case DataSourceType.INITIALIZING:
        return '⏳';
      case DataSourceType.ERROR:
        return '❌';
      default:
        return '❓';
    }
  }

  /**
   * Get display text for current status
   */
  getDisplayText(): string {
    if (!this.status) return 'Chargement...';

    switch (this.status.type) {
      case DataSourceType.DATABASE:
        return 'Base de données';
      case DataSourceType.MOCK:
        return 'Mode hors ligne';
      case DataSourceType.INITIALIZING:
        return 'Initialisation...';
      case DataSourceType.ERROR:
        return 'Erreur';
      default:
        return 'Inconnu';
    }
  }
}
