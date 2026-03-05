import {
  Component,
  OnInit,
  inject,
  DestroyRef,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { BehaviorSubject } from 'rxjs';

import { AvailablePlayer } from '../../../draft/models/draft.interface';
import {
  PlayerSearchFilterComponent,
  PlayerFilter,
} from '../../../../shared/components/player-search-filter/player-search-filter.component';
import { PlayerCardComponent } from '../../../../shared/components/player-card/player-card.component';
import { SparklineChartComponent } from '../../../../shared/components/sparkline-chart/sparkline-chart.component';
import { PlayerCatalogueService } from '../../services/player-catalogue.service';

const MAX_COMPARE = 2;
const VIRTUAL_SCROLL_ITEM_SIZE = 80; // browse mode height in px
const FILTER_DEBOUNCE_MS = 200;
const SNACKBAR_DURATION_INFO_MS = 3_000;
const SNACKBAR_DURATION_CONFIRM_MS = 4_000;

@Component({
  selector: 'app-player-catalogue-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ScrollingModule,
    MatSnackBarModule,
    PlayerSearchFilterComponent,
    PlayerCardComponent,
    SparklineChartComponent,
  ],
  templateUrl: './player-catalogue-page.component.html',
  styleUrls: ['./player-catalogue-page.component.scss'],
})
export class PlayerCataloguePageComponent implements OnInit {
  private readonly catalogueService = inject(PlayerCatalogueService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly itemSize = VIRTUAL_SCROLL_ITEM_SIZE;

  allPlayers: AvailablePlayer[] = [];
  filteredPlayers: AvailablePlayer[] = [];
  comparedPlayers: AvailablePlayer[] = [];
  loading = false;
  currentSearchTerm = '';

  availableRegions: string[] = [];
  availableTranches: string[] = [];

  private readonly filter$ = new BehaviorSubject<PlayerFilter>({
    searchTerm: '',
    region: null,
    tranche: null,
    hideUnavailable: true,
    hideTaken: false,
  });

  ngOnInit(): void {
    this.filter$
      .pipe(
        debounceTime(FILTER_DEBOUNCE_MS),
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
        switchMap(filter => {
          this.loading = true;
          this.currentSearchTerm = filter.searchTerm;
          this.cdr.markForCheck();
          return this.catalogueService.getPlayers({
            region: filter.region,
            tranche: filter.tranche,
            search: filter.searchTerm || null,
            available: filter.hideUnavailable ? true : undefined,
          });
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(players => {
        this.allPlayers = players;
        this.filteredPlayers = players;
        this.availableRegions = [...new Set(players.map(p => String(p.region)))];
        this.availableTranches = [...new Set(players.map(p => String(p.tranche)))];
        this.loading = false;
        this.cdr.markForCheck();
      });
  }

  onFilterChanged(filter: PlayerFilter): void {
    this.filter$.next(filter);
  }

  onCardSelected(player: AvailablePlayer): void {
    this.toggleCompare(player);
  }

  toggleCompare(player: AvailablePlayer): void {
    const idx = this.comparedPlayers.findIndex(p => p.id === player.id);
    if (idx >= 0) {
      this.comparedPlayers = this.comparedPlayers.filter(p => p.id !== player.id);
      return;
    }
    if (this.comparedPlayers.length >= MAX_COMPARE) {
      this.snackBar.open(
        `Comparaison limitée à ${MAX_COMPARE} joueurs`,
        'OK',
        { duration: SNACKBAR_DURATION_INFO_MS }
      );
      return;
    }
    this.comparedPlayers = [...this.comparedPlayers, player];
  }

  isCompared(player: AvailablePlayer): boolean {
    return this.comparedPlayers.some(p => p.id === player.id);
  }

  clearCompare(): void {
    this.comparedPlayers = [];
  }

  onReport(player: AvailablePlayer): void {
    this.snackBar.open(`Signalement envoyé pour ${player.username}`, 'Fermer', { duration: SNACKBAR_DURATION_CONFIRM_MS });
  }

  onReportEmpty(): void {
    const pseudo = this.currentSearchTerm;
    this.snackBar.open(
      `Signalement envoyé${pseudo ? ' pour ' + pseudo : ''}`,
      'Fermer',
      { duration: SNACKBAR_DURATION_CONFIRM_MS }
    );
  }

  trackById(_index: number, player: AvailablePlayer): string {
    return player.id;
  }
}
