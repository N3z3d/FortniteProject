import { Component, OnInit, OnDestroy, inject, DestroyRef } from '@angular/core';
import { ComponentWithDraftState } from '../../../../core/guards/draft-active.guard';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { DraftService } from '../../services/draft.service';
import { SimultaneousDraftService } from '../../services/simultaneous-draft.service';
import {
  WebSocketService,
  SimultaneousEventMessage,
} from '../../../../core/services/websocket.service';
import { UserContextService } from '../../../../core/services/user-context.service';
import { DraftTimerComponent } from '../draft-timer/draft-timer.component';
import { PlayerCardComponent } from '../../../../shared/components/player-card/player-card.component';
import {
  PlayerSearchFilterComponent,
  PlayerFilter,
} from '../../../../shared/components/player-search-filter/player-search-filter.component';
import {
  CoinFlipAnimationComponent,
  CoinFlipData,
} from '../../../../shared/components/coin-flip-animation/coin-flip-animation.component';
import { AvailablePlayer, DraftBoardState } from '../../models/draft.interface';

export type SimultaneousPhase = 'submitting' | 'waiting' | 'reselecting' | 'done';

const RESELECT_SECONDS = 45;
const SNACKBAR_DURATION_MS = 3_000;

/**
 * Page for simultaneous draft mode.
 *
 * All participants select a player anonymously within the window.
 * The server detects conflicts and resolves them via coin flip.
 * The loser must re-select within 45 seconds.
 */
@Component({
  selector: 'app-simultaneous-draft-page',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    ScrollingModule,
    DraftTimerComponent,
    PlayerCardComponent,
    PlayerSearchFilterComponent,
  ],
  templateUrl: './simultaneous-draft-page.component.html',
  styleUrls: ['./simultaneous-draft-page.component.scss'],
})
export class SimultaneousDraftPageComponent implements OnInit, OnDestroy, ComponentWithDraftState {
  private readonly route = inject(ActivatedRoute);
  private readonly draftService = inject(DraftService);
  private readonly simultaneousService = inject(SimultaneousDraftService);
  private readonly wsService = inject(WebSocketService);
  private readonly userContext = inject(UserContextService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  gameId: string | null = null;
  draft: DraftBoardState | null = null;
  phase: SimultaneousPhase = 'submitting';
  submittedCount = 0;
  totalCount = 0;
  windowId: string | null = null;
  filteredPlayers: AvailablePlayer[] = [];
  selectedPlayer: AvailablePlayer | null = null;
  wsConnected = true;
  regions: string[] = [];
  lostSlotLabel: string | null = null;
  readonly reselectSeconds = RESELECT_SECONDS;

  private myParticipantId: string | null = null;
  private activeFilter: PlayerFilter | null = null;

  ngOnInit(): void {
    this.gameId = this.route.snapshot.paramMap.get('id');
    if (!this.gameId) return;

    this.loadDraftState(this.gameId);
    this.loadStatus(this.gameId);
    this.subscribeToSimultaneous(this.gameId);
    this.trackConnectionStatus();
  }

  isDraftActive(): boolean {
    return this.phase !== 'done';
  }

  ngOnDestroy(): void {
    this.wsService.disconnect();
  }

  onFilterChange(filter: PlayerFilter): void {
    this.activeFilter = filter;
    this.filteredPlayers = this.applyFilter(this.draft?.availablePlayers ?? [], filter);
  }

  onPlayerSelect(player: AvailablePlayer): void {
    this.selectedPlayer = this.selectedPlayer?.id === player.id ? null : player;
  }

  submitSelection(): void {
    if (!this.selectedPlayer || !this.windowId || !this.myParticipantId || !this.gameId) return;

    this.simultaneousService
      .submitSelection(this.gameId, this.windowId, this.myParticipantId, this.selectedPlayer.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.phase = 'waiting';
        this.selectedPlayer = null;
      });
  }

  onReselectTimerExpired(): void {
    const best = this.computeBestAvailable();
    if (!best) return;
    this.selectedPlayer = best;
    this.submitSelection();
  }

