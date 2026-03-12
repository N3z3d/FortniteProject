package com.fortnite.pronos.service.admin;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.domain.port.out.TradeRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.dto.admin.AdminUserDto;
import com.fortnite.pronos.dto.admin.DashboardSummaryDto;
import com.fortnite.pronos.dto.admin.RecentActivityDto;
import com.fortnite.pronos.dto.admin.SystemHealthDto;
import com.fortnite.pronos.dto.admin.SystemMetricsDto;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

  private final UserRepositoryPort userRepository;
  private final GameRepositoryPort gameRepository;
  private final TradeRepositoryPort tradeRepository;
  private final AdminSystemMetricsService systemMetricsService;
  private final AdminRecentActivityService recentActivityService;
  private final AdminGameCatalogService gameCatalogService;

  public DashboardSummaryDto getDashboardSummary() {
    Map<String, Long> gamesByStatus = new LinkedHashMap<>();
    Arrays.stream(GameStatus.values())
        .forEach(s -> gamesByStatus.put(s.name(), gameRepository.countByStatus(s)));

    return DashboardSummaryDto.builder()
        .totalUsers(userRepository.count())
        .totalGames(gameRepository.count())
        .totalTrades(tradeRepository.count())
        .gamesByStatus(gamesByStatus)
        .build();
  }

  public SystemHealthDto getSystemHealth() {
    return systemMetricsService.getSystemHealth();
  }

  public RecentActivityDto getRecentActivity(int hours) {
    return recentActivityService.getRecentActivity(hours);
  }

  public List<AdminUserDto> getAllUsers() {
    return gameCatalogService.getAllUsers();
  }

  public List<Game> getAllGames(String status) {
    return gameCatalogService.getAllGames(status);
  }

  public SystemMetricsDto getSystemMetrics() {
    return systemMetricsService.getSystemMetrics();
  }
}
