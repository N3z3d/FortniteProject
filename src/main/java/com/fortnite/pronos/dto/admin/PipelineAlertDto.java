package com.fortnite.pronos.dto.admin;

import java.time.OffsetDateTime;

public record PipelineAlertDto(
    AlertLevel level,
    long unresolvedCount,
    OffsetDateTime oldestUnresolvedAt,
    long elapsedHours,
    OffsetDateTime checkedAt) {

  public enum AlertLevel {
    NONE,
    WARNING,
    CRITICAL
  }
}
