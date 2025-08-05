import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError, timer } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';

// Interfaces pour les types de données
export interface Draft {
  id: string;
  gameId: string;
  status: 'PENDING' | 'ACTIVE' | 'PAUSED' | 'FINISHED' | 'CANCELLED';
  currentRound: number;
  currentPick: number;
  totalRounds: number;
  createdAt: string;
  updatedAt: string;
  startedAt?: string;
  finishedAt?: string;
}

export interface DraftBoardState {
  draft: Draft;
  participants: GameParticipant[];
  availablePlayers: Player[];
  picks: DraftPick[];
}

export interface GameParticipant {
  id: string;
  gameId: string;
  userId: string;
  username: string;
  draftOrder: number;
  lastSelectionTime: string;
  selectedPlayers: Player[];
  isCurrentTurn: boolean;
  timeRemaining?: number;
}

export interface Player {
  id: string;
  nickname: string;
  username: string;
  region: string;
  tranche: string;
  currentSeason: number;
  selected?: boolean;
}

export interface DraftPick {
  id: string;
  draftId: string;
  participantId: string;
  playerId: string;
  round: number;
  pickNumber: number;
  selectionTime: string;
  timeTakenSeconds?: number;
  autoPick: boolean;
}

export interface DraftHistoryEntry {
  pickId: string;
  participantUsername: string;
  playerNickname: string;
  selectionTime: string;
  autoPick: boolean;
}

export interface DraftStatistics {
  totalPicks: number;
  autoPicks: number;
  manualPicks: number;
  totalParticipants: number;
  picksByRegion: Record<string, number>;
}

export interface DraftInitializeRequest {
  gameId: string;
}

export interface PlayerSelectionRequest {
  playerId: string;
}

@Injectable({
  providedIn: 'root'
})
export class DraftService {
  private readonly apiUrl = `${environment.apiUrl}/drafts`;
  
  // State management avec BehaviorSubject
  private draftStateSubject = new BehaviorSubject<DraftBoardState | null>(null);
  public draftState$ = this.draftStateSubject.asObservable();

  // Auto-refresh timer
  private refreshTimer?: any;

  constructor(private http: HttpClient) {}

  // Méthodes publiques principales
  initializeDraft(gameId: string): Observable<Draft> {
    const request: DraftInitializeRequest = { gameId };
    
    return this.http.post<Draft>(`${this.apiUrl}/initialize`, request)
      .pipe(
        tap(draft => this.handleDraftInitialization(draft, gameId)),
        catchError(this.handleError)
      );
  }

  getDraftBoardState(gameId: string): Observable<DraftBoardState> {
    return this.http.get<DraftBoardState>(`${this.apiUrl}/${gameId}/board-state`)
      .pipe(
        tap(state => this.updateDraftState(state)),
        catchError(this.handleError)
      );
  }

  makePlayerSelection(gameId: string, playerId: string): Observable<DraftPick> {
    const request: PlayerSelectionRequest = { playerId };
    
    return this.http.post<DraftPick>(`${this.apiUrl}/${gameId}/select`, request)
      .pipe(
        tap(() => this.refreshDraftState(gameId)),
        catchError(this.handleError)
      );
  }

  pauseDraft(gameId: string): Observable<boolean> {
    return this.http.post<boolean>(`${this.apiUrl}/${gameId}/pause`, {})
      .pipe(
        tap(() => this.refreshDraftState(gameId)),
        catchError(this.handleError)
      );
  }

  resumeDraft(gameId: string): Observable<boolean> {
    return this.http.post<boolean>(`${this.apiUrl}/${gameId}/resume`, {})
      .pipe(
        tap(() => this.refreshDraftState(gameId)),
        catchError(this.handleError)
      );
  }

  cancelDraft(gameId: string): Observable<boolean> {
    return this.http.post<boolean>(`${this.apiUrl}/${gameId}/cancel`, {})
      .pipe(catchError(this.handleError));
  }

  handleTimeouts(gameId: string): Observable<DraftPick[]> {
    return this.http.post<DraftPick[]>(`${this.apiUrl}/${gameId}/handle-timeouts`, {})
      .pipe(
        tap(() => this.refreshDraftState(gameId)),
        catchError(this.handleError)
      );
  }

