import { Component, OnInit, OnDestroy, inject, DestroyRef } from '@angular/core';
import { ComponentWithDraftState } from '../../../../core/guards/draft-active.guard';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, first, interval, skip, switchMap } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { DraftService } from '../../services/draft.service';
import { WebSocketService } from '../../../../core/services/websocket.service';
import { UserContextService } from '../../../../core/services/user-context.service';
import { DraftTimerComponent } from '../draft-timer/draft-timer.component';
import { PlayerCardComponent } from '../../../../shared/components/player-card/player-card.component';
import {
  PlayerSearchFilterComponent,
  PlayerFilter,
} from '../../../../shared/components/player-search-filter/player-search-filter.component';
import { SnakeOrderBarComponent, SnakeParticipant } from '../../../../shared/components/snake-order-bar/snake-order-bar.component';
import { AvailablePlayer, DraftBoardState } from '../../models/draft.interface';

const TURN_CHANGED_EVENT = 'TURN_CHANGED';
const PICK_CONFIRM_SNACKBAR_DURATION_MS = 3_000;
/** Fallback polling interval (ms) — refreshes board state when WS events may be missed. */
const BOARD_POLL_INTERVAL_MS = 5_000;

@Component({
  selector: 'app-snake-draft-page',
  standalone: true,
  imports: [
    CommonModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    ScrollingModule,
    DraftTimerComponent,
    PlayerCardComponent,
    PlayerSearchFilterComponent,
    SnakeOrderBarComponent,
  ],
  templateUrl: './snake-draft-page.component.html',
  styleUrls: ['./snake-draft-page.component.scss'],
})
export class SnakeDraftPageComponent implements OnInit, OnDestroy, ComponentWithDraftState {
  private readonly route = inject(ActivatedRoute);
  private readonly draftService = inject(DraftService);
  private readonly wsService = inject(WebSocketService);
  private readonly userContext = inject(UserContextService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  gameId: string | null = null;
  draft: DraftBoardState | null = null;
  phase: 'warmup' | 'my-turn' | 'waiting' | 'idle' | 'done' = 'idle';
  participants: SnakeParticipant[] = [];
  currentIndex = 0;
  isMyTurn = false;
  filteredPlayers: AvailablePlayer[] = [];
  selectedPlayer: AvailablePlayer | null = null;
  recommendedPlayer: AvailablePlayer | null = null;
  wsConnected = true;
  isReconnecting = false;
  regions: string[] = [];
  isPickPending = false;
  pickExpiresAt: string | null = null;
  currentRegion = 'GLOBAL';
  trancheFloor = 1;
  tranchesEnabled = true;
  hideIneligible = false;

  /** playerId → dynamic rank within current region (1-based, computed from totalPoints DESC) */
  private regionPlayerRanks = new Map<string, number>();
  private activeFilter: PlayerFilter | null = null;

  ngOnInit(): void {
    this.gameId = this.route.snapshot.paramMap.get('id');
    if (!this.gameId) return;

    this.loadDraftState(this.gameId);
    this.wsService.connect(); // ensure STOMP session is active (idempotent)
    this.subscribeToWebSocket(this.gameId);
    this.trackConnectionStatus();
    this.startPollingFallback(this.gameId);
  }

  isDraftActive(): boolean {
    return this.draft !== null && !this.isDraftDone();
  }

  ngOnDestroy(): void {
    // BUG-05 fix: do NOT call disconnect() — WebSocketService is a singleton shared across pages.
    // Subscriptions are cleaned up via takeUntilDestroyed(this.destroyRef).
  }

  onFilterChange(filter: PlayerFilter): void {
    this.activeFilter = filter;
    const regionFiltered = this.applyRegionFilter(this.draft?.availablePlayers ?? []);
    this.filteredPlayers = this.applyFilter(regionFiltered, filter);
  }

  isPlayerEligible(player: AvailablePlayer): boolean {
    if (!this.tranchesEnabled) return true;
    const rank = this.regionPlayerRanks.get(player.id) ?? Number.MAX_SAFE_INTEGER;
    return rank >= this.trancheFloor;
  }

  toggleHideIneligible(): void {
    this.hideIneligible = !this.hideIneligible;
    const regionFiltered = this.applyRegionFilter(this.draft?.availablePlayers ?? []);
    this.filteredPlayers = this.applyFilter(regionFiltered, this.activeFilter);
  }

  onRegionSelect(region: string): void {
    if (!this.gameId || !region || region === this.currentRegion || this.isPickPending) {
      return;
    }

    this.selectedPlayer = null;
    this.currentRegion = region;

    if (this.draft) {
      const regionFiltered = this.applyRegionFilter(this.draft.availablePlayers, region);
      this.regionPlayerRanks = this.computeRegionRanks(regionFiltered);
      this.filteredPlayers = this.applyFilter(regionFiltered, this.activeFilter);
      this.recommendedPlayer = null;
    }

    this.refreshDraftState(this.gameId, region);
  }

  onPlayerSelect(player: AvailablePlayer): void {
    this.selectedPlayer = this.selectedPlayer?.id === player.id ? null : player;
  }

  onTimerExpired(): void {
    if (!this.recommendedPlayer || !this.gameId || !this.isMyTurn || this.isPickPending) return;
    this.doConfirmPick(this.recommendedPlayer);
  }

  onPickCancelled(): void {
    this.isPickPending = false;
    this.selectedPlayer = null;
  }

  confirmPick(): void {
    if (!this.selectedPlayer || !this.gameId) return;
    this.doConfirmPick(this.selectedPlayer);
  }

  private doConfirmPick(player: AvailablePlayer): void {
    if (!this.gameId || !this.isMyTurn || this.isPickPending) return;

    this.isPickPending = true;
    this.draftService
      .submitSnakePick(this.gameId, player.id, this.currentRegion)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: state => {
          this.applyDraftState(state);
          this.showNarrativeToast(player);
          this.selectedPlayer = null;
          this.isPickPending = false;
        },
        error: (err: { status?: number; error?: { message?: string; error?: string } }) => {
          this.isPickPending = false;
          this.selectedPlayer = null;
          const backendMsg = err?.error?.message || err?.error?.error || '';
          let userMsg: string;
          if (err?.status === 409) {
            userMsg = 'Ce joueur a déjà été sélectionné, choisissez un autre.';
          } else if (err?.status === 400) {
            userMsg = this.translateBackendError(backendMsg);
          } else {
            userMsg = 'Erreur inattendue, veuillez réessayer.';
          }
          this.snackBar.open(userMsg, undefined, {
            duration: PICK_CONFIRM_SNACKBAR_DURATION_MS,
            panelClass: 'snack-pick-error',
          });
          if (this.gameId) {
            this.refreshDraftState(this.gameId, this.currentRegion);
          }
        },
      });
  }

  private loadDraftState(gameId: string): void {
    this.refreshDraftState(gameId);
  }

  private refreshDraftState(gameId: string, regionHint?: string | null): void {
    this.draftService
      .getSnakeBoardState(gameId, regionHint ?? undefined)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(state => this.applyDraftState(state));
  }

  private subscribeToWebSocket(gameId: string): void {
    // Listen for draft events (Subject is always available regardless of WS state)
    this.wsService.draftEvents
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(event => this.handleDraftEvent(event));

    // Wait for WS to be active before subscribing to the draft topic.
    // subscribeToDraft() is a no-op when called before the STOMP handshake completes
    // (race condition on page load in a new browser context).
    this.wsService.isConnected$
      .pipe(
        first(connected => connected),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => this.wsService.subscribeToDraft(gameId));
  }

  private trackConnectionStatus(): void {
    // skip(1) avoids the initial false emission from BehaviorSubject(false)
    // on page load — banner only shows after a genuine connection loss
    this.wsService.isConnected$
      .pipe(skip(1), takeUntilDestroyed(this.destroyRef))
      .subscribe(connected => {
        this.wsConnected = connected;
        // When disconnected, enter reconnecting state.
        // When reconnected, clear reconnecting state immediately (PICK_PROMPT will recalibrate timer).
        if (!connected) {
          this.isReconnecting = true;
        } else {
          this.isReconnecting = false;
        }
      });
  }

  private applyDraftState(state: DraftBoardState): void {
    this.draft = state;
    this.currentRegion = state.currentRegion ?? 'GLOBAL';
    this.pickExpiresAt = state.pickExpiresAt ?? null;
    this.participants = this.extractParticipants(state);
    this.currentIndex = this.computeCurrentIndex(state);
    this.isMyTurn = this.computeIsMyTurn(state);
    this.regions = this.extractRegions(state);
    this.trancheFloor = state.trancheFloor ?? 1;
    this.tranchesEnabled = state.tranchesEnabled ?? true;
    const regionFiltered = this.applyRegionFilter(state.availablePlayers);
    this.regionPlayerRanks = this.computeRegionRanks(regionFiltered);
    this.filteredPlayers = this.applyFilter(regionFiltered, this.activeFilter);
    this.recommendedPlayer = this.resolveRecommendedPlayer(state, regionFiltered);
    // Update phase — keeps draft content mounted during turn transitions (BUG-01 fix)
    if (this.isDraftDone()) {
      this.phase = 'done';
    } else if (this.isMyTurn) {
      this.phase = 'my-turn';
    } else if (this.phase === 'idle') {
      this.phase = 'warmup';
    } else {
      this.phase = 'waiting';
    }
  }

  private handleDraftEvent(event: { event?: string; draftId?: string; round?: number; expiresAt?: string; region?: string; participantId?: string; participantUsername?: string }): void {
    if (!this.draft?.draft.id) {
      return;
    }
    if (event.draftId && event.draftId !== this.draft.draft.id) {
      return;
    }

    // BUG-06 fix: accept both DraftEventMessage (event field) and SnakeTurnResponse-shaped
    // broadcasts (round + draftId, no event field) from the backend snake draft service.
    const isTurnEvent =
      event.event === 'PICK_PROMPT' ||
      event.event === TURN_CHANGED_EVENT ||
      (event.round !== undefined && event.draftId !== undefined);

    if (isTurnEvent) {
      if (event.region) {
        this.currentRegion = event.region;
      }
      if (event.expiresAt) {
        this.pickExpiresAt = event.expiresAt;
      }
      // BUG-01 fix: use nextPlayerId from WS broadcast to transition phase optimistically
      // before getSnakeBoardState() completes — eliminates null gap between PICK_MADE/PICK_PROMPT.
      if (event.participantId !== undefined && this.phase !== 'idle' && this.phase !== 'done') {
        const currentUsername = this.userContext.getCurrentUser()?.username;
        const nextUsername = this.resolveTurnUsername(event.participantId, event.participantUsername);
        const isNext = !!currentUsername &&
          !!nextUsername &&
          nextUsername.toLowerCase() === currentUsername.toLowerCase();
        this.phase = isNext ? 'my-turn' : 'waiting';
        this.isMyTurn = isNext;
      }
      if (this.gameId) {
        this.draftService
          .getSnakeBoardState(this.gameId, event.region ?? this.currentRegion)
          .pipe(
            takeUntilDestroyed(this.destroyRef),
            catchError(() => EMPTY)
          )
          .subscribe(state => this.applyDraftState(state));
      }
    }
  }

  private extractParticipants(state: DraftBoardState): SnakeParticipant[] {
    return state.participants.map(entry => {
      const p = 'participant' in entry ? entry.participant : entry;
      return { id: p.id, username: p.username };
    });
  }

  private computeCurrentIndex(state: DraftBoardState): number {
    const currentParticipant = state.currentParticipant;
    if (!currentParticipant) return 0;
    const idx = this.participants.findIndex(p => p.id === currentParticipant.id);
    return idx >= 0 ? idx : 0;
  }

  private computeIsMyTurn(state: DraftBoardState): boolean {
    const username = this.userContext.getCurrentUser()?.username;
    return (
      !!username &&
      state.currentParticipant?.username?.toLowerCase() === username.toLowerCase()
    );
  }

  private extractRegions(state: DraftBoardState): string[] {
    const configuredRegions = (state.regions ?? []).filter(region => !!region);
    if (configuredRegions.length > 0) {
      return configuredRegions;
    }

    const seen = new Set<string>();
    state.availablePlayers.forEach(p => {
      if (p.region) seen.add(String(p.region));
    });
    return Array.from(seen).sort();
  }

  /**
   * Computes dynamic rank (1-based) for each player within the region, sorted by totalPoints DESC.
   * Rank 1 = highest points = best player.
   */
  private computeRegionRanks(regionPlayers: AvailablePlayer[]): Map<string, number> {
    const sorted = [...regionPlayers].sort((a, b) => (b.totalPoints ?? 0) - (a.totalPoints ?? 0));
    const ranks = new Map<string, number>();
    sorted.forEach((p, i) => ranks.set(p.id, i + 1));
    return ranks;
  }

  private applyRegionFilter(players: AvailablePlayer[], region = this.currentRegion): AvailablePlayer[] {
    if (!region || region === 'GLOBAL') return players;
    return players.filter(p => p.region === region);
  }

  private applyFilter(players: AvailablePlayer[], filter: PlayerFilter | null): AvailablePlayer[] {
    let result = filter ? players.filter(player => this.matchesFilter(player, filter)) : players;
    if (this.hideIneligible && this.tranchesEnabled) {
      result = result.filter(p => this.isPlayerEligible(p));
    }
    return result;
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

  private translateBackendError(msg: string): string {
    if (msg.includes('Tranche violation')) {
      const rankMatch = msg.match(/rank (\d+)/);
      const floorMatch = msg.match(/floor (\d+)/);
      const rank = rankMatch ? rankMatch[1] : '?';
      const floor = floorMatch ? floorMatch[1] : '?';
      return `Ce joueur est trop bien classé pour ce slot (rang ${rank}, minimum requis : ${floor}).`;
    }
    if (msg.includes('Joueur hors region')) {
      return msg;
    }
    const requiredRegionMatch = msg.match(/required region ([A-Z]+)/);
    if (requiredRegionMatch) {
      return `Joueur hors region - region attendue : ${requiredRegionMatch[1]}`;
    }
    if (msg.includes('Region violation')) {
      return 'Ce joueur ne correspond pas à la région en cours.';
    }
    if (msg.includes('already selected')) {
      return 'Ce joueur a déjà été sélectionné, choisissez un autre.';
    }
    return msg || 'Sélection invalide — vérifiez la région et la tranche.';
  }

  private isDraftDone(): boolean {
    const status = this.draft?.status;
    return status === 'COMPLETED' || status === 'CANCELLED';
  }

  private resolveRecommendedPlayer(
    state: DraftBoardState,
    players: AvailablePlayer[]
  ): AvailablePlayer | null {
    if (!state.recommendedPlayerId) {
      return null;
    }
    return players.find(player => player.id === state.recommendedPlayerId) ?? null;
  }

  private resolveTurnUsername(
    participantId: string,
    participantUsername?: string
  ): string | undefined {
    if (participantUsername) {
      return participantUsername;
    }
    return this.participants.find(participant => participant.id === participantId)?.username;
  }

  private startPollingFallback(gameId: string): void {
    interval(BOARD_POLL_INTERVAL_MS)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        switchMap(() =>
          this.isDraftActive()
            ? this.draftService
                .getSnakeBoardState(gameId, this.currentRegion)
                .pipe(catchError(() => EMPTY))
            : EMPTY
        )
      )
      .subscribe(state => this.applyDraftState(state));
  }

  private showNarrativeToast(player: AvailablePlayer): void {
    const region = player.region ? String(player.region) : '';
    const rank = player.totalPoints ? `${player.totalPoints} pts` : '';
    this.snackBar.open(
      `Tu as selectionne ${player.username} - ${rank} ${region}`.trim(),
      undefined,
      { duration: PICK_CONFIRM_SNACKBAR_DURATION_MS, panelClass: 'snack-pick-confirm' }
    );
  }
}
