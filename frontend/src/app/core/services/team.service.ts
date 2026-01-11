import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserContextService } from './user-context.service';

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

  constructor(private http: HttpClient, private userContextService: UserContextService) {}

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
    if (userId) {
      return this.http.get<TeamDto[]>(`${this.apiUrl}/user/${userId}/season/${season}`);
    }

    const username = this.userContextService.getCurrentUser()?.username || 'Thibaut';
    const params = new HttpParams().set('user', username).set('year', season.toString());
    return this.http.get<TeamDto[]>(`${this.apiUrl}/user`, { params });
  }

  getTeamsByGame(gameId: string): Observable<TeamDto[]> {
    if (!gameId) {
      return this.getAllTeamsForSeason();
    }
    return this.http.get<TeamDto[]>(`${this.apiUrl}/game/${gameId}`);
  }
}
