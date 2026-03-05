import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatBadgeModule } from '@angular/material/badge';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Subject, takeUntil, forkJoin, catchError, of } from 'rxjs';
import { PipelineService } from '../../services/pipeline.service';
import {
  PlayerIdentityEntry,
  PipelineCount,
  PipelineRegionalStats,
  CorrectMetadataRequest,
  ScrapeLogEntry,
  PipelineAlertStatus,
  AlertLevel
} from '../../models/admin.models';
import {
  AdminPipelineTableComponent,
  ResolvedEvent,
  RejectedEvent
} from '../admin-pipeline-table/admin-pipeline-table.component';
import {
  CorrectMetadataDialogComponent,
  CorrectMetadataDialogData
} from '../correct-metadata-dialog/correct-metadata-dialog.component';

const SNACKBAR_DURATION_SUCCESS_MS = 3_000;
const SNACKBAR_DURATION_ERROR_MS = 4_000;
const CORRECT_METADATA_DIALOG_WIDTH = '480px';

@Component({
  selector: 'app-admin-pipeline-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatTabsModule,
    MatSnackBarModule,
    MatBadgeModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    AdminPipelineTableComponent
  ],
  templateUrl: './admin-pipeline-page.component.html',
  styleUrls: ['./admin-pipeline-page.component.scss']
})
export class AdminPipelinePageComponent implements OnInit, OnDestroy {
  private readonly pipelineService = inject(PipelineService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroy$ = new Subject<void>();

  loading = signal(true);
  dataLoadError = false;

  unresolvedEntries: PlayerIdentityEntry[] = [];
  resolvedEntries: PlayerIdentityEntry[] = [];
  count: PipelineCount = { unresolvedCount: 0, resolvedCount: 0 };
  regionalStats: PipelineRegionalStats[] = [];
  regionalError = false;
  scrapeLog: ScrapeLogEntry[] = [];
  pipelineAlert: PipelineAlertStatus | null = null;
  alertError = false;

  get isInboxZero(): boolean {
    return !this.loading() && this.unresolvedEntries.length === 0;
  }

  protected alertLabel(level: AlertLevel): string {
    return level === 'CRITICAL' ? '🔴 CRITIQUE' : '⚠️ ATTENTION';
  }

  ngOnInit(): void {
    this.loadData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onResolved(event: ResolvedEvent): void {
    this.pipelineService
      .resolvePlayer({ playerId: event.playerId, epicId: event.epicId })
      .pipe(takeUntil(this.destroy$))
      .subscribe(result => {
        if (result) {
          this.snackBar.open(`✓ Joueur résolu : ${event.epicId}`, 'Fermer', { duration: SNACKBAR_DURATION_SUCCESS_MS });
          this.loadData();
        } else {
          this.snackBar.open('Erreur lors de la résolution', 'Fermer', { duration: SNACKBAR_DURATION_ERROR_MS });
        }
      });
  }

  onRejected(event: RejectedEvent): void {
    this.pipelineService
      .rejectPlayer({ playerId: event.playerId, reason: event.reason })
      .pipe(takeUntil(this.destroy$))
      .subscribe(result => {
        if (result) {
          this.snackBar.open('✗ Joueur rejeté', 'Fermer', { duration: SNACKBAR_DURATION_SUCCESS_MS });
          this.loadData();
        } else {
          this.snackBar.open('Erreur lors du rejet', 'Fermer', { duration: SNACKBAR_DURATION_ERROR_MS });
        }
      });
  }

  onCorrectRequested(entry: PlayerIdentityEntry): void {
    const ref = this.dialog.open(CorrectMetadataDialogComponent, {
      data: { entry } as CorrectMetadataDialogData,
      width: CORRECT_METADATA_DIALOG_WIDTH
    });
    ref
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((result: CorrectMetadataRequest | undefined) => {
        if (!result) return;
        this.pipelineService
          .correctMetadata(entry.playerId, result)
          .pipe(takeUntil(this.destroy$))
          .subscribe(updated => {
            if (updated) {
              this.snackBar.open('✏ Fiche joueur corrigée', 'Fermer', { duration: SNACKBAR_DURATION_SUCCESS_MS });
              this.loadData();
            } else {
              this.snackBar.open('Erreur lors de la correction', 'Fermer', { duration: SNACKBAR_DURATION_ERROR_MS });
            }
          });
      });
  }

  refresh(): void {
    this.loadData();
  }

  private loadData(): void {
    this.loading.set(true);
    this.dataLoadError = false;

    this.alertError = false;

    forkJoin({
      unresolved: this.pipelineService
        .getUnresolved()
        .pipe(catchError(() => { this.dataLoadError = true; return of([] as PlayerIdentityEntry[]); })),
      resolved: this.pipelineService
        .getResolved()
        .pipe(catchError(() => { this.dataLoadError = true; return of([] as PlayerIdentityEntry[]); })),
      count: this.pipelineService
        .getCount()
        .pipe(catchError(() => { this.dataLoadError = true; return of({ unresolvedCount: 0, resolvedCount: 0 } as PipelineCount); })),
      scrapeLog: this.pipelineService.getScrapeLog().pipe(catchError(() => of([] as ScrapeLogEntry[]))),
      pipelineAlert: this.pipelineService
        .getUnresolvedAlertStatus()
        .pipe(catchError(() => { this.alertError = true; return of(null as PipelineAlertStatus | null); }))
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe(({ unresolved, resolved, count, scrapeLog, pipelineAlert }) => {
        this.unresolvedEntries = unresolved;
        this.resolvedEntries = resolved;
        this.count = count;
        this.scrapeLog = scrapeLog;
        this.pipelineAlert = pipelineAlert;
        this.loading.set(false);
        this.cdr.markForCheck();
      });

    this.pipelineService
      .getRegionalStatus()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: stats => {
          this.regionalStats = stats;
          this.regionalError = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.regionalError = true;
          this.cdr.markForCheck();
        }
      });
  }
}
