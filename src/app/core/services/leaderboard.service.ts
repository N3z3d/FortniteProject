import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LeaderboardEntry {
  teamId: string;
  teamName: string;
  rank: number;
  totalPoints: number;
  ownerId: string;
  ownerUsername: string;
  players?: any[];
}

export interface LeaderboardStats {
  totalTeams: number;
  totalPlayers: number;
  totalPoints: number;
  averagePoints: number;
  regionStats: Record<string, number>;
}

@Injectable({
  providedIn: 'root'
})
export class LeaderboardService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8081/api';

  getLeaderboard(season?: number, region?: string): Observable<LeaderboardEntry[]> {
    let url = `${this.apiUrl}/leaderboard`;
    const params: string[] = [];
    
    if (season) params.push(`season=${season}`);
    if (region) params.push(`region=${region}`);
    
    if (params.length > 0) {
      url += '?' + params.join('&');
    }
    
    return this.http.get<LeaderboardEntry[]>(url);
  }

  getLeaderboardStats(season?: number): Observable<LeaderboardStats> {
    let url = `${this.apiUrl}/leaderboard/stats`;
    if (season) {
      url += `?season=${season}`;
    }
    return this.http.get<LeaderboardStats>(url);
  }

  getPlayerLeaderboard(season?: number): Observable<any[]> {
    let url = `${this.apiUrl}/players/leaderboard`;
    if (season) {
      url += `?season=${season}`;
    }
    return this.http.get<any[]>(url);
  }
} 