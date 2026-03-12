import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';

export interface DraftAuditEntry {
  id: string;
  type: 'SWAP_SOLO' | 'TRADE_PROPOSED' | 'TRADE_ACCEPTED' | 'TRADE_REJECTED';
  occurredAt: string;
  participantId: string | null;
  proposerParticipantId: string | null;
  targetParticipantId: string | null;
  playerOutId: string;
  playerInId: string;
}

@Injectable({ providedIn: 'root' })
export class DraftAuditService {
  private readonly http = inject(HttpClient);

  getAudit(gameId: string): Observable<DraftAuditEntry[]> {
    return this.http
      .get<DraftAuditEntry[]>(`${environment.apiUrl}/api/games/${gameId}/draft/audit`)
      .pipe(catchError(() => of([])));
  }
}
