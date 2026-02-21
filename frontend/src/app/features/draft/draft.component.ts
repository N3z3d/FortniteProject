import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';

import { DraftService, DraftBoardState, Player, GameParticipant } from './services/draft.service';
import { DraftPlayerFilterService } from './services/draft-player-filter.service';
import { DraftProgressService } from './services/draft-progress.service';
import { DraftStateHelperService } from './services/draft-state-helper.service';
import { PlayerRegion } from './models/draft.interface';
import { LoggerService } from '../../core/services/logger.service';
import { UserContextService } from '../../core/services/user-context.service';
import { TranslationService } from '../../core/services/translation.service';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';
import { DraftTimerComponent } from './components/draft-timer/draft-timer.component';
import { DraftPlayerListComponent } from './components/draft-player-list/draft-player-list.component';
import { DraftRosterComponent } from './components/draft-roster/draft-roster.component';
import { DRAFT_CONSTANTS, FILTER_OPTIONS } from './constants/draft.constants';
import { UiErrorFeedbackService } from '../../core/services/ui-error-feedback.service';

@Component({
  selector: 'app-draft',
  templateUrl: './draft.component.html',
  styleUrls: ['./draft.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    DraftTimerComponent,
    DraftPlayerListComponent,
    DraftRosterComponent
  ]
})
export class DraftComponent implements OnInit, OnDestroy {
  draftState: DraftBoardState | null = null;
  isLoading = false;
  error: string | null = null;
  isSelectingPlayer = false;
  currentUserId: string | null = null;
  gameId: string | null = null;

  selectedRegion: PlayerRegion | 'ALL' = FILTER_OPTIONS.ALL_REGIONS;
  searchTerm = FILTER_OPTIONS.DEFAULT_SEARCH_TERM;
  selectedTranche: string | 'ALL' = FILTER_OPTIONS.ALL_TRANCHES;
  showAllResults = false;
  showDebugPanel = false;

  private destroy$ = new Subject<void>();
  private refreshInterval: any;

  constructor(
    private readonly draftService: DraftService,
    private readonly filterService: DraftPlayerFilterService,
    private readonly progressService: DraftProgressService,
    private readonly helperService: DraftStateHelperService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly uiFeedback: UiErrorFeedbackService,
    private readonly dialog: MatDialog,
    private readonly logger: LoggerService,
    private readonly userContextService: UserContextService,
    public readonly t: TranslationService
  ) {}