  getDraftHistory(gameId: string): Observable<DraftHistoryEntry[]> {
    return this.http.get<DraftHistoryEntry[]>(`${this.apiUrl}/${gameId}/history`)
      .pipe(catchError(this.handleError));
  }

  getDraftStatistics(gameId: string): Observable<DraftStatistics> {
    return this.http.get<DraftStatistics>(`${this.apiUrl}/${gameId}/statistics`)
      .pipe(catchError(this.handleError));
  }

  getAvailablePlayers(gameId: string): Observable<Player[]> {
    return this.http.get<Player[]>(`${this.apiUrl}/${gameId}/available-players`)
      .pipe(catchError(this.handleError));
  }

  getParticipantSelectionOrder(gameId: string): Observable<GameParticipant[]> {
    return this.http.get<GameParticipant[]>(`${this.apiUrl}/${gameId}/participants/order`)
      .pipe(catchError(this.handleError));
  }

  getCurrentParticipant(gameId: string): Observable<GameParticipant> {
    return this.http.get<GameParticipant>(`${this.apiUrl}/${gameId}/current-participant`)
      .pipe(catchError(this.handleError));
  }

  getParticipantSelections(gameId: string, participantId: string): Observable<DraftPick[]> {
    return this.http.get<DraftPick[]>(`${this.apiUrl}/${gameId}/participants/${participantId}/selections`)
      .pipe(catchError(this.handleError));
  }

  // Méthodes de gestion d'état
  startAutoRefresh(gameId: string, intervalMs: number = 5000): void {
    this.stopAutoRefresh();
    this.refreshTimer = timer(0, intervalMs).pipe(
      switchMap(() => this.getDraftBoardState(gameId))
    ).subscribe();
  }

  stopAutoRefresh(): void {
    if (this.refreshTimer) {
      this.refreshTimer.unsubscribe();
      this.refreshTimer = null;
    }
  }

  refreshDraftState(gameId: string): void {
    this.getDraftBoardState(gameId).subscribe();
  }

  getCurrentDraftState(): DraftBoardState | null {
    return this.draftStateSubject.value;
  }

  clearDraftState(): void {
    this.draftStateSubject.next(null);
  }

  // Méthodes utilitaires
  isDraftActive(draft: Draft): boolean {
    return draft.status === 'ACTIVE';
  }

  isDraftPaused(draft: Draft): boolean {
    return draft.status === 'PAUSED';
  }

  isDraftFinished(draft: Draft): boolean {
    return draft.status === 'FINISHED';
  }

  isDraftCancelled(draft: Draft): boolean {
    return draft.status === 'CANCELLED';
  }

  getDraftProgress(draft: Draft): { current: number; total: number; percentage: number } {
    const totalPicks = draft.totalRounds * draft.currentPick;
    const currentPick = (draft.currentRound - 1) * draft.currentPick + draft.currentPick;
    const percentage = Math.round((currentPick / totalPicks) * 100);
    
    return {
      current: currentPick,
      total: totalPicks,
      percentage
    };
  }

  formatTime(seconds: number): string {
    if (!seconds) return '00:00';
    
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    
    if (hours > 0) {
      return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }
    return `${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }

  // Méthodes privées
  private handleDraftInitialization(draft: Draft, gameId: string): void {
    // Démarrer l'auto-refresh après l'initialisation
    this.startAutoRefresh(gameId);
  }

  private updateDraftState(state: DraftBoardState): void {
    this.draftStateSubject.next(state);
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'Une erreur est survenue';
    
    if (error.error instanceof ErrorEvent) {
      // Erreur côté client
      errorMessage = `Erreur: ${error.error.message}`;
    } else {
      // Erreur côté serveur
      if (error.status === 404) {
        errorMessage = 'Draft non trouvé';
      } else if (error.status === 400) {
        errorMessage = error.error?.message || 'Requête invalide';
      } else if (error.status === 403) {
        errorMessage = 'Accès refusé';
      } else if (error.status >= 500) {
        errorMessage = 'Erreur serveur';
      }
    }
    
    console.error('DraftService error:', error);
    return throwError(() => new Error(errorMessage));
  }
} 