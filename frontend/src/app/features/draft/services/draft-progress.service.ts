import { Injectable } from '@angular/core';
import { DraftBoardState, GameParticipant, Player } from './draft.service';

/**
 * Service responsible for calculating draft progress, timer, and roster information.
 * Provides methods to track draft advancement and team composition.
 */
@Injectable({
  providedIn: 'root'
})
export class DraftProgressService {

  /**
   * Calculates draft progress as percentage (0-100)
   */
  calculateProgress(state: DraftBoardState | null): number {
    if (!state?.draft) return 0;

    const totalRounds = state.draft.totalRounds || 0;
    const currentPickValue = state.draft.currentPick || 0;
    const totalPicks = totalRounds * state.participants.length;
    const currentPick = currentPickValue > 0 ? currentPickValue - 1 : 0;

    return Math.min((currentPick / totalPicks) * 100, 100);
  }

  /**
   * Gets progress text (e.g., "5 / 20")
   */
  getProgressText(state: DraftBoardState | null): string {
    if (!state?.draft) return '';

    const totalRounds = state.draft.totalRounds || 0;
    const currentPickValue = state.draft.currentPick || 0;
    const totalPicks = totalRounds * state.participants.length;

    return `${currentPickValue} / ${totalPicks}`;
  }

  /**
   * Formats time in MM:SS format
   */
  formatTime(seconds: number): string {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
  }

  /**
   * Gets current user's team (selected players)
   */
  getCurrentUserTeam(state: DraftBoardState | null, currentUserId: string | null): Player[] {
    if (!state || !currentUserId) return [];

    const currentParticipant = state.participants.find((p: any) => {
      const normalized = this.normalizeParticipant(p);
      return normalized?.id === currentUserId;
    });

    return this.normalizeParticipant(currentParticipant)?.selectedPlayers || [];
  }

  /**
   * Calculates remaining roster slots for current user
   */
  getRemainingSlots(currentTeam: Player[], maxSlots: number = 5): number {
    return Math.max(0, maxSlots - currentTeam.length);
  }

  /**
   * Gets current turn player
   */
  getCurrentTurnPlayer(state: DraftBoardState | null): GameParticipant | null {
    if (!state) return null;

    const entry = state.participants.find((p: any) =>
      (p as any).isCurrentTurn || (p as any).participant?.isCurrentTurn
    );

    return this.normalizeParticipant(entry) || null;
  }

  /**
   * Checks if it's the current user's turn
   */
  isCurrentUserTurn(state: DraftBoardState | null, currentUserId: string | null): boolean {
    if (!state || !currentUserId) return false;

    const currentParticipant = state.participants.find((p: any) =>
      (p as any).isCurrentTurn || (p as any).participant?.isCurrentTurn
    );

    const participant = this.normalizeParticipant(currentParticipant);
    return participant?.id === currentUserId;
  }

  /**
   * Normalizes participant from different API response formats
   */
  private normalizeParticipant(entry: any): GameParticipant | null {
    if (!entry) return null;
    return (entry as any).participant ? (entry as any).participant : (entry as GameParticipant);
  }
}
