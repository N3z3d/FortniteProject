import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { AvailablePlayer } from '../../draft/models/draft.interface';
import { RankSnapshot } from '../../../shared/components/sparkline-chart/sparkline-chart.component';

export interface PlayerSearchParams {
  region?: string | null;
  tranche?: string | null;
  search?: string | null;
  available?: boolean;
}

interface CataloguePlayerDto {
  id: string;
  nickname: string;
  region: string;
  tranche: string;
  locked: boolean;
  currentSeason?: number | null;
}

const DEFAULT_CURRENT_SEASON = 2025;

@Injectable({ providedIn: 'root' })
export class PlayerCatalogueService {
  private readonly http = inject(HttpClient);
  private readonly playersUrl = `${environment.apiUrl}/players`;

  getPlayers(params: PlayerSearchParams = {}): Observable<AvailablePlayer[]> {
    return this.loadCataloguePlayers(params).pipe(
      map(players => this.filterPlayers(players, params)),
      catchError(() => of([] as AvailablePlayer[]))
    );
  }

  getSparkline(playerId: string, region = 'EU', days = 14): Observable<RankSnapshot[]> {
    const params = new HttpParams().set('region', region).set('days', String(days));
    return this.http
      .get<RankSnapshot[]>(`${this.playersUrl}/${playerId}/sparkline`, { params })
      .pipe(catchError(() => of([] as RankSnapshot[])));
  }

  private loadCataloguePlayers(
    params: PlayerSearchParams
  ): Observable<AvailablePlayer[]> {
    const searchTerm = params.search?.trim();
    if (searchTerm) {
      return this.http
        .get<CataloguePlayerDto[]>(`${this.playersUrl}/catalogue/search`, {
          params: new HttpParams().set('q', searchTerm),
        })
        .pipe(map(players => this.mapCataloguePlayers(players)));
    }

    return this.http
      .get<CataloguePlayerDto[]>(`${this.playersUrl}/catalogue`, {
        params: this.buildCatalogueParams(params),
      })
      .pipe(map(players => this.mapCataloguePlayers(players)));
  }

  private buildCatalogueParams(params: PlayerSearchParams): HttpParams {
    let httpParams = new HttpParams();
    if (params.region) {
      httpParams = httpParams.set('region', params.region);
    }
    return httpParams;
  }

  private mapCataloguePlayers(
    players: CataloguePlayerDto[]
  ): AvailablePlayer[] {
    return players.map(player => ({
      id: player.id,
      username: player.nickname,
      nickname: player.nickname,
      region: player.region,
      tranche: player.tranche,
      available: !player.locked,
      currentSeason: player.currentSeason ?? DEFAULT_CURRENT_SEASON,
    }));
  }

  private filterPlayers(
    players: AvailablePlayer[],
    params: PlayerSearchParams
  ): AvailablePlayer[] {
    return players.filter(player => {
      const matchesRegion = !params.region || String(player.region) === params.region;
      const matchesTranche =
        !params.tranche || String(player.tranche) === params.tranche;
      const matchesAvailability =
        params.available == null || player.available === params.available;
      return matchesRegion && matchesTranche && matchesAvailability;
    });
  }
}