  ngOnInit(): void {
    this.initializeComponent();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
      this.refreshInterval = null;
    }
  }

  loadDraftState(): void {
    if (!this.gameId) return;
    this.isLoading = true;
    this.error = null;
    this.draftService.getDraftBoardState(this.gameId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (state) => { this.draftState = state; this.isLoading = false; },
        error: (err) => this.handleLoadError(err)
      });
  }

  selectPlayer(player: Player): void {
    if (!this.canSelectPlayer()) return;
    const gameId = this.gameId;
    if (!gameId) return;
    this.isSelectingPlayer = true;
    this.draftService.makePlayerSelection(gameId, player.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.showNotification(
            this.helperService.formatTemplate(this.t.t('draft.success.playerSelectedWithName'), { player: player.nickname })
          );
          this.loadDraftState();
          this.isSelectingPlayer = false;
        },
        error: () => {
          this.logger.error('Draft: Failed to select player');
          this.setError(this.t.t('draft.errors.selectPlayerFailed'));
          this.isSelectingPlayer = false;
        }
      });
  }

  pauseDraft(): void {
    if (!this.gameId) return;
    this.executeDraftToggle('pause', this.t.t('draft.success.draftPaused'), this.t.t('draft.errors.pauseFailed'));
  }

  resumeDraft(): void {
    if (!this.gameId) return;
    this.executeDraftToggle('resume', this.t.t('draft.success.draftResumed'), this.t.t('draft.errors.resumeFailed'));
  }

  cancelDraft(): void {
    if (!this.gameId) return;
    this.showCancelConfirmation();
  }

  handleTimeouts(): void {
    const gameId = this.gameId;
    if (!gameId) return;
    this.draftService.handleTimeouts(gameId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (picks) => {
          this.showNotification(
            this.helperService.formatTemplate(this.t.t('draft.success.timeoutsHandled'), { count: picks.length })
          );
          this.loadDraftState();
        },
        error: () => this.setError(this.t.t('draft.errors.timeoutsFailed'))
      });
  }

  isCurrentUserTurn(): boolean {
    return this.progressService.isCurrentUserTurn(this.draftState, this.currentUserId);
  }

  canSelectPlayer(): boolean {
    const status = this.draftState?.draft?.status || this.draftState?.status;
    return this.gameId !== null && !this.isSelectingPlayer && this.isCurrentUserTurn() && status === 'ACTIVE';
  }

  getDraftProgress(): number {
    return this.progressService.calculateProgress(this.draftState);
  }

  getDraftProgressText(): string {
    return this.progressService.getProgressText(this.draftState);
  }

  getCurrentTurnPlayer(): GameParticipant | null {
    return this.progressService.getCurrentTurnPlayer(this.draftState);
  }

  getSmartSuggestions(): any[] {
    if (!this.draftState || this.searchTerm) return [];
    return this.filterService.getSmartSuggestions(this.getFilteredPlayers(), 3);
  }

  getWaitingMessage(): string {
    const player = this.getCurrentTurnPlayer();
    return this.helperService.getWaitingMessage(player?.username || null, (key) => this.t.t(key));
  }

  getCurrentUserTeam(): Player[] {
    return this.progressService.getCurrentUserTeam(this.draftState, this.currentUserId);
  }

  getRemainingSlots(): number {
    return this.progressService.getRemainingSlots(this.getCurrentUserTeam(), 5);
  }

  getFilteredPlayers(): Player[] {
    if (!this.draftState) return [];
    return this.filterService.filterPlayers(this.draftState.availablePlayers, {
      selectedRegion: this.selectedRegion,
      selectedTranche: this.selectedTranche,
      searchTerm: this.searchTerm
    });
  }

  formatTime(seconds: number): string {
    return this.progressService.formatTime(seconds);
  }

  private initializeComponent(): void {
    this.gameId = this.route.snapshot.paramMap.get('id');
    if (!this.gameId) {
      this.error = this.t.t('draft.errors.missingGameId');
      return;
    }
    this.loadDraftState();
    this.refreshInterval = setInterval(() => this.loadDraftState(), DRAFT_CONSTANTS.AUTO_REFRESH_INTERVAL);
    this.loadCurrentUser();
  }

  private loadCurrentUser(): void {
    const user = this.userContextService.getCurrentUser();
    if (user?.id) {
      this.currentUserId = user.id;
    } else {
      this.logger.warn('DraftComponent: no user found, using fallback');
      this.currentUserId = 'participant-1';
    }
  }

  private handleLoadError(error: any): void {
    this.logger.error('Draft: Failed to load draft state', error);
    this.error = error.message || this.t.t('draft.errors.loadDraftFailed');
    this.isLoading = false;
    if (error.message?.includes('not found')) {
      this.initializeDraft();
    }
  }

  private initializeDraft(): void {
    if (!this.gameId) return;
    this.draftService.initializeDraft(this.gameId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => { this.showNotification(this.t.t('draft.success.draftInitialized')); this.loadDraftState(); },
        error: (err) => this.setError(err.message || this.t.t('draft.errors.initializeFailed'))
      });
  }

  private executeDraftToggle(action: 'pause' | 'resume', successMsg: string, errorMsg: string): void {
    const gameId = this.gameId;
    if (!gameId) return;
    const action$ = action === 'pause'
      ? this.draftService.pauseDraft(gameId)
      : this.draftService.resumeDraft(gameId);
    action$.pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (success) => { if (success) { this.showNotification(successMsg); this.loadDraftState(); } },
        error: (err) => {
          this.logger.error(`Draft: Failed to ${action} draft`, err);
          this.setError(err.message || errorMsg);
        }
      });
  }

  private showCancelConfirmation(): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: {
        title: this.t.t('draft.ui.cancelDraftTitle'),
        message: this.t.t('draft.ui.cancelDraftMessage'),
        confirmText: this.t.t('draft.ui.cancelDraftConfirm'),
        cancelText: this.t.t('draft.ui.cancelDraftCancel'),
        confirmColor: 'warn'
      }
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result === true) this.executeCancellation();
    });
  }

  private executeCancellation(): void {
    const gameId = this.gameId;
    if (!gameId) return;
    this.draftService.cancelDraft(gameId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (success) => {
          if (success) {
            this.showNotification(this.t.t('draft.success.draftCancelled'));
            this.router.navigate(['/games']);
          }
        },
        error: () => this.setError(this.t.t('draft.errors.cancelFailed'))
      });
  }

  private setError(message: string): void {
    this.error = message;
    this.uiFeedback.showErrorMessage(message, DRAFT_CONSTANTS.SNACKBAR_DURATION);
  }

  private showNotification(message: string): void {
    this.uiFeedback.showSuccessMessage(message, DRAFT_CONSTANTS.SNACKBAR_DURATION);
  }
}
