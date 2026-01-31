import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
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
export class GameDetailComponent implements OnInit {
  public readonly t = inject(TranslationService);

  game: Game | null = null;
  participants: GameParticipant[] = [];
  loading = false;
  error: string | null = null;
  participantsError: string | null = null;
  gameId: string = '';

  constructor(
    private readonly gameDataService: GameDataService,
    private readonly actions: GameDetailActionsService,
    private readonly permissions: GameDetailPermissionsService,
    private readonly ui: GameDetailUIService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) { }

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.gameId = params['id'];
      if (this.gameId) {
        this.loadGameDetails();
      }
    });
  }

  loadGameDetails(): void {
    this.loading = true;
    this.error = null;
    this.participantsError = null;

    this.gameDataService.getGameById(this.gameId).subscribe({
      next: (game: Game) => {
        this.game = game;
        this.loading = false;
        
        // Validation des données reçues
        const validation = this.gameDataService.validateGameData(game);
        if (!validation.isValid) {
          console.warn('Game data validation failed:', validation.errors);
        }

        this.loadParticipants();
      },
      error: (error: Error) => {
        this.error = error.message;
        this.loading = false;
        console.error('Error loading game details:', error);
      }
    });
  }

  loadParticipants(): void {
    this.participantsError = null;
    this.gameDataService.getGameParticipants(this.gameId).subscribe({
      next: (participants: GameParticipant[]) => {
        this.participants = participants;
        this.participantsError = null;
      },
      error: (error: Error) => {
        const message = error.message || 'Erreur lors du chargement des participants';
        this.participantsError = message;
        console.error('Error loading participants:', error);
      }
    });
  }

  retryLoad(): void {
    this.error = null;
    this.participantsError = null;
    this.loadGameDetails();
  }

  startDraft(): void {
    this.actions.startDraft(this.gameId, () => this.loadGameDetails());
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
    this.actions.confirmStartDraft(this.gameId, () => this.loadGameDetails());
  }

  copyInvitationCode(): void {
    if (!this.game?.invitationCode) return;
    this.actions.copyInvitationCode(this.game.invitationCode);
  }

  regenerateInvitationCode(duration: '24h' | '48h' | '7d' | 'permanent' = 'permanent'): void {
    if (!this.game) return;
    this.actions.regenerateInvitationCode(this.gameId, duration, (updatedGame) => {
      if (this.game) {
        this.game.invitationCode = updatedGame.invitationCode;
        this.game.invitationCodeExpiresAt = updatedGame.invitationCodeExpiresAt;
        this.game.isInvitationCodeExpired = updatedGame.isInvitationCodeExpired;
      }
    });
  }

  promptRegenerateCode(): void {
    this.actions.promptRegenerateCode(this.gameId, (updatedGame) => {
      if (this.game) {
        this.game.invitationCode = updatedGame.invitationCode;
        this.game.invitationCodeExpiresAt = updatedGame.invitationCodeExpiresAt;
        this.game.isInvitationCodeExpired = updatedGame.isInvitationCodeExpired;
      }
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
} 
