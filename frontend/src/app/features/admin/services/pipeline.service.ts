import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, of, retry, timer } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  PlayerIdentityEntry,
  EpicIdSuggestion,
  PipelineCount,
  PipelineRegionalStats,
  ResolvePlayerRequest,
  RejectPlayerRequest,
  CorrectMetadataRequest,
  ScrapeLogEntry,
  PipelineAlertStatus,
  AdapterInfo
} from '../models/admin.models';

const SUGGEST_RETRY_COUNT = 3;
// retryCount is 1-indexed (1..SUGGEST_RETRY_COUNT), matching array indices 0..length-1
const SUGGEST_RETRY_DELAYS_MS: readonly [number, number, number] = [1_000, 2_000, 4_000];

export interface SuggestEpicIdRetryCallbacks {
  onRetry?: () => void;
  onRateLimitExhausted?: () => void;
  onResolutionUnavailable?: () => void;
}

@Injectable({ providedIn: 'root' })
export class PipelineService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/admin/players`;
  private readonly scrapeBaseUrl = `${environment.apiUrl}/api/admin/scraping`;
  private readonly metaBaseUrl = `${environment.apiUrl}/api/meta`;

  getUnresolved(): Observable<PlayerIdentityEntry[]> {
    return this.http
      .get<PlayerIdentityEntry[]>(`${this.baseUrl}/unresolved`)
      .pipe(catchError(() => of([])));
  }

  getResolved(): Observable<PlayerIdentityEntry[]> {
    return this.http
      .get<PlayerIdentityEntry[]>(`${this.baseUrl}/resolved`)
      .pipe(catchError(() => of([])));
  }

  getCount(): Observable<PipelineCount> {
    return this.http
      .get<PipelineCount>(`${this.baseUrl}/pipeline/count`)
      .pipe(catchError(() => of({ unresolvedCount: 0, resolvedCount: 0 })));
  }

  resolvePlayer(body: ResolvePlayerRequest): Observable<PlayerIdentityEntry | null> {
    return this.http
      .post<PlayerIdentityEntry>(`${this.baseUrl}/resolve`, body)
      .pipe(catchError(() => of(null)));
  }

  rejectPlayer(body: RejectPlayerRequest): Observable<PlayerIdentityEntry | null> {
    return this.http
      .post<PlayerIdentityEntry>(`${this.baseUrl}/reject`, body)
      .pipe(catchError(() => of(null)));
  }

  getRegionalStatus(): Observable<PipelineRegionalStats[]> {
    return this.http
      .get<PipelineRegionalStats[]>(`${this.baseUrl}/pipeline/regional-status`)
      .pipe(catchError(() => of([])));
  }

  correctMetadata(playerId: string, body: CorrectMetadataRequest): Observable<PlayerIdentityEntry | null> {
    return this.http
      .patch<PlayerIdentityEntry>(`${this.baseUrl}/${playerId}/metadata`, body)
      .pipe(catchError(() => of(null)));
  }

  getScrapeLog(limit = 50): Observable<ScrapeLogEntry[]> {
    return this.http
      .get<ScrapeLogEntry[]>(`${this.scrapeBaseUrl}/logs?limit=${limit}`)
      .pipe(catchError(() => of([])));
  }

  getUnresolvedAlertStatus(): Observable<PipelineAlertStatus | null> {
    return this.http
      .get<PipelineAlertStatus>(`${this.scrapeBaseUrl}/alert`)
      .pipe(catchError(() => of(null)));
  }

  getSuggestedEpicId(
    playerId: string,
    callbacks: SuggestEpicIdRetryCallbacks = {}
  ): Observable<EpicIdSuggestion | null> {
    return this.http
      .get<EpicIdSuggestion>(`${this.baseUrl}/${playerId}/suggest-epic-id`)
      .pipe(
        retry({
          count: SUGGEST_RETRY_COUNT,
          delay: (error: unknown, retryCount: number) => {
            if (!this.isRateLimitError(error)) throw error;
            callbacks.onRetry?.();
            return timer(SUGGEST_RETRY_DELAYS_MS[retryCount - 1]);
          }
        }),
        catchError((error: unknown) => {
          if (this.isRateLimitError(error)) {
            callbacks.onRateLimitExhausted?.();
          } else if (this.isResolutionUnavailableError(error)) {
            callbacks.onResolutionUnavailable?.();
          }
          return of(null);
        })
      );
  }

  private isRateLimitError(error: unknown): error is HttpErrorResponse {
    return error instanceof HttpErrorResponse && error.status === 429;
  }

  private isResolutionUnavailableError(error: unknown): error is HttpErrorResponse {
    return error instanceof HttpErrorResponse && (error.status === 0 || error.status >= 500);
  }

  getAvailableRegions(): Observable<string[]> {
    return this.http
      .get<string[]>(`${this.metaBaseUrl}/regions`)
      .pipe(catchError(() => of([])));
  }

  getAdapterInfo(): Observable<AdapterInfo | null> {
    return this.http
      .get<AdapterInfo>(`${this.baseUrl}/pipeline/adapter-info`)
      .pipe(catchError(() => of(null)));
  }
}
