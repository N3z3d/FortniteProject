package com.fortnite.pronos.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemMetricsDto {

  private JvmInfo jvm;
  private HttpInfo http;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class JvmInfo {

    private long heapUsedBytes;
    private long heapMaxBytes;
    private double heapUsagePercent;
    private int threadCount;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class HttpInfo {

    private double totalRequests;
    private double errorRate;
  }
}
