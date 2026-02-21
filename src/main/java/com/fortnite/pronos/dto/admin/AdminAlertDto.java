package com.fortnite.pronos.dto.admin;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAlertDto {

  public enum Severity {
    INFO,
    WARNING,
    CRITICAL
  }

  private String code;
  private Severity severity;
  private String title;
  private String message;
  private double currentValue;
  private double thresholdValue;
  private LocalDateTime triggeredAt;
}
