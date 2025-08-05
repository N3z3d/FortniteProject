import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DashboardStats {
  totalPoints: number;
  pointsByRegion: {
    EU: number;
    NAC: number;
    NAW: number;
    BR: number;
    ASIA: number;
    OCE: number;
    ME: number;
  };
  remainingTrades: number;
  recentMovements: {
    id: string;
    type: 'TRADE' | 'SCORE_UPDATE' | 'TEAM_UPDATE';
    description: string;
    timestamp: string;
  }[];
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private apiUrl = `${environment.apiUrl}/dashboard`;

  constructor(private http: HttpClient) {}

  getStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.apiUrl}/stats`);
  }

  getRecentMovements(): Observable<DashboardStats['recentMovements']> {
    return this.http.get<DashboardStats['recentMovements']>(`${this.apiUrl}/movements`);
  }
} 