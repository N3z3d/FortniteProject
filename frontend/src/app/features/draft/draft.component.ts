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
import {
  REGION_LABELS,
  STATUS_COLORS,
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
    private draftService: DraftService,
    private route: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
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
    const currentParticipant = this.draftState.participants.find(p => p.isCurrentTurn);
    return currentParticipant?.id === this.currentUserId;
  }

  canSelectPlayer(): boolean {
    return this.gameId !== null && 
           this.isCurrentUserTurn() && 
           this.draftState?.draft.status === 'ACTIVE';
  }

  // Méthodes de formatage
  getStatusColor(status: DraftStatus): string {
    return STATUS_COLORS[status] || 'primary';
  }

  getStatusLabel(status: DraftStatus): string {
    return STATUS_LABELS[status] || status;
  }

  // === NOUVELLES MÉTHODES POUR L'INTERFACE SIMPLIFIÉE ===
  
  getDraftProgress(): number {
    if (!this.draftState) return 0;
    const totalPicks = this.draftState.draft.totalRounds * this.draftState.participants.length;
    const currentPick = this.draftState.draft.currentPick - 1;
    return Math.min((currentPick / totalPicks) * 100, 100);
  }

  getDraftProgressText(): string {
    if (!this.draftState) return '';
    return `${this.draftState.draft.currentPick} / ${this.draftState.draft.totalRounds * this.draftState.participants.length}`;
  }

  getCurrentTurnPlayer(): GameParticipant | null {
    if (!this.draftState) return null;
    return this.draftState.participants.find(p => p.isCurrentTurn) || null;
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
    const currentParticipant = this.draftState.participants.find(p => p.id === this.currentUserId);
    return currentParticipant?.selectedPlayers || [];
  }

  getRemainingSlots(): number {
    const currentTeam = this.getCurrentUserTeam();
    return Math.max(0, 5 - currentTeam.length); // 5 joueurs max par équipe
  }

  getRegionLabel(region: string): string {
    return REGION_LABELS[region as PlayerRegion] || region;
  }

  getTrancheLabel(tranche: string): string {
    return `Tranche ${tranche}`;
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
    if (confirm('Êtes-vous sûr de vouloir annuler ce draft ? Cette action est irréversible.')) {
      this.cancelDraft();
    }
  }

  // Méthodes utilitaires
  getPlayerById(playerId: string): Player | undefined {
    return this.draftState?.availablePlayers.find(p => p.id === playerId);
  }

  getRegionQuotas(): { region: PlayerRegion; limit: number }[] {
    // TODO: Implémenter quand l'interface sera corrigée
    return [];
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
    // TODO: Implémenter la récupération de l'utilisateur actuel
    this.currentUserId = 'participant-1'; // Temporaire pour les tests
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
    // TODO: Implémenter la confirmation de suppression
    if (confirm('Êtes-vous sûr de vouloir annuler ce draft ? Cette action est irréversible.')) {
      this.executeDraftCancellation();
    }
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
    const regions = [...new Set(this.draftState.availablePlayers.map(p => p.region))];
    return regions.filter((region): region is PlayerRegion => 
      ['EU', 'NAC', 'BR', 'ASIA', 'OCE', 'ME'].includes(region)
    );
  }

  private extractUniqueTranches(): string[] {
    return [...new Set(this.draftState!.availablePlayers.map(p => p.tranche))];
  }

  // Méthodes privées de notification
  private showSuccessMessage(message: string): void {
    this.snackBar.open(message, 'Fermer', { duration: DRAFT_CONSTANTS.SNACKBAR_DURATION });
  }

  private showErrorMessage(message: string): void {
    this.snackBar.open(message, 'Fermer', { duration: DRAFT_CONSTANTS.SNACKBAR_DURATION });
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
