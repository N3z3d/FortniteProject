package com.fortnite.pronos.controller;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fortnite.pronos.dto.admin.AdminAlertDto;
import com.fortnite.pronos.dto.admin.AdminAlertThresholdsDto;
import com.fortnite.pronos.dto.admin.AdminUserDto;
import com.fortnite.pronos.dto.admin.DashboardSummaryDto;
import com.fortnite.pronos.dto.admin.RealTimeAnalyticsDto;
import com.fortnite.pronos.dto.admin.RecentActivityDto;
import com.fortnite.pronos.dto.admin.SystemHealthDto;
import com.fortnite.pronos.dto.admin.SystemMetricsDto;
import com.fortnite.pronos.dto.admin.VisitAnalyticsDto;
import com.fortnite.pronos.dto.common.ApiResponse;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.service.admin.AdminAlertService;
import com.fortnite.pronos.service.admin.AdminDashboardService;
import com.fortnite.pronos.service.admin.AdminVisitAnalyticsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@Validated
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

  private static final int MIN_HOURS = 1;
  private static final int MAX_HOURS_WINDOW = 168; // 7 days × 24 hours
  private static final int MIN_RECENT_ACTIVITY_HOURS = MIN_HOURS;
  private static final int MAX_RECENT_ACTIVITY_HOURS = MAX_HOURS_WINDOW;
  private static final int MIN_ALERT_HOURS = MIN_HOURS;
  private static final int MAX_ALERT_HOURS = MAX_HOURS_WINDOW;
  private static final int MIN_VISIT_ANALYTICS_HOURS = MIN_HOURS;
  private static final int MAX_VISIT_ANALYTICS_HOURS = MAX_HOURS_WINDOW;
  private static final String HOURS_VALIDATION_ERROR_MESSAGE =
      "Le parametre hours doit etre entre 1 et 168";
  private static final String VALIDATION_ERROR_CODE = "VALIDATION_ERROR";

  private final AdminDashboardService adminDashboardService;
  private final AdminAlertService adminAlertService;
  private final AdminVisitAnalyticsService adminVisitAnalyticsService;

  @GetMapping("/dashboard/summary")
  public ResponseEntity<ApiResponse<DashboardSummaryDto>> getDashboardSummary() {
    log.info("Admin: fetching dashboard summary");
    return ResponseEntity.ok(ApiResponse.success(adminDashboardService.getDashboardSummary()));
  }

  @GetMapping("/dashboard/health")
  public ResponseEntity<ApiResponse<SystemHealthDto>> getSystemHealth() {
    log.info("Admin: fetching system health");
    return ResponseEntity.ok(ApiResponse.success(adminDashboardService.getSystemHealth()));
  }

  @GetMapping("/dashboard/recent-activity")
  public ResponseEntity<ApiResponse<RecentActivityDto>> getRecentActivity(
      @RequestParam(defaultValue = "24")
          @Min(value = MIN_RECENT_ACTIVITY_HOURS, message = "hours doit etre >= 1")
          @Max(value = MAX_RECENT_ACTIVITY_HOURS, message = "hours doit etre <= 168")
          int hours) {
    if (hours < MIN_RECENT_ACTIVITY_HOURS || hours > MAX_RECENT_ACTIVITY_HOURS) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(HOURS_VALIDATION_ERROR_MESSAGE, VALIDATION_ERROR_CODE));
    }
    log.info("Admin: fetching recent activity for last {} hours", hours);
    return ResponseEntity.ok(ApiResponse.success(adminDashboardService.getRecentActivity(hours)));
  }

  @GetMapping("/users")
  public ResponseEntity<ApiResponse<List<AdminUserDto>>> getAllUsers() {
    log.info("Admin: fetching all users");
    return ResponseEntity.ok(ApiResponse.success(adminDashboardService.getAllUsers()));
  }

  @GetMapping("/games")
  public ResponseEntity<ApiResponse<List<Game>>> getAllGames(
      @RequestParam(required = false) String status) {
    log.info("Admin: fetching all games (filter: {})", status);
    return ResponseEntity.ok(ApiResponse.success(adminDashboardService.getAllGames(status)));
  }

  @GetMapping("/system/metrics")
  public ResponseEntity<ApiResponse<SystemMetricsDto>> getSystemMetrics() {
    log.info("Admin: fetching system metrics");
    return ResponseEntity.ok(ApiResponse.success(adminDashboardService.getSystemMetrics()));
  }

  @GetMapping("/dashboard/realtime")
  public ResponseEntity<ApiResponse<RealTimeAnalyticsDto>> getRealTimeAnalytics() {
    log.info("Admin: fetching real-time analytics snapshot");
    return ResponseEntity.ok(ApiResponse.success(adminVisitAnalyticsService.getRealTimeSnapshot()));
  }

  @GetMapping("/dashboard/visits")
  public ResponseEntity<ApiResponse<VisitAnalyticsDto>> getVisitAnalytics(
      @RequestParam(defaultValue = "24") int hours) {
    if (hours < MIN_VISIT_ANALYTICS_HOURS || hours > MAX_VISIT_ANALYTICS_HOURS) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(HOURS_VALIDATION_ERROR_MESSAGE, VALIDATION_ERROR_CODE));
    }
    return ResponseEntity.ok(
        ApiResponse.success(adminVisitAnalyticsService.getVisitAnalytics(hours)));
  }

  @GetMapping("/alerts")
  public ResponseEntity<ApiResponse<List<AdminAlertDto>>> getAlerts(
      @RequestParam(defaultValue = "24") int hours,
      @RequestParam(defaultValue = "5") double httpErrorRateThreshold,
      @RequestParam(defaultValue = "85") double heapUsageThreshold,
      @RequestParam(defaultValue = "90") double diskUsageThreshold,
      @RequestParam(defaultValue = "80") double dbPoolUsageThreshold,
      @RequestParam(defaultValue = "10") int criticalErrorsThreshold) {
    if (hours < MIN_ALERT_HOURS || hours > MAX_ALERT_HOURS) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(HOURS_VALIDATION_ERROR_MESSAGE, VALIDATION_ERROR_CODE));
    }
    AdminAlertThresholdsDto thresholds =
        AdminAlertThresholdsDto.builder()
            .httpErrorRatePercent(httpErrorRateThreshold)
            .heapUsagePercent(heapUsageThreshold)
            .diskUsagePercent(diskUsageThreshold)
            .databaseConnectionUsagePercent(dbPoolUsageThreshold)
            .criticalErrorsLast24Hours(criticalErrorsThreshold)
            .build();
    return ResponseEntity.ok(
        ApiResponse.success(adminAlertService.getActiveAlerts(hours, thresholds)));
  }

  @GetMapping("/alerts/thresholds")
  public ResponseEntity<ApiResponse<AdminAlertThresholdsDto>> getAlertThresholdDefaults() {
    return ResponseEntity.ok(ApiResponse.success(adminAlertService.getDefaultThresholds()));
  }
}
