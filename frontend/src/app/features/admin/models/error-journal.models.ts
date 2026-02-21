export interface ErrorEntry {
  id: string;
  timestamp: string;
  exceptionType: string;
  message: string;
  statusCode: number;
  errorCode: string;
  path: string;
  stackTrace: string;
}

export interface ErrorStatistics {
  totalErrors: number;
  errorsByType: Record<string, number>;
  errorsByStatusCode: Record<number, number>;
  topErrors: TopErrorEntry[];
  trendGranularity?: 'HOUR' | 'DAY' | string;
  errorTrend?: ErrorTrendPoint[];
}

export interface TopErrorEntry {
  type: string;
  message: string;
  count: number;
  lastOccurrence: string;
}

export interface ErrorTrendPoint {
  periodStart: string;
  count: number;
}
