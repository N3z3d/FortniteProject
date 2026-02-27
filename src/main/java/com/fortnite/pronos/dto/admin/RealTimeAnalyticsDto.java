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
public class RealTimeAnalyticsDto {

  private int activeUsersNow;
  private int activeSessionsNow;
  private List<ActivePageDto> activePagesNow;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ActivePageDto {
    private String path;
    private int visitorCount;
  }
}
