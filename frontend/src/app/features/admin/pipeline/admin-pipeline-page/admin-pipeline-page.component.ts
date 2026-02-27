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
import { Subject, takeUntil, forkJoin } from 'rxjs';
import { PipelineService } from '../../services/pipeline.service';
import { PlayerIdentityEntry, PipelineCount } from '../../models/admin.models';
import { AdminPipelineTableComponent, ResolvedEvent, RejectedEvent } from '../admin-pipeline-table/admin-pipeline-table.component';

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
    AdminPipelineTableComponent
  ],
  templateUrl: './admin-pipeline-page.component.html',
  styleUrls: ['./admin-pipeline-page.component.scss']
})
export class AdminPipelinePageComponent implements OnInit, OnDestroy {
  private readonly pipelineService = inject(PipelineService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroy$ = new Subject<void>();

  loading = signal(true);
  error = signal(false);

  unresolvedEntries: PlayerIdentityEntry[] = [];
  resolvedEntries: PlayerIdentityEntry[] = [];
  count: PipelineCount = { unresolvedCount: 0, resolvedCount: 0 };

  get isInboxZero(): boolean {
    return !this.loading() && this.unresolvedEntries.length === 0;
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
          this.snackBar.open(`✓ Joueur résolu : ${event.epicId}`, 'Fermer', { duration: 3000 });
          this.loadData();
        } else {
          this.snackBar.open('Erreur lors de la résolution', 'Fermer', { duration: 4000 });
        }
      });
  }

  onRejected(event: RejectedEvent): void {
    this.pipelineService
      .rejectPlayer({ playerId: event.playerId, reason: event.reason })
      .pipe(takeUntil(this.destroy$))
      .subscribe(result => {
        if (result) {
          this.snackBar.open('✗ Joueur rejeté', 'Fermer', { duration: 3000 });
          this.loadData();
        } else {
          this.snackBar.open('Erreur lors du rejet', 'Fermer', { duration: 4000 });
        }
      });
  }

  refresh(): void {
    this.loadData();
  }

  private loadData(): void {
    this.loading.set(true);
    this.error.set(false);

    forkJoin({
      unresolved: this.pipelineService.getUnresolved(),
      resolved: this.pipelineService.getResolved(),
      count: this.pipelineService.getCount()
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ unresolved, resolved, count }) => {
          this.unresolvedEntries = unresolved;
          this.resolvedEntries = resolved;
          this.count = count;
          this.loading.set(false);
          this.cdr.markForCheck();
        },
        error: () => {
          this.error.set(true);
          this.loading.set(false);
          this.cdr.markForCheck();
        }
      });
  }
}
