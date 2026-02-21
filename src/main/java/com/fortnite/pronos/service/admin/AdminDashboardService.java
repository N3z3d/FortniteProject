package com.fortnite.pronos.service.admin;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.stereotype.Service;

import com.zaxxer.hikari.HikariDataSource;

import com.fortnite.pronos.dto.admin.DashboardSummaryDto;
import com.fortnite.pronos.dto.admin.RecentActivityDto;
import com.fortnite.pronos.dto.admin.SystemHealthDto;
import com.fortnite.pronos.dto.admin.SystemMetricsDto;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"java:S1172"})
public class AdminDashboardService {

  private static final int DEFAULT_HOURS = 24;
  private static final int MAX_RECENT_ENTRIES = 10;

  private final com.fortnite.pronos.repository.UserRepository userRepository;
  private final com.fortnite.pronos.repository.GameRepository gameRepository;
  private final com.fortnite.pronos.repository.TradeRepository tradeRepository;
  private final MeterRegistry meterRegistry;
  private final DataSource dataSource;

  public DashboardSummaryDto getDashboardSummary() {
    Map<String, Long> gamesByStatus = new LinkedHashMap<>();
    Arrays.stream(com.fortnite.pronos.model.GameStatus.values())
        .forEach(s -> gamesByStatus.put(s.name(), gameRepository.countByStatus(s)));

    return DashboardSummaryDto.builder()
        .totalUsers(userRepository.count())
        .totalGames(gameRepository.count())
        .totalTrades(tradeRepository.count())
        .gamesByStatus(gamesByStatus)
        .build();
  }

  public SystemHealthDto getSystemHealth() {
    return SystemHealthDto.builder()
        .status("UP")
        .uptimeMillis(ManagementFactory.getRuntimeMXBean().getUptime())
        .databasePool(buildDatabasePoolInfo())
        .disk(buildDiskInfo())
        .build();
  }

  public RecentActivityDto getRecentActivity(int hours) {
    int effectiveHours = hours > 0 ? hours : DEFAULT_HOURS;
    LocalDateTime since = LocalDateTime.now().minusHours(effectiveHours);

    List<com.fortnite.pronos.model.Game> recentGames = gameRepository.findByCreatedAtAfter(since);
    List<com.fortnite.pronos.model.Trade> recentTrades = filterRecentTrades(since);

    return RecentActivityDto.builder()
        .recentGamesCount(recentGames.size())
        .recentTradesCount(recentTrades.size())
        .recentUsersCount(userRepository.count())
        .recentGames(mapGamesToActivity(recentGames))
        .recentTrades(mapTradesToActivity(recentTrades))
        .build();
  }

  public List<com.fortnite.pronos.model.User> getAllUsers() {
    return userRepository.findAll();
  }

  public List<com.fortnite.pronos.model.Game> getAllGames(String status) {
    if (status == null || status.isBlank()) {
      return gameRepository.findAll();
    }
    try {
      com.fortnite.pronos.model.GameStatus gameStatus =
          com.fortnite.pronos.model.GameStatus.valueOf(status);
      return gameRepository.findByStatus(gameStatus);
    } catch (IllegalArgumentException e) {
      log.warn("Invalid game status filter: {}", status);
      return gameRepository.findAll();
    }
  }

  public SystemMetricsDto getSystemMetrics() {
    Runtime runtime = Runtime.getRuntime();
    long heapUsed = runtime.totalMemory() - runtime.freeMemory();
    long heapMax = runtime.maxMemory();

    return SystemMetricsDto.builder()
        .jvm(
            SystemMetricsDto.JvmInfo.builder()
                .heapUsedBytes(heapUsed)
                .heapMaxBytes(heapMax)
                .heapUsagePercent(heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0)
                .threadCount(Thread.activeCount())
                .build())
        .http(buildHttpInfo())
        .build();
  }

  private SystemHealthDto.DatabasePoolInfo buildDatabasePoolInfo() {
    if (dataSource instanceof HikariDataSource hikari) {
      var pool = hikari.getHikariPoolMXBean();
      return SystemHealthDto.DatabasePoolInfo.builder()
          .activeConnections(pool.getActiveConnections())
          .idleConnections(pool.getIdleConnections())
          .totalConnections(pool.getTotalConnections())
          .maxConnections(hikari.getMaximumPoolSize())
          .build();
    }
    return SystemHealthDto.DatabasePoolInfo.builder()
        .activeConnections(-1)
        .idleConnections(-1)
        .totalConnections(-1)
        .maxConnections(-1)
        .build();
  }

  private SystemHealthDto.DiskInfo buildDiskInfo() {
    File root = new File(".");
    long total = root.getTotalSpace();
    long free = root.getFreeSpace();
    double usage = total > 0 ? (double) (total - free) / total * 100 : 0;

    return SystemHealthDto.DiskInfo.builder()
        .totalSpaceBytes(total)
        .freeSpaceBytes(free)
        .usagePercent(Math.round(usage * 100.0) / 100.0)
        .build();
  }

  private SystemMetricsDto.HttpInfo buildHttpInfo() {
    double totalRequests = getMeterValue("http.server.requests", "count");
    double errorCount = getMeterValue("http.server.requests.error", "count");
    double errorRate = totalRequests > 0 ? errorCount / totalRequests * 100 : 0;

    return SystemMetricsDto.HttpInfo.builder()
        .totalRequests(totalRequests)
        .errorRate(errorRate)
        .build();
  }

  private double getMeterValue(String name, String statistic) {
    try {
      var meter = meterRegistry.find(name).timer();
      if (meter != null) {
        return meter.count();
      }
    } catch (Exception e) {
      log.debug("Meter '{}' not available: {}", name, e.getMessage());
    }
    return 0;
  }

  private List<com.fortnite.pronos.model.Trade> filterRecentTrades(LocalDateTime since) {
    return tradeRepository.findAll().stream()
        .filter(t -> t.getProposedAt() != null && t.getProposedAt().isAfter(since))
        .toList();
  }

  private List<RecentActivityDto.ActivityEntry> mapGamesToActivity(
      List<com.fortnite.pronos.model.Game> games) {
    return games.stream()
        .limit(MAX_RECENT_ENTRIES)
        .map(
            g ->
                RecentActivityDto.ActivityEntry.builder()
                    .id(g.getId().toString())
                    .name(g.getName())
                    .status(g.getStatus() != null ? g.getStatus().name() : "UNKNOWN")
                    .createdAt(g.getCreatedAt() != null ? g.getCreatedAt().toString() : "")
                    .build())
        .toList();
  }

  private List<RecentActivityDto.ActivityEntry> mapTradesToActivity(
      List<com.fortnite.pronos.model.Trade> trades) {
    return trades.stream()
        .limit(MAX_RECENT_ENTRIES)
        .map(
            t ->
                RecentActivityDto.ActivityEntry.builder()
                    .id(t.getId().toString())
                    .name("Trade #" + t.getId().toString().substring(0, 8))
                    .status(t.getStatus() != null ? t.getStatus().name() : "UNKNOWN")
                    .createdAt(t.getProposedAt() != null ? t.getProposedAt().toString() : "")
                    .build())
        .toList();
  }
}
