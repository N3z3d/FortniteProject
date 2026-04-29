import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';
import { MatBadgeModule } from '@angular/material/badge';

import { GameDataService } from '../services/game-data.service';
import { GameDetailActionsService } from '../services/game-detail-actions.service';
import { GameDetailPermissionsService } from '../services/game-detail-permissions.service';
import { GameDetailUIService } from '../services/game-detail-ui.service';
import { Game, GameStatus, GameParticipant } from '../models/game.interface';
import { TranslationService } from '../../../core/services/translation.service';
import { UserGamesStore } from '../../../core/services/user-games.store';
import { UiErrorFeedbackService } from '../../../core/services/ui-error-feedback.service';
import { LoggerService } from '../../../core/services/logger.service';
import { GamesRealtimeService } from '../../../core/services/games-realtime.service';
import { WebSocketService } from '../../../core/services/websocket.service';

@Component({
  selector: 'app-game-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatProgressBarModule,
    MatListModule,
    MatDividerModule,
    MatBadgeModule
  ],
  templateUrl: './game-detail.component.html',
  styleUrls: ['./game-detail.component.css']
})
export class GameDetailComponent implements OnInit, OnDestroy {
  public readonly t = inject(TranslationService);

  game: Game | null = null;
  participants: GameParticipant[] = [];
  loading = false;
  error: string | null = null;
  participantsError: string | null = null;
  gameId: string = '';
  isStartingDraft = false;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly gameDataService: GameDataService,
    private readonly actions: GameDetailActionsService,
    private readonly permissions: GameDetailPermissionsService,
    private readonly ui: GameDetailUIService,
    private readonly userGamesStore: UserGamesStore,
    private readonly uiFeedback: UiErrorFeedbackService,
    private readonly logger: LoggerService,
    private readonly gamesRealtimeService: GamesRealtimeService,
    private readonly websocketService: WebSocketService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) { }

  ngOnInit(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.gameId = params['id'];
      if (this.gameId) {
        this.loadGameDetails();
        this.websocketService.subscribeToGameEvents(this.gameId);
      }
    });
    this.subscribeToRealtimeUpdates();
    this.subscribeToDraftStarted();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadGameDetails(): void {
    this.loading = true;
    this.error = null;
    this.participantsError = null;
    this.userGamesStore.refreshGames().pipe(takeUntil(this.destroy$)).subscribe({
      next: (games) => {
        if (!this.isGameVisibleForCurrentUser(games)) {
          this.handleInaccessibleGame();
          return;
        }
        this.fetchGameDetails();
      },
      // If refresh fails, we still try to load details to avoid blocking the screen on transient API errors.
      error: () => this.fetchGameDetails()
    });
  }

  private fetchGameDetails(): void {
    this.gameDataService.getGameById(this.gameId).subscribe({
      next: (game: Game) => {
        this.game = game;
        this.loading = false;
        
        // Validation des données reçues
        const validation = this.gameDataService.validateGameData(game);
        if (!validation.isValid) {
          this.logger.warn('GameDetailComponent: game data validation failed', {
            gameId: this.gameId,
            errors: validation.errors
          });
        }

        this.loadParticipants();
      },
      error: (error: Error) => {
        if (this.isNotFoundError(error)) {
          this.handleNotFoundGame();
          this.loading = false;
          return;
        }

        this.error = error.message;
        this.loading = false;
        this.logger.error('GameDetailComponent: failed to load game details', {
          gameId: this.gameId,
          error
        });
      }
    });
  }

  private isGameVisibleForCurrentUser(games: Game[]): boolean {
    return games.some(game => game.id === this.gameId);
  }

  loadParticipants(): void {
    this.participantsError = null;
    this.gameDataService.getGameParticipants(this.gameId).subscribe({
      next: (participants: GameParticipant[]) => {
        this.participants = participants;
        if (this.game) {
          this.game.participants = participants;
        }
        this.participantsError = null;
      },
      error: (error: Error) => {
        const message = error.message || 'Erreur lors du chargement des participants';
        this.participantsError = message;
        this.logger.error('GameDetailComponent: failed to load participants', {
          gameId: this.gameId,
          error
        });
      }
    });
  }

  retryLoad(): void {
    this.error = null;
    this.participantsError = null;
    this.loadGameDetails();
  }

  startDraft(): void {
    if (this.isStartingDraft) {
      return;
    }
    this.isStartingDraft = true;
    this.actions.startDraft(
      this.gameId,
      () => {
        this.loadGameDetails();
        this.router.navigate(['/games', this.gameId, 'draft', 'snake']);
      },
      () => {
        this.isStartingDraft = false;
      }
    );
  }

  archiveGame(): void {
    if (!this.game) return;
    this.actions.archiveGame(this.gameId);
  }

  leaveGame(): void {
    if (!this.game) return;
    this.actions.leaveGame(this.gameId);
  }

  confirmArchive(): void {
    if (!this.game) return;
    this.actions.confirmArchive(this.gameId);
  }

  confirmLeave(): void {
    if (!this.game) return;
    this.actions.confirmLeave(this.gameId);
  }

  permanentlyDeleteGame(): void {
    if (!this.game) return;
    this.actions.permanentlyDeleteGame(this.gameId);
  }

  /**
   * @deprecated Utiliser permanentlyDeleteGame() pour suppression définitive
   */
  deleteGame(): void {
    this.permanentlyDeleteGame();
  }

  joinGame(): void {
    this.actions.joinGame(this.gameId, () => {
      this.loadGameDetails();
      this.loadParticipants();
    });
  }

  canStartDraft(): boolean {
    return this.permissions.canStartDraft(this.game);
  }

  canArchiveGame(): boolean {
    return this.permissions.canArchiveGame(this.game);
  }

  canLeaveGame(): boolean {
    return this.permissions.canLeaveGame(this.game);
  }

  canDeleteGame(): boolean {
    return this.permissions.canDeleteGame(this.game);
  }

  canShowDeleteGameAction(): boolean {
    return this.permissions.canSeeDeleteGameAction(this.game);
  }

  isDeleteActionDisabled(): boolean {
    return this.canShowDeleteGameAction() && !this.canDeleteGame();
  }

  getDeleteRestrictionReasonKey(): string | null {
    return this.permissions.getDeleteRestrictionReasonKey(this.game);
  }

  getDeleteTooltipKey(): string {
    const reasonKey = this.getDeleteRestrictionReasonKey();
    return reasonKey ?? 'games.detail.deleteTooltip';
  }

  canJoinGame(): boolean {
    return this.permissions.canJoinGame(this.game);
  }

  getStatusColor(status: GameStatus): string {
    return this.ui.getStatusColor(status);
  }

  getStatusLabel(status: GameStatus): string {
    return this.ui.getStatusLabel(status);
  }

  getParticipantPercentage(): number {
    return this.ui.getParticipantPercentage(this.game);
  }

  getGameStatistics() {
    return this.ui.getGameStatistics(this.game);
  }

  getParticipantColor(): string {
    return this.ui.getParticipantColor(this.game);
  }

  getTimeAgo(date: string | Date | null | undefined): string {
    return this.ui.getTimeAgo(date);
  }

  onBack(): void {
    this.router.navigate(['/games']);
  }

  confirmDelete(): void {
    this.actions.confirmDelete(this.gameId);
  }

  confirmStartDraft(): void {
    if (this.isStartingDraft) {
      return;
    }
    this.isStartingDraft = true;
    this.actions.confirmStartDraft(
      this.gameId,
      () => {
        this.loadGameDetails();
        this.router.navigate(['/games', this.gameId, 'draft', 'snake']);
      },
      () => {
        this.isStartingDraft = false;
      }
    );
  }

  isStartDraftActionDisabled(): boolean {
    return this.loading || this.isStartingDraft;
  }

  copyInvitationCode(): void {
    if (!this.game?.invitationCode) return;
    this.actions.copyInvitationCode(this.game.invitationCode);
  }

  regenerateInvitationCode(duration: '24h' | '48h' | '7d' | 'permanent' = 'permanent'): void {
    if (!this.game) return;
    this.actions.regenerateInvitationCode(this.game.id || this.gameId, duration, 'regenerate', (updatedGame) => {
      this.applyInvitationCodeState(updatedGame);
    });
  }

  promptRegenerateCode(): void {
    const hasExistingCode = !!this.game?.invitationCode;
    this.actions.promptRegenerateCode(this.game?.id || this.gameId, hasExistingCode, (updatedGame) => {
      this.applyInvitationCodeState(updatedGame);
    });
  }

  confirmDeleteInvitationCode(): void {
    if (!this.game?.invitationCode) {
      return;
    }

    this.actions.confirmDeleteInvitationCode(this.game.id || this.gameId, (updatedGame) => {
      this.applyInvitationCodeState(updatedGame);
    });
  }

  getInvitationCodeExpiry(): string {
    return this.ui.getInvitationCodeExpiry(this.game);
  }

  canRegenerateCode(): boolean {
    return this.permissions.canRegenerateCode(this.game);
  }

  canRenameGame(): boolean {
    return this.permissions.canRenameGame(this.game);
  }

  renameGame(): void {
    if (!this.game) return;
    this.actions.promptRenameGame(this.gameId, this.game.name, (updatedGame) => {
      if (this.game) {
        this.game.name = updatedGame.name;
      }
    });
  }

  getCreator(): GameParticipant | null {
    return this.ui.getCreator(this.participants);
  }

  getNonCreatorParticipants(): GameParticipant[] {
    return this.ui.getNonCreatorParticipants(this.participants);
  }

  getParticipantStatusIcon(participant: GameParticipant): string {
    return this.ui.getParticipantStatusIcon(participant);
  }

  getParticipantStatusColor(participant: GameParticipant): string {
    return this.ui.getParticipantStatusColor(participant);
  }

  getParticipantStatusLabel(participant: GameParticipant): string {
    return this.ui.getParticipantStatusLabel(participant);
  }

  getDraftRoute(game: Game): string[] {
    const mode = game.draftMode === 'SIMULTANEOUS' ? 'simultaneous' : 'snake';
    return ['/games', game.id, 'draft', mode];
  }

  private applyInvitationCodeState(updatedGame: Game): void {
    if (!this.game) {
      return;
    }

    this.game.invitationCode = updatedGame.invitationCode;
    this.game.invitationCodeExpiresAt = updatedGame.invitationCodeExpiresAt;
    this.game.isInvitationCodeExpired = updatedGame.isInvitationCodeExpired;
  }

  private isNotFoundError(error: Error): boolean {
    const withStatus = error as Error & { status?: number };
    if (withStatus.status === 404) {
      return true;
    }

    const message = (error.message || '').toLowerCase();
    return message.includes('not found') ||
      message.includes('introuvable') ||
      message.includes('no existe');
  }

  private handleNotFoundGame(): void {
    this.handleInaccessibleGame();
  }

  private handleInaccessibleGame(): void {
    this.game = null;
    this.participants = [];
    this.loading = false;
    this.userGamesStore.removeGame(this.gameId);
    this.uiFeedback.showError(null, 'games.detail.gameUnavailable', { duration: 5000 });
    this.router.navigate(['/games']);
  }

  private subscribeToDraftStarted(): void {
    this.websocketService.gameNotifications
      .pipe(
        takeUntil(this.destroy$),
        filter(n => !!n && n.type === 'DRAFT_STARTED' && n.gameId === this.gameId)
      )
      .subscribe(() => {
        if (this.router.url.includes('draft')) {
          return;
        }
        this.router.navigate(['/games', this.gameId, 'draft', 'snake']);
      });
  }

  private subscribeToRealtimeUpdates(): void {
    this.gamesRealtimeService.events$
      .pipe(takeUntil(this.destroy$))
      .subscribe((event) => {
        if (event.type === 'CONNECTED' || !this.gameId) {
          return;
        }

        if (event.gameId && event.gameId !== this.gameId) {
          return;
        }

        this.loadGameDetails();
      });
  }
}
