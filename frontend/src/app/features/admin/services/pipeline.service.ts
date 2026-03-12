import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  PlayerIdentityEntry,
  PipelineCount,
  PipelineRegionalStats,
  ResolvePlayerRequest,
  RejectPlayerRequest,
  CorrectMetadataRequest,
  ScrapeLogEntry,
  PipelineAlertStatus
} from '../models/admin.models';

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

  getAvailableRegions(): Observable<string[]> {
    return this.http
      .get<string[]>(`${this.metaBaseUrl}/regions`)
      .pipe(catchError(() => of([])));
  }
}
