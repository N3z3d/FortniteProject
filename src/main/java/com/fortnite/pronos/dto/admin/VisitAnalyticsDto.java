package com.fortnite.pronos.dto.admin;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitAnalyticsDto {

  private long pageViews;
  private int uniqueVisitors;
  private int activeSessions;
  private double averageSessionDurationSeconds;
  private double bounceRatePercent;
  private List<PageViewDto> topPages;
  private List<NavigationFlowDto> topNavigationFlows;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PageViewDto {
    private String path;
    private long views;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class NavigationFlowDto {
    private String fromPath;
    private String toPath;
    private long transitions;
  }
}
