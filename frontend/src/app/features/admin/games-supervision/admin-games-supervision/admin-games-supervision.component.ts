import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { Subject, takeUntil } from 'rxjs';

import {
  GameSupervisionEntry,
  GameSupervisionStatus
} from '../../models/admin.models';
import { AdminService } from '../../services/admin.service';

type StatusFilter = GameSupervisionStatus | 'ALL';

@Component({
  selector: 'app-admin-games-supervision',
  standalone: true,
  imports: [CommonModule, DatePipe, MatTableModule, MatTabsModule, MatProgressSpinnerModule],
  templateUrl: './admin-games-supervision.component.html',
  styleUrl: './admin-games-supervision.component.scss'
})
export class AdminGamesSupervisionComponent implements OnInit, OnDestroy {
  private readonly adminService = inject(AdminService);
  private readonly destroy$ = new Subject<void>();

  readonly loading = signal(false);
  readonly allGames = signal<GameSupervisionEntry[]>([]);
  readonly selectedStatus = signal<StatusFilter>('ALL');
  loadError = false;

  readonly filteredGames = computed(() => {
    const status = this.selectedStatus();
    return status === 'ALL'
      ? this.allGames()
      : this.allGames().filter(g => g.status === status);
  });

  readonly displayedColumns = ['name', 'status', 'participants', 'creator', 'createdAt'];
  readonly statusFilters: StatusFilter[] = ['ALL', 'CREATING', 'DRAFTING', 'ACTIVE'];

  ngOnInit(): void {
    this.loadGames();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  protected loadGames(): void {
    this.loading.set(true);
    this.loadError = false;
    this.adminService
      .getGamesSupervision()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: games => {
          this.allGames.set(games);
          this.loading.set(false);
        },
        error: () => {
          this.loadError = true;
          this.loading.set(false);
        }
      });
  }

  selectStatus(status: StatusFilter): void {
    this.selectedStatus.set(status);
  }

  protected statusClass(status: GameSupervisionStatus): string {
    return `status-badge--${status.toLowerCase()}`;
  }
}
