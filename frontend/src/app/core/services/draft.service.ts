import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Draft {
  id: string;
  season: number;
  status: 'PENDING' | 'ACTIVE' | 'COMPLETED';
  startTime: string;
  endTime: string;
}

export interface DraftPick {
  id: string;
  draft: Draft;
  pickNumber: number;
  playerId: string;
  participant: string;
}

@Injectable({ providedIn: 'root' })
export class DraftService {
  private readonly baseUrl = `${environment.apiUrl}/api/drafts`;

  constructor(private http: HttpClient, private zone: NgZone) {}

  getCurrent(): Observable<Draft> {
    return this.http.get<Draft>(`${this.baseUrl}/current`);
  }

  start(season: number): Observable<Draft> {
    return this.http.post<Draft>(`${this.baseUrl}/start`, undefined, { params: { season } as any });
  }

  pick(draftId: string, playerId: string, participant: string) {
    return this.http.post(`${this.baseUrl}/pick`, undefined, {
      params: { draftId, playerId, participant } as any,
    });
  }

  stream(): Observable<{ event: string; data: any }> {
    const subject = new Subject<{ event: string; data: any }>();
    const es = new EventSource(`${this.baseUrl}/stream`);
    es.addEventListener('START', (e: MessageEvent) => {
      this.zone.run(() => subject.next({ event: 'START', data: JSON.parse(e.data) }));
    });
    es.addEventListener('PICK', (e: MessageEvent) => {
      this.zone.run(() => subject.next({ event: 'PICK', data: JSON.parse(e.data) }));
    });
    es.onerror = () => subject.error('SSE error');
    return subject.asObservable();
  }
} 