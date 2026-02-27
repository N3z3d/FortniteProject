import { Component, OnInit, OnDestroy, inject, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject } from 'rxjs';

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

@Component({
  selector: 'app-snake-draft-page',
  standalone: true,
  imports: [
    CommonModule,
    MatToolbarModule,
    MatButtonModule,
    MatTabsModule,
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
export class SnakeDraftPageComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly draftService = inject(DraftService);
  private readonly wsService = inject(WebSocketService);
  private readonly userContext = inject(UserContextService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  gameId: string | null = null;
  draft: DraftBoardState | null = null;
  participants: SnakeParticipant[] = [];
  currentIndex = 0;
  isMyTurn = false;
  filteredPlayers: AvailablePlayer[] = [];
  selectedPlayer: AvailablePlayer | null = null;
  recommendedPlayer: AvailablePlayer | null = null;
  wsConnected = true;
  regions: string[] = [];
  activeRegionIndex = 0;
  isPickPending = false;

  private activeFilter: PlayerFilter | null = null;
  private readonly destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.gameId = this.route.snapshot.paramMap.get('id');
    if (!this.gameId) return;

    this.loadDraftState(this.gameId);
    this.subscribeToWebSocket(this.gameId);
    this.trackConnectionStatus();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onFilterChange(filter: PlayerFilter): void {
    this.activeFilter = filter;
    this.filteredPlayers = this.applyFilter(this.draft?.availablePlayers ?? [], filter);
  }

  onPlayerSelect(player: AvailablePlayer): void {
    this.selectedPlayer = this.selectedPlayer?.id === player.id ? null : player;
  }

  onTimerExpired(): void {
    if (!this.recommendedPlayer || !this.gameId) return;
    this.isPickPending = true;
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
    const myParticipant = this.getMyParticipant();
    if (!myParticipant || !this.gameId) return;

    this.wsService.publishDraftPick(this.gameId, myParticipant.id, player.id);
    this.showNarrativeToast(player);
    this.selectedPlayer = null;
    this.isPickPending = false;
  }

  private loadDraftState(gameId: string): void {
    this.draftService
      .getDraftBoardState(gameId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(state => this.applyDraftState(state));
  }

  private subscribeToWebSocket(gameId: string): void {
    this.wsService.subscribeToDraft(gameId);
    this.wsService.draftEvents
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(event => this.handleDraftEvent(event));
  }

  private trackConnectionStatus(): void {
    this.wsService.isConnected$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(connected => {
        this.wsConnected = connected;
      });
  }

  private applyDraftState(state: DraftBoardState): void {
    this.draft = state;
    this.participants = this.extractParticipants(state);
    this.currentIndex = this.computeCurrentIndex(state);
    this.isMyTurn = this.computeIsMyTurn(state);
    this.regions = this.extractRegions(state);
    this.filteredPlayers = this.applyFilter(state.availablePlayers, this.activeFilter);
    this.recommendedPlayer = this.computeRecommendation(state.availablePlayers);
  }

  private handleDraftEvent(event: { event: string; draftId: string }): void {
    if (event.event === TURN_CHANGED_EVENT && this.gameId) {
      this.draftService
        .getDraftBoardState(this.gameId)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(state => this.applyDraftState(state));
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
    return !!username && state.currentParticipant?.username === username;
  }

  private extractRegions(state: DraftBoardState): string[] {
    const seen = new Set<string>();
    state.availablePlayers.forEach(p => {
      if (p.region) seen.add(String(p.region));
    });
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

  private computeRecommendation(players: AvailablePlayer[]): AvailablePlayer | null {
    const available = players.filter(p => p.available !== false && p.selected !== true);
    if (!available.length) return null;
    return available.reduce((best, p) =>
      (p.totalPoints ?? 0) > (best.totalPoints ?? 0) ? p : best
    );
  }

  private getMyParticipant(): SnakeParticipant | null {
    const username = this.userContext.getCurrentUser()?.username;
    return this.participants.find(p => p.username === username) ?? null;
  }

  private showNarrativeToast(player: AvailablePlayer): void {
    const region = player.region ? String(player.region) : '';
    const rank = player.totalPoints ? `Rank ${player.totalPoints}` : '';
    this.snackBar.open(
      `Tu as sÃ©lectionnÃ© ${player.username} â€” ${rank} ${region} âœ“`,
      undefined,
      { duration: 3000, panelClass: 'snack-pick-confirm' }
    );
  }
}

