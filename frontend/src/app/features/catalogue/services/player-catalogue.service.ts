import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { AvailablePlayer } from '../../draft/models/draft.interface';
import { RankSnapshot } from '../../../shared/components/sparkline-chart/sparkline-chart.component';

export interface PlayerSearchParams {
  region?: string | null;
  tranche?: string | null;
  search?: string | null;
  available?: boolean;
}

@Injectable({ providedIn: 'root' })
export class PlayerCatalogueService {
  private readonly http = inject(HttpClient);
  private readonly playersUrl = `${environment.apiUrl}/players`;

  getPlayers(params: PlayerSearchParams = {}): Observable<AvailablePlayer[]> {
    let httpParams = new HttpParams();
    if (params.region) httpParams = httpParams.set('region', params.region);
    if (params.tranche) httpParams = httpParams.set('tranche', params.tranche);
    if (params.search) httpParams = httpParams.set('search', params.search);
    if (params.available != null) {
      httpParams = httpParams.set('available', String(params.available));
    }
    return this.http.get<AvailablePlayer[]>(`${this.playersUrl}/search`, { params: httpParams }).pipe(
      catchError(() => of([] as AvailablePlayer[]))
    );
  }

  getSparkline(playerId: string, region = 'EU', days = 14): Observable<RankSnapshot[]> {
    const params = new HttpParams().set('region', region).set('days', String(days));
    return this.http
      .get<RankSnapshot[]>(`${this.playersUrl}/${playerId}/sparkline`, { params })
      .pipe(catchError(() => of([] as RankSnapshot[])));
  }
}
