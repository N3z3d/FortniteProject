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

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string;
  timestamp: string;
}
