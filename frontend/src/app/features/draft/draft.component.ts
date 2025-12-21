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
import { DraftStatus, PlayerRegion } from './models/draft.interface';
import { LoggerService } from '../../core/services/logger.service';
import { UserContextService } from '../../core/services/user-context.service';
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
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly snackBar: MatSnackBar,
    private readonly dialog: MatDialog,
    private readonly logger: LoggerService,
    private readonly userContextService: UserContextService
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

    this.setLoadingState(true);
    this.clearError();

    this.draftService.getDraftBoardState(this.gameId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: this.handleDraftStateSuccess.bind(this),
        error: this.handleDraftStateError.bind(this)
      });
  }

  selectPlayer(player: Player): void {
    if (!this.canSelectPlayer()) return;

    this.setSelectingState(true);
    this.performPlayerSelection(player);
  }

  pauseDraft(): void {
    if (!this.gameId) return;
    this.performDraftAction('pause', 'Draft mis en pause');
  }

  resumeDraft(): void {
    if (!this.gameId) return;
    this.performDraftAction('resume', 'Draft repris');
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
    if (!this.draftState || !this.currentUserId) return false;
    const currentParticipant = this.draftState.participants.find((p: any) =>
      (p as any).isCurrentTurn || (p as any).participant?.isCurrentTurn
    );
    const participant = this.normalizeParticipant(currentParticipant);
    return participant?.id === this.currentUserId;
  }

  canSelectPlayer(): boolean {
    const status = this.draftState?.draft?.status ?? this.draftState?.status;
    return this.gameId !== null && 
           !this.isSelectingPlayer &&
           this.isCurrentUserTurn() && 
           status === 'ACTIVE';
  }

  // Méthodes de formatage
  getStatusColor(status: DraftStatus | string): string {
    switch (status) {
      case 'ACTIVE':
      case 'IN_PROGRESS':
        return 'accent';
      case 'PAUSED':
      case 'CANCELLED':
      case 'ERROR':
        return 'warn';
      default:
        return 'primary';
    }
  }

  getStatusLabel(status: DraftStatus | string): string {
    return STATUS_LABELS[status] || status;
  }

  // === NOUVELLES MÉTHODES POUR L'INTERFACE SIMPLIFIÉE ===
  
  getDraftProgress(): number {
    if (!this.draftState?.draft) return 0;
    const totalRounds = this.draftState.draft.totalRounds || 0;
    const currentPickValue = this.draftState.draft.currentPick || 0;
    const totalPicks = totalRounds * this.draftState.participants.length;
    const currentPick = currentPickValue > 0 ? currentPickValue - 1 : 0;
    return Math.min((currentPick / totalPicks) * 100, 100);
  }

  getDraftProgressText(): string {
    if (!this.draftState?.draft) return '';
    const totalRounds = this.draftState.draft.totalRounds || 0;
    const currentPickValue = this.draftState.draft.currentPick || 0;
    return `${currentPickValue} / ${totalRounds * this.draftState.participants.length}`;
  }

  getCurrentTurnPlayer(): GameParticipant | null {
    if (!this.draftState) return null;
    const entry = this.draftState.participants.find((p: any) =>
      (p as any).isCurrentTurn || (p as any).participant?.isCurrentTurn
    );
    return this.normalizeParticipant(entry) || null;
  }

  getSmartSuggestions(): any[] {
    if (!this.draftState || this.searchTerm) return [];
    
    // Retourne les top 3 joueurs disponibles avec scoring simulé
    const availablePlayers = this.getFilteredPlayers().slice(0, 3);
    return availablePlayers.map((player, index) => ({
      player,
      rank: index + 1,
      score: Math.floor(Math.random() * 1000) + 500 // Score simulé
    }));
  }

  getWaitingMessage(): string {
    const currentPlayer = this.getCurrentTurnPlayer();
    if (!currentPlayer) return 'En attente du prochain tour';
    return `C'est au tour de ${currentPlayer.username}`;
  }

  getCurrentUserTeam(): Player[] {
    if (!this.draftState || !this.currentUserId) return [];
    const currentParticipant = this.draftState.participants.find((p: any) =>
      this.normalizeParticipant(p)?.id === this.currentUserId
    );
    return this.normalizeParticipant(currentParticipant)?.selectedPlayers || [];
  }

  getRemainingSlots(): number {
    const currentTeam = this.getCurrentUserTeam();
    return Math.max(0, 5 - currentTeam.length); // 5 joueurs max par équipe
  }

  getRegionLabel(region: string): string {
    return REGION_LABELS[region as PlayerRegion] || region;
  }

  getTrancheLabel(tranche: string): string {
    const match = /^T(\d+)$/i.exec(tranche);
    if (match) {
      return `Tranche ${match[1]}`;
    }
    return tranche;
  }

  formatTime(seconds: number): string {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
  }

  // Méthodes de filtrage
  getFilteredPlayers(): Player[] {
    if (!this.draftState) return [];

    return this.draftState.availablePlayers
      .filter(this.filterByRegion.bind(this))
      .filter(this.filterByTranche.bind(this))
      .filter(this.filterBySearch.bind(this));
  }

  getAvailableRegions(): PlayerRegion[] {
    if (!this.draftState) return [];
    return this.extractUniqueRegions();
  }

  getAvailableTranches(): string[] {
    if (!this.draftState) return [];
    return this.extractUniqueTranches();
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
      this.setError('ID de game manquant');
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

  // Méthodes privées de gestion d'état
  private setLoadingState(loading: boolean): void {
    this.isLoading = loading;
  }

  private setSelectingState(selecting: boolean): void {
    this.isSelectingPlayer = selecting;
  }

  private setError(message: string): void {
    this.error = message;
  }

  private clearError(): void {
    this.error = null;
  }

  // Méthodes privées de gestion des réponses
  private handleDraftStateSuccess(state: DraftBoardState): void {
    this.draftState = state;
    this.setLoadingState(false);
  }

  private handleDraftStateError(error: any): void {
    this.logger.error('Draft: Failed to load draft state', error);
    const errorMessage = error.message || 'Erreur lors du chargement du draft';
    this.setError(errorMessage);
    this.setLoadingState(false);
    
    if (error.message?.includes('not found')) {
      this.initializeDraft();
    }
  }

  private handlePlayerSelectionSuccess(player: Player): void {
    this.showSuccessMessage(`Joueur ${player.nickname} sélectionné!`);
    this.refreshDraftState();
    this.setSelectingState(false);
  }

  private handlePlayerSelectionError(): void {
    this.logger.error('Draft: Failed to select player');
    this.setError('Erreur lors de la sélection du joueur');
    this.showErrorMessage('Erreur lors de la sélection du joueur');
    this.setSelectingState(false);
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

  private performDraftAction(action: 'pause' | 'resume', successMessage: string): void {
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
          const errorMessage = error.message || `Erreur lors de la ${action}`;
          this.setError(errorMessage);
          this.showErrorMessage(`Erreur lors de la ${action}`);
        }
      });
  }

  private performTimeoutHandling(): void {
    this.draftService.handleTimeouts(this.gameId!)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (picks) => {
          this.showSuccessMessage(`${picks.length} timeouts gérés`);
          this.refreshDraftState();
        },
        error: (error) => {
          this.setError('Erreur lors de la gestion des timeouts');
          this.showErrorMessage('Erreur lors de la gestion des timeouts');
        }
      });
  }

  private showCancelConfirmation(): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: {
        title: 'Annuler le draft',
        message: 'Êtes-vous sûr de vouloir annuler ce draft ? Cette action est irréversible.',
        confirmText: 'Annuler le draft',
        cancelText: 'Retour',
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
            this.showSuccessMessage('Draft annulé');
            this.onBack();
          }
        },
        error: (error) => {
          this.setError('Erreur lors de l\'annulation');
          this.showErrorMessage('Erreur lors de l\'annulation');
        }
      });
  }

  // Méthodes privées de gestion des réponses d'initialisation
  private handleDraftInitializationSuccess(): void {
    this.showSuccessMessage('Draft initialisé avec succès!');
    this.loadDraftState();
  }

  private handleDraftInitializationError(error: any): void {
    const errorMessage = error.message || 'Erreur lors de l\'initialisation du draft';
    this.setError(errorMessage);
    this.showErrorMessage('Erreur lors de l\'initialisation du draft');
  }

  // Méthodes privées de filtrage
  private filterByRegion(player: Player): boolean {
    return this.selectedRegion === 'ALL' || player.region === this.selectedRegion;
  }

  private filterByTranche(player: Player): boolean {
    return this.selectedTranche === 'ALL' || player.tranche === this.selectedTranche;
  }

  private filterBySearch(player: Player): boolean {
    if (!this.searchTerm.trim()) return true;
    
    const searchLower = this.searchTerm.toLowerCase();
    return player.nickname.toLowerCase().includes(searchLower) ||
           player.username.toLowerCase().includes(searchLower);
  }

  private extractUniqueRegions(): PlayerRegion[] {
    if (!this.draftState) return [];
    const regions = [...new Set(this.draftState.availablePlayers.map((p: Player) => p.region))];
    return regions.filter((region): region is PlayerRegion => typeof region === 'string');
  }

  private extractUniqueTranches(): string[] {
    return this.draftState
      ? [...new Set(this.draftState.availablePlayers.map((p: Player) => p.tranche))]
      : [];
  }

  // Méthodes privées de notification
  private showSuccessMessage(message: string): void {
    this.snackBar.open(message, 'Fermer', { duration: DRAFT_CONSTANTS.SNACKBAR_DURATION });
  }

  private showErrorMessage(message: string): void {
    this.snackBar.open(message, 'Fermer', { duration: DRAFT_CONSTANTS.SNACKBAR_DURATION });
  }

  private normalizeParticipant(entry: any): GameParticipant | null {
    if (!entry) return null;
    return (entry as any).participant ? (entry as any).participant : (entry as GameParticipant);
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
