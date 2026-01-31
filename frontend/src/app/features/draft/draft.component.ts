import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';

import { DraftService, DraftBoardState, Player, GameParticipant } from './services/draft.service';
import { DraftPlayerFilterService } from './services/draft-player-filter.service';
import { DraftProgressService } from './services/draft-progress.service';
import { DraftStateHelperService } from './services/draft-state-helper.service';
import { DraftStatus, PlayerRegion } from './models/draft.interface';
import { LoggerService } from '../../core/services/logger.service';
import { UserContextService } from '../../core/services/user-context.service';
import { TranslationService } from '../../core/services/translation.service';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';
import {
  REGION_LABELS,
  STATUS_LABELS,
  DRAFT_CONSTANTS,
  ERROR_MESSAGES,
  SUCCESS_MESSAGES,
  FILTER_OPTIONS,
  UI_CONFIG
} from './constants/draft.constants';

@Component({
  selector: 'app-draft',
  templateUrl: './draft.component.html',
  styleUrls: ['./draft.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressBarModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ]
})
export class DraftComponent implements OnInit, OnDestroy {
  // Propriétés d'état
  draftState: DraftBoardState | null = null;
  isLoading = false;
  error: string | null = null;
  isSelectingPlayer = false;
  currentUserId: string | null = null;
  gameId: string | null = null;

  // Filtres et sélections
  selectedRegion: PlayerRegion | 'ALL' = FILTER_OPTIONS.ALL_REGIONS;
  searchTerm = FILTER_OPTIONS.DEFAULT_SEARCH_TERM;
  selectedTranche: string | 'ALL' = FILTER_OPTIONS.ALL_TRANCHES;
  showAllResults = false;
  showDebugPanel = false;

  // Timer pour le rafraîchissement automatique
  private destroy$ = new Subject<void>();
  private refreshInterval: any;

  constructor(
    private readonly draftService: DraftService,
    private readonly filterService: DraftPlayerFilterService,
    private readonly progressService: DraftProgressService,
    private readonly helperService: DraftStateHelperService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly snackBar: MatSnackBar,
    private readonly dialog: MatDialog,
    private readonly logger: LoggerService,
    private readonly userContextService: UserContextService,
    public readonly t: TranslationService
  ) {}

  ngOnInit(): void {
    this.initializeComponent();
  }

  ngOnDestroy(): void {
    this.cleanup();
  }

  // Méthodes publiques principales
  loadDraftState(): void {
    if (!this.gameId) return;

    this.isLoading = true;
    this.error = null;

    this.draftService.getDraftBoardState(this.gameId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: this.handleDraftStateSuccess.bind(this),
        error: this.handleDraftStateError.bind(this)
      });
  }

  selectPlayer(player: Player): void {
    if (!this.canSelectPlayer()) return;

    this.isSelectingPlayer = true;
    this.performPlayerSelection(player);
  }

  pauseDraft(): void {
    if (!this.gameId) return;
    this.performDraftAction(
      'pause',
      this.t.t('draft.success.draftPaused'),
      this.t.t('draft.errors.pauseFailed')
    );
  }

  resumeDraft(): void {
    if (!this.gameId) return;
    this.performDraftAction(
      'resume',
      this.t.t('draft.success.draftResumed'),
      this.t.t('draft.errors.resumeFailed')
    );
  }

  cancelDraft(): void {
    if (!this.gameId) return;
    this.showCancelConfirmation();
  }

  handleTimeouts(): void {
    if (!this.gameId) return;
    this.performTimeoutHandling();
  }

  refreshDraftState(): void {
    this.loadDraftState();
  }

  // Méthodes de validation
  isCurrentUserTurn(): boolean {
    return this.progressService.isCurrentUserTurn(this.draftState, this.currentUserId);
  }

  canSelectPlayer(): boolean {
    const status = this.draftState?.draft?.status || this.draftState?.status;
    return this.gameId !== null &&
           !this.isSelectingPlayer &&
           this.isCurrentUserTurn() &&
           status === 'ACTIVE';
  }

  // Méthodes de formatage
  getStatusColor(status: DraftStatus | string): string {
    return this.helperService.getStatusColor(status);
  }

  getStatusLabel(status: DraftStatus | string): string {
    const key = this.helperService.getStatusLabelKey(status);
    return this.t.t(key);
  }

  // === MÉTHODES POUR L'INTERFACE (déléguées aux services) ===

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
    const filteredPlayers = this.getFilteredPlayers();
    return this.filterService.getSmartSuggestions(filteredPlayers, 3);
  }

  getSearchResultsTitle(count: number): string {
    return this.helperService.getSearchResultsTitle(count, (key) => this.t.t(key));
  }

  getShowAllResultsLabel(count: number): string {
    return this.helperService.getShowAllResultsLabel(count, (key) => this.t.t(key));
  }

  getSuggestionRankLabel(rank: number): string {
    return this.helperService.getSuggestionRankLabel(rank, (key) => this.t.t(key));
  }

  getSlotsRemainingLabel(): string {
    const remaining = this.getRemainingSlots();
    return this.helperService.getSlotsRemainingLabel(remaining, (key) => this.t.t(key));
  }

  getWaitingMessage(): string {
    const currentPlayer = this.getCurrentTurnPlayer();
    return this.helperService.getWaitingMessage(currentPlayer?.username || null, (key) => this.t.t(key));
  }

  getCurrentUserTeam(): Player[] {
    return this.progressService.getCurrentUserTeam(this.draftState, this.currentUserId);
  }

  getRemainingSlots(): number {
    const currentTeam = this.getCurrentUserTeam();
    return this.progressService.getRemainingSlots(currentTeam, 5);
  }

  getRegionLabel(region: string): string {
    const key = this.helperService.getRegionLabelKey(region);
    return this.t.t(key, region);
  }

  getTrancheLabel(tranche: string): string {
    return this.helperService.getTrancheLabel(tranche, (key) => this.t.t(key));
  }

  formatTime(seconds: number): string {
    return this.progressService.formatTime(seconds);
  }

  // Méthodes de filtrage (déléguées au service)
  getFilteredPlayers(): Player[] {
    if (!this.draftState) return [];
    return this.filterService.filterPlayers(this.draftState.availablePlayers, {
      selectedRegion: this.selectedRegion,
      selectedTranche: this.selectedTranche,
      searchTerm: this.searchTerm
    });
  }

  getAvailableRegions(): PlayerRegion[] {
    if (!this.draftState) return [];
    return this.filterService.extractUniqueRegions(this.draftState.availablePlayers);
  }

  getAvailableTranches(): string[] {
    if (!this.draftState) return [];
    return this.filterService.extractUniqueTranches(this.draftState.availablePlayers);
  }

  clearFilters(): void {
    this.selectedRegion = 'ALL';
    this.selectedTranche = 'ALL';
    this.searchTerm = '';
  }

  // Méthodes de navigation
  onBack(): void {
    this.router.navigate(['/games']);
  }

  confirmCancel(): void {
    this.cancelDraft();
  }

  // Méthodes utilitaires
  getPlayerById(playerId: string): Player | undefined {
    return this.draftState?.availablePlayers.find((p: Player) => p.id === playerId);
  }

  getRegionQuotas(): { region: PlayerRegion; limit: number }[] {
    if (!this.draftState?.rules?.regionQuotas) return [];
    return Object.entries(this.draftState.rules.regionQuotas)
      .map(([region, limit]) => ({ region: region as PlayerRegion, limit: Number(limit) }));
  }

  // Méthodes privées d'initialisation
  private initializeComponent(): void {
    this.gameId = this.extractGameId();
    if (!this.gameId) {
      this.error = this.t.t('draft.errors.missingGameId');
      return;
    }

    this.loadDraftState();
    this.startAutoRefresh();
    this.getCurrentUser();
  }

  private extractGameId(): string | null {
    return this.route.snapshot.paramMap.get('id');
  }

  private getCurrentUser(): void {
    const user = this.userContextService.getCurrentUser();
    if (user?.id) {
      this.currentUserId = user.id;
      this.logger.debug('DraftComponent: current user loaded', { userId: this.currentUserId });
    } else {
      this.logger.warn('DraftComponent: no user found, using fallback');
      this.currentUserId = 'participant-1'; // Fallback uniquement si aucun user
    }
  }

  // Méthodes privées de gestion des réponses
  private handleDraftStateSuccess(state: DraftBoardState): void {
    this.draftState = state;
    this.isLoading = false;
  }

  private handleDraftStateError(error: any): void {
    this.logger.error('Draft: Failed to load draft state', error);
    const errorMessage = error.message || this.t.t('draft.errors.loadDraftFailed');
    this.error = errorMessage;
    this.isLoading = false;

    if (error.message?.includes('not found')) {
      this.initializeDraft();
    }
  }

  private handlePlayerSelectionSuccess(player: Player): void {
    this.showSuccessMessage(
      this.helperService.formatTemplate(this.t.t('draft.success.playerSelectedWithName'), { player: player.nickname })
    );
    this.refreshDraftState();
    this.isSelectingPlayer = false;
  }

  private handlePlayerSelectionError(): void {
    this.logger.error('Draft: Failed to select player');
    const message = this.t.t('draft.errors.selectPlayerFailed');
    this.error = message;
    this.showErrorMessage(message);
    this.isSelectingPlayer = false;
  }

  // Méthodes privées d'actions
  private initializeDraft(): void {
    if (!this.gameId) return;

    this.draftService.initializeDraft(this.gameId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: this.handleDraftInitializationSuccess.bind(this),
        error: this.handleDraftInitializationError.bind(this)
      });
  }

  private performPlayerSelection(player: Player): void {
    if (!this.gameId) return;

    this.draftService.makePlayerSelection(this.gameId, player.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => this.handlePlayerSelectionSuccess(player),
        error: this.handlePlayerSelectionError.bind(this)
      });
  }

  private performDraftAction(action: 'pause' | 'resume', successMessage: string, errorMessage: string): void {
    const actionMethod = action === 'pause' ?
      this.draftService.pauseDraft(this.gameId!) :
      this.draftService.resumeDraft(this.gameId!);

    actionMethod.pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (success) => {
          if (success) {
            this.showSuccessMessage(successMessage);
            this.refreshDraftState();
          }
        },
        error: (error) => {
          this.logger.error(`Draft: Failed to ${action} draft`, error);
          const resolvedMessage = error.message || errorMessage;
          this.error = resolvedMessage;
          this.showErrorMessage(resolvedMessage);
        }
      });
  }

  private performTimeoutHandling(): void {
    this.draftService.handleTimeouts(this.gameId!)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (picks) => {
          this.showSuccessMessage(
            this.helperService.formatTemplate(this.t.t('draft.success.timeoutsHandled'), { count: picks.length })
          );
          this.refreshDraftState();
        },
        error: (error) => {
          const message = this.t.t('draft.errors.timeoutsFailed');
          this.error = message;
          this.showErrorMessage(message);
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
      if (result === true) {
        this.executeDraftCancellation();
      }
    });
  }

  private executeDraftCancellation(): void {
    this.draftService.cancelDraft(this.gameId!)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (success) => {
          if (success) {
            this.showSuccessMessage(this.t.t('draft.success.draftCancelled'));
            this.onBack();
          }
        },
        error: (error) => {
          const message = this.t.t('draft.errors.cancelFailed');
          this.error = message;
          this.showErrorMessage(message);
        }
      });
  }

  // Méthodes privées de gestion des réponses d'initialisation
  private handleDraftInitializationSuccess(): void {
    this.showSuccessMessage(this.t.t('draft.success.draftInitialized'));
    this.loadDraftState();
  }

  private handleDraftInitializationError(error: any): void {
    const errorMessage = error.message || this.t.t('draft.errors.initializeFailed');
    this.error = errorMessage;
    this.showErrorMessage(errorMessage);
  }


  // Méthodes privées de notification
  private showSuccessMessage(message: string): void {
    this.snackBar.open(message, this.t.t('common.close'), { duration: DRAFT_CONSTANTS.SNACKBAR_DURATION });
  }

  private showErrorMessage(message: string): void {
    this.snackBar.open(message, this.t.t('common.close'), { duration: DRAFT_CONSTANTS.SNACKBAR_DURATION });
  }

  // Méthodes privées de gestion du cycle de vie
  private startAutoRefresh(): void {
    this.refreshInterval = setInterval(() => {
      this.refreshDraftState();
    }, DRAFT_CONSTANTS.AUTO_REFRESH_INTERVAL);
  }

  private stopAutoRefresh(): void {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
      this.refreshInterval = null;
    }
  }

  private cleanup(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.stopAutoRefresh();
  }
}
