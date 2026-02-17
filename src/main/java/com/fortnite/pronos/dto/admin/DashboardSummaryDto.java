package com.fortnite.pronos.dto.admin;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {

  private long totalUsers;
  private long totalGames;
  private long totalTrades;
  private Map<String, Long> gamesByStatus;
}
