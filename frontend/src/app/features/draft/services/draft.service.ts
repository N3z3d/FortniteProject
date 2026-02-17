import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject, Observable, Subscription, throwError, timer } from 'rxjs';
import { catchError, filter, map, switchMap, tap } from 'rxjs/operators';

import { environment } from '../../../../environments/environment';
import { LoggerService } from '../../../core/services/logger.service';
import { TranslationService } from '../../../core/services/translation.service';
import {
  extractBackendErrorDetails,
  toSafeUserMessage
} from '../../../core/utils/user-facing-error-message.util';
import {
  Draft,
  DraftBoardState,
  DraftHistoryEntry,
  DraftInitializeRequest,
  DraftPick,
  DraftStatistics,
  DraftStatusInfo,
  GameParticipant,
  Player,
  PlayerSelectionRequest
} from '../models/draft.interface';

export type {
  Draft,
  DraftBoardState,
  DraftHistoryEntry,
  DraftInitializeRequest,
  DraftPick,
  DraftStatistics,
  DraftStatusInfo,
  GameParticipant,
  Player,
  PlayerSelectionRequest
} from '../models/draft.interface';

@Injectable({
  providedIn: 'root'
})
export class DraftService implements OnDestroy {
  private readonly apiUrl = `${environment.apiUrl}/api/drafts`;

  private draftStateSubject = new BehaviorSubject<DraftBoardState | null>(null);
  public draftState$ = this.draftStateSubject.asObservable().pipe(
    filter((state): state is DraftBoardState => state !== null)
  );
  private currentGameIdSubject = new BehaviorSubject<string | null>(null);
  public currentGameId$ = this.currentGameIdSubject.asObservable();

  private refreshTimer: Subscription | null = null;

  constructor(
    private readonly http: HttpClient,
    private readonly t: TranslationService,
    private readonly logger: LoggerService
  ) { }

  ngOnDestroy(): void {
    this.stopAutoRefresh();
    this.draftStateSubject.complete();
    this.currentGameIdSubject.complete();
  }

  initializeDraft(gameId: string): Observable<Draft> {
    const request: DraftInitializeRequest = { gameId };

    return this.http.post<Draft>(`${this.apiUrl}/initialize`, request).pipe(
      tap(draft => this.handleDraftInitialization(gameId)),
      catchError(this.handleError.bind(this))
    );
  }

  getDraftBoardState(gameId: string): Observable<DraftBoardState> {
    return this.http.get<DraftBoardState>(`${this.apiUrl}/${gameId}/board-state`).pipe(
      tap(state => this.updateDraftState(state)),
      catchError(this.handleError.bind(this))
    );
  }

  makePlayerSelection(gameId: string, playerId: string): Observable<DraftPick> {
    const request: PlayerSelectionRequest = { playerId };

    return this.http.post<DraftPick>(`${this.apiUrl}/${gameId}/select`, request).pipe(
      tap(() => this.refreshDraftState(gameId)),
      catchError(this.handleError.bind(this))
    );
  }

  pauseDraft(gameId: string): Observable<boolean> {
    return this.http.post<boolean>(`${this.apiUrl}/${gameId}/pause`, {}).pipe(
      tap(() => this.refreshDraftState(gameId)),
      catchError(this.handleError.bind(this))
    );
  }

  resumeDraft(gameId: string): Observable<boolean> {
    return this.http.post<boolean>(`${this.apiUrl}/${gameId}/resume`, {}).pipe(
      tap(() => this.refreshDraftState(gameId)),
      catchError(this.handleError.bind(this))
    );
  }

  cancelDraft(gameId: string): Observable<boolean> {
    return this.http.post<boolean>(`${this.apiUrl}/${gameId}/cancel`, {}).pipe(
      catchError(this.handleError.bind(this))
    );
  }

  handleTimeouts(gameId: string): Observable<DraftPick[]> {
    return this.http.post<DraftPick[]>(`${this.apiUrl}/${gameId}/handle-timeouts`, {}).pipe(
      tap(() => this.refreshDraftState(gameId)),
      catchError(this.handleError.bind(this))
    );
  }

