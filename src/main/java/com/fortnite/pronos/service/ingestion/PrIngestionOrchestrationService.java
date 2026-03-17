package com.fortnite.pronos.service.ingestion;

import java.io.StringReader;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalTime;
import java.time.Year;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fortnite.pronos.model.IngestionRun;
import com.fortnite.pronos.model.PrRegion;
import com.fortnite.pronos.service.ingestion.PrIngestionService.PrIngestionConfig;
import com.fortnite.pronos.service.ingestion.PrIngestionService.PrIngestionResult;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@ConditionalOnProperty(
    name = "ingestion.pr.scheduled.enabled",
    havingValue = "true",
    matchIfMissing = false)
public class PrIngestionOrchestrationService {

  static final int WINDOW_START_HOUR = 5;
  static final int WINDOW_END_HOUR_EXCLUSIVE = 8;
  static final int SMOKE_MIN_ROWS = 10;

  public static final List<PrRegion> SUPPORTED_REGIONS =
      List.of(
          PrRegion.EU,
          PrRegion.NAC,
          PrRegion.NAW,
          PrRegion.BR,
          PrRegion.ASIA,
          PrRegion.OCE,
          PrRegion.ME,
          PrRegion.GLOBAL);

  private final PrIngestionService ingestionService;
  private final PrRegionCsvSourcePort regionCsvSourcePort;
  private final Clock clock;
  private final CsvCachePort csvCachePort;

  @Autowired
  public PrIngestionOrchestrationService(
      PrIngestionService ingestionService,
      PrRegionCsvSourcePort regionCsvSourcePort,
      CsvCachePort csvCachePort) {
    this(ingestionService, regionCsvSourcePort, Clock.systemUTC(), csvCachePort);
  }

  PrIngestionOrchestrationService(
      PrIngestionService ingestionService,
      PrRegionCsvSourcePort regionCsvSourcePort,
      Clock clock,
      CsvCachePort csvCachePort) {
    this.ingestionService = ingestionService;
    this.regionCsvSourcePort = regionCsvSourcePort;
    this.clock = clock;
    this.csvCachePort = csvCachePort;
  }

  @Scheduled(cron = "${ingestion.pr.scheduled.cron:0 0 5 * * *}")
  public void triggerScheduledIngestion() {
    runScheduledIngestion();
  }

  public MultiRegionIngestionResult runScheduledIngestion() {
    LocalTime now = LocalTime.now(clock);
    if (!isInsideRunWindow(now)) {
      log.debug("PR scheduled ingestion skipped: outside 05h-08h window, now={}", now);
      return new MultiRegionIngestionResult(BatchStatus.SKIPPED, 0, Map.of(), 0L);
    }

    Map<PrRegion, String> regionFailures = new EnumMap<>(PrRegion.class);
    int regionsProcessed = 0;
    long startedAtMillis = clock.millis();

    for (PrRegion region : SUPPORTED_REGIONS) {
      String failure = processRegion(region);
      if (failure == null) {
        regionsProcessed++;
      } else {
        regionFailures.put(region, failure);
      }
    }

    long durationMs = Math.max(0L, Duration.ofMillis(clock.millis() - startedAtMillis).toMillis());
    BatchStatus status = regionFailures.isEmpty() ? BatchStatus.SUCCESS : BatchStatus.PARTIAL;
    return new MultiRegionIngestionResult(
        status, regionsProcessed, Map.copyOf(regionFailures), durationMs);
  }

  private String processRegion(PrRegion region) {
    try {
      Optional<String> csvOpt = regionCsvSourcePort.fetchCsv(region);
      String csv;
      if (csvOpt.isEmpty()) {
        Optional<String> cached = csvCachePort.load(region);
        if (cached.isEmpty()) {
          return "no_data";
        }
        log.info("Using CSV cache fallback for region={}", region);
        csv = cached.get();
      } else {
        csv = csvOpt.get();
        int rowCount = countDataRows(csv);
        if (rowCount < SMOKE_MIN_ROWS) {
          log.warn(
              "Smoke check failed for region={}: {} rows < {}", region, rowCount, SMOKE_MIN_ROWS);
          return "smoke_check_failed";
        }
        csvCachePort.save(region, csv);
      }
      PrIngestionResult result =
          ingestionService.ingest(
              new StringReader(csv),
              new PrIngestionConfig(
                  "SCHEDULED_PR_" + region.name(), Year.now(clock).getValue(), true));
      return result.status() == IngestionRun.Status.SUCCESS ? null : "ingestion_" + result.status();
    } catch (Exception exception) {
      String message = exception.getMessage();
      return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
  }

  private int countDataRows(String csv) {
    String[] lines = csv.split("\n");
    int count = 0;
    for (int i = 1; i < lines.length; i++) {
      if (!lines[i].trim().isEmpty()) {
        count++;
      }
    }
    return count;
  }

  private boolean isInsideRunWindow(LocalTime now) {
    LocalTime windowStart = LocalTime.of(WINDOW_START_HOUR, 0);
    LocalTime windowEnd = LocalTime.of(WINDOW_END_HOUR_EXCLUSIVE, 0);
    return !now.isBefore(windowStart) && now.isBefore(windowEnd);
  }

  public enum BatchStatus {
    SUCCESS,
    PARTIAL,
    SKIPPED
  }

  public record MultiRegionIngestionResult(
      BatchStatus status,
      int regionsProcessed,
      Map<PrRegion, String> regionFailures,
      long durationMs) {}
}
