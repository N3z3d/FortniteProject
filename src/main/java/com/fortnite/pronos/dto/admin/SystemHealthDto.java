package com.fortnite.pronos.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealthDto {

  private String status;
  private long uptimeMillis;
  private DatabasePoolInfo databasePool;
  private DiskInfo disk;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DatabasePoolInfo {

    private int activeConnections;
    private int idleConnections;
    private int totalConnections;
    private int maxConnections;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DiskInfo {

    private long totalSpaceBytes;
    private long freeSpaceBytes;
    private double usagePercent;
  }
}
