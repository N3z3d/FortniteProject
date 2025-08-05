import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface TeamDto {
  id: string;
  name: string;
  season: number;
  totalScore: number;
  ownerUsername: string;
  players: TeamPlayerDto[];
  createdAt: string;
  updatedAt: string;
}

export interface TeamPlayerDto {
  playerId: string;
  nickname: string;
  region: string;
  tranche: string;
}

@Injectable({
  providedIn: 'root'
})
export class TeamService {
  private apiUrl = `${environment.apiUrl}/api/teams`;

  constructor(private http: HttpClient) {}

  getTeamForUserAndSeason(userId: string, season: number = 2025): Observable<TeamDto> {
    return this.http.get<TeamDto>(`${this.apiUrl}/user/${userId}/season/${season}`);
  }

  getAllTeamsForSeason(season: number = 2025): Observable<TeamDto[]> {
    const url = `${this.apiUrl}/season/${season}`;
    return this.http.get<TeamDto[]>(url);
  }

  createTeam(userId: string, name: string, season: number = 2025): Observable<TeamDto> {
    return this.http.post<TeamDto>(this.apiUrl, {
      userId,
      name,
      season
    });
  }

  updateTeam(teamId: string, teamDto: Partial<TeamDto>): Observable<TeamDto> {
    return this.http.post<TeamDto>(`${this.apiUrl}/${teamId}`, teamDto);
  }

  deleteTeam(teamId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${teamId}`);
  }

  addPlayerToTeam(userId: string, season: number, playerId: string, position: number): Observable<TeamDto> {
    return this.http.post<TeamDto>(`${this.apiUrl}/user/${userId}/season/${season}/players/add`, {
      playerId,
      position
    });
  }

  getUserTeams(userId?: string, season: number = 2025): Observable<TeamDto[]> {
    const url = userId 
      ? `${this.apiUrl}/user/${userId}` 
      : `${this.apiUrl}/user/current`;
    
    const params = new HttpParams().set('season', season.toString());
    return this.http.get<TeamDto[]>(url, { params });
  }
} 