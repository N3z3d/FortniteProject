import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  inject,
  OnInit
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { forkJoin } from 'rxjs';
import { AdminService } from '../services/admin.service';
import { AdminAuditEntry, ScrapeLogEntry } from '../models/admin.models';

@Component({
  selector: 'app-admin-logs',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatTabsModule,
    MatChipsModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule
  ],
  templateUrl: './admin-logs.component.html',
  styleUrl: './admin-logs.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdminLogsComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly cdr = inject(ChangeDetectorRef);

  scrapeLogs: ScrapeLogEntry[] = [];
  auditEntries: AdminAuditEntry[] = [];
  loading = false;
  error: string | null = null;

  readonly scrapeColumns = ['source', 'startedAt', 'finishedAt', 'status', 'totalRowsWritten', 'errorMessage'];
  readonly auditColumns = ['timestamp', 'actor', 'action', 'entityType', 'entityId', 'details'];

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.error = null;
    forkJoin({
      scrape: this.adminService.getScrapingLogs(50),
      audit: this.adminService.getAuditLog(50)
    }).subscribe({
      next: ({ scrape, audit }) => {
        this.scrapeLogs = scrape;
        this.auditEntries = audit;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Erreur lors du chargement des logs.';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  scrapeStatusClass(status: string): string {
    switch (status) {
      case 'SUCCESS': return 'status-success';
      case 'PARTIAL': return 'status-partial';
      case 'ERROR': return 'status-error';
      case 'RUNNING': return 'status-running';
      default: return '';
    }
  }
}
