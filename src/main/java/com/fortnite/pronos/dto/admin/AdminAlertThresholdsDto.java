package com.fortnite.pronos.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAlertThresholdsDto {

  private static final double DEFAULT_HTTP_ERROR_RATE_PERCENT = 5;
  private static final double DEFAULT_HEAP_USAGE_PERCENT = 85;
  private static final double DEFAULT_DISK_USAGE_PERCENT = 90;
  private static final double DEFAULT_DATABASE_CONNECTION_USAGE_PERCENT = 80;
  private static final int DEFAULT_CRITICAL_ERRORS_LAST_24_HOURS = 10;

  private double httpErrorRatePercent;
  private double heapUsagePercent;
  private double diskUsagePercent;
  private double databaseConnectionUsagePercent;
  private int criticalErrorsLast24Hours;

  public static AdminAlertThresholdsDto defaults() {
    return AdminAlertThresholdsDto.builder()
        .httpErrorRatePercent(DEFAULT_HTTP_ERROR_RATE_PERCENT)
        .heapUsagePercent(DEFAULT_HEAP_USAGE_PERCENT)
        .diskUsagePercent(DEFAULT_DISK_USAGE_PERCENT)
        .databaseConnectionUsagePercent(DEFAULT_DATABASE_CONNECTION_USAGE_PERCENT)
        .criticalErrorsLast24Hours(DEFAULT_CRITICAL_ERRORS_LAST_24_HOURS)
        .build();
  }
}
