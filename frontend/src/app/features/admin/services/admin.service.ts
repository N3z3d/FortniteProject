import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import {
  AdminAuditEntry,
  AdminUserEntry,
  ApiResponse,
  AdminAlert,
  AlertThresholds,
  DashboardSummary,
  DbTableInfo,
  GameSupervisionEntry,
  GameSupervisionStatus,
  IncidentEntry,
  PipelineRegionalStats,
  RealTimeAnalytics,
  RecentActivity,
  ScrapeLogEntry,
  SqlQueryResult,
  SystemHealth,
  SystemMetrics,
  VisitAnalytics
} from '../models/admin.models';
import { ErrorEntry, ErrorStatistics } from '../models/error-journal.models';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly baseUrl = `${environment.apiUrl}/api/admin`;

  constructor(private readonly http: HttpClient) {}

  getDashboardSummary(): Observable<DashboardSummary> {
    return this.http
      .get<ApiResponse<DashboardSummary>>(`${this.baseUrl}/dashboard/summary`)
      .pipe(map(r => r.data));
  }

  getSystemHealth(): Observable<SystemHealth> {
    return this.http
      .get<ApiResponse<SystemHealth>>(`${this.baseUrl}/dashboard/health`)
      .pipe(map(r => r.data));
  }

  getRecentActivity(hours: number = 24): Observable<RecentActivity> {
    return this.http
      .get<ApiResponse<RecentActivity>>(`${this.baseUrl}/dashboard/recent-activity`, {
        params: { hours: hours.toString() }
      })
      .pipe(map(r => r.data));
  }

  getSystemMetrics(): Observable<SystemMetrics> {
    return this.http
      .get<ApiResponse<SystemMetrics>>(`${this.baseUrl}/system/metrics`)
      .pipe(map(r => r.data));
  }

  getVisitAnalytics(hours: number = 24): Observable<VisitAnalytics> {
    return this.http
      .get<ApiResponse<VisitAnalytics>>(`${this.baseUrl}/dashboard/visits`, {
        params: { hours: hours.toString() }
      })
      .pipe(map(r => r.data));
  }

  getRealTimeAnalytics(): Observable<RealTimeAnalytics> {
    return this.http
      .get<ApiResponse<RealTimeAnalytics>>(`${this.baseUrl}/dashboard/realtime`)
      .pipe(map(r => r.data));
  }

  getUsers(): Observable<AdminUserEntry[]> {
    return this.http
      .get<ApiResponse<AdminUserEntry[]>>(`${this.baseUrl}/users`)
      .pipe(map(r => r.data));
  }

  getDatabaseTables(): Observable<DbTableInfo[]> {
    return this.http
      .get<ApiResponse<DbTableInfo[]>>(`${this.baseUrl}/database/tables`)
      .pipe(map(r => r.data));
  }

  getAlerts(hours: number = 24): Observable<AdminAlert[]> {
    return this.http
      .get<ApiResponse<AdminAlert[]>>(`${this.baseUrl}/alerts`, {
        params: { hours: hours.toString() }
      })
      .pipe(map(r => r.data ?? []));
  }

  getAlertThresholds(): Observable<AlertThresholds> {
    return this.http
      .get<ApiResponse<AlertThresholds>>(`${this.baseUrl}/alerts/thresholds`)
      .pipe(map(r => r.data));
  }

  getErrors(limit: number = 50, statusCode?: number, type?: string): Observable<ErrorEntry[]> {
    const params: Record<string, string> = { limit: limit.toString() };
    if (statusCode !== undefined) {
      params['statusCode'] = statusCode.toString();
    }
    if (type) {
      params['type'] = type;
    }
    return this.http
      .get<ApiResponse<ErrorEntry[]>>(`${this.baseUrl}/errors`, { params })
      .pipe(map(r => r.data));
  }

  getErrorStatistics(hours: number = 24): Observable<ErrorStatistics> {
    return this.http
      .get<ApiResponse<ErrorStatistics>>(`${this.baseUrl}/errors/stats`, {
        params: { hours: hours.toString() }
      })
      .pipe(map(r => r.data));
  }

  getErrorDetail(id: string): Observable<ErrorEntry> {
    return this.http
      .get<ApiResponse<ErrorEntry>>(`${this.baseUrl}/errors/${id}`)
      .pipe(map(r => r.data));
  }

  getIncidents(limit: number = 50, gameId?: string): Observable<IncidentEntry[]> {
    const params: Record<string, string> = { limit: limit.toString() };
    if (gameId) {
      params['gameId'] = gameId;
    }
    return this.http
      .get<ApiResponse<IncidentEntry[]>>(`${this.baseUrl}/incidents`, { params })
      .pipe(map(r => r.data));
  }

  getPipelineRegionalStatus(): Observable<PipelineRegionalStats[]> {
    return this.http.get<PipelineRegionalStats[]>(
      `${this.baseUrl}/players/pipeline/regional-status`
    );
  }

  getScrapingLogs(limit: number = 50): Observable<ScrapeLogEntry[]> {
    return this.http.get<ScrapeLogEntry[]>(`${environment.apiUrl}/api/admin/scraping/logs`, {
      params: { limit: limit.toString() }
    });
  }

  getAuditLog(limit: number = 50): Observable<AdminAuditEntry[]> {
    return this.http.get<AdminAuditEntry[]>(`${this.baseUrl}/audit-log`, {
      params: { limit: limit.toString() }
    });
  }

  getGamesSupervision(status?: GameSupervisionStatus): Observable<GameSupervisionEntry[]> {
    const url = `${environment.apiUrl}/api/admin/supervision/games`;
    const params: Record<string, string> = {};
    if (status) {
      params['status'] = status;
    }
    return this.http
      .get<GameSupervisionEntry[]>(url, { params })
      .pipe(catchError(() => of([] as GameSupervisionEntry[])));
  }

  executeSqlQuery(query: string): Observable<SqlQueryResult> {
    return this.http
      .post<ApiResponse<SqlQueryResult>>(`${this.baseUrl}/database/query`, { query })
      .pipe(map(r => r.data));
  }
}
