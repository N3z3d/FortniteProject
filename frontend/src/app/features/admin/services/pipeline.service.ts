import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  PlayerIdentityEntry,
  PipelineCount,
  ResolvePlayerRequest,
  RejectPlayerRequest
} from '../models/admin.models';

@Injectable({ providedIn: 'root' })
export class PipelineService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/admin/players`;

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
}
