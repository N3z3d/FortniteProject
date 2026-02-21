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

  private double httpErrorRatePercent;
  private double heapUsagePercent;
  private double diskUsagePercent;
  private double databaseConnectionUsagePercent;
  private int criticalErrorsLast24Hours;

  public static AdminAlertThresholdsDto defaults() {
    return AdminAlertThresholdsDto.builder()
        .httpErrorRatePercent(5)
        .heapUsagePercent(85)
        .diskUsagePercent(90)
        .databaseConnectionUsagePercent(80)
        .criticalErrorsLast24Hours(10)
        .build();
  }
}
