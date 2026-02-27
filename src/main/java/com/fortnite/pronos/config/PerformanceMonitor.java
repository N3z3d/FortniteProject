package com.fortnite.pronos.config;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;

import org.springframework.scheduling.annotation.Scheduled;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PerformanceMonitor {

  private static final double PERCENT_SCALE = 100.0;
  private static final double CPU_LOAD_MULTIPLIER = 10.0;
  private static final long MEMORY_ALERT_THRESHOLD_PERCENT = 80L;
  private static final long PLAYER_LIST_ALERT_MILLISECONDS = 1_000L;
  private static final long CACHE_HIT_RATIO_ALERT_PERCENT = 70L;
  private static final long BYTES_PER_MEGABYTE = 1_024L * 1_024L;
  private static final long PERFORMANCE_MONITOR_RATE_MILLISECONDS = 30_000L;
  private static final long PERFORMANCE_REPORT_RATE_MILLISECONDS = 300_000L;

  private final MeterRegistry meterRegistry;
  private final Timer playerListTimer;
  private final Timer leaderboardTimer;
  private final Counter playerApiCalls;
  private final Counter cacheHits;
  private final Counter cacheMisses;
  private final MemoryMXBean memoryBean;
  private final OperatingSystemMXBean osBean;

  public PerformanceMonitor(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    this.memoryBean = ManagementFactory.getMemoryMXBean();
    this.osBean = ManagementFactory.getOperatingSystemMXBean();

    this.playerListTimer =
        Timer.builder("api.players.list.duration")
            .description("Temps de reponse pour la liste des joueurs (147 joueurs)")
            .register(meterRegistry);

    this.leaderboardTimer =
        Timer.builder("api.leaderboard.duration")
            .description("Temps de generation du leaderboard")
            .register(meterRegistry);

    this.playerApiCalls =
        Counter.builder("api.players.calls")
            .description("Nombre d'appels a l'API joueurs")
            .register(meterRegistry);

    this.cacheHits =
        Counter.builder("cache.hits")
            .description("Cache hits pour optimiser les performances")
            .register(meterRegistry);

    this.cacheMisses =
        Counter.builder("cache.misses")
            .description("Cache misses - impact performance")
            .register(meterRegistry);
  }

  @PostConstruct
  public void setupGauges() {
    Gauge.builder(
            "jvm.memory.used.percentage",
            this,
            monitor -> {
              long used = memoryBean.getHeapMemoryUsage().getUsed();
              long max = memoryBean.getHeapMemoryUsage().getMax();
              return max > 0 ? (((double) used / max) * PERCENT_SCALE) : 0;
            })
        .description("Pourcentage de memoire utilisee")
        .register(meterRegistry);

    Gauge.builder(
            "system.cpu.usage.percentage",
            this,
            monitor ->
                osBean.getSystemLoadAverage() >= 0
                    ? (osBean.getSystemLoadAverage() * CPU_LOAD_MULTIPLIER)
                    : 0)
        .description("Utilisation CPU systeme")
        .register(meterRegistry);
  }

  public Timer.Sample startPlayerListTimer() {
    playerApiCalls.increment();
    return Timer.start(meterRegistry);
  }

  public void stopPlayerListTimer(Timer.Sample sample) {
    sample.stop(playerListTimer);
  }

  public Timer.Sample startLeaderboardTimer() {
    return Timer.start(meterRegistry);
  }

  public void stopLeaderboardTimer(Timer.Sample sample) {
    sample.stop(leaderboardTimer);
  }

  public void recordCacheHit() {
    cacheHits.increment();
  }

  public void recordCacheMiss() {
    cacheMisses.increment();
  }

  @Scheduled(fixedRate = PERFORMANCE_MONITOR_RATE_MILLISECONDS)
  public void monitorPerformance() {
    long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
    long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
    double memoryPercentage =
        maxMemory > 0 ? (((double) usedMemory / maxMemory) * PERCENT_SCALE) : 0;

    if (memoryPercentage > MEMORY_ALERT_THRESHOLD_PERCENT) {
      log.warn(
          "[ALERT] MEMOIRE: {}% utilise - Risque de degradation avec 147 joueurs!",
          String.format(Locale.ROOT, "%.1f", memoryPercentage));
    }

    double avgPlayerListTime = playerListTimer.mean(TimeUnit.MILLISECONDS);
    if (avgPlayerListTime > PLAYER_LIST_ALERT_MILLISECONDS) {
      log.warn(
          "[ALERT] PERFORMANCE: Temps moyen liste joueurs = {}ms - Trop lent pour 147 joueurs!",
          String.format(Locale.ROOT, "%.0f", avgPlayerListTime));
    }

    double totalCacheOps = cacheHits.count() + cacheMisses.count();
    if (totalCacheOps > 0) {
      double hitRatio = ((cacheHits.count() / totalCacheOps) * PERCENT_SCALE);
      if (hitRatio < CACHE_HIT_RATIO_ALERT_PERCENT) {
        log.warn(
            "[ALERT] CACHE: Hit ratio = {}% - Performance degradee!",
            String.format(Locale.ROOT, "%.1f", hitRatio));
      }
    }
  }

  @Scheduled(fixedRate = PERFORMANCE_REPORT_RATE_MILLISECONDS)
  public void detailedPerformanceReport() {
    log.info("[PERF] === RAPPORT PERFORMANCE 149 JOUEURS ===");

    long usedMb = memoryBean.getHeapMemoryUsage().getUsed() / BYTES_PER_MEGABYTE;
    long maxMb = memoryBean.getHeapMemoryUsage().getMax() / BYTES_PER_MEGABYTE;
    double memoryUsagePercentage = maxMb > 0 ? (((double) usedMb / maxMb) * PERCENT_SCALE) : 0;
    log.info(
        "[MEM] Memoire: {} MB / {} MB ({}%)",
        usedMb, maxMb, String.format(Locale.ROOT, "%.1f", memoryUsagePercentage));

    log.info(
        "[API] Joueurs: {} appels, temps moyen = {}ms",
        (long) playerApiCalls.count(),
        String.format(Locale.ROOT, "%.0f", playerListTimer.mean(TimeUnit.MILLISECONDS)));

    log.info(
        "[LEADERBOARD] temps moyen = {}ms",
        String.format(Locale.ROOT, "%.0f", leaderboardTimer.mean(TimeUnit.MILLISECONDS)));

    double totalCacheOps = cacheHits.count() + cacheMisses.count();
    if (totalCacheOps > 0) {
      double hitRatio = ((cacheHits.count() / totalCacheOps) * PERCENT_SCALE);
      log.info(
          "[CACHE] {}% hit ratio ({} hits, {} misses)",
          String.format(Locale.ROOT, "%.1f", hitRatio),
          (long) cacheHits.count(),
          (long) cacheMisses.count());
    }

    log.info("[PERF] === FIN RAPPORT PERFORMANCE ===");
  }
}
