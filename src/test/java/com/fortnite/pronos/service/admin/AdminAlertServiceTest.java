package com.fortnite.pronos.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.dto.admin.AdminAlertDto;
import com.fortnite.pronos.dto.admin.AdminAlertThresholdsDto;
import com.fortnite.pronos.dto.admin.ErrorStatisticsDto;
import com.fortnite.pronos.dto.admin.SystemHealthDto;
import com.fortnite.pronos.dto.admin.SystemMetricsDto;

@ExtendWith(MockitoExtension.class)
class AdminAlertServiceTest {

  @Mock private AdminDashboardService adminDashboardService;
  @Mock private ErrorJournalService errorJournalService;

  private AdminAlertService service;

  @BeforeEach
  void setUp() {
    service = new AdminAlertService(adminDashboardService, errorJournalService);
  }

  @Nested
  class GetActiveAlerts {

    @Test
    void shouldReturnCriticalAlertWhenSystemIsDown() {
      when(adminDashboardService.getSystemHealth()).thenReturn(systemHealth("DOWN", 20, 90, 10));
      when(adminDashboardService.getSystemMetrics()).thenReturn(systemMetrics(30, 1.0));
      when(errorJournalService.getErrorStatistics(24)).thenReturn(errorStatistics(1));

      List<AdminAlertDto> alerts = service.getActiveAlerts(24, AdminAlertThresholdsDto.defaults());

      assertThat(alerts).isNotEmpty();
      assertThat(alerts)
          .anySatisfy(
              alert -> {
                assertThat(alert.getCode()).isEqualTo("SYSTEM_DOWN");
                assertThat(alert.getSeverity()).isEqualTo(AdminAlertDto.Severity.CRITICAL);
              });
    }

    @Test
    void shouldReturnWarningWhenHeapUsageIsAboveThreshold() {
      when(adminDashboardService.getSystemHealth()).thenReturn(systemHealth("UP", 20, 30, 10));
      when(adminDashboardService.getSystemMetrics()).thenReturn(systemMetrics(92, 1.0));
      when(errorJournalService.getErrorStatistics(24)).thenReturn(errorStatistics(0));
      AdminAlertThresholdsDto thresholds =
          AdminAlertThresholdsDto.builder()
              .heapUsagePercent(85)
              .httpErrorRatePercent(5)
              .diskUsagePercent(90)
              .databaseConnectionUsagePercent(80)
              .criticalErrorsLast24Hours(10)
              .build();

      List<AdminAlertDto> alerts = service.getActiveAlerts(24, thresholds);

      assertThat(alerts)
          .anySatisfy(
              alert -> {
                assertThat(alert.getCode()).isEqualTo("HEAP_USAGE_HIGH");
                assertThat(alert.getSeverity()).isEqualTo(AdminAlertDto.Severity.WARNING);
              });
    }

    @Test
    void shouldReturnNoAlertsWhenEverythingIsHealthy() {
      when(adminDashboardService.getSystemHealth()).thenReturn(systemHealth("UP", 20, 20, 10));
      when(adminDashboardService.getSystemMetrics()).thenReturn(systemMetrics(30, 1.0));
      when(errorJournalService.getErrorStatistics(24)).thenReturn(errorStatistics(0));

      List<AdminAlertDto> alerts = service.getActiveAlerts(24, AdminAlertThresholdsDto.defaults());

      assertThat(alerts).isEmpty();
    }
  }

  private static SystemHealthDto systemHealth(
      String status, double diskUsagePercent, int activeConnections, int maxConnections) {
    return SystemHealthDto.builder()
        .status(status)
        .disk(SystemHealthDto.DiskInfo.builder().usagePercent(diskUsagePercent).build())
        .databasePool(
            SystemHealthDto.DatabasePoolInfo.builder()
                .activeConnections(activeConnections)
                .maxConnections(maxConnections)
                .build())
        .build();
  }

  private static SystemMetricsDto systemMetrics(double heapUsagePercent, double httpErrorRate) {
    return SystemMetricsDto.builder()
        .jvm(SystemMetricsDto.JvmInfo.builder().heapUsagePercent(heapUsagePercent).build())
        .http(SystemMetricsDto.HttpInfo.builder().errorRate(httpErrorRate).build())
        .build();
  }

  private static ErrorStatisticsDto errorStatistics(int serverErrors) {
    return ErrorStatisticsDto.builder()
        .errorsByStatusCode(java.util.Map.of(500, (long) serverErrors))
        .build();
  }
}
