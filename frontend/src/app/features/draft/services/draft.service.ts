import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import {
  BehaviorSubject,
  Observable,
  Subscription,
  forkJoin,
  of,
  throwError,
  timer
} from 'rxjs';
import { catchError, filter, map, switchMap, tap } from 'rxjs/operators';

import { environment } from '../../../../environments/environment';
import { LoggerService } from '../../../core/services/logger.service';
import { TranslationService } from '../../../core/services/translation.service';
import {
  extractBackendErrorDetails,
  toSafeUserMessage
} from '../../../core/utils/user-facing-error-message.util';
import {
  AvailablePlayer,
  Draft,
  DraftBoardState,
  DraftHistoryEntry,
  DraftInitializeRequest,
  DraftPick,
  DraftProgress,
  DraftParticipantInfo,
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

interface CataloguePlayerDto {
  id: string;
  nickname: string;
  region: string;
  tranche: string;
  locked: boolean;
  currentSeason?: number | null;
  prPoints?: number | null;
}

interface SnakeTurnDto {
  draftId: string;
  region: string;
  participantId: string;
  round: number;
  pickNumber: number;
  reversed: boolean;
  expiresAt?: string | null;
}

interface SnakeRecommendationDto {
  id: string;
  nickname: string;
  region: string | null;
  tranche: string | null;
  trancheFloor: number;
}

interface ApiEnvelope<T> {
  success: boolean;
  data: T;
}

interface SnakeBoardPlayerDto {
  playerId: string | null;
  nickname: string;
  region: string;
  tranche: string;
  currentScore: number;
}

interface SnakeBoardParticipantDto {
  participantId: string;
  username: string;
  totalPlayers?: number;
  selectedPlayers?: SnakeBoardPlayerDto[] | null;
}

interface SnakeBoardParticipantUserDto {
  userId: string;
  username: string;
}

interface SnakeBoardGameDetailDto {
  gameId: string;
  regions?: string[];
  tranchesEnabled?: boolean;
  draftInfo?: {
    draftId: string;
    status: string;
    currentRound?: number | null;
    currentPick?: number | null;
    totalRounds?: number | null;
  } | null;
  participants: SnakeBoardParticipantDto[];
}

interface SnakePickRequest {
  playerId: string;
  region: string;
}

const DEFAULT_CURRENT_SEASON = 2025;
const DEFAULT_SNAKE_REGION = 'GLOBAL';
const DEFAULT_SNAKE_PICK_DURATION_SECONDS = 60;
const SNAKE_REGION_STORAGE_KEY_PREFIX = 'draft:snake:region:';

@Injectable({
  providedIn: 'root'
})
export class DraftService implements OnDestroy {
  private readonly apiUrl = `${environment.apiUrl}/api/drafts`;
  private readonly gamesApiUrl = `${environment.apiUrl}/api/games`;
  private readonly playersApiUrl = `${environment.apiUrl}/players`;

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

  getSnakeBoardState(gameId: string, regionHint?: string): Observable<DraftBoardState> {
    return this.loadSnakeGameDetail(gameId).pipe(
      switchMap(detail => {
        const region = this.resolveSnakeBoardRegion(gameId, detail, regionHint);
        return this.loadSnakeTurn(gameId, region).pipe(
          switchMap(turn =>
            forkJoin({
              participantUsers: this.loadSnakeParticipantUsers(gameId),
              availablePlayers: this.loadSnakeCataloguePlayers(),
              recommendation: this.loadSnakeRecommendation(gameId, turn.region),
            }).pipe(
              map(({ participantUsers, availablePlayers, recommendation }) =>
                this.buildSnakeBoardState(
                  gameId,
                  detail,
                  participantUsers,
                  availablePlayers,
                  turn,
                  recommendation
                )
              )
            )
          )
        );
      }),
      tap(state => {
        this.currentGameIdSubject.next(gameId);
        this.persistSnakeRegion(gameId, state.currentRegion);
        this.updateDraftState(state);
      }),
      catchError(this.handleError.bind(this))
    );
  }

  submitSnakePick(
    gameId: string,
    playerId: string,
    region = DEFAULT_SNAKE_REGION
  ): Observable<DraftBoardState> {
    const request: SnakePickRequest = { playerId, region };

    return this.http
      .post<ApiEnvelope<SnakeTurnDto>>(`${this.gamesApiUrl}/${gameId}/draft/snake/pick`, request)
      .pipe(switchMap(response => this.getSnakeBoardState(gameId, response.data.region)));
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

  private loadSnakeGameDetail(gameId: string): Observable<SnakeBoardGameDetailDto> {
    return this.http.get<SnakeBoardGameDetailDto>(`${this.gamesApiUrl}/${gameId}/details`);
  }

  private loadSnakeParticipantUsers(gameId: string): Observable<SnakeBoardParticipantUserDto[]> {
    return this.http.get<SnakeBoardParticipantUserDto[]>(`${this.gamesApiUrl}/${gameId}/participants`);
  }

  private loadSnakeCataloguePlayers(): Observable<AvailablePlayer[]> {
    return this.http
      .get<CataloguePlayerDto[]>(`${this.playersApiUrl}/catalogue`)
      .pipe(map(players => players.map(player => this.mapSnakeCataloguePlayer(player))));
  }

  private loadSnakeTurn(gameId: string, region: string): Observable<SnakeTurnDto> {
    const turnUrl = `${this.gamesApiUrl}/${gameId}/draft/snake/turn?region=${region}`;
    return this.http.get<ApiEnvelope<SnakeTurnDto>>(turnUrl).pipe(
      map(response => response.data),
      catchError((error: HttpErrorResponse) => this.initializeSnakeTurn(gameId, error))
    );
  }

  private loadSnakeRecommendation(
    gameId: string,
    region: string
  ): Observable<SnakeRecommendationDto | null> {
    const recommendUrl = `${this.gamesApiUrl}/${gameId}/draft/snake/recommend?region=${region}`;
    return this.http.get<ApiEnvelope<SnakeRecommendationDto>>(recommendUrl).pipe(
      map(response => response.data ?? null),
      catchError((error: HttpErrorResponse) => {
        if (error.status !== 404) {
          this.logger.warn('DraftService: snake recommendation unavailable', {
            gameId,
            region,
            status: error.status,
          });
        }
        return of(null);
      })
    );
  }

  private initializeSnakeTurn(
    gameId: string,
    error: HttpErrorResponse
  ): Observable<SnakeTurnDto> {
    if (error.status !== 404) {
      return throwError(() => error);
    }

    return this.http
      .post<ApiEnvelope<SnakeTurnDto>>(`${this.gamesApiUrl}/${gameId}/draft/snake/initialize`, {})
      .pipe(map(response => response.data));
  }

  private buildSnakeBoardState(
    gameId: string,
    detail: SnakeBoardGameDetailDto,
    participantUsers: SnakeBoardParticipantUserDto[],
    availablePlayers: AvailablePlayer[],
    turn: SnakeTurnDto,
    recommendation: SnakeRecommendationDto | null
  ): DraftBoardState {
    const selectedPlayers = this.collectSelectedPlayers(detail.participants);
    const currentParticipantId = this.resolveCurrentSnakeParticipantId(
      detail.participants,
      participantUsers,
      turn.participantId
    );
    const currentParticipant = this.findCurrentSnakeParticipant(detail.participants, currentParticipantId);
    const participants = detail.participants.map(participant =>
      this.mapSnakeParticipant(participant, currentParticipant?.id)
    );

    const trancheFloor = this.computeTrancheFloor(turn.round, turn.pickNumber, participants.length);

    return {
      draft: this.buildSnakeDraftSummary(gameId, detail, turn),
      regions: detail.regions ?? [],
      currentRegion: turn.region,
      pickExpiresAt: turn.expiresAt ?? null,
      recommendedPlayerId: recommendation?.id ?? null,
      trancheFloor,
      tranchesEnabled: detail.tranchesEnabled ?? true,
      gameId,
      status: detail.draftInfo?.status ?? 'ACTIVE',
      currentRound: turn.round,
      currentPick: turn.pickNumber,
      participants,
      availablePlayers: availablePlayers.map(player =>
        this.markSnakePlayerAvailability(player, selectedPlayers)
      ),
      selectedPlayers,
      currentParticipant,
      progress: this.buildSnakeProgress(detail, turn, participants.length),
      rules: { timePerPick: DEFAULT_SNAKE_PICK_DURATION_SECONDS, snakeDraft: true },
    };
  }

  private mapSnakeCataloguePlayer(player: CataloguePlayerDto): AvailablePlayer {
    return {
      id: player.id,
      username: player.nickname,
      nickname: player.nickname,
      region: player.region,
      tranche: player.tranche,
      totalPoints: player.prPoints ?? undefined,
      available: !player.locked,
      currentSeason: player.currentSeason ?? DEFAULT_CURRENT_SEASON,
      selected: false,
    };
  }

  private collectSelectedPlayers(participants: SnakeBoardParticipantDto[]): Player[] {
    return participants.flatMap(participant =>
      (participant.selectedPlayers ?? [])
        .filter((player): player is SnakeBoardPlayerDto & { playerId: string } => !!player.playerId)
        .map(player => ({
          id: player.playerId,
          username: player.nickname,
          nickname: player.nickname,
          region: player.region,
          tranche: player.tranche,
          currentSeason: DEFAULT_CURRENT_SEASON,
          totalPoints: player.currentScore,
          selected: true,
          available: false,
        }))
    );
  }

  private findCurrentSnakeParticipant(
    participants: SnakeBoardParticipantDto[],
    participantId?: string
  ): GameParticipant | undefined {
    const participant = participants.find(entry => entry.participantId === participantId);
    return participant ? this.toSnakeGameParticipant(participant, true) : undefined;
  }

  private resolveCurrentSnakeParticipantId(
    participants: SnakeBoardParticipantDto[],
    participantUsers: SnakeBoardParticipantUserDto[],
    currentUserId: string
  ): string | undefined {
    const currentUser = participantUsers.find(participant => participant.userId === currentUserId);
    if (!currentUser) {
      return undefined;
    }

    return participants.find(participant =>
      this.usernamesMatch(participant.username, currentUser.username)
    )?.participantId;
  }

  private usernamesMatch(left?: string | null, right?: string | null): boolean {
    return !!left && !!right && left.toLowerCase() === right.toLowerCase();
  }

  private mapSnakeParticipant(
    participant: SnakeBoardParticipantDto,
    currentParticipantId?: string
  ): DraftParticipantInfo {
    return {
      participant: this.toSnakeGameParticipant(
        participant,
        participant.participantId === currentParticipantId
      ),
      selections: [],
      isCurrentTurn: participant.participantId === currentParticipantId,
      timeRemaining: null,
      hasTimedOut: false,
    };
  }

  private toSnakeGameParticipant(
    participant: SnakeBoardParticipantDto,
    isCurrentTurn: boolean
  ): GameParticipant {
    const selectedPlayers = (participant.selectedPlayers ?? [])
      .filter((player): player is SnakeBoardPlayerDto & { playerId: string } => !!player.playerId)
      .map(player => ({
        id: player.playerId,
        username: player.nickname,
        nickname: player.nickname,
        region: player.region,
        tranche: player.tranche,
        currentSeason: DEFAULT_CURRENT_SEASON,
        totalPoints: player.currentScore,
        selected: true,
        available: false,
      }));

    return {
      id: participant.participantId,
      username: participant.username,
      selectedPlayers,
      draftOrder: undefined,
      isCurrentTurn,
    };
  }

  private buildSnakeDraftSummary(
    gameId: string,
    detail: SnakeBoardGameDetailDto,
    turn: SnakeTurnDto
  ): DraftBoardState['draft'] {
    return {
      id: detail.draftInfo?.draftId ?? turn.draftId,
      gameId,
      status: detail.draftInfo?.status ?? 'ACTIVE',
      currentRound: turn.round,
      currentPick: turn.pickNumber,
      totalRounds: detail.draftInfo?.totalRounds ?? undefined,
    };
  }

  private markSnakePlayerAvailability(
    player: AvailablePlayer,
    selectedPlayers: Player[]
  ): AvailablePlayer {
    const isSelected = selectedPlayers.some(selectedPlayer => selectedPlayer.id === player.id);
    return {
      ...player,
      selected: isSelected,
      available: player.available !== false && !isSelected,
    };
  }

  /**
   * Computes the tranche floor for the current pick.
   * Formula: trancheSize = 5 + (participants - 2), slot = (round-1)*participants + pick,
   * floor = (slot-1)*trancheSize + 1.
   */
  private computeTrancheFloor(round: number, _pickNumber: number, participantCount: number): number {
    const trancheSize = 5 + Math.max(0, participantCount - 2);
    // Floor is round-based: all participants in the same round share the same floor.
    return (round - 1) * trancheSize + 1;
  }

  private resolveSnakeBoardRegion(
    gameId: string,
    detail: SnakeBoardGameDetailDto,
    regionHint?: string
  ): string {
    const configuredRegions = detail.regions ?? [];
    const persistedRegion = this.readPersistedSnakeRegion(gameId);
    const candidate = regionHint ?? persistedRegion ?? configuredRegions[0] ?? DEFAULT_SNAKE_REGION;
    if (!configuredRegions.length || candidate === DEFAULT_SNAKE_REGION) {
      return candidate;
    }
    return configuredRegions.includes(candidate) ? candidate : configuredRegions[0];
  }

  private readPersistedSnakeRegion(gameId: string): string | undefined {
    try {
      return sessionStorage.getItem(`${SNAKE_REGION_STORAGE_KEY_PREFIX}${gameId}`) ?? undefined;
    } catch {
      return undefined;
    }
  }

  private persistSnakeRegion(gameId: string, region?: string | null): void {
    if (!region) {
      return;
    }
    try {
      sessionStorage.setItem(`${SNAKE_REGION_STORAGE_KEY_PREFIX}${gameId}`, region);
    } catch {
      // Ignore storage failures (private mode, security settings).
    }
  }

  private buildSnakeProgress(
    detail: SnakeBoardGameDetailDto,
    turn: SnakeTurnDto,
    participantCount: number
  ): DraftProgress {
    const totalRounds = Math.max(detail.draftInfo?.totalRounds ?? turn.round, 1);
    const totalPicks = totalRounds * Math.max(participantCount, 1);
    const completedPicks = Math.max(turn.pickNumber - 1, 0);
    const progressPercentage = totalPicks > 0 ? Math.round((completedPicks / totalPicks) * 100) : 0;

    return {
      currentRound: turn.round,
      currentPick: turn.pickNumber,
      totalRounds,
      totalPicks,
      completedPicks,
      progressPercentage,
      estimatedTimeRemaining: null,
    };
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
