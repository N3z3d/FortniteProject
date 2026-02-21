package com.fortnite.pronos.dto.admin;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Aggregated error statistics for the admin error journal. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorStatisticsDto {

  private int totalErrors;
  private Map<String, Long> errorsByType;
  private Map<Integer, Long> errorsByStatusCode;
  private List<TopErrorEntry> topErrors;
  private String trendGranularity;
  private List<TrendPoint> errorTrend;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TopErrorEntry {
    private String type;
    private String message;
    private int count;
    private LocalDateTime lastOccurrence;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TrendPoint {
    private LocalDateTime periodStart;
    private int count;
  }
}
