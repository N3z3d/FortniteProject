package com.fortnite.pronos.service.admin;

import java.io.File;
import java.lang.management.ManagementFactory;

import javax.sql.DataSource;

import org.springframework.stereotype.Service;

import com.zaxxer.hikari.HikariDataSource;

import com.fortnite.pronos.dto.admin.SystemHealthDto;
import com.fortnite.pronos.dto.admin.SystemMetricsDto;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"java:S1172"})
class AdminSystemMetricsService {

  private static final double PERCENT_BASE = 100.0;

  private final MeterRegistry meterRegistry;
  private final DataSource dataSource;

  SystemHealthDto getSystemHealth() {
    return SystemHealthDto.builder()
        .status("UP")
        .uptimeMillis(ManagementFactory.getRuntimeMXBean().getUptime())
        .databasePool(buildDatabasePoolInfo())
        .disk(buildDiskInfo())
        .build();
  }

  SystemMetricsDto getSystemMetrics() {
    Runtime runtime = Runtime.getRuntime();
    long heapUsed = runtime.totalMemory() - runtime.freeMemory();
    long heapMax = runtime.maxMemory();

    return SystemMetricsDto.builder()
        .jvm(
            SystemMetricsDto.JvmInfo.builder()
                .heapUsedBytes(heapUsed)
                .heapMaxBytes(heapMax)
                .heapUsagePercent(toPercent(heapUsed, heapMax))
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
    double usage = toPercent(((double) total) - free, total);

    return SystemHealthDto.DiskInfo.builder()
        .totalSpaceBytes(total)
        .freeSpaceBytes(free)
        .usagePercent(roundToTwoDecimals(usage))
        .build();
  }

  private SystemMetricsDto.HttpInfo buildHttpInfo() {
    double totalRequests = getMeterValue("http.server.requests");
    double errorCount = getMeterValue("http.server.requests.error");
    double errorRate = toPercent(errorCount, totalRequests);

    return SystemMetricsDto.HttpInfo.builder()
        .totalRequests(totalRequests)
        .errorRate(errorRate)
        .build();
  }

  private double getMeterValue(String name) {
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

  private double toPercent(double numerator, double denominator) {
    if (denominator <= 0) {
      return 0.0;
    }
    return (numerator / denominator) * PERCENT_BASE;
  }

  private double roundToTwoDecimals(double value) {
    return Math.round(value * PERCENT_BASE) / PERCENT_BASE;
  }
}