  getDraftHistory(gameId: string): Observable<DraftHistoryEntry[]> {
    return this.http.get<DraftHistoryEntry[]>(`${this.apiUrl}/${gameId}/history`).pipe(
      catchError(this.handleError.bind(this))
    );
  }

  getDraftStatus(gameId: string): Observable<DraftStatusInfo> {
    return this.http.get<DraftStatusInfo>(`${this.apiUrl}/${gameId}/status`).pipe(
      catchError(this.handleError.bind(this))
    );
  }

  getAllDraftPicks(gameId: string): Observable<DraftPick[]> {
    return this.http.get<DraftPick[]>(`${this.apiUrl}/${gameId}/picks`).pipe(
      catchError(this.handleError.bind(this))
    );
  }

  selectPlayer(gameId: string, playerId: string): Observable<boolean> {
    return this.http.post<{ success: boolean }>(`${this.apiUrl}/${gameId}/select`, { playerId }).pipe(
      map(response => response?.success === true),
      catchError(this.handleError.bind(this))
    );
  }

  getDraftStatistics(gameId: string): Observable<DraftStatistics> {
    return this.http.get<DraftStatistics>(`${this.apiUrl}/${gameId}/statistics`).pipe(
      catchError(this.handleError.bind(this))
    );
  }

  getAvailablePlayers(gameId: string, region?: string): Observable<Player[]> {
    const url = region
      ? `${this.apiUrl}/${gameId}/available-players?region=${region}`
      : `${this.apiUrl}/${gameId}/available-players`;

    return this.http.get<Player[]>(url).pipe(
      catchError(this.handleError.bind(this))
    );
  }

  getParticipantSelectionOrder(gameId: string): Observable<GameParticipant[]> {
    return this.http.get<GameParticipant[]>(`${this.apiUrl}/${gameId}/participants/order`).pipe(
      catchError(this.handleError.bind(this))
    );
  }

  getCurrentParticipant(gameId: string): Observable<GameParticipant> {
    return this.http.get<GameParticipant>(`${this.apiUrl}/${gameId}/current-participant`).pipe(
      catchError(this.handleError.bind(this))
    );
  }

  getParticipantSelections(gameId: string, participantId: string): Observable<DraftPick[]> {
    return this.http.get<DraftPick[]>(`${this.apiUrl}/${gameId}/participants/${participantId}/selections`).pipe(
      catchError(this.handleError.bind(this))
    );
  }

  startAutoRefresh(gameId: string, intervalMs = 5000): void {
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
    const totalRounds = draft.totalRounds || 0;
    const currentPickValue = draft.currentPick || 0;
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
    if (!seconds) {
      return '00:00';
    }

    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;

    if (hours > 0) {
      return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }

    return `${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }

  private handleDraftInitialization(gameId: string): void {
    this.currentGameIdSubject.next(gameId);
    this.startAutoRefresh(gameId);
  }

  private updateDraftState(state: DraftBoardState): void {
    this.draftStateSubject.next(state);
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    const safeBackendMessage = toSafeUserMessage(extractBackendErrorDetails(error).message);
    const errorMessage = this.resolveErrorMessage(error, safeBackendMessage);

    this.logger.error('DraftService: request failed', {
      status: error.status,
      message: errorMessage,
      error
    });
    return throwError(() => new Error(errorMessage));
  }

  private resolveErrorMessage(error: HttpErrorResponse, backendMessage: string | null): string {
    if (error.error instanceof ErrorEvent) {
      return `${this.t.t('common.error')}: ${error.error.message}`;
    }

    switch (error.status) {
      case 0:
        return this.t.t('draft.errors.connectionError');
      case 400:
        return backendMessage || this.t.t('draft.errors.invalidSelection');
      case 401:
        return this.t.t('errors.unauthorized');
      case 403:
        return backendMessage || this.t.t('draft.errors.unauthorized');
      case 404:
        return backendMessage || this.t.t('draft.errors.gameNotFound');
      case 500:
      case 502:
      case 503:
      case 504:
        return this.t.t('draft.errors.serverError');
      default:
        return backendMessage || this.t.t('errors.generic');
    }
  }
}
