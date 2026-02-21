package com.fortnite.pronos.service.admin;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.dto.admin.AdminAlertDto;
import com.fortnite.pronos.dto.admin.AdminAlertThresholdsDto;
import com.fortnite.pronos.dto.admin.ErrorStatisticsDto;
import com.fortnite.pronos.dto.admin.SystemHealthDto;
import com.fortnite.pronos.dto.admin.SystemMetricsDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminAlertService {

  private static final String STATUS_UP = "UP";

  private final AdminDashboardService adminDashboardService;
  private final ErrorJournalService errorJournalService;

  public List<AdminAlertDto> getActiveAlerts(int hours, AdminAlertThresholdsDto thresholds) {
    AdminAlertThresholdsDto safeThresholds =
        thresholds != null ? thresholds : AdminAlertThresholdsDto.defaults();
    SystemHealthDto health = adminDashboardService.getSystemHealth();
    SystemMetricsDto metrics = adminDashboardService.getSystemMetrics();
    ErrorStatisticsDto errorStats = errorJournalService.getErrorStatistics(hours);
    List<AdminAlertDto> alerts = new ArrayList<>();
    addSystemStatusAlert(health, alerts);
    addHttpErrorRateAlert(metrics, safeThresholds, alerts);
    addHeapUsageAlert(metrics, safeThresholds, alerts);
    addDiskUsageAlert(health, safeThresholds, alerts);
    addDatabasePoolAlert(health, safeThresholds, alerts);
    addCriticalErrorsAlert(errorStats, safeThresholds, alerts);
    return alerts;
  }

  public AdminAlertThresholdsDto getDefaultThresholds() {
    return AdminAlertThresholdsDto.defaults();
  }

  private void addSystemStatusAlert(SystemHealthDto health, List<AdminAlertDto> alerts) {
    if (health == null || STATUS_UP.equalsIgnoreCase(health.getStatus())) {
      return;
    }
    alerts.add(
        buildAlert(
            "SYSTEM_DOWN",
            AdminAlertDto.Severity.CRITICAL,
            "System status is down",
            "Backend health endpoint reports DOWN status.",
            1,
            0));
  }

  private void addHttpErrorRateAlert(
      SystemMetricsDto metrics, AdminAlertThresholdsDto thresholds, List<AdminAlertDto> alerts) {
    double errorRate =
        metrics != null && metrics.getHttp() != null ? metrics.getHttp().getErrorRate() : 0;
    if (errorRate < thresholds.getHttpErrorRatePercent()) {
      return;
    }
    alerts.add(
        buildAlert(
            "HTTP_ERROR_RATE_HIGH",
            AdminAlertDto.Severity.WARNING,
            "HTTP error rate is high",
            "HTTP error rate exceeded configured threshold.",
            errorRate,
            thresholds.getHttpErrorRatePercent()));
  }

  private void addHeapUsageAlert(
      SystemMetricsDto metrics, AdminAlertThresholdsDto thresholds, List<AdminAlertDto> alerts) {
    double heapUsage =
        metrics != null && metrics.getJvm() != null ? metrics.getJvm().getHeapUsagePercent() : 0;
    if (heapUsage < thresholds.getHeapUsagePercent()) {
      return;
    }
    alerts.add(
        buildAlert(
            "HEAP_USAGE_HIGH",
            AdminAlertDto.Severity.WARNING,
            "JVM heap usage is high",
            "Heap usage exceeded configured threshold.",
            heapUsage,
            thresholds.getHeapUsagePercent()));
  }

  private void addDiskUsageAlert(
      SystemHealthDto health, AdminAlertThresholdsDto thresholds, List<AdminAlertDto> alerts) {
    double diskUsage =
        health != null && health.getDisk() != null ? health.getDisk().getUsagePercent() : 0;
    if (diskUsage < thresholds.getDiskUsagePercent()) {
      return;
    }
    alerts.add(
        buildAlert(
            "DISK_USAGE_HIGH",
            AdminAlertDto.Severity.WARNING,
            "Disk usage is high",
            "Disk usage exceeded configured threshold.",
            diskUsage,
            thresholds.getDiskUsagePercent()));
  }

  private void addDatabasePoolAlert(
      SystemHealthDto health, AdminAlertThresholdsDto thresholds, List<AdminAlertDto> alerts) {
    if (health == null || health.getDatabasePool() == null) {
      return;
    }
    int maxConnections = health.getDatabasePool().getMaxConnections();
    if (maxConnections <= 0) {
      return;
    }
    double usagePercent = health.getDatabasePool().getActiveConnections() * 100.0 / maxConnections;
    if (usagePercent < thresholds.getDatabaseConnectionUsagePercent()) {
      return;
    }
    alerts.add(
        buildAlert(
            "DB_POOL_USAGE_HIGH",
            AdminAlertDto.Severity.WARNING,
            "Database pool usage is high",
            "Database active connections exceeded configured threshold.",
            usagePercent,
            thresholds.getDatabaseConnectionUsagePercent()));
  }

  private void addCriticalErrorsAlert(
      ErrorStatisticsDto stats, AdminAlertThresholdsDto thresholds, List<AdminAlertDto> alerts) {
    int serverErrors = countServerErrors(stats);
    if (serverErrors < thresholds.getCriticalErrorsLast24Hours()) {
      return;
    }
    alerts.add(
        buildAlert(
            "CRITICAL_ERRORS_SPIKE",
            AdminAlertDto.Severity.CRITICAL,
            "Critical errors spike detected",
            "Server errors in selected window exceeded configured threshold.",
            serverErrors,
            thresholds.getCriticalErrorsLast24Hours()));
  }

  private int countServerErrors(ErrorStatisticsDto stats) {
    if (stats == null || stats.getErrorsByStatusCode() == null) {
      return 0;
    }
    return stats.getErrorsByStatusCode().entrySet().stream()
        .filter(entry -> entry.getKey() >= 500)
        .mapToInt(entry -> entry.getValue().intValue())
        .sum();
  }

  private AdminAlertDto buildAlert(
      String code,
      AdminAlertDto.Severity severity,
      String title,
      String message,
      double currentValue,
      double thresholdValue) {
    return AdminAlertDto.builder()
        .code(code)
        .severity(severity)
        .title(title)
        .message(message)
        .currentValue(currentValue)
        .thresholdValue(thresholdValue)
        .triggeredAt(LocalDateTime.now())
        .build();
  }
}
