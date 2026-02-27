import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, takeUntil } from 'rxjs';

import { AdminService } from '../services/admin.service';
import { TranslationService } from '../../../core/services/translation.service';
import { IncidentEntry, IncidentType } from '../models/admin.models';

@Component({
  selector: 'app-admin-incident-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatTableModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  template: `
    <div class="incident-list-container">
      <mat-card>
        <mat-card-header>
          <mat-card-title>
            <mat-icon>report_problem</mat-icon>
            {{ t.t('admin.incidents.title') }}
          </mat-card-title>
        </mat-card-header>

        <mat-card-content>
          <div class="filters" role="search" [attr.aria-label]="t.t('admin.incidents.filter.label')">
            <mat-form-field appearance="outline">
              <mat-label>{{ t.t('admin.incidents.filter.type') }}</mat-label>
              <mat-select [(ngModel)]="typeFilter" (ngModelChange)="applyFilters()">
                <mat-option [value]="null">{{ t.t('admin.incidents.filter.allTypes') }}</mat-option>
                <mat-option *ngFor="let type of incidentTypes" [value]="type">
                  {{ t.t('admin.incidents.type.' + type.toLowerCase()) }}
                </mat-option>
              </mat-select>
            </mat-form-field>

            <button
              mat-stroked-button
              (click)="clearFilters()"
              [attr.aria-label]="t.t('admin.errors.filter.clear')"
            >
              <mat-icon>clear</mat-icon>
              {{ t.t('admin.errors.filter.clear') }}
            </button>

            <button
              mat-icon-button
              (click)="loadData()"
              [attr.aria-label]="t.t('admin.dashboard.refresh')"
            >
              <mat-icon>refresh</mat-icon>
            </button>
          </div>

          <div *ngIf="loading" class="loading-state" role="status" [attr.aria-label]="t.t('common.loading')">
            <mat-spinner diameter="48"></mat-spinner>
          </div>

          <div *ngIf="error && !loading" class="error-state" role="alert">
            <mat-icon color="warn">error_outline</mat-icon>
            <span>{{ t.t('admin.incidents.loadError') }}</span>
            <button mat-button color="primary" (click)="loadData()">
              {{ t.t('admin.dashboard.refresh') }}
            </button>
          </div>

          <div *ngIf="!loading && !error">
            <p *ngIf="incidents.length === 0" class="empty-state">
              <mat-icon>check_circle_outline</mat-icon>
              {{ t.t('admin.incidents.empty') }}
            </p>

            <table
              *ngIf="incidents.length > 0"
              mat-table
              [dataSource]="incidents"
              class="incident-table"
              [attr.aria-label]="t.t('admin.incidents.title')"
            >
              <ng-container matColumnDef="timestamp">
                <th mat-header-cell *matHeaderCellDef>{{ t.t('admin.errors.table.timestamp') }}</th>
                <td mat-cell *matCellDef="let row">{{ formatTimestamp(row.timestamp) }}</td>
              </ng-container>

              <ng-container matColumnDef="gameName">
                <th mat-header-cell *matHeaderCellDef>{{ t.t('admin.incidents.table.gameName') }}</th>
                <td mat-cell *matCellDef="let row">{{ row.gameName }}</td>
              </ng-container>

              <ng-container matColumnDef="reporterUsername">
                <th mat-header-cell *matHeaderCellDef>{{ t.t('admin.incidents.table.reporter') }}</th>
                <td mat-cell *matCellDef="let row">{{ row.reporterUsername }}</td>
              </ng-container>

              <ng-container matColumnDef="incidentType">
                <th mat-header-cell *matHeaderCellDef>{{ t.t('admin.incidents.table.type') }}</th>
                <td mat-cell *matCellDef="let row">
                  <span [class]="'type-badge type-' + row.incidentType.toLowerCase()">
                    {{ t.t('admin.incidents.type.' + row.incidentType.toLowerCase()) }}
                  </span>
                </td>
              </ng-container>

              <ng-container matColumnDef="description">
                <th mat-header-cell *matHeaderCellDef>{{ t.t('admin.incidents.table.description') }}</th>
                <td
                  mat-cell
                  *matCellDef="let row"
                  [matTooltip]="row.description"
                  matTooltipShowDelay="500"
                >
                  {{ truncate(row.description, 80) }}
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
            </table>
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .incident-list-container { padding: 16px; }
    .filters { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; margin-bottom: 16px; }
    .filters mat-form-field { min-width: 180px; }
    .loading-state, .error-state { display: flex; align-items: center; justify-content: center; gap: 12px; padding: 32px; }
    .empty-state { display: flex; align-items: center; gap: 8px; padding: 24px; color: #9e9e9e; }
    .incident-table { width: 100%; }
    .type-badge { padding: 2px 8px; border-radius: 12px; font-size: 12px; font-weight: 600; text-transform: uppercase; }
    .type-cheating { background: #fce4e4; color: #c62828; }
    .type-abuse { background: #fff3e0; color: #e65100; }
    .type-bug { background: #e8f5e9; color: #2e7d32; }
    .type-dispute { background: #e3f2fd; color: #1565c0; }
    .type-other { background: #f3e5f5; color: #6a1b9a; }
  `]
})
export class AdminIncidentListComponent implements OnInit, OnDestroy {
  public readonly t = inject(TranslationService);
  private readonly adminService = inject(AdminService);
  private readonly destroy$ = new Subject<void>();

  loading = true;
  error = false;

  incidents: IncidentEntry[] = [];

  displayedColumns = ['timestamp', 'gameName', 'reporterUsername', 'incidentType', 'description'];

  typeFilter: IncidentType | null = null;

  readonly incidentTypes: IncidentType[] = ['CHEATING', 'ABUSE', 'BUG', 'DISPUTE', 'OTHER'];

  ngOnInit(): void {
    this.loadData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadData(): void {
    this.loading = true;
    this.error = false;

    this.adminService
      .getIncidents(50)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: data => {
          this.incidents = this.applyTypeFilter(data);
          this.loading = false;
        },
        error: () => {
          this.error = true;
          this.loading = false;
        }
      });
  }

  applyFilters(): void {
    this.loadData();
  }

  clearFilters(): void {
    this.typeFilter = null;
    this.loadData();
  }

  formatTimestamp(ts: string): string {
    if (!ts) return '';
    return new Date(ts).toLocaleString();
  }

  truncate(text: string, maxLength: number): string {
    if (!text || text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
  }

  private applyTypeFilter(data: IncidentEntry[]): IncidentEntry[] {
    if (!this.typeFilter) return data;
    return data.filter(i => i.incidentType === this.typeFilter);
  }
}
