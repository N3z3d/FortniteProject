package com.fortnite.pronos.dto.admin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AdminAlertThresholdsDto")
class AdminAlertThresholdsDtoTest {

  @Test
  @DisplayName("defaults should expose expected operational thresholds")
  void defaultsShouldExposeExpectedOperationalThresholds() {
    AdminAlertThresholdsDto defaults = AdminAlertThresholdsDto.defaults();

    assertThat(defaults.getHttpErrorRatePercent()).isEqualTo(5);
    assertThat(defaults.getHeapUsagePercent()).isEqualTo(85);
    assertThat(defaults.getDiskUsagePercent()).isEqualTo(90);
    assertThat(defaults.getDatabaseConnectionUsagePercent()).isEqualTo(80);
    assertThat(defaults.getCriticalErrorsLast24Hours()).isEqualTo(10);
  }
}