  private loadDraftState(gameId: string): void {
    this.draftService
      .getDraftBoardState(gameId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(state => this.applyDraftState(state));
  }

  private loadStatus(gameId: string): void {
    this.simultaneousService
      .getStatus(gameId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(status => {
        if (status) {
          this.windowId = status.windowId;
          this.submittedCount = status.submitted;
          this.totalCount = status.total;
        }
      });
  }

  private subscribeToSimultaneous(gameId: string): void {
    this.wsService.subscribeToSimultaneous(gameId);
    this.wsService.simultaneousEvents
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(event => this.handleSimultaneousEvent(event));
  }

  private trackConnectionStatus(): void {
    this.wsService.isConnected$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(connected => { this.wsConnected = connected; });
  }

  private applyDraftState(state: DraftBoardState): void {
    this.draft = state;
    this.filteredPlayers = this.applyFilter(state.availablePlayers, this.activeFilter);
    this.regions = this.extractRegions(state);
    this.myParticipantId = this.resolveMyParticipantId(state);
  }

  private handleSimultaneousEvent(event: SimultaneousEventMessage): void {
    switch (event.type) {
      case 'SUBMISSION_COUNT':
        this.submittedCount = event.submitted ?? this.submittedCount;
        this.totalCount = event.total ?? this.totalCount;
        break;
      case 'ALL_RESOLVED':
        this.phase = 'done';
        break;
      case 'CONFLICT_RESOLVED':
        this.handleConflictResolved(event);
        break;
    }
  }

  private handleConflictResolved(event: SimultaneousEventMessage): void {
    const amILoser = event.loserParticipantId === this.myParticipantId;
    const amIWinner = event.winnerParticipantId === this.myParticipantId;
    const winnerName = this.resolveParticipantName(event.winnerParticipantId ?? '');
    const loserName = this.resolveParticipantName(event.loserParticipantId ?? '');
    const contestedName = this.resolvePlayerName(event.contestedPlayerId ?? '');

    const dialogData: CoinFlipData = {
      player1: winnerName,
      player2: loserName,
      contestedPlayer: contestedName,
      winner: winnerName,
    };

    const dialogRef = this.dialog.open(CoinFlipAnimationComponent, {
      maxWidth: '100vw',
      panelClass: 'coin-flip-panel',
      data: dialogData,
    });

    dialogRef
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(result => this.applyConflictResult(result, amILoser, amIWinner, contestedName));
  }

  private applyConflictResult(
    result: { action: string } | null,
    amILoser: boolean,
    amIWinner: boolean,
    contestedName: string
  ): void {
    if (amILoser) {
      this.phase = 'reselecting';
      this.lostSlotLabel = contestedName;
    } else if (amIWinner) {
      this.snackBar.open(`Tu conserves ${contestedName} ✓`, undefined, { duration: SNACKBAR_DURATION_MS });
      this.phase = 'waiting';
    }
  }

  private resolveMyParticipantId(state: DraftBoardState): string | null {
    const username = this.userContext.getCurrentUser()?.username;
    if (!username) return null;
    for (const entry of state.participants) {
      const p = 'participant' in entry ? entry.participant : entry;
      if (p.username === username) return p.id;
    }
    return null;
  }

  private resolveParticipantName(participantId: string): string {
    if (!this.draft) return participantId;
    for (const entry of this.draft.participants) {
      const p = 'participant' in entry ? entry.participant : entry;
      if (p.id === participantId) return p.username;
    }
    return participantId;
  }

  private resolvePlayerName(playerId: string): string {
    return this.draft?.availablePlayers.find(p => p.id === playerId)?.username ?? playerId;
  }

  private extractRegions(state: DraftBoardState): string[] {
    const seen = new Set<string>();
    state.availablePlayers.forEach(p => { if (p.region) seen.add(String(p.region)); });
    return Array.from(seen).sort();
  }

  private applyFilter(players: AvailablePlayer[], filter: PlayerFilter | null): AvailablePlayer[] {
    if (!filter) return players;
    return players.filter(player => this.matchesFilter(player, filter));
  }

  private matchesFilter(player: AvailablePlayer, filter: PlayerFilter): boolean {
    if (filter.searchTerm && !player.username.toLowerCase().includes(filter.searchTerm)) {
      return false;
    }
    if (filter.region && player.region !== filter.region) {
      return false;
    }
    if (filter.tranche && player.tranche !== filter.tranche) {
      return false;
    }
    if (filter.hideUnavailable && player.available === false) {
      return false;
    }
    if (filter.hideTaken && player.selected === true) {
      return false;
    }
    return true;
  }

  private computeBestAvailable(): AvailablePlayer | null {
    const available = (this.draft?.availablePlayers ?? []).filter(
      p => p.available !== false && p.selected !== true
    );
    if (!available.length) return null;
    return available.reduce((best, p) =>
      (p.totalPoints ?? 0) > (best.totalPoints ?? 0) ? p : best
    );
  }
}
