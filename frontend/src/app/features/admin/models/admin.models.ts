export interface DashboardSummary {
  totalUsers: number;
  totalGames: number;
  totalTrades: number;
  gamesByStatus: Record<string, number>;
}

export interface SystemHealth {
  status: string;
  uptimeMillis: number;
  databasePool: DatabasePoolInfo;
  disk: DiskInfo;
}

export interface DatabasePoolInfo {
  activeConnections: number;
  idleConnections: number;
  totalConnections: number;
  maxConnections: number;
}

export interface DiskInfo {
  totalSpaceBytes: number;
  freeSpaceBytes: number;
  usagePercent: number;
}

export interface RecentActivity {
  recentGamesCount: number;
  recentTradesCount: number;
  recentUsersCount: number;
  recentGames: ActivityEntry[];
  recentTrades: ActivityEntry[];
}

export interface ActivityEntry {
  id: string;
  name: string;
  status: string;
  createdAt: string;
}

export interface SystemMetrics {
  jvm: JvmInfo;
  http: HttpInfo;
}

export type AlertSeverity = 'INFO' | 'WARNING' | 'CRITICAL';

export interface AdminAlert {
  code: string;
  severity: AlertSeverity;
  title: string;
  message: string;
  currentValue: number;
  thresholdValue: number;
  triggeredAt: string;
}

export interface AlertThresholds {
  httpErrorRatePercent: number;
  heapUsagePercent: number;
  diskUsagePercent: number;
  databaseConnectionUsagePercent: number;
  criticalErrorsLast24Hours: number;
}

export interface GeoDistributionEntry {
  country: string;
  visitCount: number;
}

export interface VisitAnalytics {
  pageViews: number;
  uniqueVisitors: number;
  activeSessions: number;
  averageSessionDurationSeconds: number;
  bounceRatePercent: number;
  topPages: VisitPageView[];
  topNavigationFlows: VisitNavigationFlow[];
  topCountries?: GeoDistributionEntry[];
}

export interface VisitPageView {
  path: string;
  views: number;
}

export interface VisitNavigationFlow {
  fromPath: string;
  toPath: string;
  transitions: number;
}

export interface JvmInfo {
  heapUsedBytes: number;
  heapMaxBytes: number;
  heapUsagePercent: number;
  threadCount: number;
}

export interface HttpInfo {
  totalRequests: number;
  errorRate: number;
}

export interface RealTimeActivePage {
  path: string;
  visitorCount: number;
}

export interface RealTimeAnalytics {
  activeUsersNow: number;
  activeSessionsNow: number;
  activePagesNow: RealTimeActivePage[];
}

export interface DbTableInfo {
  tableName: string;
  entityName: string;
  rowCount: number;
  sizeDescription: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string;
  timestamp: string;
}

export type IdentityStatus = 'UNRESOLVED' | 'RESOLVED' | 'REJECTED';

export interface PlayerIdentityEntry {
  id: string | null;
  playerId: string;
  playerUsername: string;
  playerRegion: string;
  epicId: string | null;
  status: IdentityStatus;
  confidenceScore: number | null;
  resolvedBy: string | null;
  resolvedAt: string | null;
  rejectedAt: string | null;
  rejectionReason: string | null;
  createdAt: string;
}

export interface PipelineCount {
  unresolvedCount: number;
  resolvedCount: number;
}

export interface ResolvePlayerRequest {
  playerId: string;
  epicId: string;
}

export interface RejectPlayerRequest {
  playerId: string;
  reason?: string;
}

export type IncidentType = 'CHEATING' | 'ABUSE' | 'BUG' | 'DISPUTE' | 'OTHER';

export interface IncidentEntry {
  id: string;
  gameId: string;
  gameName: string;
  reporterId: string;
  reporterUsername: string;
  incidentType: IncidentType;
  description: string;
  timestamp: string;
}

export interface IncidentReportRequest {
  incidentType: IncidentType;
  description: string;
}
