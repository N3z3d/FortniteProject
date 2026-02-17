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
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { Subject, forkJoin, takeUntil } from 'rxjs';
import { AdminService } from '../services/admin.service';
import { TranslationService } from '../../../core/services/translation.service';
import { ErrorEntry, ErrorStatistics } from '../models/error-journal.models';
import { ErrorDetailDialogComponent } from './error-detail-dialog/error-detail-dialog.component';

@Component({
  selector: 'app-error-journal',
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
    MatTooltipModule,
    MatDialogModule,
    MatPaginatorModule
  ],
  templateUrl: './error-journal.component.html',
  styleUrls: ['./error-journal.component.scss']
})
export class ErrorJournalComponent implements OnInit, OnDestroy {
  public readonly t = inject(TranslationService);
  private readonly adminService = inject(AdminService);
  private readonly dialog = inject(MatDialog);
  private readonly destroy$ = new Subject<void>();

  loading = true;
  error = false;

  errors: ErrorEntry[] = [];
  statistics: ErrorStatistics | null = null;

  displayedColumns = ['timestamp', 'exceptionType', 'statusCode', 'message', 'path', 'actions'];

  statusFilter: number | null = null;
  typeFilter = '';

  pageSize = 50;
  pageSizeOptions = [25, 50, 100];

  statusOptions = [
    { value: null, label: 'All' },
    { value: 400, label: '400' },
    { value: 401, label: '401' },
    { value: 403, label: '403' },
    { value: 404, label: '404' },
    { value: 409, label: '409' },
    { value: 500, label: '500' }
  ];

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

    forkJoin({
      errors: this.adminService.getErrors(
        this.pageSize,
        this.statusFilter ?? undefined,
        this.typeFilter || undefined
      ),
      statistics: this.adminService.getErrorStatistics(24)
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: data => {
          this.errors = data.errors;
          this.statistics = data.statistics;
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
    this.statusFilter = null;
    this.typeFilter = '';
    this.loadData();
  }

  onPageChange(event: PageEvent): void {
    this.pageSize = event.pageSize;
    this.loadData();
  }

  openDetail(entry: ErrorEntry): void {
    this.dialog.open(ErrorDetailDialogComponent, {
      data: entry,
      width: '700px',
      maxHeight: '80vh'
    });
  }

  onErrorRowKeydown(event: KeyboardEvent, entry: ErrorEntry): void {
    if (event.key !== 'Enter' && event.key !== ' ') {
      return;
    }

    event.preventDefault();
    this.openDetail(entry);
  }

  getStatusClass(statusCode: number): string {
    if (statusCode >= 500) return 'status-5xx';
    if (statusCode >= 400) return 'status-4xx';
    return 'status-other';
  }

  getStatusEntries(): { key: string; value: number }[] {
    if (!this.statistics?.errorsByStatusCode) return [];
    return Object.entries(this.statistics.errorsByStatusCode).map(([key, value]) => ({
      key,
      value
    }));
  }

  getTypeEntries(): { key: string; value: number }[] {
    if (!this.statistics?.errorsByType) return [];
    return Object.entries(this.statistics.errorsByType).map(([key, value]) => ({
      key,
      value
    }));
  }

  formatTimestamp(ts: string): string {
    if (!ts) return '';
    const date = new Date(ts);
    return date.toLocaleString();
  }

  truncateMessage(message: string, maxLength: number = 80): string {
    if (!message || message.length <= maxLength) return message;
    return message.substring(0, maxLength) + '...';
  }
}
