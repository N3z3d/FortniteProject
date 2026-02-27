import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { environment } from '../../../../environments/environment';

export interface SimultaneousStatusResponse {
  windowId: string;
  submitted: number;
  total: number;
}

@Injectable({ providedIn: 'root' })
export class SimultaneousDraftService {
  private readonly apiUrl = `${environment.apiUrl}/api/draft/simultaneous`;
  private readonly http = inject(HttpClient);

  getStatus(draftId: string): Observable<SimultaneousStatusResponse | null> {
    return this.http.get<SimultaneousStatusResponse>(`${this.apiUrl}/${draftId}/status`).pipe(
      catchError(() => of(null))
    );
  }

  submitSelection(
    draftId: string,
    windowId: string,
    participantId: string,
    playerId: string
  ): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${draftId}/submit`, {
      windowId,
      participantId,
      playerId,
    });
  }
}
