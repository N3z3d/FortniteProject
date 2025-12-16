import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject, Observable, Subscription, throwError, timer } from 'rxjs';
import { catchError, map, switchMap, tap, filter } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import {
  Draft,
  DraftBoardState,
  GameParticipant,
  Player,
  DraftPick,
  DraftHistoryEntry,
  DraftStatistics,
  DraftInitializeRequest,
  PlayerSelectionRequest,
  DraftStatusInfo
} from '../models/draft.interface';

export type {
  Draft,
  DraftBoardState,
  GameParticipant,
  Player,
  DraftPick,
  DraftHistoryEntry,
  DraftStatistics,
  DraftInitializeRequest,
  PlayerSelectionRequest,
  DraftStatusInfo
} from '../models/draft.interface';

@Injectable({
  providedIn: 'root'
})
export class DraftService implements OnDestroy {
  private readonly apiUrl = `${environment.apiUrl}/api/drafts`;
  
  // State management avec BehaviorSubject
  private draftStateSubject = new BehaviorSubject<DraftBoardState | null>(null);
  public draftState$ = this.draftStateSubject.asObservable().pipe(
    filter((state): state is DraftBoardState => state !== null)
  );
  private currentGameIdSubject = new BehaviorSubject<string | null>(null);
  public currentGameId$ = this.currentGameIdSubject.asObservable();

  // Auto-refresh timer
  private refreshTimer: Subscription | null = null;

  constructor(private http: HttpClient) {}

  ngOnDestroy(): void {
    this.stopAutoRefresh();
    this.draftStateSubject.complete();
    this.currentGameIdSubject.complete();
  }

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

  getDraftStatus(gameId: string): Observable<DraftStatusInfo> {
    return this.http.get<DraftStatusInfo>(`${this.apiUrl}/${gameId}/status`)
      .pipe(catchError(this.handleError));
  }

  getAllDraftPicks(gameId: string): Observable<DraftPick[]> {
    return this.http.get<DraftPick[]>(`${this.apiUrl}/${gameId}/picks`)
      .pipe(catchError(this.handleError));
  }

  selectPlayer(gameId: string, playerId: string): Observable<boolean> {
    return this.http.post<{ success: boolean }>(`${this.apiUrl}/${gameId}/select`, { playerId })
      .pipe(
        map(response => response?.success === true),
        catchError(this.handleError)
      );
  }

  getDraftStatistics(gameId: string): Observable<DraftStatistics> {
    return this.http.get<DraftStatistics>(`${this.apiUrl}/${gameId}/statistics`)
      .pipe(catchError(this.handleError));
  }

  getAvailablePlayers(gameId: string, region?: string): Observable<Player[]> {
    const url = region
      ? `${this.apiUrl}/${gameId}/available-players?region=${region}`
      : `${this.apiUrl}/${gameId}/available-players`;

    return this.http.get<Player[]>(url)
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
    this.refreshTimer?.unsubscribe();
    this.refreshTimer = null;
  }

  refreshDraftState(gameId: string): void {
    this.getDraftBoardState(gameId).subscribe();
  }

  getCurrentDraftState(): DraftBoardState | null {
    return this.draftStateSubject.value;
  }

  clearDraftState(): void {
    this.draftStateSubject.next(null);
    this.currentGameIdSubject.next(null);
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
    const totalRounds = draft.totalRounds ?? 0;
    const currentPickValue = draft.currentPick ?? 0;
    const totalPicks = totalRounds * currentPickValue || 0;
    const currentPick = (draft.currentRound - 1) * currentPickValue + currentPickValue;
    const percentage = totalPicks > 0 ? Math.round((currentPick / totalPicks) * 100) : 0;
    
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
    this.currentGameIdSubject.next(gameId);
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
