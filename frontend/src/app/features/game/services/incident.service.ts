import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { environment } from '../../../../environments/environment';
import { ApiResponse, IncidentEntry, IncidentReportRequest } from '../../admin/models/admin.models';

@Injectable({
  providedIn: 'root'
})
export class IncidentService {
  private readonly apiUrl = `${environment.apiUrl}/api`;

  constructor(private readonly http: HttpClient) {}

  reportIncident(gameId: string, request: IncidentReportRequest): Observable<IncidentEntry> {
    return this.http
      .post<ApiResponse<IncidentEntry>>(`${this.apiUrl}/games/${gameId}/incidents`, request)
      .pipe(map(r => r.data));
  }
}
