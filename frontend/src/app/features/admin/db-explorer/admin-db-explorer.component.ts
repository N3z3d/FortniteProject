import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { Subject, takeUntil } from 'rxjs';

import { DbTableInfo, SqlQueryResult } from '../models/admin.models';
import { AdminService } from '../services/admin.service';

@Component({
  selector: 'app-admin-db-explorer',
  standalone: true,
  imports: [CommonModule, MatTableModule, MatProgressSpinnerModule, MatButtonModule, FormsModule],
  templateUrl: './admin-db-explorer.component.html',
  styleUrl: './admin-db-explorer.component.scss'
})
export class AdminDbExplorerComponent implements OnInit, OnDestroy {
  private readonly adminService = inject(AdminService);
  private readonly destroy$ = new Subject<void>();

  readonly loading = signal(false);
  readonly tables = signal<DbTableInfo[]>([]);
  loadError = false;

  sqlQuery = '';
  readonly queryLoading = signal(false);
  readonly queryResult = signal<SqlQueryResult | null>(null);
  queryError: string | null = null;

  readonly displayedColumns = ['tableName', 'entityName', 'rowCount', 'sizeDescription'];

  ngOnInit(): void {
    this.loadTables();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadTables(): void {
    this.loading.set(true);
    this.loadError = false;
    this.adminService
      .getDatabaseTables()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: data => {
          this.tables.set(data);
          this.loading.set(false);
        },
        error: () => {
          this.loadError = true;
          this.loading.set(false);
        }
      });
  }

  executeQuery(): void {
    if (!this.sqlQuery.trim()) {
      return;
    }
    this.queryLoading.set(true);
    this.queryResult.set(null);
    this.queryError = null;
    this.adminService
      .executeSqlQuery(this.sqlQuery)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: result => {
          this.queryResult.set(result);
          this.queryLoading.set(false);
        },
        error: err => {
          this.queryError =
            err?.error?.message ?? err?.message ?? 'Erreur lors de l\'exécution de la requête.';
          this.queryLoading.set(false);
        }
      });
  }

  getCellValue(row: Record<string, unknown>, col: string): string {
    const val = row[col];
    if (val === null || val === undefined) return '—';
    return String(val);
  }
}
